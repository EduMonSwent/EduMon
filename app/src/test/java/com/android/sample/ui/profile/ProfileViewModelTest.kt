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
import org.junit.Assert.assertNotEquals
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
        val (vm, _) = vmWith()
        val base = vm.accentEffective.value
        vm.setAccentVariant(AccentVariant.Light)
        advanceUntilIdle()
        val light = vm.accentEffective.value
        assertNotEquals(base, light)
      }

  // ========== Toggle Functions ==========

  @Test
  fun toggleNotifications_flips_flag() =
      runTest(dispatcher) {
        val (vm, _) = vmWith()
        val before = vm.userProfile.value.notificationsEnabled
        vm.toggleNotifications()
        advanceUntilIdle()
        assertEquals(!before, vm.userProfile.value.notificationsEnabled)
      }

  @Test
  fun toggleLocation_flips_flag() =
      runTest(dispatcher) {
        val (vm, _) = vmWith()
        val before = vm.userProfile.value.locationEnabled
        vm.toggleLocation()
        advanceUntilIdle()
        assertEquals(!before, vm.userProfile.value.locationEnabled)
      }

  @Test
  fun toggleFocusMode_flips_flag() =
      runTest(dispatcher) {
        val (vm, _) = vmWith()
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

              suspend fun increaseStudyTimeBy(time: Int) {}

              suspend fun increaseStreakIfCorrect() {}
            }
        val (vm, _) = vmWith(profileRepo, RecordingUserStatsRepository())

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

        assertEquals("hat", vm.equippedId(AccessorySlot.HEAD))
        assertEquals("wings", vm.equippedId(AccessorySlot.BACK))
        assertEquals(null, vm.equippedId(AccessorySlot.LEGS))
      }

  // ========== UserStats integration ==========

  @Test
  fun addCoins_forwards_to_userStatsRepository() =
      runTest(dispatcher) {
        val (vm, statsRepo) = vmWith()
        vm.addCoins(50)
        advanceUntilIdle()

        assertEquals(50, statsRepo.lastCoinsDelta)
        assertEquals(50, statsRepo.stats.value.coins)
      }
}
