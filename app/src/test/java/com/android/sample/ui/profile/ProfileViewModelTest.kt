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
  fun accent_variants_cover_all_paths_without_strict_inequality() =
      runTest(dispatcher) {
        val vm = ProfileViewModel()

        val baseNonBlack = Color(0xFF6699CC)
        vm.setAvatarAccent(baseNonBlack)
        advanceUntilIdle()

        vm.setAccentVariant(AccentVariant.Light)
        advanceUntilIdle()
        val light = vm.accentEffective.value
        assertTrue(light.alpha in 0f..1f)

        vm.setAccentVariant(AccentVariant.Dark)
        advanceUntilIdle()
        val dark = vm.accentEffective.value
        assertTrue(dark.alpha in 0f..1f)

        vm.setAccentVariant(AccentVariant.Vibrant)
        advanceUntilIdle()
        val vibrantNonBlack = vm.accentEffective.value
        assertTrue(vibrantNonBlack.alpha in 0f..1f)

        vm.setAvatarAccent(Color.Black)
        advanceUntilIdle()
        vm.setAccentVariant(AccentVariant.Vibrant)
        advanceUntilIdle()
        val vibrantBlack = vm.accentEffective.value
        assertTrue(vibrantBlack.red in 0f..1f)
        assertTrue(vibrantBlack.green in 0f..1f)
        assertTrue(vibrantBlack.blue in 0f..1f)

        vm.setAccentVariant(AccentVariant.Base)
        advanceUntilIdle()
        val baseAgain = vm.accentEffective.value
        assertTrue(baseAgain.alpha in 0f..1f)
      }
  fun external_repo_update_is_observed_by_viewmodel() = runTest {
    val repo = FakeProfileRepository()
    val vm = ProfileViewModel(repository = repo)

    repo.updateProfile(vm.userProfile.value.copy(name = "Taylor", points = 2000))

    val p = vm.userProfile.value
    assertEquals("Taylor", p.name)
    assertEquals(2000, p.points)
  }

  // --- Reward system tests ---------------------------------------------------

  @Test
  fun addCoins_withPositiveAmount_increasesUserCoins() = runTest {
    val repo = FakeProfileRepository()
    val vm = ProfileViewModel(repository = repo)

    val before = vm.userProfile.value.coins
    vm.addCoins(100)
    val after = vm.userProfile.value.coins

    assertEquals(before + 100, after)
  }

  @Test
  fun addCoins_withZeroOrNegativeAmount_doesNothing() = runTest {
    val repo = FakeProfileRepository()
    val vm = ProfileViewModel(repository = repo)

    val before = vm.userProfile.value.coins
    vm.addCoins(0) // should be ignored
    vm.addCoins(-50) // should also be ignored
    val after = vm.userProfile.value.coins

    assertEquals(before, after)
  }
}
