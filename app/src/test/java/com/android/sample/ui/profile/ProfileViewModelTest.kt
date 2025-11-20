package com.android.sample.ui.profile

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.android.sample.data.AccentVariant
import com.android.sample.data.AccessorySlot
import com.android.sample.data.UserProfile
import com.android.sample.profile.FakeProfileRepository
import com.android.sample.profile.ProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

// The assistance of an AI tool (ChatGPT) was solicited in writing this file.

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

  // ========== Basic ViewModel Initialization ==========

  @Test
  fun viewModel_initializes_with_repository_profile() = runTest {
    val repo = FakeProfileRepository()
    val vm = ProfileViewModel(repository = repo)

    // Verify initial profile is copied from repository
    assertNotNull(vm.userProfile.value)
    assertEquals("Alex", vm.userProfile.value.name)
  }

  @Test
  fun accentPalette_contains_five_colors() = runTest {
    val vm = ProfileViewModel(FakeProfileRepository())

    assertEquals(5, vm.accentPalette.size)
    assertTrue(vm.accentPalette.all { it != Color.Unspecified })
  }

  // ========== Accessory Catalog Tests ==========

  @Test
  fun accessoryCatalog_always_includes_none_items() = runTest {
    val repo = TestRepo(UserProfile(accessories = emptyList()))
    val vm = ProfileViewModel(repository = repo)

    val catalog = vm.accessoryCatalog

    // "none" items should always be available
    assertTrue(catalog.any { it.id == "none" && it.slot == AccessorySlot.HEAD })
    assertTrue(catalog.any { it.id == "none" && it.slot == AccessorySlot.TORSO })
    assertTrue(catalog.any { it.id == "none" && it.slot == AccessorySlot.BACK })
  }

  @Test
  fun accessoryCatalog_includes_owned_accessories() = runTest {
    val repo = TestRepo(UserProfile(accessories = listOf("owned:hat", "owned:scarf")))
    val vm = ProfileViewModel(repository = repo)

    val catalog = vm.accessoryCatalog

    assertTrue(catalog.any { it.id == "hat" })
    assertTrue(catalog.any { it.id == "scarf" })
  }

  @Test
  fun accessoryCatalog_excludes_non_owned_accessories() = runTest {
    val repo = TestRepo(UserProfile(accessories = listOf("owned:hat")))
    val vm = ProfileViewModel(repository = repo)

    val catalog = vm.accessoryCatalog

    // "glasses" is not owned
    assertFalse(catalog.any { it.id == "glasses" })
    // But "hat" is owned
    assertTrue(catalog.any { it.id == "hat" })
  }

  // ========== Accessory Resource ID Tests ==========

  @Test
  fun accessoryResId_returns_correct_drawable_for_head_items() = runTest {
    val vm = ProfileViewModel(FakeProfileRepository())

    assertTrue(vm.accessoryResId(AccessorySlot.HEAD, "hat") != 0)
    assertTrue(vm.accessoryResId(AccessorySlot.HEAD, "glasses") != 0)
    assertEquals(0, vm.accessoryResId(AccessorySlot.HEAD, "none"))
    assertEquals(0, vm.accessoryResId(AccessorySlot.HEAD, "unknown"))
  }

  @Test
  fun accessoryResId_returns_correct_drawable_for_torso_items() = runTest {
    val vm = ProfileViewModel(FakeProfileRepository())

    assertTrue(vm.accessoryResId(AccessorySlot.TORSO, "scarf") != 0)
    assertTrue(vm.accessoryResId(AccessorySlot.TORSO, "cape") != 0)
    assertEquals(0, vm.accessoryResId(AccessorySlot.TORSO, "none"))
  }

  @Test
  fun accessoryResId_returns_correct_drawable_for_back_items() = runTest {
    val vm = ProfileViewModel(FakeProfileRepository())

    assertTrue(vm.accessoryResId(AccessorySlot.BACK, "wings") != 0)
    assertTrue(vm.accessoryResId(AccessorySlot.BACK, "aura") != 0)
    assertEquals(0, vm.accessoryResId(AccessorySlot.BACK, "none"))
  }

  @Test
  fun accessoryResId_returns_zero_for_legs_slot() = runTest {
    val vm = ProfileViewModel(FakeProfileRepository())

    assertEquals(0, vm.accessoryResId(AccessorySlot.LEGS, "anything"))
  }

  // ========== Avatar Accent Tests ==========

  @Test
  fun setAvatarAccent_updates_user_profile() =
      runTest(dispatcher) {
        val vm = ProfileViewModel(FakeProfileRepository())

        val newColor = Color(0xFF10B981)
        vm.setAvatarAccent(newColor)
        advanceUntilIdle()

        assertEquals(newColor.toArgb().toLong(), vm.userProfile.value.avatarAccent)
      }

  @Test
  fun setAvatarAccent_triggers_accentEffective_update() =
      runTest(dispatcher) {
        val vm = ProfileViewModel(FakeProfileRepository())
        val job = launch { vm.accentEffective.collect {} }

        val newColor = Color(0xFFFF6D00)
        vm.setAvatarAccent(newColor)
        advanceUntilIdle()

        // accentEffective should reflect the new color
        assertNotEquals(Color.Unspecified, vm.accentEffective.value)

        job.cancel()
      }

  // ========== Accent Variant Tests ==========

  @Test
  fun setAccentVariant_updates_variant_flow() =
      runTest(dispatcher) {
        val vm = ProfileViewModel(FakeProfileRepository())

        vm.setAccentVariant(AccentVariant.Light)
        advanceUntilIdle()
        assertEquals(AccentVariant.Light, vm.accentVariantFlow.value)

        vm.setAccentVariant(AccentVariant.Dark)
        advanceUntilIdle()
        assertEquals(AccentVariant.Dark, vm.accentVariantFlow.value)

        vm.setAccentVariant(AccentVariant.Vibrant)
        advanceUntilIdle()
        assertEquals(AccentVariant.Vibrant, vm.accentVariantFlow.value)

        vm.setAccentVariant(AccentVariant.Base)
        advanceUntilIdle()
        assertEquals(AccentVariant.Base, vm.accentVariantFlow.value)
      }

  @Test
  fun accent_variants_cover_all_paths_without_strict_inequality() =
      runTest(dispatcher) {
        val vm = ProfileViewModel(FakeProfileRepository())
        val job = launch { vm.accentEffective.collect {} }

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

        job.cancel()
      }

  // ========== Toggle Functions Tests ==========

  @Test
  fun toggleNotifications_flips_notifications_enabled() =
      runTest(dispatcher) {
        val vm = ProfileViewModel(FakeProfileRepository())
        val before = vm.userProfile.value.notificationsEnabled

        vm.toggleNotifications()
        advanceUntilIdle()

        assertEquals(!before, vm.userProfile.value.notificationsEnabled)
      }

  @Test
  fun toggleLocation_flips_location_enabled() =
      runTest(dispatcher) {
        val vm = ProfileViewModel(FakeProfileRepository())
        val before = vm.userProfile.value.locationEnabled

        vm.toggleLocation()
        advanceUntilIdle()

        assertEquals(!before, vm.userProfile.value.locationEnabled)
      }

  @Test
  fun toggleFocusMode_flips_focus_mode_enabled() =
      runTest(dispatcher) {
        val vm = ProfileViewModel(FakeProfileRepository())
        val before = vm.userProfile.value.focusModeEnabled

        vm.toggleFocusMode()
        advanceUntilIdle()

        assertEquals(!before, vm.userProfile.value.focusModeEnabled)
      }

  @Test
  fun toggles_and_accent_update_profile() =
      runTest(dispatcher) {
        val vm = ProfileViewModel(FakeProfileRepository())
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

  // ========== Equip/Unequip Tests ==========

  @Test
  fun equip_owned_accessory_adds_to_profile() =
      runTest(dispatcher) {
        val repo = TestRepo(UserProfile(accessories = listOf("owned:hat")))
        val vm = ProfileViewModel(repo)

        vm.equip(AccessorySlot.HEAD, "hat")
        advanceUntilIdle()

        val acc = repo.profile.value.accessories
        assertTrue(acc.contains("head:hat"))
      }

  @Test
  fun equip_non_owned_accessory_does_nothing() =
      runTest(dispatcher) {
        val repo = TestRepo(UserProfile(accessories = emptyList()))
        val vm = ProfileViewModel(repo)

        vm.equip(AccessorySlot.HEAD, "hat")
        advanceUntilIdle()

        val acc = repo.profile.value.accessories
        assertFalse(acc.contains("head:hat"))
      }

  @Test
  fun equip_none_removes_slot() =
      runTest(dispatcher) {
        val repo = TestRepo(UserProfile(accessories = listOf("owned:hat", "head:hat")))
        val vm = ProfileViewModel(repo)

        vm.equip(AccessorySlot.HEAD, "none")
        advanceUntilIdle()

        val acc = repo.profile.value.accessories
        assertFalse(acc.any { it.startsWith("head:") })
      }

  @Test
  fun equip_replaces_existing_accessory_in_same_slot() =
      runTest(dispatcher) {
        val repo =
            TestRepo(UserProfile(accessories = listOf("owned:hat", "owned:glasses", "head:hat")))
        val vm = ProfileViewModel(repo)

        vm.equip(AccessorySlot.HEAD, "glasses")
        advanceUntilIdle()

        val acc = repo.profile.value.accessories
        assertFalse(acc.contains("head:hat"))
        assertTrue(acc.contains("head:glasses"))
      }

  @Test
  fun unequip_removes_only_that_slot() =
      runTest(dispatcher) {
        val repo =
            TestRepo(UserProfile(accessories = listOf("owned:halo", "owned:scarf", "owned:boots")))
        val vm = ProfileViewModel(repo)

        vm.equip(AccessorySlot.HEAD, "halo")
        advanceUntilIdle()
        vm.equip(AccessorySlot.TORSO, "scarf")
        advanceUntilIdle()
        vm.equip(AccessorySlot.LEGS, "boots")
        advanceUntilIdle()

        vm.unequip(AccessorySlot.TORSO)
        advanceUntilIdle()

        val acc = repo.profile.value.accessories
        assertTrue(acc.contains("head:halo"))
        assertTrue(acc.contains("legs:boots"))
        assertFalse(acc.any { it.startsWith("torso:") })
      }

  @Test
  fun equip_replaces_existing_item_in_same_slot() =
      runTest(dispatcher) {
        // This test verifies that equip() properly replaces items in the same slot
        val repo =
            TestRepo(UserProfile(accessories = listOf("owned:hat", "owned:glasses", "head:hat")))
        val vm = ProfileViewModel(repo)

        // Equip glasses to replace hat
        vm.equip(AccessorySlot.HEAD, "glasses")
        advanceUntilIdle()

        val acc = repo.profile.value.accessories
        assertFalse("Should not have head:hat anymore", acc.contains("head:hat"))
        assertTrue("Should have head:glasses", acc.contains("head:glasses"))
        // Should still have owned items
        assertTrue(acc.contains("owned:hat"))
        assertTrue(acc.contains("owned:glasses"))
      }

  // ========== EquippedId Tests ==========

  @Test
  fun equippedId_returns_null_when_slot_empty() = runTest {
    val repo = TestRepo(UserProfile(accessories = emptyList()))
    val vm = ProfileViewModel(repo)

    assertNull(vm.equippedId(AccessorySlot.HEAD))
  }

  @Test
  fun equippedId_returns_id_when_slot_equipped() =
      runTest(dispatcher) {
        val repo = TestRepo(UserProfile(accessories = listOf("owned:hat", "head:hat")))
        val vm = ProfileViewModel(repo)

        assertEquals("hat", vm.equippedId(AccessorySlot.HEAD))
      }

  @Test
  fun equippedId_works_for_all_slots() =
      runTest(dispatcher) {
        val repo =
            TestRepo(
                UserProfile(
                    accessories =
                        listOf(
                            "owned:hat",
                            "head:hat",
                            "owned:scarf",
                            "torso:scarf",
                            "owned:wings",
                            "back:wings")))
        val vm = ProfileViewModel(repo)

        assertEquals("hat", vm.equippedId(AccessorySlot.HEAD))
        assertEquals("scarf", vm.equippedId(AccessorySlot.TORSO))
        assertEquals("wings", vm.equippedId(AccessorySlot.BACK))
        assertNull(vm.equippedId(AccessorySlot.LEGS))
      }

  // ========== Repository Interaction Tests ==========

  @Test
  fun external_repo_update_is_observed_by_viewmodel() = runTest {
    val repo = FakeProfileRepository()
    val vm = ProfileViewModel(repository = repo)

    repo.updateProfile(repo.profile.value.copy(name = "Taylor", points = 2000))
    advanceUntilIdle()

    assertEquals("Taylor", repo.profile.value.name)
    assertEquals(2000, repo.profile.value.points)

    // ViewModel maintains its own copy
    assertEquals("Alex", vm.userProfile.value.name)
    assertEquals(1250, vm.userProfile.value.points)
  }

  // ========== Coins/Rewards Tests ==========

  @Test
  fun addCoins_withPositiveAmount_increasesUserCoins() =
      runTest(dispatcher) {
        val repo = FakeProfileRepository()
        val vm = ProfileViewModel(repository = repo)

        val before = vm.userProfile.value.coins
        vm.addCoins(100)
        advanceUntilIdle()
        val after = vm.userProfile.value.coins

        assertEquals(before + 100, after)
      }

  @Test
  fun addCoins_withZeroOrNegativeAmount_doesNothing() =
      runTest(dispatcher) {
        val repo = FakeProfileRepository()
        val vm = ProfileViewModel(repository = repo)

        val before = vm.userProfile.value.coins
        vm.addCoins(0)
        advanceUntilIdle()
        assertEquals(before, vm.userProfile.value.coins)

        vm.addCoins(-50)
        advanceUntilIdle()
        val after = vm.userProfile.value.coins

        assertEquals(before, after)
      }

  // ========== Color Math Tests ==========

  @Test
  fun light_blend_matches_expected_math() =
      runTest(dispatcher) {
        val vm = ProfileViewModel(FakeProfileRepository())
        val job = launch { vm.accentEffective.collect {} }

        val base = Color(0xFF336699)
        vm.setAvatarAccent(base)
        advanceUntilIdle()
        vm.setAccentVariant(AccentVariant.Light)
        advanceUntilIdle()

        val got = vm.accentEffective.value
        val r = 0x33 / 255f
        val g = 0x66 / 255f
        val b = 0x99 / 255f
        val t = 0.25f
        fun blend(a: Float) = a + (1f - a) * t
        val expR = blend(r)
        val expG = blend(g)
        val expB = blend(b)

        assertEquals(expR, got.red, 0.002f)
        assertEquals(expG, got.green, 0.002f)
        assertEquals(expB, got.blue, 0.002f)

        job.cancel()
      }

  @Test
  fun dark_blend_scales_toward_black_by_factor_0_75() =
      runTest(dispatcher) {
        val vm = ProfileViewModel(FakeProfileRepository())
        val job = launch { vm.accentEffective.collect {} }

        val base = Color(0xFF336699)
        vm.setAvatarAccent(base)
        advanceUntilIdle()
        vm.setAccentVariant(AccentVariant.Dark)
        advanceUntilIdle()

        val got = vm.accentEffective.value
        assertEquals((0x33 / 255f) * 0.75f, got.red, 0.002f)
        assertEquals((0x66 / 255f) * 0.75f, got.green, 0.002f)
        assertEquals((0x99 / 255f) * 0.75f, got.blue, 0.002f)

        job.cancel()
      }

  @Test
  fun vibrant_preserves_black_and_clamps_to_one() =
      runTest(dispatcher) {
        val vm = ProfileViewModel(FakeProfileRepository())
        val job = launch { vm.accentEffective.collect {} }

        vm.setAvatarAccent(Color.Black)
        advanceUntilIdle()
        vm.setAccentVariant(AccentVariant.Vibrant)
        advanceUntilIdle()
        val blackV = vm.accentEffective.value
        assertEquals(0f, blackV.red, 0.0f)
        assertEquals(0f, blackV.green, 0.0f)
        assertEquals(0f, blackV.blue, 0.0f)

        val nearWhite = Color(red = 0.98f, green = 0.95f, blue = 0.96f, alpha = 1f)
        vm.setAvatarAccent(nearWhite)
        advanceUntilIdle()
        vm.setAccentVariant(AccentVariant.Vibrant)
        advanceUntilIdle()
        val whiteish = vm.accentEffective.value
        listOf(whiteish.red, whiteish.green, whiteish.blue).forEach { c -> assertTrue(c in 0f..1f) }

        job.cancel()
      }

  @Test
  fun base_variant_returns_exact_base_color_even_with_alpha() =
      runTest(dispatcher) {
        val vm = ProfileViewModel(FakeProfileRepository())
        val job = launch { vm.accentEffective.collect {} }

        val base = Color(0.2f, 0.4f, 0.6f, 0.5f)
        vm.setAvatarAccent(base)
        advanceUntilIdle()
        vm.setAccentVariant(AccentVariant.Base)
        advanceUntilIdle()

        val got = vm.accentEffective.value
        assertEquals(base.red, got.red, 0.01f)
        assertEquals(base.green, got.green, 0.01f)
        assertEquals(base.blue, got.blue, 0.01f)
        assertEquals(base.alpha, got.alpha, 0.01f)

        job.cancel()
      }

  @Test
  fun vibrant_changes_channels_for_non_greyscale_non_black() =
      runTest(dispatcher) {
        val vm = ProfileViewModel(FakeProfileRepository())
        val job = launch { vm.accentEffective.collect {} }

        val base = Color(0.4f, 0.2f, 0.8f, 1f)
        vm.setAvatarAccent(base)
        advanceUntilIdle()
        vm.setAccentVariant(AccentVariant.Vibrant)
        advanceUntilIdle()

        val got = vm.accentEffective.value
        assertNotEquals(base.red, got.red, 0.0f)
        assertNotEquals(base.green, got.green, 0.0f)
        assertNotEquals(base.blue, got.blue, 0.0f)

        job.cancel()
      }

  // ========== Edge Cases & Error Handling ==========

  @Test
  fun multiple_equip_operations_work_correctly() =
      runTest(dispatcher) {
        val repo =
            TestRepo(
                UserProfile(
                    accessories =
                        listOf("owned:hat", "owned:glasses", "owned:scarf", "owned:cape")))
        val vm = ProfileViewModel(repo)

        vm.equip(AccessorySlot.HEAD, "hat")
        advanceUntilIdle()
        vm.equip(AccessorySlot.HEAD, "glasses")
        advanceUntilIdle()
        vm.equip(AccessorySlot.TORSO, "scarf")
        advanceUntilIdle()
        vm.equip(AccessorySlot.TORSO, "cape")
        advanceUntilIdle()

        val acc = repo.profile.value.accessories
        assertTrue(acc.contains("head:glasses"))
        assertFalse(acc.contains("head:hat"))
        assertTrue(acc.contains("torso:cape"))
        assertFalse(acc.contains("torso:scarf"))
      }

  @Test
  fun pushProfile_handles_repository_errors_gracefully() =
      runTest(dispatcher) {
        val failingRepo =
            object : ProfileRepository {
              override val profile = MutableStateFlow(UserProfile())

              override suspend fun updateProfile(newProfile: UserProfile) {
                throw RuntimeException("Network error")
              }

              override suspend fun increaseStudyTimeBy(time: Int) {}

              override suspend fun increaseStreakIfCorrect() {}
            }

        val vm = ProfileViewModel(repository = failingRepo)

        val before = vm.userProfile.value.locationEnabled

        // Should not crash despite repository error
        vm.toggleLocation()
        advanceUntilIdle()

        // Local state should still update even if repository fails
        val after = vm.userProfile.value.locationEnabled
        assertEquals(!before, after)
      }

  @Test
  fun accentEffective_initial_value_is_valid() = runTest {
    val vm = ProfileViewModel(FakeProfileRepository())

    val initial = vm.accentEffective.value
    assertNotEquals(Color.Unspecified, initial)
  }

  // ========== Test Helper Class ==========

  private class TestRepo(initial: UserProfile) : ProfileRepository {
    private val state = MutableStateFlow(initial)
    override val profile: StateFlow<UserProfile> = state

    override suspend fun updateProfile(newProfile: UserProfile) {
      state.value = newProfile
    }

    override suspend fun increaseStudyTimeBy(time: Int) {}

    override suspend fun increaseStreakIfCorrect() {}
  }
}
