package com.android.sample.rewards

import com.android.sample.feature.rewards.GrantedRewardsSummary
import com.android.sample.feature.rewards.LevelReward
import org.junit.Assert.*
import org.junit.Test

// The assistance of an AI tool (ChatGPT) was solicited in writing this test file.
class LevelRewardModelTest {

  @Test
  fun `empty summary is reported as empty`() {
    val summary = GrantedRewardsSummary()

    assertTrue(summary.isEmpty)
    assertTrue(summary.rewardedLevels.isEmpty())
    assertEquals(0, summary.coinsGranted)
    assertTrue(summary.accessoryIdsGranted.isEmpty())
  }

  @Test
  fun `summary with coins is not empty`() {
    val summary = GrantedRewardsSummary(coinsGranted = 4) // e.g. level 2 → coins=4

    assertFalse(summary.isEmpty)
    assertEquals(4, summary.coinsGranted)
  }

  @Test
  fun `summary with levels or accessories is not empty`() {
    val summary1 = GrantedRewardsSummary(
      rewardedLevels = listOf(2, 3)
    )
    assertFalse(summary1.isEmpty)

    val summary2 = GrantedRewardsSummary(
      accessoryIdsGranted = listOf("hat")  // level 2 reward in new config
    )
    assertFalse(summary2.isEmpty)
  }


  @Test
  fun `level reward stores its fields correctly`() {
    val reward = LevelReward(
      level = 4,
      coins = 8,                   // level * 2
      accessoryIds = listOf("glasses")
    )

    assertEquals(4, reward.level)
    assertEquals(8, reward.coins)
    assertEquals(listOf("glasses"), reward.accessoryIds)
  }
}
