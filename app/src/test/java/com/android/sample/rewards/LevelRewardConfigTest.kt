// app/src/test/java/com/android/sample/rewards/LevelRewardConfigTest.kt
package com.android.sample.rewards

import com.android.sample.feature.rewards.LevelRewardConfig
import org.junit.Assert.*
import org.junit.Test

// The assistance of an AI tool (ChatGPT) was solicited in writing this test file.
class LevelRewardConfigTest {

  @Test
  fun `config contains rewards for defined levels`() {
    // The exact levels are from your LevelRewardConfig object
    val reward1 = LevelRewardConfig.rewardForLevel(1)
    val reward2 = LevelRewardConfig.rewardForLevel(2)
    val reward3 = LevelRewardConfig.rewardForLevel(3)
    val reward4 = LevelRewardConfig.rewardForLevel(4)
    val reward5 = LevelRewardConfig.rewardForLevel(5)

    assertNotNull(reward1)
    assertNotNull(reward2)
    assertNotNull(reward3)
    assertNotNull(reward4)
    assertNotNull(reward5)

    // Spot-check a couple of fields to ensure mapping is correct
    assertEquals(50, reward1!!.coins)
    assertTrue(reward1.accessoryIds.contains("badge"))

    assertEquals(75, reward2!!.coins)
    assertTrue(reward2.accessoryIds.contains("scarf"))
    assertEquals(50, reward2.extraPoints)

    assertEquals(100, reward3!!.coins)
    assertTrue(reward3.accessoryIds.contains("boots"))
    assertEquals(100, reward3.extraPoints)
  }

  @Test
  fun `config returns null for non existing level`() {
    val reward = LevelRewardConfig.rewardForLevel(999)
    assertNull(reward)
  }
}
