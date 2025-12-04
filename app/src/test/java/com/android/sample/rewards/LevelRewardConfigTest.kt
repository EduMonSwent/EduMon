package com.android.sample.rewards

import com.android.sample.feature.rewards.LevelRewardConfig
import org.junit.Assert.*
import org.junit.Test

class LevelRewardConfigTest {

  @Test
  fun `config returns correct coin amounts`() {
    val r1 = LevelRewardConfig.rewardForLevel(1)
    val r2 = LevelRewardConfig.rewardForLevel(2)
    val r5 = LevelRewardConfig.rewardForLevel(5)

    assertEquals(2, r1.coins) // 1 * 2
    assertEquals(4, r2.coins) // 2 * 2
    assertEquals(10, r5.coins) // 5 * 2
  }

  @Test
  fun `config returns correct accessories for defined levels`() {
    val r2 = LevelRewardConfig.rewardForLevel(2)
    val r4 = LevelRewardConfig.rewardForLevel(4)
    val r6 = LevelRewardConfig.rewardForLevel(6)
    val r8 = LevelRewardConfig.rewardForLevel(8)
    val r10 = LevelRewardConfig.rewardForLevel(10)
    val r20 = LevelRewardConfig.rewardForLevel(20)

    assertEquals(listOf("hat"), r2.accessoryIds)
    assertEquals(listOf("glasses"), r4.accessoryIds)
    assertEquals(listOf("scarf"), r6.accessoryIds)
    assertEquals(listOf("cape"), r8.accessoryIds)
    assertEquals(listOf("wings"), r10.accessoryIds)
    assertEquals(listOf("aura"), r20.accessoryIds)
  }

  @Test
  fun `undefined levels return empty accessory list`() {
    val r1 = LevelRewardConfig.rewardForLevel(1)
    val r3 = LevelRewardConfig.rewardForLevel(3)
    val r7 = LevelRewardConfig.rewardForLevel(7)
    val r999 = LevelRewardConfig.rewardForLevel(999)

    assertTrue(r1.accessoryIds.isEmpty())
    assertTrue(r3.accessoryIds.isEmpty())
    assertTrue(r7.accessoryIds.isEmpty())
    assertTrue(r999.accessoryIds.isEmpty())
  }

  @Test
  fun `rewardForLevel never returns null`() {
    assertNotNull(LevelRewardConfig.rewardForLevel(1))
    assertNotNull(LevelRewardConfig.rewardForLevel(250))
    assertNotNull(LevelRewardConfig.rewardForLevel(9999))
  }
}
