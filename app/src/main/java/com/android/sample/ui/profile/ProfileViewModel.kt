package com.android.sample.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

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

class ProfileViewModel(private val repository: ProfileRepository = FakeProfileRepository()) :
    ViewModel() {

  // Expose the repository as the single source of truth
  val userProfile: StateFlow<UserProfile> = repository.profile

  fun toggleNotifications() {
    val current = userProfile.value
    viewModelScope.launch {
      repository.updateProfile(current.copy(notificationsEnabled = !current.notificationsEnabled))
    }
  }

  fun toggleLocation() {
    val current = userProfile.value
    viewModelScope.launch {
      repository.updateProfile(current.copy(locationEnabled = !current.locationEnabled))
    }
  }

  fun toggleFocusMode() {
    val current = userProfile.value
    viewModelScope.launch {
      repository.updateProfile(current.copy(focusModeEnabled = !current.focusModeEnabled))
    }
  }
}
