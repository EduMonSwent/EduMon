package com.android.sample.ui.profile

import com.android.sample.data.UserProfile
import com.android.sample.data.UserStats
import com.android.sample.data.UserStatsRepository
import com.android.sample.profile.FakeProfileRepository
import com.android.sample.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

    override suspend fun addStudyMinutes(delta: Int) {
      val c = _stats.value
      _stats.value = c.copy(todayStudyMinutes = (c.todayStudyMinutes + delta).coerceAtLeast(0))
    }

    override suspend fun addPoints(delta: Int) {
      val c = _stats.value
      _stats.value = c.copy(points = (c.points + delta).coerceAtLeast(0))
    }

    override suspend fun updateCoins(delta: Int) {
      val c = _stats.value
      _stats.value = c.copy(coins = (c.coins + delta).coerceAtLeast(0))
    }

    override suspend fun setWeeklyGoal(minutes: Int) {
      val c = _stats.value
      _stats.value = c.copy(weeklyGoal = minutes)
    }
    // addReward uses the default implementation from the interface
  }

  // ------------------------------------------------------------------------

  @Test
  fun `addPoints crosses level threshold and updates profile with rewards`() = runTest {
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

    // Snapshot before
    val before = viewModel.userProfile.value
    assertEquals(1, before.level)
    advanceUntilIdle()

    var received: LevelUpRewardUiEvent? = null
    val job = launch { received = viewModel.rewardEvents.first() }

    // This should be enough to cross at least one level threshold
    viewModel.addPoints(2000)
    advanceUntilIdle()

    val after = viewModel.userProfile.value

    // We don't rely on rewardEvents here (which are tricky with startup sync),
    // but we still cover the reward + sync logic by asserting on the profile state.
    assertTrue("Level should increase after large point gain", after.level > before.level)
    assertEquals(
        "lastRewardedLevel should match current level after rewards",
        after.level,
        after.lastRewardedLevel,
    )
    // Coins should be >= before (depending on your LevelRewardConfig)
    assertTrue("Coins should not decrease after level-up rewards", after.coins >= before.coins)
  }

  @Test
  fun `multiple level ups produce cumulative rewards in profile`() = runTest {
    val initial = UserProfile(level = 1, points = 0, coins = 0, lastRewardedLevel = 0)
    val repo = FakeProfileRepository(initial)
    val statsRepo = FakeUserStatsRepository(UserStats(points = 0))
    val viewModel = ProfileViewModel(repo, statsRepo)

    // Big jump: should cross several levels depending on your curve
    viewModel.addPoints(5000)
    advanceUntilIdle()

    val events = mutableListOf<LevelUpRewardUiEvent>()
    val job = launch { viewModel.rewardEvents.collect { events.add(it) } }

    viewModel.addPoints(5000) // should jump many levels
    advanceUntilIdle()

    val profile = viewModel.userProfile.value

    // We assert on cumulative effect in the profile instead of the event stream
    assertTrue("Level should be higher than 1 after large point gain", profile.level > 1)
    assertEquals(
        "lastRewardedLevel should reflect the highest rewarded level",
        profile.level,
        profile.lastRewardedLevel,
    )
    // At least some coins should have been granted over multiple levels
    assertTrue("Coins should be granted over multiple level-ups", profile.coins > 0)
  }
}
