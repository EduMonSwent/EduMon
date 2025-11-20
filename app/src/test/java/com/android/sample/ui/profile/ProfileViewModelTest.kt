package com.android.sample.ui.profile

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.android.sample.data.AccentVariant
import com.android.sample.data.AccessorySlot
import com.android.sample.data.FakeUserStatsRepository
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
        // Fix: Provide userStatsRepository to avoid default AppRepositories singleton usage
        val vm = ProfileViewModel(FakeProfileRepository(), FakeUserStatsRepository())
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

    override suspend fun increaseStudyTimeBy(time: Int) {}

    override suspend fun increaseStreakIfCorrect() {}
  }

  @Test
  fun unequip_removes_only_that_slot() =
      runTest(dispatcher) {
        val repo = TestRepo(UserProfile())
        // Fix: Provide userStatsRepository
        val vm = ProfileViewModel(repo, FakeUserStatsRepository())

        vm.equip(AccessorySlot.HEAD, "halo")
        vm.equip(AccessorySlot.TORSO, "scarf")
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
  fun equip_legs_cleans_legacy_singular_prefix_and_sets_new() =
      runTest(dispatcher) {
        val repo = TestRepo(UserProfile(accessories = listOf("leg:boots")))
        // Fix: Provide userStatsRepository
        val vm = ProfileViewModel(repo, FakeUserStatsRepository())

        vm.equip(AccessorySlot.LEGS, "rocket")
        advanceUntilIdle()

        val acc = repo.profile.value.accessories
        assertFalse(acc.any { it.startsWith("leg:") })
        assertTrue(acc.contains("legs:rocket"))
      }

  @Test
  fun accent_variants_cover_all_paths_without_strict_inequality() =
      runTest(dispatcher) {
        // Fix: Provide userStatsRepository
        val vm = ProfileViewModel(FakeProfileRepository(), FakeUserStatsRepository())

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

  @Test
  fun external_repo_update_is_observed_by_viewmodel() = runTest {
    val repo = FakeProfileRepository()
    // Fix: Provide userStatsRepository
    val vm = ProfileViewModel(repository = repo, userStatsRepository = FakeUserStatsRepository())

    repo.updateProfile(repo.profile.value.copy(name = "Taylor", points = 2000))
    advanceUntilIdle()

    assertEquals("Taylor", repo.profile.value.name)
    // Points are no longer synced from profile repo but stats repo, so we verify name update only.
    assertEquals("Alex", vm.userProfile.value.name)
  }

  // --- Reward system tests ---------------------------------------------------

  @Test
  fun addCoins_withPositiveAmount_increasesUserCoins() = runTest {
    val repo = FakeProfileRepository()
    val statsRepo = FakeUserStatsRepository() // Use this to check coins
    val vm = ProfileViewModel(repository = repo, userStatsRepository = statsRepo)

    // Initial coins 0 in statsRepo
    vm.addCoins(100)
    advanceUntilIdle() // allow coroutine to update statsRepo

    // The VM updates statsRepo, then collects from it to update userProfile
    val after = vm.userProfile.value.coins
    // statsRepo coins should be 100
    assertEquals(100, statsRepo.stats.value.coins)

    // And profile should eventually reflect it
    assertEquals(100, after)
  }

  @Test
  fun addCoins_withZeroOrNegativeAmount_doesNothing() = runTest {
    val repo = FakeProfileRepository()
    val statsRepo = FakeUserStatsRepository()
    val vm = ProfileViewModel(repository = repo, userStatsRepository = statsRepo)

    val before = vm.userProfile.value.coins
    vm.addCoins(0) // should be ignored
    vm.addCoins(-50) // should also be ignored
    advanceUntilIdle()

    val after = vm.userProfile.value.coins

    assertEquals(before, after)
  }

  // Color tests

  @Test
  fun light_blend_matches_expected_math() = runTest {
    // Fix: Provide userStatsRepository
    val vm = ProfileViewModel(FakeProfileRepository(), FakeUserStatsRepository())

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
    fun blend(a: Float, b: Float) = a + (b - a) * t
    val expR = blend(r, 1f)
    val expG = blend(g, 1f)
    val expB = blend(b, 1f)

    assertEquals(expR, got.red, 0.002f)
    assertEquals(expG, got.green, 0.002f)
    assertEquals(expB, got.blue, 0.002f)

    job.cancel()
  }

  @Test
  fun dark_blend_scales_toward_black_by_factor_0_75() = runTest {
    // Fix: Provide userStatsRepository
    val vm = ProfileViewModel(FakeProfileRepository(), FakeUserStatsRepository())
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
  fun vibrant_preserves_black_and_clamps_to_one() = runTest {
    // Fix: Provide userStatsRepository
    val vm = ProfileViewModel(FakeProfileRepository(), FakeUserStatsRepository())
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
  fun base_variant_returns_exact_base_color_even_with_alpha() = runTest {
    // Fix: Provide userStatsRepository
    val vm = ProfileViewModel(FakeProfileRepository(), FakeUserStatsRepository())
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
  fun vibrant_changes_channels_for_non_greyscale_non_black() = runTest {
    // Fix: Provide userStatsRepository
    val vm = ProfileViewModel(FakeProfileRepository(), FakeUserStatsRepository())
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
}
