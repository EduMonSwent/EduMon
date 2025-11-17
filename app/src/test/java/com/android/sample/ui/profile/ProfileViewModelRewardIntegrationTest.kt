// app/src/test/java/com/android/sample/ui/profile/ProfileViewModelRewardIntegrationTest.kt
package com.android.sample.ui.profile

import com.android.sample.data.UserProfile
import com.android.sample.feature.rewards.LevelRewardConfig
import com.android.sample.profile.FakeProfileRepository
import com.android.sample.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

// The assistance of an AI tool (ChatGPT) was solicited in writing this test file.
@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelRewardIntegrationTest {

  // 👇 NEW: set a Test Main dispatcher for this class
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
}
