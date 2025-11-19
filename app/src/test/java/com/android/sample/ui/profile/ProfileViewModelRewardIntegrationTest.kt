// app/src/test/java/com/android/sample/ui/profile/ProfileViewModelRewardIntegrationTest.kt
package com.android.sample.ui.profile

import com.android.sample.data.UserProfile
import com.android.sample.feature.rewards.LevelRewardConfig
import com.android.sample.profile.FakeProfileRepository
import com.android.sample.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

// The assistance of an AI tool (ChatGPT) was solicited in writing this test file.
@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelRewardIntegrationTest {

  @get:Rule val mainDispatcherRule = MainDispatcherRule()

  @Test
  fun `debugLevelUpForTests applies rewards and emits event`() = runTest {
    // Start with a user at level 1, lastRewardedLevel = 1, no coins
    val initialProfile = UserProfile(level = 1, coins = 0, points = 0, lastRewardedLevel = 1)
    val repo = FakeProfileRepository(initialProfile)
    val viewModel = ProfileViewModel(repository = repo)

    // Sanity check
    assertEquals(1, viewModel.userProfile.value.level)
    assertEquals(0, viewModel.userProfile.value.coins)

    // Collect the first reward event
    var receivedEvent: LevelUpRewardUiEvent? = null
    val job = launch { receivedEvent = viewModel.rewardEvents.first() }

    // Act: trigger a level up -> goes from 1 to 2
    viewModel.debugLevelUpForTests()

    // Wait for the event
    job.join()

    val event = receivedEvent
    assertNotNull("Expected a reward event to be emitted", event)
    assertTrue(event is LevelUpRewardUiEvent.RewardsGranted)
    event as LevelUpRewardUiEvent.RewardsGranted

    assertEquals(2, event.newLevel)
    assertFalse(event.summary.isEmpty)
    assertTrue(event.summary.rewardedLevels.contains(2))

    val level2Reward = LevelRewardConfig.rewardForLevel(2)!!
    assertEquals(level2Reward.coins, event.summary.coinsGranted)
    assertEquals(level2Reward.extraPoints, event.summary.extraPointsGranted)

    val current = viewModel.userProfile.value
    assertEquals(2, current.level)
    assertEquals(2, current.lastRewardedLevel)
    assertEquals(level2Reward.coins, current.coins)
    assertEquals(level2Reward.extraPoints, current.points)
  }

  @Test
  fun `applyProfileWithPotentialRewards updates profile and repo when level does not increase`() =
      runTest {
        // given: a user at level 3 with some points
        val initialProfile = UserProfile(level = 3, points = 100, coins = 0, lastRewardedLevel = 3)
        val repo = FakeProfileRepository(initialProfile)
        val viewModel = ProfileViewModel(repository = repo)

        // when: we apply a change that modifies points but NOT the level
        viewModel.debugNoLevelChangeForTests()

        // then: profile is updated locally
        val updated = viewModel.userProfile.value
        assertEquals(3, updated.level) // level unchanged
        assertEquals(110, updated.points) // points increased by +10
        assertEquals(3, updated.lastRewardedLevel) // reward tracking unchanged
        advanceUntilIdle()
        // and: repository has also been updated via pushProfile()
        assertEquals(updated, repo.profile.value)
      }

  @Test
  fun `addPoints increases points but not level when threshold not reached`() = runTest {
    // given: user at level 1 with 100 points
    val initial = UserProfile(level = 1, points = 100, coins = 0, lastRewardedLevel = 1)
    val repo = FakeProfileRepository(initial)
    val viewModel = ProfileViewModel(repository = repo)

    // when: we add less than 200 points (so total < 300)
    viewModel.addPoints(50) // 100 -> 150

    val updated = viewModel.userProfile.value

    // then: points increased, level unchanged
    assertEquals(150, updated.points)
    assertEquals(1, updated.level)
    assertEquals(1, updated.lastRewardedLevel)
    advanceUntilIdle()

    // no rewards should be granted; profile pushed to repo
    assertEquals(updated, repo.profile.value)
  }

  @Test
  fun `addPoints crosses threshold and triggers level up and rewards`() = runTest {
    // Start with 0 points at level 1, and no rewarded levels yet
    val initial = UserProfile(level = 1, points = 0, coins = 0, lastRewardedLevel = 0)
    val repo = FakeProfileRepository(initial)
    val viewModel = ProfileViewModel(repository = repo)

    // Listen for first reward event
    var receivedEvent: LevelUpRewardUiEvent? = null
    val job = launch { receivedEvent = viewModel.rewardEvents.first() }

    // when: we add 300 points -> should give level 2
    viewModel.addPoints(300)

    // wait for event emission
    job.join()

    val event = receivedEvent
    assertNotNull("Expected a reward event after level-up", event)
    assertTrue(event is LevelUpRewardUiEvent.RewardsGranted)
    event as LevelUpRewardUiEvent.RewardsGranted

    // then: level bumped and rewards applied
    assertEquals(2, event.newLevel)
    assertFalse(event.summary.isEmpty)

    // Newly rewarded levels are 1 and 2 (because lastRewardedLevel started at 0)
    assertTrue(event.summary.rewardedLevels.contains(1))
    assertTrue(event.summary.rewardedLevels.contains(2))

    // Compute expected total rewards for levels 1 + 2
    val level1Reward = LevelRewardConfig.rewardForLevel(1)!!
    val level2Reward = LevelRewardConfig.rewardForLevel(2)!!

    val expectedCoins = level1Reward.coins + level2Reward.coins
    val expectedExtraPoints = level1Reward.extraPoints + level2Reward.extraPoints

    // Summary should reflect combined rewards
    assertEquals(expectedCoins, event.summary.coinsGranted)
    assertEquals(expectedExtraPoints, event.summary.extraPointsGranted)

    // Check profile state
    val current = viewModel.userProfile.value
    assertEquals(2, current.level)
    assertEquals(2, current.lastRewardedLevel)

    // Points: base (300 from addPoints) + extraPoints from level 1 + 2
    assertEquals(300 + expectedExtraPoints, current.points)

    // Coins: sum of both levels
    assertEquals(expectedCoins, current.coins)
  }
}
