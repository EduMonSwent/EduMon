package com.android.sample.ui.location

// This code has been written partially using A.I (LLM).

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.sample.repos_providors.AppRepositories
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// Parts of this code were written with ChatGPT assistance

class StudyTogetherViewModel(
    private val friendRepository: FriendRepository = AppRepositories.friendRepository,
    initialMode: FriendMode = FriendMode.STUDY,
    /**
     * Initial live flag; even if signed in, we can start with default EPFL until user enables live.
     */
    liveLocation: Boolean = true,
    // Allow injecting emulator auth in tests; default to production auth.
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

  // Freeze "online vs local-only" at startup.
  private val auth: FirebaseAuth = firebaseAuth
  private val presenceEnabled: Boolean = auth.currentUser != null

  // UI state (location stored directly in state).
  private val _uiState = MutableStateFlow(StudyTogetherUiState(userPosition = DEFAULT_LOCATION))
  val uiState: StateFlow<StudyTogetherUiState> = _uiState.asStateFlow()

  // Live flag (simple boolean; not a flow).
  private var liveLocationEnabled: Boolean = liveLocation

  private val isSignedIn: Boolean
    get() = auth.currentUser != null

  // Presence bits (used only if presenceEnabled && live rules allow).
  private var currentMode: FriendMode = initialMode
  private val displayName: String =
      auth.currentUser?.displayName?.takeIf { !it.isNullOrBlank() } ?: "Me"
  private var profileEnsured = false

  // Last device location (from UI).
  private var lastDeviceLatLng: LatLng? = null

  // Throttling for presence writes.
  private var lastSentAtMs: Long = 0L
  private var lastSentLatLng: LatLng? = null
  // These were in the original code; keeping as-is to avoid changing behavior.
  private val minSendIntervalMs = 10_000L
  private val minMoveMeters = 10f

  // Foreground campus entry notification support (Option A)
  // Campus-entry notifications are handled by CampusEntryPollWorker in the background.
  // The ViewModel no longer posts notifications directly and doesn't need a Context.

  init {
    // Live friends.
    viewModelScope.launch {
      friendRepository.friendsFlow.collect { list -> _uiState.update { it.copy(friends = list) } }
    }
  }

  /**
   * UI gives current device coords (after permission). We decide what to show and whether to write
   * presence.
   */
  fun consumeLocation(lat: Double, lon: Double) {
    val device = LatLng(lat, lon)

    val userLocation = if (liveLocationEnabled) device else DEFAULT_LOCATION

    lastDeviceLatLng = userLocation

    val onCampus = isOnEpflCampus(userLocation)

    // Always show location on the map.
    _uiState.update {
      it.copy(userPosition = userLocation, isOnCampus = onCampus, isLocationInitialized = true)
    }

    if (!isSignedIn) return

    viewModelScope.launch {
      try {
        // Ensure profile once.
        if (!profileEnsured) {
          ensureMyProfile(
              name = displayName,
              mode = currentMode,
              lat = userLocation.latitude,
              lon = userLocation.longitude,
              auth = auth)
          profileEnsured = true
        }
        if (shouldSendPresence(device)) {
          updateMyPresence(
              name = displayName,
              mode = currentMode,
              lat = userLocation.latitude,
              lon = userLocation.longitude,
              auth = auth)
          lastSentLatLng = userLocation
          lastSentAtMs = System.currentTimeMillis()
        }
      } catch (e: Throwable) {
        _uiState.update {
          it.copy(errorMessage = "Presence update failed: ${e.message ?: "unknown"}")
        }
      }
    }

    // Campus entry notification no longer handled here. Background polling via
    // CampusEntryPollWorker posts notifications even when the app is closed.
  }

  /** Change user mode; if signed in we send presence with the chosen policy’s location. */
  fun setMode(mode: FriendMode) {
    currentMode = mode
    if (!presenceEnabled) return
    val chosen = if (liveLocationEnabled) (lastDeviceLatLng ?: return) else DEFAULT_LOCATION
    viewModelScope.launch {
      try {
        updateMyPresence(
            name = displayName,
            mode = currentMode,
            lat = chosen.latitude,
            lon = chosen.longitude,
            auth = auth)
        lastSentLatLng = chosen
        lastSentAtMs = System.currentTimeMillis()
      } catch (_: Throwable) {
        // Ignore presence errors for mode change in UI.
      }
    }
  }

  // --- UI helpers ---

  fun selectFriend(friend: FriendStatus) {
    _uiState.update { it.copy(selectedFriend = friend, isUserSelected = false) }
  }

  fun selectUser() {
    _uiState.update { it.copy(isUserSelected = true, selectedFriend = null) }
  }

  fun clearSelection() {
    _uiState.update { it.copy(isUserSelected = false, selectedFriend = null) }
  }

  fun addFriendByUid(uid: String) {
    viewModelScope.launch {
      try {
        friendRepository.addFriendByUid(uid.trim())
        _uiState.update { it.copy(errorMessage = null) }
      } catch (e: Throwable) {
        val msg =
            when (e) {
              is IllegalArgumentException -> {
                val raw = e.message.orEmpty()
                when {
                  raw.contains("already friend", ignoreCase = true) -> "You're already friends."
                  raw.isBlank() -> "Invalid input."
                  else -> raw
                }
              }
              else -> "Couldn't add friend: ${e.localizedMessage ?: "Error"}"
            }
        _uiState.update { it.copy(errorMessage = msg) }
      }
    }
  }

  fun consumeError() {
    _uiState.update { it.copy(errorMessage = null) }
  }

  // --- presence throttle helpers ---

  private fun shouldSendPresence(curr: LatLng): Boolean {
    val now = System.currentTimeMillis()
    val movedEnough = lastSentLatLng?.let { distanceMeters(it, curr) >= minMoveMeters } ?: true
    val longEnough = (now - lastSentAtMs) >= minSendIntervalMs
    return movedEnough || longEnough
  }

  private fun distanceMeters(a: LatLng, b: LatLng): Float {
    val out = FloatArray(1)
    android.location.Location.distanceBetween(a.latitude, a.longitude, b.latitude, b.longitude, out)
    return out[0]
  }

  // Rough bounding box around the EPFL Lausanne campus.
  private fun isOnEpflCampus(position: LatLng): Boolean {
    val lat = position.latitude
    val lng = position.longitude

    val minLat = 46.515
    val maxLat = 46.525
    val minLng = 6.555
    val maxLng = 6.575

    return lat in minLat..maxLat && lng in minLng..maxLng
  }
}

/* -------------------- presence helpers -------------------- */

/**
 * Ensure the current user has a profile document in /profiles/{uid}.
 *
 * Uses the FirebaseApp attached to the provided FirebaseAuth.
 */
private suspend fun ensureMyProfile(
    name: String,
    mode: FriendMode,
    lat: Double,
    lon: Double,
    auth: FirebaseAuth
) {
  val uid = auth.currentUser?.uid ?: return
  val app = auth.app
  val db = FirebaseFirestore.getInstance(app)

  val docRef = db.collection("profiles").document(uid)
  val snap = docRef.get().await()
  if (!snap.exists()) {
    val payload =
        mapOf(
            "name" to name,
            "mode" to mode.name, // Tests expect "BREAK", "STUDY", etc.
            "location" to GeoPoint(lat, lon))
    docRef.set(payload).await()
  }
}

/**
 * Update presence fields (name, mode, location) in /profiles/{uid}.
 *
 * Uses the FirebaseApp attached to the provided FirebaseAuth.
 */
private suspend fun updateMyPresence(
    name: String,
    mode: FriendMode,
    lat: Double,
    lon: Double,
    auth: FirebaseAuth
) {
  val uid = auth.currentUser?.uid ?: return
  val app = auth.app
  val db = FirebaseFirestore.getInstance(app)

  val payload = mapOf("name" to name, "mode" to mode.name, "location" to GeoPoint(lat, lon))

  db.collection("profiles").document(uid).set(payload, SetOptions.merge()).await()
}
