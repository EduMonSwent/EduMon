package com.android.sample.ui.profile

import com.android.sample.data.UserProfile
import com.android.sample.data.UserStats
import com.android.sample.data.UserStatsRepository
import com.android.sample.profile.FakeProfileRepository
import com.android.sample.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelRewardIntegrationTest {

  @get:Rule val mainDispatcherRule = MainDispatcherRule()

  // Fake UserStatsRepository -----------------------------------------------
  private class FakeUserStatsRepository(initial: UserStats = UserStats()) : UserStatsRepository {
    private val _stats = MutableStateFlow(initial)
    override val stats: StateFlow<UserStats> = _stats

    override suspend fun start() {}

    override suspend fun addStudyMinutes(extraMinutes: Int) {}

    override suspend fun addPoints(delta: Int) {
      val c = _stats.value
      _stats.value = c.copy(points = (c.points + delta).coerceAtLeast(0))
    }

    override suspend fun updateCoins(delta: Int) {
      val c = _stats.value
      _stats.value = c.copy(coins = (c.coins + delta).coerceAtLeast(0))
    }

    override suspend fun setWeeklyGoal(goalMinutes: Int) {
      val c = _stats.value
      _stats.value = c.copy(weeklyGoal = goalMinutes)
    }
  }

  // ------------------------------------------------------------------------

  @Test
  fun `debugLevelUpForTests applies rewards and emits event`() = runTest {
    // Set lastRewardedLevel to 0 so that leveling up to 2 will trigger rewards for levels 1 and 2
    val initial = UserProfile(level = 1, coins = 0, points = 0, lastRewardedLevel = 0)
    val repo = FakeProfileRepository(initial)
    val statsRepo = FakeUserStatsRepository()
    val viewModel = ProfileViewModel(repo, statsRepo)

    advanceUntilIdle()

    assertEquals(1, viewModel.userProfile.value.level)

    var received: LevelUpRewardUiEvent? = null
    val job = launch { received = viewModel.rewardEvents.first() }

    // Force level up to 2 (debug path bypasses stats)
    viewModel.debugLevelUpForTests()
    advanceUntilIdle()
    job.join()

    assertNotNull("Expected reward event to be emitted", received)
    val event = received as LevelUpRewardUiEvent.RewardsGranted
    assertEquals(2, event.newLevel)

    // Verify rewards were granted
    assertTrue("Expected rewards to be granted", !event.summary.isEmpty)
    assertTrue(
        "Expected level 2 to be in rewarded levels", event.summary.rewardedLevels.contains(2))

    val profile = viewModel.userProfile.value
    assertEquals(2, profile.lastRewardedLevel)
  }

  @Test
  fun `addPoints crosses level threshold and triggers rewards`() = runTest {
    val initial = UserProfile(level = 1, points = 0, coins = 0, lastRewardedLevel = 0)
    val repo = FakeProfileRepository(initial)
    val statsRepo = FakeUserStatsRepository(UserStats(points = 0))
    val viewModel = ProfileViewModel(repo, statsRepo)

    advanceUntilIdle()

    var received: LevelUpRewardUiEvent? = null
    val job = launch { received = viewModel.rewardEvents.first() }

    // Enough to jump multiple levels depending on your leveling curve
    viewModel.addPoints(2000)
    advanceUntilIdle()
    job.join()

    val event = received as LevelUpRewardUiEvent.RewardsGranted
    assertFalse(event.summary.isEmpty)

    val profile = viewModel.userProfile.value
    assertEquals(profile.level, event.newLevel)
    assertEquals(profile.lastRewardedLevel, event.newLevel)
  }

  @Test
  fun `multiple level ups produce cumulative rewards`() = runTest {
    val initial = UserProfile(level = 1, points = 0, coins = 0, lastRewardedLevel = 0)
    val repo = FakeProfileRepository(initial)
    val statsRepo = FakeUserStatsRepository(UserStats(points = 0))
    val viewModel = ProfileViewModel(repo, statsRepo)

    advanceUntilIdle()

    val events = mutableListOf<LevelUpRewardUiEvent>()
    val job = launch { viewModel.rewardEvents.collect { events.add(it) } }

    viewModel.addPoints(5000) // should jump many levels
    advanceUntilIdle()

    assertTrue(events.isNotEmpty())
    val event = events.first() as LevelUpRewardUiEvent.RewardsGranted

    assertEquals(viewModel.userProfile.value.level, event.newLevel)
    assertTrue(event.summary.rewardedLevels.isNotEmpty())

    job.cancel()
  }
}
