// StudyTogetherViewModel.kt
package com.android.sample.ui.location

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
  liveLocation: Boolean = true // simpler: default to true for maps; feel free to flip
) : ViewModel() {

  private val auth: FirebaseAuth = FirebaseAuth.getInstance()
  private val isSignedIn get() = auth.currentUser != null

  private val _uiState = MutableStateFlow(StudyTogetherUiState(userPosition = DEFAULT_LOCATION))
  val uiState: StateFlow<StudyTogetherUiState> = _uiState.asStateFlow()

  private var currentMode: FriendMode = initialMode
  private val displayName: String =
    auth.currentUser?.displayName?.takeIf { !it.isNullOrBlank() } ?: "Me"

  private var profileEnsured = false
  private var lastDeviceLatLng: LatLng? = null

  // Throttle presence writes a bit
  private var lastSentAtMs: Long = 0L
  private var lastSentLatLng: LatLng? = null
  private val minSendIntervalMs = 10_000L
  private val minMoveMeters = 25f

  init {
    // Live friends (no changes needed in the screen)
    viewModelScope.launch {
      friendRepository.friendsFlow.collect { list ->
        _uiState.update { it.copy(friends = list) }
      }
    }
  }

  /** UI passes the latest device coordinates. Show them immediately; write presence (throttled) if signed in. */
  fun consumeLocation(lat: Double, lon: Double) {
    val device = LatLng(lat, lon)
    lastDeviceLatLng = device

    // Always show live device position on the map (simpler UX)
    _uiState.update { it.copy(userPosition = device) }

    if (!isSignedIn) return

    viewModelScope.launch {
      try {
        if (!profileEnsured) {
          ensureMyProfile(displayName, currentMode, device.latitude, device.longitude)
          profileEnsured = true
        }
        if (shouldSendPresence(device)) {
          updateMyPresence(displayName, currentMode, device.latitude, device.longitude)
          lastSentLatLng = device
          lastSentAtMs = System.currentTimeMillis()
        }
      } catch (e: Throwable) {
        _uiState.update {
          it.copy(errorMessage = "Presence update failed: ${e.message ?: "unknown"}")
        }
      }
    }
  }

  /** Mode change → update presence with the last known device location if signed in. */
  fun setMode(mode: FriendMode) {
    currentMode = mode
    if (!isSignedIn) return
    val loc = lastDeviceLatLng ?: return
    viewModelScope.launch {
      try {
        updateMyPresence(displayName, currentMode, loc.latitude, loc.longitude)
        lastSentLatLng = loc
        lastSentAtMs = System.currentTimeMillis()
      } catch (_: Throwable) { /* ignore */ }
    }
  }

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
        val msg = when (e) {
          is IllegalArgumentException -> e.message ?: "Invalid input."
          else -> "Couldn't add friend: ${e.localizedMessage ?: "Error"}"
        }
        _uiState.update { it.copy(errorMessage = msg) }
      }
    }
  }

  fun consumeError() {
    _uiState.update { it.copy(errorMessage = null) }
  }

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
}
