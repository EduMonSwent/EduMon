package com.android.sample

// This code has been written partially using A.I (LLM).

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.profile.ProfileRepositoryProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Covers MainActivity setContent/theme wiring and profile sync with Firebase Auth.
 *
 * This version does not mock FirebaseUser; it tests the pure data helper instead.
 */
@RunWith(AndroidJUnit4::class)
class MainActivityTest {

  @get:Rule val compose = createAndroidComposeRule<MainActivity>()

  @Test
  fun launches_and_composition_exists() {
    compose.onRoot().assertExists()
  }

  @Test
  fun syncProfileFromAuthData_updates_profile_with_google_data() = runBlocking {
    val repo = ProfileRepositoryProvider.repository
    val initialProfile = repo.profile.first()

    compose.activity.syncProfileFromAuthData(
        displayName = "Google Test User", email = "googletest@gmail.com")
    compose.waitForIdle()

    val profile = repo.profile.first()
    assertEquals("Google Test User", profile.name)
    assertEquals("googletest@gmail.com", profile.email)
    // Other fields unaffected
    assertEquals(initialProfile.level, profile.level)
    assertEquals(initialProfile.points, profile.points)
  }

  @Test
  fun syncProfileFromAuthData_handles_null_displayName() = runBlocking {
    val repo = ProfileRepositoryProvider.repository
    val initialProfile = repo.profile.first()

    compose.activity.syncProfileFromAuthData(displayName = null, email = "user@gmail.com")
    compose.waitForIdle()

    val profile = repo.profile.first()
    // Name falls back to existing profile name
    assertEquals(initialProfile.name, profile.name)
    assertEquals("user@gmail.com", profile.email)
  }

  @Test
  fun syncProfileFromAuthData_handles_null_email() = runBlocking {
    val repo = ProfileRepositoryProvider.repository
    val initialProfile = repo.profile.first()

    compose.activity.syncProfileFromAuthData(displayName = "Test User", email = null)
    compose.waitForIdle()

    val profile = repo.profile.first()
    assertEquals("Test User", profile.name)
    // Email falls back to existing value
    assertEquals(initialProfile.email, profile.email)
  }

  @Test
  fun syncProfileFromAuthData_preserves_other_profile_data() = runBlocking {
    val repo = ProfileRepositoryProvider.repository
    val initialProfile = repo.profile.first()

    compose.activity.syncProfileFromAuthData(displayName = "New User Name", email = "new@gmail.com")
    compose.waitForIdle()

    val updatedProfile = repo.profile.first()
    assertEquals("New User Name", updatedProfile.name)
    assertEquals("new@gmail.com", updatedProfile.email)
    assertEquals(initialProfile.level, updatedProfile.level)
    assertEquals(initialProfile.points, updatedProfile.points)
  }
}
