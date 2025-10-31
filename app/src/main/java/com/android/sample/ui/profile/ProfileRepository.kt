package com.android.sample.ui.profile

import com.android.sample.ui.login.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Abstraction for loading and updating the user profile. */
interface ProfileRepository {
  val profile: StateFlow<UserProfile>

  suspend fun updateProfile(newProfile: UserProfile)
}

/** Simple in-memory fake repository for the profile feature. */
class FakeProfileRepository(initial: UserProfile = UserProfile()) : ProfileRepository {
  private val _profile = MutableStateFlow(initial)
  override val profile: StateFlow<UserProfile> = _profile

  override suspend fun updateProfile(newProfile: UserProfile) {
    _profile.value = newProfile
  }
}
