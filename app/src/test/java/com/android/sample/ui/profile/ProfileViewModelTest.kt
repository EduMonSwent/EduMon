package com.android.sample.ui.profile

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.android.sample.R
import com.android.sample.data.AccentVariant
import com.android.sample.data.AccessorySlot
import com.android.sample.data.UserProfile
import com.android.sample.profile.ProfileRepository
import com.android.sample.ui.stats.model.StudyStats
import io.mockk.*
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

// The assistance of an AI tool (Claude) was solicited in writing this file.

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

  private val dispatcher = StandardTestDispatcher()
  private lateinit var mockRepository: ProfileRepository
  private lateinit var profileFlow: MutableStateFlow<UserProfile>

  private val testProfile =
      UserProfile(
          name = "TestUser",
          email = "test@university.edu",
          level = 1,
          points = 150,
          coins = 100,
          streak = 5,
          notificationsEnabled = true,
          locationEnabled = false,
          focusModeEnabled = false,
          avatarAccent = Color.Blue.toArgb().toLong(),
          accessories = listOf("owned:hat", "head:hat"),
          studyStats = StudyStats(totalTimeMin = 45, dailyGoalMin = 180),
          lastRewardedLevel = 0)

  @Before
  fun setUp() {
    Dispatchers.setMain(dispatcher)

    profileFlow = MutableStateFlow(testProfile)
    mockRepository = mockk(relaxed = true)
    every { mockRepository.profile } returns profileFlow
    coEvery { mockRepository.updateProfile(any()) } returns Unit
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
    unmockkAll()
  }

  // ========== Basic ViewModel Initialization ==========

  @Test
  fun viewModel_initializes_with_repository_profile() =
      runTest(dispatcher) {
        val vm = ProfileViewModel(repository = mockRepository)
        advanceUntilIdle()

        assertNotNull(vm.userProfile.value)
        assertEquals("TestUser", vm.userProfile.value.name)
        assertEquals(150, vm.userProfile.value.points)
      }

  @Test
  fun viewModel_collects_profile_updates_from_repository() =
      runTest(dispatcher) {
        val vm = ProfileViewModel(repository = mockRepository)
        advanceUntilIdle()

        val updatedProfile = testProfile.copy(name = "UpdatedUser", points = 500)
        profileFlow.emit(updatedProfile)
        advanceUntilIdle()

        assertEquals("UpdatedUser", vm.userProfile.value.name)
        assertEquals(500, vm.userProfile.value.points)
      }

  @Test
  fun accentPalette_contains_five_colors() =
      runTest(dispatcher) {
        val vm = ProfileViewModel(mockRepository)

        assertEquals(5, vm.accentPalette.size)
        assertTrue(vm.accentPalette.all { it != Color.Unspecified })
      }

  @Test
  fun points_per_level_constant_equals_300() {
    assertEquals(300, ProfileViewModel.POINTS_PER_LEVEL)
  }

  // ========== Accessory Catalog Tests ==========

  @Test
  fun accessoryCatalog_always_includes_none_items() =
      runTest(dispatcher) {
        val repo = TestRepo(UserProfile(accessories = emptyList()))
        val vm = ProfileViewModel(repository = repo)

        val catalog = vm.accessoryCatalog

        assertTrue(catalog.any { it.id == "none" && it.slot == AccessorySlot.HEAD })
        assertTrue(catalog.any { it.id == "none" && it.slot == AccessorySlot.TORSO })
        assertTrue(catalog.any { it.id == "none" && it.slot == AccessorySlot.BACK })
      }

  @Test
  fun accessoryCatalog_includes_owned_accessories() =
      runTest(dispatcher) {
        val repo = TestRepo(UserProfile(accessories = listOf("owned:hat", "owned:scarf")))
        val vm = ProfileViewModel(repository = repo)

        val catalog = vm.accessoryCatalog

        assertTrue(catalog.any { it.id == "hat" })
        assertTrue(catalog.any { it.id == "scarf" })
      }

  @Test
  fun accessoryCatalog_excludes_non_owned_accessories() =
      runTest(dispatcher) {
        val repo = TestRepo(UserProfile(accessories = listOf("owned:hat")))
        val vm = ProfileViewModel(repository = repo)

        val catalog = vm.accessoryCatalog

        assertFalse(catalog.any { it.id == "glasses" })
        assertTrue(catalog.any { it.id == "hat" })
      }

  @Test
  fun accessoryCatalog_includes_all_owned_items() =
      runTest(dispatcher) {
        val repo =
            TestRepo(
                UserProfile(
                    accessories =
                        listOf(
                            "owned:hat",
                            "owned:glasses",
                            "owned:scarf",
                            "owned:cape",
                            "owned:wings",
                            "owned:aura")))
        val vm = ProfileViewModel(repository = repo)

        val catalog = vm.accessoryCatalog

        assertTrue(catalog.any { it.id == "hat" })
        assertTrue(catalog.any { it.id == "glasses" })
        assertTrue(catalog.any { it.id == "scarf" })
        assertTrue(catalog.any { it.id == "cape" })
        assertTrue(catalog.any { it.id == "wings" })
        assertTrue(catalog.any { it.id == "aura" })
      }

  // ========== Accessory Resource ID Tests ==========

  @Test
  fun accessoryResId_returns_correct_drawable_for_head_items() =
      runTest(dispatcher) {
        val vm = ProfileViewModel(mockRepository)

        assertEquals(R.drawable.cosmetic_hat, vm.accessoryResId(AccessorySlot.HEAD, "hat"))
        assertEquals(R.drawable.cosmetic_glasses, vm.accessoryResId(AccessorySlot.HEAD, "glasses"))
        assertEquals(0, vm.accessoryResId(AccessorySlot.HEAD, "none"))
        assertEquals(0, vm.accessoryResId(AccessorySlot.HEAD, "unknown"))
      }

  @Test
  fun accessoryResId_returns_correct_drawable_for_torso_items() =
      runTest(dispatcher) {
        val vm = ProfileViewModel(mockRepository)

        assertEquals(R.drawable.cosmetic_scarf, vm.accessoryResId(AccessorySlot.TORSO, "scarf"))
        assertEquals(R.drawable.cosmetic_cape, vm.accessoryResId(AccessorySlot.TORSO, "cape"))
        assertEquals(0, vm.accessoryResId(AccessorySlot.TORSO, "none"))
        assertEquals(0, vm.accessoryResId(AccessorySlot.TORSO, "unknown"))
      }

  @Test
  fun accessoryResId_returns_correct_drawable_for_back_items() =
      runTest(dispatcher) {
        val vm = ProfileViewModel(mockRepository)

        assertEquals(R.drawable.cosmetic_wings, vm.accessoryResId(AccessorySlot.BACK, "wings"))
        assertEquals(R.drawable.cosmetic_aura, vm.accessoryResId(AccessorySlot.BACK, "aura"))
        assertEquals(0, vm.accessoryResId(AccessorySlot.BACK, "none"))
        assertEquals(0, vm.accessoryResId(AccessorySlot.BACK, "unknown"))
      }

  @Test
  fun accessoryResId_returns_zero_for_legs_slot() =
      runTest(dispatcher) {
        val vm = ProfileViewModel(mockRepository)

        assertEquals(0, vm.accessoryResId(AccessorySlot.LEGS, "anything"))
        assertEquals(0, vm.accessoryResId(AccessorySlot.LEGS, "hat"))
      }

  // ========== Avatar Accent Tests ==========

  @Test
  fun setAvatarAccent_updates_user_profile() =
      runTest(dispatcher) {
        val vm = ProfileViewModel(mockRepository)
        advanceUntilIdle()

        val newColor = Color(0xFF10B981)
        vm.setAvatarAccent(newColor)
        advanceUntilIdle()

        assertEquals(newColor.toArgb().toLong(), vm.userProfile.value.avatarAccent)
        coVerify { mockRepository.updateProfile(any()) }
      }

  @Test
  fun setAvatarAccent_triggers_accentEffective_update() =
      runTest(dispatcher) {
        val vm = ProfileViewModel(mockRepository)
        val job = launch { vm.accentEffective.collect {} }
        advanceUntilIdle()

        val newColor = Color(0xFFFF6D00)
        vm.setAvatarAccent(newColor)
        advanceUntilIdle()

        assertNotEquals(Color.Unspecified, vm.accentEffective.value)

        job.cancel()
      }

  @Test
  fun accentEffective_initial_value_is_valid() =
      runTest(dispatcher) {
        val vm = ProfileViewModel(mockRepository)
        advanceUntilIdle()

        val initial = vm.accentEffective.value
        assertNotEquals(Color.Unspecified, initial)
      }

  // ========== Accent Variant Tests ==========

  @Test
  fun setAccentVariant_updates_variant_flow() =
      runTest(dispatcher) {
        val vm = ProfileViewModel(mockRepository)
        advanceUntilIdle()

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
  fun accent_variants_cover_all_paths() =
      runTest(dispatcher) {
        val vm = ProfileViewModel(mockRepository)
        val job = launch { vm.accentEffective.collect {} }
        advanceUntilIdle()

        val baseColor = Color(0xFF6699CC)
        vm.setAvatarAccent(baseColor)
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
        val vibrant = vm.accentEffective.value
        assertTrue(vibrant.alpha in 0f..1f)

        vm.setAccentVariant(AccentVariant.Base)
        advanceUntilIdle()
        val base = vm.accentEffective.value
        assertTrue(base.alpha in 0f..1f)

        job.cancel()
      }

  @Test
  fun light_blend_matches_expected_math() =
      runTest(dispatcher) {
        val vm = ProfileViewModel(mockRepository)
        val job = launch { vm.accentEffective.collect {} }
        advanceUntilIdle()

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
        val vm = ProfileViewModel(mockRepository)
        val job = launch { vm.accentEffective.collect {} }
        advanceUntilIdle()

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
        val vm = ProfileViewModel(mockRepository)
        val job = launch { vm.accentEffective.collect {} }
        advanceUntilIdle()

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
        val vm = ProfileViewModel(mockRepository)
        val job = launch { vm.accentEffective.collect {} }
        advanceUntilIdle()

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
        val vm = ProfileViewModel(mockRepository)
        val job = launch { vm.accentEffective.collect {} }
        advanceUntilIdle()

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

  // ========== Toggle Functions Tests ==========

  @Test
  fun toggleNotifications_flips_notifications_enabled() =
      runTest(dispatcher) {
        val vm = ProfileViewModel(mockRepository)
        advanceUntilIdle()
        val before = vm.userProfile.value.notificationsEnabled

        vm.toggleNotifications()
        advanceUntilIdle()

        assertEquals(!before, vm.userProfile.value.notificationsEnabled)
        coVerify { mockRepository.updateProfile(any()) }
      }

  @Test
  fun toggleLocation_flips_location_enabled() =
      runTest(dispatcher) {
        val vm = ProfileViewModel(mockRepository)
        advanceUntilIdle()
        val before = vm.userProfile.value.locationEnabled

        vm.toggleLocation()
        advanceUntilIdle()

        assertEquals(!before, vm.userProfile.value.locationEnabled)
        coVerify { mockRepository.updateProfile(any()) }
      }

  @Test
  fun toggleFocusMode_flips_focus_mode_enabled() =
      runTest(dispatcher) {
        val vm = ProfileViewModel(mockRepository)
        advanceUntilIdle()
        val before = vm.userProfile.value.focusModeEnabled

        vm.toggleFocusMode()
        advanceUntilIdle()

        assertEquals(!before, vm.userProfile.value.focusModeEnabled)
        coVerify { mockRepository.updateProfile(any()) }
      }

  @Test
  fun multiple_toggle_operations_work_correctly() =
      runTest(dispatcher) {
        val vm = ProfileViewModel(mockRepository)
        advanceUntilIdle()

        vm.toggleNotifications()
        advanceUntilIdle()
        val afterFirst = vm.userProfile.value.notificationsEnabled

        vm.toggleNotifications()
        advanceUntilIdle()
        val afterSecond = vm.userProfile.value.notificationsEnabled

        assertEquals(!afterFirst, afterSecond)
      }

  // ========== Equip/Unequip Tests ==========

  @Test
  fun equip_adds_owned_accessory_to_profile() =
      runTest(dispatcher) {
        val repo = TestRepo(UserProfile(accessories = listOf("owned:hat")))
        val vm = ProfileViewModel(repo)
        advanceUntilIdle()

        vm.equip(AccessorySlot.HEAD, "hat")
        advanceUntilIdle()

        assertTrue(vm.userProfile.value.accessories.contains("head:hat"))
      }

  @Test
  fun equip_does_not_add_unowned_accessory() =
      runTest(dispatcher) {
        val repo = TestRepo(UserProfile(accessories = emptyList()))
        val vm = ProfileViewModel(repo)
        advanceUntilIdle()

        val beforeSize = vm.userProfile.value.accessories.size
        vm.equip(AccessorySlot.HEAD, "hat")
        advanceUntilIdle()

        assertEquals(beforeSize, vm.userProfile.value.accessories.size)
        assertFalse(vm.userProfile.value.accessories.contains("head:hat"))
      }

  @Test
  fun equip_none_removes_accessory_from_slot() =
      runTest(dispatcher) {
        val repo = TestRepo(UserProfile(accessories = listOf("owned:hat", "head:hat")))
        val vm = ProfileViewModel(repo)
        advanceUntilIdle()

        vm.equip(AccessorySlot.HEAD, "none")
        advanceUntilIdle()

        assertFalse(vm.userProfile.value.accessories.any { it.startsWith("head:") })
      }

  @Test
  fun equip_replaces_existing_accessory_in_same_slot() =
      runTest(dispatcher) {
        val repo =
            TestRepo(UserProfile(accessories = listOf("owned:hat", "owned:glasses", "head:hat")))
        val vm = ProfileViewModel(repo)
        advanceUntilIdle()

        vm.equip(AccessorySlot.HEAD, "glasses")
        advanceUntilIdle()

        assertTrue(vm.userProfile.value.accessories.contains("head:glasses"))
        assertFalse(vm.userProfile.value.accessories.contains("head:hat"))
      }

  @Test
  fun multiple_equip_operations_work_correctly() =
      runTest(dispatcher) {
        val repo =
            TestRepo(
                UserProfile(
                    accessories =
                        listOf("owned:hat", "owned:glasses", "owned:scarf", "owned:cape")))
        val vm = ProfileViewModel(repo)
        advanceUntilIdle()

        vm.equip(AccessorySlot.HEAD, "hat")
        advanceUntilIdle()
        vm.equip(AccessorySlot.HEAD, "glasses")
        advanceUntilIdle()
        vm.equip(AccessorySlot.TORSO, "scarf")
        advanceUntilIdle()
        vm.equip(AccessorySlot.TORSO, "cape")
        advanceUntilIdle()

        val acc = vm.userProfile.value.accessories
        assertTrue(acc.contains("head:glasses"))
        assertFalse(acc.contains("head:hat"))
        assertTrue(acc.contains("torso:cape"))
        assertFalse(acc.contains("torso:scarf"))
      }

  @Test
  fun unequip_removes_accessory_from_slot() =
      runTest(dispatcher) {
        val repo = TestRepo(UserProfile(accessories = listOf("owned:hat", "head:hat")))
        val vm = ProfileViewModel(repo)
        advanceUntilIdle()

        vm.unequip(AccessorySlot.HEAD)
        advanceUntilIdle()

        assertFalse(vm.userProfile.value.accessories.any { it.startsWith("head:") })
      }

  @Test
  fun unequip_on_empty_slot_does_nothing() =
      runTest(dispatcher) {
        val repo = TestRepo(UserProfile(accessories = emptyList()))
        val vm = ProfileViewModel(repo)
        advanceUntilIdle()

        vm.unequip(AccessorySlot.HEAD)
        advanceUntilIdle()

        assertEquals(0, vm.userProfile.value.accessories.size)
      }

  @Test
  fun equippedId_returns_correct_id_for_equipped_accessory() =
      runTest(dispatcher) {
        val repo = TestRepo(UserProfile(accessories = listOf("head:hat", "torso:scarf")))
        val vm = ProfileViewModel(repo)
        advanceUntilIdle()

        assertEquals("hat", vm.equippedId(AccessorySlot.HEAD))
        assertEquals("scarf", vm.equippedId(AccessorySlot.TORSO))
      }

  @Test
  fun equippedId_returns_null_when_no_accessory_equipped() =
      runTest(dispatcher) {
        val repo = TestRepo(UserProfile(accessories = emptyList()))
        val vm = ProfileViewModel(repo)
        advanceUntilIdle()

        assertNull(vm.equippedId(AccessorySlot.HEAD))
        assertNull(vm.equippedId(AccessorySlot.TORSO))
        assertNull(vm.equippedId(AccessorySlot.BACK))
      }

  @Test
  fun equippedId_handles_all_slots_correctly() =
      runTest(dispatcher) {
        val repo =
            TestRepo(UserProfile(accessories = listOf("head:hat", "torso:scarf", "back:wings")))
        val vm = ProfileViewModel(repo)
        advanceUntilIdle()

        assertEquals("hat", vm.equippedId(AccessorySlot.HEAD))
        assertEquals("scarf", vm.equippedId(AccessorySlot.TORSO))
        assertEquals("wings", vm.equippedId(AccessorySlot.BACK))
        assertNull(vm.equippedId(AccessorySlot.LEGS))
      }

  // ========== Coins Tests ==========

  @Test
  fun addCoins_withPositiveAmount_increasesUserCoins() =
      runTest(dispatcher) {
        val vm = ProfileViewModel(mockRepository)
        advanceUntilIdle()

        val before = vm.userProfile.value.coins
        vm.addCoins(100)
        advanceUntilIdle()
        val after = vm.userProfile.value.coins

        assertEquals(before + 100, after)
        coVerify { mockRepository.updateProfile(any()) }
      }

  @Test
  fun addCoins_withZero_doesNothing() =
      runTest(dispatcher) {
        val vm = ProfileViewModel(mockRepository)
        advanceUntilIdle()

        val before = vm.userProfile.value.coins
        vm.addCoins(0)
        advanceUntilIdle()

        assertEquals(before, vm.userProfile.value.coins)
      }

  @Test
  fun addCoins_withNegativeAmount_doesNothing() =
      runTest(dispatcher) {
        val vm = ProfileViewModel(mockRepository)
        advanceUntilIdle()

        val before = vm.userProfile.value.coins
        vm.addCoins(-50)
        advanceUntilIdle()

        assertEquals(before, vm.userProfile.value.coins)
      }

  // ========== Points and Level Tests ==========

  @Test
  fun addPoints_withPositiveAmount_increasesPoints() =
      runTest(dispatcher) {
        val vm = ProfileViewModel(mockRepository)
        advanceUntilIdle()

        val before = vm.userProfile.value.points
        vm.addPoints(50)
        advanceUntilIdle()

        assertEquals(before + 50, vm.userProfile.value.points)
      }

  @Test
  fun addPoints_withZero_doesNothing() =
      runTest(dispatcher) {
        val vm = ProfileViewModel(mockRepository)
        advanceUntilIdle()

        val before = vm.userProfile.value.points
        vm.addPoints(0)
        advanceUntilIdle()

        assertEquals(before, vm.userProfile.value.points)
      }

  @Test
  fun addPoints_withNegativeAmount_doesNothing() =
      runTest(dispatcher) {
        val vm = ProfileViewModel(mockRepository)
        advanceUntilIdle()

        val before = vm.userProfile.value.points
        vm.addPoints(-10)
        advanceUntilIdle()

        assertEquals(before, vm.userProfile.value.points)
      }

  @Test
  fun addPoints_computes_correct_level_from_points() =
      runTest(dispatcher) {
        val repo = TestRepo(UserProfile(points = 0, level = 1))
        val vm = ProfileViewModel(repo)
        advanceUntilIdle()

        vm.addPoints(300) // Should reach level 2
        advanceUntilIdle()
        assertEquals(2, vm.userProfile.value.level)

        vm.addPoints(300) // Should reach level 3
        advanceUntilIdle()
        assertEquals(3, vm.userProfile.value.level)

        vm.addPoints(600) // Should reach level 5
        advanceUntilIdle()
        assertEquals(5, vm.userProfile.value.level)
      }

  @Test
  fun addPoints_does_not_level_up_below_threshold() =
      runTest(dispatcher) {
        val repo = TestRepo(UserProfile(points = 250, level = 1))
        val vm = ProfileViewModel(repo)
        advanceUntilIdle()

        vm.addPoints(49) // 250 + 49 = 299 -> still level 1
        advanceUntilIdle()

        assertEquals(1, vm.userProfile.value.level)
        assertEquals(299, vm.userProfile.value.points)
      }

  // ========== Debug Functions Tests ==========

  @Test
  fun debugLevelUpForTests_increases_level_by_1() =
      runTest(dispatcher) {
        val vm = ProfileViewModel(mockRepository)
        advanceUntilIdle()

        val beforeLevel = vm.userProfile.value.level
        vm.debugLevelUpForTests()
        advanceUntilIdle()

        assertEquals(beforeLevel + 1, vm.userProfile.value.level)
      }

  @Test
  fun debugNoLevelChangeForTests_adds_points_without_level_change() =
      runTest(dispatcher) {
        val repo = TestRepo(UserProfile(points = 50, level = 1))
        val vm = ProfileViewModel(repo)
        advanceUntilIdle()

        vm.debugNoLevelChangeForTests()
        advanceUntilIdle()

        assertEquals(1, vm.userProfile.value.level)
        assertEquals(60, vm.userProfile.value.points)
      }

  // ========== Error Handling Tests ==========

  @Test
  fun pushProfile_handles_repository_errors_gracefully() =
      runTest(dispatcher) {
        val failingRepo =
            object : ProfileRepository {
              override val profile = MutableStateFlow(testProfile)

              override suspend fun updateProfile(newProfile: UserProfile) {
                throw RuntimeException("Network error")
              }

              override suspend fun increaseStudyTimeBy(time: Int) {}

              override suspend fun increaseStreakIfCorrect() {}
            }

        val vm = ProfileViewModel(repository = failingRepo)
        advanceUntilIdle()

        val before = vm.userProfile.value.locationEnabled

        vm.toggleLocation()
        advanceUntilIdle()

        val after = vm.userProfile.value.locationEnabled
        assertEquals(!before, after)
      }

  @Test
  fun repository_update_failure_does_not_crash_viewmodel() =
      runTest(dispatcher) {
        coEvery { mockRepository.updateProfile(any()) } throws Exception("Network error")

        val vm = ProfileViewModel(mockRepository)
        advanceUntilIdle()

        vm.addCoins(10)
        advanceUntilIdle()

        assertEquals(110, vm.userProfile.value.coins)
      }

  // ========== Complex Scenarios ==========

  @Test
  fun multiple_profile_updates_in_sequence_work_correctly() =
      runTest(dispatcher) {
        val vm = ProfileViewModel(mockRepository)
        advanceUntilIdle()

        vm.addCoins(10)
        advanceUntilIdle()

        vm.toggleNotifications()
        advanceUntilIdle()

        vm.setAvatarAccent(Color.Green)
        advanceUntilIdle()

        val profile = vm.userProfile.value
        assertEquals(110, profile.coins)
        assertEquals(false, profile.notificationsEnabled)
        assertEquals(Color.Green.toArgb().toLong(), profile.avatarAccent)
      }

  @Test
  fun equip_with_owned_accessory_in_different_slots_works() =
      runTest(dispatcher) {
        val repo = TestRepo(UserProfile(accessories = listOf("owned:hat", "owned:scarf")))
        val vm = ProfileViewModel(repo)
        advanceUntilIdle()

        vm.equip(AccessorySlot.HEAD, "hat")
        advanceUntilIdle()
        vm.equip(AccessorySlot.TORSO, "scarf")
        advanceUntilIdle()

        assertTrue(vm.userProfile.value.accessories.contains("head:hat"))
        assertTrue(vm.userProfile.value.accessories.contains("torso:scarf"))
      }

  @Test
  fun level_computation_handles_zero_and_negative_points() =
      runTest(dispatcher) {
        val repo = TestRepo(UserProfile(points = 0, level = 1))
        val vm = ProfileViewModel(repo)
        advanceUntilIdle()

        assertEquals(1, vm.userProfile.value.level)
      }

  @Test
  fun accentEffective_combines_profile_accent_with_all_variants() =
      runTest(dispatcher) {
        val vm = ProfileViewModel(mockRepository)
        val job = launch { vm.accentEffective.collect {} }
        advanceUntilIdle()

        val variants =
            listOf(
                AccentVariant.Base, AccentVariant.Light, AccentVariant.Dark, AccentVariant.Vibrant)

        for (variant in variants) {
          vm.setAccentVariant(variant)
          advanceUntilIdle()

          val effectiveColor = vm.accentEffective.value
          assertNotNull(effectiveColor)
          assertNotEquals(Color.Unspecified, effectiveColor)
        }

        job.cancel()
      }

  @Test
  fun owned_accessories_are_correctly_parsed() =
      runTest(dispatcher) {
        val repo =
            TestRepo(
                UserProfile(
                    accessories = listOf("owned:hat", "owned:glasses", "head:hat", "random:data")))
        val vm = ProfileViewModel(repo)
        advanceUntilIdle()

        val catalog = vm.accessoryCatalog

        assertTrue(catalog.any { it.id == "hat" })
        assertTrue(catalog.any { it.id == "glasses" })
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
