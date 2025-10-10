package com.android.sample.ui.profile

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UserProfile(
    val name: String = "Alex",
    val email: String = "alex@university.edu",
    val level: Int = 5,
    val points: Int = 1250,
    val streak: Int = 7,
    val studyTimeToday: Int = 45,
    val dailyGoal: Int = 180,
    val notificationsEnabled: Boolean = true,
    val locationEnabled: Boolean = true,
    val focusModeEnabled: Boolean = false
)

class ProfileViewModel : ViewModel() {

  private val _userProfile = MutableStateFlow(UserProfile())
  val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

  fun toggleNotifications() {
    _userProfile.value =
        _userProfile.value.copy(notificationsEnabled = !_userProfile.value.notificationsEnabled)
  }

  fun toggleLocation() {
    _userProfile.value =
        _userProfile.value.copy(locationEnabled = !_userProfile.value.locationEnabled)
  }

  fun toggleFocusMode() {
    _userProfile.value =
        _userProfile.value.copy(focusModeEnabled = !_userProfile.value.focusModeEnabled)
  }
}
