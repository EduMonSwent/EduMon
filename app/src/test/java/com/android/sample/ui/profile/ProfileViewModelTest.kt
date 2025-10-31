package com.android.sample.ui.profile

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.android.sample.ui.login.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
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
        val vm = ProfileViewModel()
        val before = vm.userProfile.value

        vm.toggleNotifications()
        vm.toggleLocation()
        vm.toggleFocusMode()
        advanceUntilIdle()

        val afterToggles = vm.userProfile.value
        assertEquals(!before.notificationsEnabled, afterToggles.notificationsEnabled)
        assertEquals(!before.locationEnabled, afterToggles.locationEnabled)
        assertEquals(!before.focusModeEnabled, afterToggles.focusModeEnabled)

        val newColor = Color(0xFF10B981)
        vm.setAvatarAccent(newColor)
        advanceUntilIdle()
        assertEquals(newColor.toArgb().toLong(), vm.userProfile.value.avatarAccent)
      }

  // ---- TestRepo conforme à l'interface actuelle (profile + updateProfile) ----
  private class TestRepo(initial: UserProfile) : ProfileRepository {
    private val state = MutableStateFlow(initial)
    override val profile: StateFlow<UserProfile> = state

    override suspend fun updateProfile(newProfile: UserProfile) {
      state.value = newProfile
    }
  }

  @Test
  fun unequip_removes_only_that_slot() =
      runTest(dispatcher) {
        val repo =
            TestRepo(UserProfile(accessories = listOf("head:halo", "torso:scarf", "legs:boots")))
        val vm = ProfileViewModel(repo)

        vm.unequip(AccessorySlot.TORSO)
        advanceUntilIdle()

        val acc = repo.profile.value.accessories
        assertTrue(acc.contains("head:halo"))
        assertTrue(acc.contains("legs:boots"))
        assertFalse(acc.any { it.startsWith("torso:") })
      }

  @Test
  fun equip_legs_cleans_legacy_singular_prefix_and_sets_new() =
      runTest(dispatcher) {
        val repo = TestRepo(UserProfile(accessories = listOf("leg:boots")))
        val vm = ProfileViewModel(repo)

        vm.equip(AccessorySlot.LEGS, "rocket")
        advanceUntilIdle()

        val acc = repo.profile.value.accessories
        assertFalse(acc.any { it.startsWith("leg:") })
        assertTrue(acc.contains("legs:rocket"))
      }

  @Test
  fun accent_variants_trigger_blend_and_boost_paths() =
      runTest(dispatcher) {
        val vm = ProfileViewModel()

        val baseInput = Color(0xFF6699CC)
        vm.setAvatarAccent(baseInput)
        advanceUntilIdle()
        vm.setAccentVariant(AccentVariant.Base)
        advanceUntilIdle()

        val baseEffective = vm.accentEffective.value
        assertEquals(baseInput.toArgb().toLong(), vm.userProfile.value.avatarAccent)
        assertEquals(baseEffective, vm.accentEffective.value) // stable en Base

        vm.setAccentVariant(AccentVariant.Light)
        advanceUntilIdle()
        val light = vm.accentEffective.value
        assertTrue(light.alpha == 1f)

        vm.setAccentVariant(AccentVariant.Vibrant)
        advanceUntilIdle()
        val vibrant = vm.accentEffective.value
        assertTrue(vibrant.alpha == 1f)

        vm.setAccentVariant(AccentVariant.Base)
        advanceUntilIdle()
        assertEquals(baseEffective, vm.accentEffective.value)
      }
}
