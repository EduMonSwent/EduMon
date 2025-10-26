package com.android.sample.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class UserProfile(
    val name: String = DEFAULT_NAME,
    val email: String = DEFAULT_EMAIL,
    val level: Int = DEFAULT_LEVEL,
    val points: Int = DEFAULT_POINTS,
    val coins: Int = DEFAULT_COINS,
    val streak: Int = DEFAULT_STREAK,
    val studyTimeToday: Int = DEFAULT_STUDY_TIME,
    val dailyGoal: Int = DEFAULT_DAILY_GOAL,
    val notificationsEnabled: Boolean = DEFAULT_NOTIFICATIONS,
    val locationEnabled: Boolean = DEFAULT_LOCATION,
    val focusModeEnabled: Boolean = DEFAULT_FOCUS_MODE
) {
  companion object {
    const val DEFAULT_NAME = "Alex"
    const val DEFAULT_EMAIL = "alex@university.edu"
    const val DEFAULT_LEVEL = 5
    const val DEFAULT_POINTS = 1250
    const val DEFAULT_COINS = 0
    const val DEFAULT_STREAK = 7
    const val DEFAULT_STUDY_TIME = 45
    const val DEFAULT_DAILY_GOAL = 180
    const val DEFAULT_NOTIFICATIONS = true
    const val DEFAULT_LOCATION = true
    const val DEFAULT_FOCUS_MODE = false
  }
}

class ProfileViewModel(private val repository: ProfileRepository = FakeProfileRepository()) :
    ViewModel() {

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

  fun addCoins(amount: Int) {
    if (amount <= 0) return
    val current = userProfile.value
    viewModelScope.launch {
      val updated = current.copy(coins = current.coins + amount)
      repository.updateProfile(updated)
    }
  }
}
