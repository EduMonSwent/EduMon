package com.android.sample.profile

import com.android.sample.data.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Abstraction for loading and updating the user profile (Profile feature only). */
interface ProfileRepository {
  val profile: StateFlow<UserProfile>

  suspend fun updateProfile(newProfile: UserProfile)

  suspend fun increaseStreakIfCorrect()

  suspend fun increaseStudyTimeBy(time: Int)
}

/** Simple in-memory fake repository for the profile feature. */
class FakeProfileRepository(initial: UserProfile = UserProfile()) : ProfileRepository {
  private val _profile = MutableStateFlow(initial)
  override val profile: StateFlow<UserProfile> = _profile

  override suspend fun updateProfile(newProfile: UserProfile) {
    _profile.value = newProfile
  }

  override suspend fun increaseStreakIfCorrect() {
    if (_profile.value.studyStats.totalTimeMin <= 0)
        updateProfile(_profile.value.copy(streak = _profile.value.streak + 1))
  }

  override suspend fun increaseStudyTimeBy(time: Int) {
    updateProfile(
        _profile.value.copy(
            studyStats =
                _profile.value.studyStats.copy(
                    totalTimeMin = _profile.value.studyStats.totalTimeMin + time)))
  }
}
