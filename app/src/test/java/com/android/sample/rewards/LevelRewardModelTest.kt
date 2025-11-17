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
    assertEquals(0, summary.extraPointsGranted)
    assertEquals(0, summary.extraStudyTimeMinGranted)
  }

  @Test
  fun `summary with coins is not empty`() {
    val summary = GrantedRewardsSummary(coinsGranted = 100)

    assertFalse(summary.isEmpty)
    assertEquals(100, summary.coinsGranted)
  }

  @Test
  fun `summary with levels or accessories is not empty`() {
    val summary1 =
        GrantedRewardsSummary(
            rewardedLevels = listOf(2, 3),
        )
    assertFalse(summary1.isEmpty)

    val summary2 =
        GrantedRewardsSummary(
            accessoryIdsGranted = listOf("badge"),
        )
    assertFalse(summary2.isEmpty)
  }

  @Test
  fun `summary with extra stats is not empty`() {
    val summary = GrantedRewardsSummary(extraPointsGranted = 50, extraStudyTimeMinGranted = 10)

    assertFalse(summary.isEmpty)
    assertEquals(50, summary.extraPointsGranted)
    assertEquals(10, summary.extraStudyTimeMinGranted)
  }

  @Test
  fun `level reward stores its fields correctly`() {
    val reward =
        LevelReward(
            level = 3,
            coins = 100,
            accessoryIds = listOf("boots"),
            extraPoints = 200,
            extraStudyTimeMin = 15)

    assertEquals(3, reward.level)
    assertEquals(100, reward.coins)
    assertEquals(listOf("boots"), reward.accessoryIds)
    assertEquals(200, reward.extraPoints)
    assertEquals(15, reward.extraStudyTimeMin)
  }
}
