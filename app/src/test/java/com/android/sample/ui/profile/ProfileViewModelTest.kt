package com.android.sample.ui.profile

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ProfileViewModelTest {

  private val dispatcher = StandardTestDispatcher()

  @Before
  fun setUp() {
    Dispatchers.setMain(dispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun toggles_and_accent_update_profile() =
      runTest(dispatcher) {
        // VM sans factory -> utilise FakeProfileRepository interne
        val vm = ProfileViewModel()
        val before = vm.userProfile.value

        // -- toggles
        vm.toggleNotifications()
        vm.toggleLocation()
        vm.toggleFocusMode()
        advanceUntilIdle()

        val afterToggles = vm.userProfile.value
        assertEquals(!before.notificationsEnabled, afterToggles.notificationsEnabled)
        assertEquals(!before.locationEnabled, afterToggles.locationEnabled)
        assertEquals(!before.focusModeEnabled, afterToggles.focusModeEnabled)

        // -- accent
        val newColor = Color(0xFF10B981) // mint
        vm.setAvatarAccent(newColor)
        advanceUntilIdle()

        assertEquals(newColor.toArgb().toLong(), vm.userProfile.value.avatarAccent)
      }
}
