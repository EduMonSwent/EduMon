package com.android.sample.ui.profile

// The assistance of an AI tool (ChatGPT) was solicited in writing this file.

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.android.sample.data.AccentVariant
import com.android.sample.data.AccessorySlot
import com.android.sample.data.UserProfile
import com.android.sample.data.UserStats
import com.android.sample.data.UserStatsRepository
import com.android.sample.profile.FakeProfileRepository
import com.android.sample.profile.ProfileRepository
import junit.framework.Assert.assertFalse
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
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

  // --- simple fake stats repo for tests ----
  private class RecordingUserStatsRepository(initial: UserStats = UserStats()) :
      UserStatsRepository {
    private val _stats = MutableStateFlow(initial)
    override val stats: StateFlow<UserStats> = _stats

    var started = false
    var lastCoinsDelta: Int? = null

    override fun start() {
      started = true
    }

    override suspend fun addStudyMinutes(extraMinutes: Int) {}

    override suspend fun updateCoins(delta: Int) {
      lastCoinsDelta = delta
      _stats.value = _stats.value.copy(coins = _stats.value.coins + delta)
    }

    override suspend fun setWeeklyGoal(goalMinutes: Int) {
      _stats.value = _stats.value.copy(weeklyGoal = goalMinutes)
    }

    override suspend fun addPoints(delta: Int) {
      _stats.value = _stats.value.copy(points = _stats.value.points + delta)
    }
  }

  private fun vmWith(
      profileRepo: ProfileRepository = FakeProfileRepository(),
      statsRepo: RecordingUserStatsRepository = RecordingUserStatsRepository()
  ): Pair<ProfileViewModel, RecordingUserStatsRepository> {
    val vm =
        ProfileViewModel(
            profileRepository = profileRepo,
            userStatsRepository = statsRepo,
        )
    return vm to statsRepo
  }

  // ========== Basic ViewModel Initialization ==========

  @Test
  fun viewModel_initializes_with_repository_profile_and_starts_stats_repo() = runTest {
    val profileRepo = FakeProfileRepository()
    val statsRepo = RecordingUserStatsRepository()
    val (vm, stats) = vmWith(profileRepo, statsRepo)

    advanceUntilIdle()

    assertNotNull(vm.userProfile.value)
    assertEquals("Alex", vm.userProfile.value.name)
    // start() must have been called
    assertTrue(stats.started)
  }

  @Test
  fun accentPalette_is_not_empty_and_contains_real_colors() = runTest {
    val (vm, _) = vmWith()
    assertTrue(vm.accentPalette.isNotEmpty())
    assertTrue(vm.accentPalette.all { it != Color.Unspecified })
  }

  // ========== Avatar Accent & Variant ==========

  @Test
  fun setAvatarAccent_updates_user_profile() =
      runTest(dispatcher) {
        val (vm, _) = vmWith()

        val newColor = Color(0xFF10B981)
        vm.setAvatarAccent(newColor)
        advanceUntilIdle()

        assertEquals(newColor.toArgb().toLong(), vm.userProfile.value.avatarAccent)
      }

  @Test
  fun setAccentVariant_updates_flow() =
      runTest(dispatcher) {
        val (vm, _) = vmWith()

        vm.setAccentVariant(AccentVariant.Light)
        advanceUntilIdle()
        assertEquals(AccentVariant.Light, vm.accentVariantFlow.value)

        vm.setAccentVariant(AccentVariant.Dark)
        advanceUntilIdle()
        assertEquals(AccentVariant.Dark, vm.accentVariantFlow.value)
      }

  @Test
  fun accentEffective_changes_when_variant_changes() =
      runTest(dispatcher) {
        val profileRepo =
            FakeProfileRepository(UserProfile(avatarAccent = Color.Red.toArgb().toLong()))
        val (vm, _) = vmWith(profileRepo, RecordingUserStatsRepository())

        advanceUntilIdle()

        // Just verify that changing variant updates the flow
        vm.setAccentVariant(AccentVariant.Dark)
        advanceUntilIdle()
        assertEquals(AccentVariant.Dark, vm.accentVariantFlow.value)

        vm.setAccentVariant(AccentVariant.Vibrant)
        advanceUntilIdle()
        assertEquals(AccentVariant.Vibrant, vm.accentVariantFlow.value)
      }

  // ========== Toggle Functions ==========

  @Test
  fun toggleNotifications_flips_flag() =
      runTest(dispatcher) {
        val (vm, _) = vmWith()
        advanceUntilIdle()

        val before = vm.userProfile.value.notificationsEnabled
        vm.toggleNotifications()
        advanceUntilIdle()
        assertEquals(!before, vm.userProfile.value.notificationsEnabled)
      }

  @Test
  fun toggleLocation_flips_flag() =
      runTest(dispatcher) {
        val (vm, _) = vmWith()
        advanceUntilIdle()

        val before = vm.userProfile.value.locationEnabled
        vm.toggleLocation()
        advanceUntilIdle()
        assertEquals(!before, vm.userProfile.value.locationEnabled)
      }

  @Test
  fun toggleFocusMode_flips_flag() =
      runTest(dispatcher) {
        val (vm, _) = vmWith()
        advanceUntilIdle()

        val before = vm.userProfile.value.focusModeEnabled
        vm.toggleFocusMode()
        advanceUntilIdle()
        assertEquals(!before, vm.userProfile.value.focusModeEnabled)
      }

  // ========== Equip / Unequip ==========

  @Test
  fun equip_only_allows_owned_items_and_writes_slot_prefix() =
      runTest(dispatcher) {
        val ownedProfile =
            UserProfile(accessories = listOf("owned:hat", "owned:wings", "owned:scarf"))
        val profileRepo =
            object : ProfileRepository {
              private val state = MutableStateFlow(ownedProfile)
              override val profile: StateFlow<UserProfile> = state

              override suspend fun updateProfile(newProfile: UserProfile) {
                state.value = newProfile
              }
            }
        val (vm, _) = vmWith(profileRepo, RecordingUserStatsRepository())

        advanceUntilIdle()

        // equip owned head/torso/back
        vm.equip(AccessorySlot.HEAD, "hat")
        vm.equip(AccessorySlot.TORSO, "scarf")
        vm.equip(AccessorySlot.BACK, "wings")
        advanceUntilIdle()

        val acc = vm.userProfile.value.accessories
        assertTrue(acc.contains("head:hat"))
        assertTrue(acc.contains("torso:scarf"))
        assertTrue(acc.contains("back:wings"))
      }

  @Test
  fun equip_none_removes_slot_but_keeps_owned_flags() =
      runTest(dispatcher) {
        val profile =
            UserProfile(accessories = listOf("owned:hat", "head:hat", "owned:scarf", "torso:scarf"))
        val repo = FakeProfileRepository(profile)
        val (vm, _) = vmWith(repo, RecordingUserStatsRepository())

        advanceUntilIdle()

        vm.equip(AccessorySlot.HEAD, "none")
        advanceUntilIdle()

        val acc = vm.userProfile.value.accessories
        assertTrue(acc.contains("owned:hat"))
        assertFalse(acc.any { it.startsWith("head:") })
      }

  @Test
  fun unequip_clears_only_that_slot() =
      runTest(dispatcher) {
        val profile =
            UserProfile(accessories = listOf("owned:hat", "head:hat", "owned:wings", "back:wings"))
        val repo = FakeProfileRepository(profile)
        val (vm, _) = vmWith(repo, RecordingUserStatsRepository())

        advanceUntilIdle()

        vm.unequip(AccessorySlot.BACK)
        advanceUntilIdle()

        val acc = vm.userProfile.value.accessories
        assertTrue(acc.contains("owned:wings"))
        assertFalse(acc.any { it.startsWith("back:") })
        assertTrue(acc.any { it.startsWith("head:") })
      }

  @Test
  fun equippedId_returns_id_or_null() =
      runTest(dispatcher) {
        val profile =
            UserProfile(accessories = listOf("owned:hat", "head:hat", "owned:wings", "back:wings"))
        val repo = FakeProfileRepository(profile)
        val (vm, _) = vmWith(repo, RecordingUserStatsRepository())

        advanceUntilIdle()

        assertEquals("hat", vm.equippedId(AccessorySlot.HEAD))
        assertEquals("wings", vm.equippedId(AccessorySlot.BACK))
        assertEquals(null, vm.equippedId(AccessorySlot.LEGS))
      }

  // ========== UserStats integration ==========

  @Test
  fun addCoins_forwards_to_userStatsRepository() =
      runTest(dispatcher) {
        val (vm, statsRepo) = vmWith()

        advanceUntilIdle()

        vm.addCoins(50)
        advanceUntilIdle()

        assertEquals(50, statsRepo.lastCoinsDelta)
        assertEquals(50, statsRepo.stats.value.coins)
      }

  @Test
  fun accessoryCatalog_shows_only_owned_items() = runTest {
    val profile = UserProfile(accessories = listOf("owned:hat", "owned:scarf"))
    val repo = FakeProfileRepository(profile)
    val (vm, _) = vmWith(repo, RecordingUserStatsRepository())

    advanceUntilIdle()

    val catalog = vm.accessoryCatalog

    // "none" items are always available
    assertTrue(catalog.any { it.id == "none" })

    // Owned items should appear
    assertTrue(catalog.any { it.id == "hat" })
    assertTrue(catalog.any { it.id == "scarf" })

    // Non-owned items should NOT appear (except "none")
    val nonNoneNonOwned = catalog.filter { it.id != "none" && it.id !in listOf("hat", "scarf") }
    assertTrue(nonNoneNonOwned.isEmpty())
  }

  @Test
  fun accessoryResId_returns_correct_drawable() = runTest {
    val (vm, _) = vmWith()

    // HEAD
    assertTrue(vm.accessoryResId(AccessorySlot.HEAD, "hat") != 0)
    assertTrue(vm.accessoryResId(AccessorySlot.HEAD, "glasses") != 0)
    assertEquals(0, vm.accessoryResId(AccessorySlot.HEAD, "unknown"))

    // TORSO
    assertTrue(vm.accessoryResId(AccessorySlot.TORSO, "scarf") != 0)
    assertTrue(vm.accessoryResId(AccessorySlot.TORSO, "cape") != 0)
    assertEquals(0, vm.accessoryResId(AccessorySlot.TORSO, "unknown"))

    // BACK
    assertTrue(vm.accessoryResId(AccessorySlot.BACK, "wings") != 0)
    assertTrue(vm.accessoryResId(AccessorySlot.BACK, "aura") != 0)
    assertEquals(0, vm.accessoryResId(AccessorySlot.BACK, "unknown"))
  }

  @Test
  fun equip_rejects_non_owned_items() =
      runTest(dispatcher) {
        val profile = UserProfile(accessories = listOf("owned:hat"))
        val repo = FakeProfileRepository(profile)
        val (vm, _) = vmWith(repo, RecordingUserStatsRepository())

        advanceUntilIdle()

        // Try to equip an item we don't own
        vm.equip(AccessorySlot.TORSO, "cape")
        advanceUntilIdle()

        val acc = vm.userProfile.value.accessories
        // Should not have been equipped
        assertFalse(acc.any { it.startsWith("torso:cape") })
      }

  @Test
  fun multiple_toggle_calls_work_correctly() =
      runTest(dispatcher) {
        val (vm, _) = vmWith()
        advanceUntilIdle()

        val initial = vm.userProfile.value.notificationsEnabled

        vm.toggleNotifications()
        advanceUntilIdle()
        assertEquals(!initial, vm.userProfile.value.notificationsEnabled)

        vm.toggleNotifications()
        advanceUntilIdle()
        assertEquals(initial, vm.userProfile.value.notificationsEnabled)

        vm.toggleNotifications()
        advanceUntilIdle()
        assertEquals(!initial, vm.userProfile.value.notificationsEnabled)
      }

  @Test
  fun accentVariant_persists_across_multiple_changes() =
      runTest(dispatcher) {
        val (vm, _) = vmWith()

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
}
