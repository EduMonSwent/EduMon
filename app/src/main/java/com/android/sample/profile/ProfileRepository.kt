package com.android.sample.profile

// This code has been written partially using A.I (LLM).

import com.android.sample.data.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Abstraction for loading and updating the user profile (visual/customization only). */
interface ProfileRepository {
  val profile: StateFlow<UserProfile>
  val isLoaded: StateFlow<Boolean>

  suspend fun updateProfile(newProfile: UserProfile)
}

/** Simple in-memory fake repository for the profile feature. */
class FakeProfileRepository(initial: UserProfile = UserProfile()) : ProfileRepository {
  private val _profile = MutableStateFlow(initial)
  override val profile: StateFlow<UserProfile> = _profile

  // Fake is always "loaded" immediately
  private val _isLoaded = MutableStateFlow(true)
  override val isLoaded: StateFlow<Boolean> = _isLoaded

  override suspend fun updateProfile(newProfile: UserProfile) {
    _profile.value = newProfile
  }
}
