// StudyTogetherViewModel.kt
package com.android.sample.ui.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.sample.R
import com.android.sample.data.notifications.NotificationUtils
import com.android.sample.repos_providors.AppRepositories
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class StudyTogetherViewModel(
    private val friendRepository: FriendRepository = AppRepositories.friendRepository,
    initialMode: FriendMode = FriendMode.STUDY,
    /**
     * Initial live flag; even if signed in, we can start with default EPFL until user enables live.
     */
    liveLocation: Boolean = true,
    // NEW: allow injecting emulator auth in tests; default to production auth
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

  // --- Black-or-white: freeze "online vs local-only" at startup
  private val auth: FirebaseAuth = firebaseAuth
  private val presenceEnabled: Boolean = auth.currentUser != null

  // --- UI state (no flows for location; we set it directly)
  private val _uiState = MutableStateFlow(StudyTogetherUiState(userPosition = DEFAULT_LOCATION))
  val uiState: StateFlow<StudyTogetherUiState> = _uiState.asStateFlow()

  // Live flag (simple boolean; not a flow)
  private var liveLocationEnabled: Boolean = liveLocation

  private val isSignedIn: Boolean
    get() = auth.currentUser != null

  // Presence bits (used only if presenceEnabled && live rules allow)
  private var currentMode: FriendMode = initialMode
  private val displayName: String =
      auth.currentUser?.displayName?.takeIf { !it.isNullOrBlank() } ?: "Me"
  private var profileEnsured = false

  // Last device location (from UI)
  private var lastDeviceLatLng: LatLng? = null

  // Throttling for presence writes
  private var lastSentAtMs: Long = 0L
  private var lastSentLatLng: LatLng? = null
  private val minSendIntervalMs = 10_000L // Update Firebase every 10 seconds (was 20s)
  private val minMoveMeters = 10f // Or when moved 10+ meters (was 25m)

  // Foreground campus entry notification support (Option A)
  private var appContext: Context? = null
  private var wasOnCampus: Boolean = false
  private var lastCampusNotifyMs: Long = 0L
  private val campusNotifyCooldownMs = 60_000L // avoid spamming if jitter

  init {
    // Live friends (no changes needed in the screen)
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

    // Always show live device position on the map (simpler UX)
    _uiState.update {
      it.copy(
          userPosition = userLocation,
          isOnCampus = onCampus,
          isLocationInitialized = true // Mark location as initialized
          )
    }

    if (!isSignedIn) return

    viewModelScope.launch {
      try {
        // Ensure once with the chosen location (if not ensured yet)
        if (!profileEnsured) {
          ensureMyProfile(displayName, currentMode, userLocation.latitude, userLocation.longitude)
          profileEnsured = true
        }
        if (shouldSendPresence(device)) {
          updateMyPresence(displayName, currentMode, userLocation.latitude, userLocation.longitude)
          lastSentLatLng = userLocation
          lastSentAtMs = System.currentTimeMillis()
        }
      } catch (e: Throwable) {
        _uiState.update {
          it.copy(errorMessage = "Presence update failed: ${e.message ?: "unknown"}")
        }
      }
    }

    // Campus entry detection
    try {
      val ctx = appContext
      if (ctx != null && onCampus && !wasOnCampus) {
        val prefs = ctx.getSharedPreferences("notifications", Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean("campus_entry_enabled", false)
        val nowMs = System.currentTimeMillis()
        if (enabled && (nowMs - lastCampusNotifyMs) >= campusNotifyCooldownMs) {
          // Permission check for Android 13+
          if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
              ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) ==
                  PackageManager.PERMISSION_GRANTED) {
            NotificationUtils.ensureChannel(ctx)
            val deepLink = ctx.getString(R.string.deep_link_format, "campus")
            val intent =
                android.content
                    .Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(deepLink))
                    .apply { `package` = ctx.packageName }
            val pi =
                android.app.PendingIntent.getActivity(
                    ctx,
                    0,
                    intent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or
                        android.app.PendingIntent.FLAG_IMMUTABLE)
            val n =
                NotificationCompat.Builder(ctx, NotificationUtils.CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(ctx.getString(R.string.campus_entry_title))
                    .setContentText(ctx.getString(R.string.campus_entry_text))
                    .setContentIntent(pi)
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .build()
            NotificationManagerCompat.from(ctx).notify(9101, n)
            lastCampusNotifyMs = nowMs
          }
        }
      }
      wasOnCampus = onCampus
    } catch (_: Exception) {
      /* ignore foreground notification failures */
    }
  }

  /** Change user mode; if signed in we send presence with the chosen policy’s location. */
  fun setMode(mode: FriendMode) {
    currentMode = mode
    if (!presenceEnabled) return
    val chosen = if (liveLocationEnabled) (lastDeviceLatLng ?: return) else DEFAULT_LOCATION
    viewModelScope.launch {
      try {
        updateMyPresence(displayName, currentMode, chosen.latitude, chosen.longitude, auth = auth)
        lastSentLatLng = chosen
        lastSentAtMs = System.currentTimeMillis()
      } catch (_: Throwable) {
        /* ignore */
      }
    }
  }

  // --- UI helpers unchanged below ---

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

                // Normalize any "already friend(s)" error to the exact text
                // the test expects: "You're already friends."
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

  fun attachContext(ctx: Context) {
    // store application context (safe reference)
    appContext = ctx.applicationContext
  }
}
