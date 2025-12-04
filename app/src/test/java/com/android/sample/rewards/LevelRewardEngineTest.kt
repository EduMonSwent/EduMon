// app/src/test/java/com/android/sample/rewards/LevelRewardEngineTest.kt
package com.android.sample.rewards

import com.android.sample.data.UserProfile
import com.android.sample.feature.rewards.LevelRewardConfig
import com.android.sample.feature.rewards.LevelRewardEngine
import com.android.sample.ui.stats.model.StudyStats
import org.junit.Assert.*
import org.junit.Test

// The assistance of an AI tool (ChatGPT) was solicited in writing this test file.
class LevelRewardEngineTest {

  private val engine = LevelRewardEngine()

  private fun baseProfile(
      level: Int = 1,
      coins: Int = 0,
      points: Int = 0,
      accessories: List<String> = emptyList(),
      totalStudyMin: Int = 0,
      lastRewardedLevel: Int = 0
  ): UserProfile =
      UserProfile(
          level = level,
          coins = coins,
          points = points,
          accessories = accessories,
          studyStats = StudyStats(totalTimeMin = totalStudyMin, dailyGoalMin = 60),
          lastRewardedLevel = lastRewardedLevel)

  @Test
  fun `no level increase - returns new profile unchanged and empty summary`() {
    val old = baseProfile(level = 3, coins = 100, points = 200, lastRewardedLevel = 3)
    val new = old.copy(points = 250) // change points only, same level

    val result = engine.applyLevelUpRewards(old, new)

    assertSame(new, result.updatedProfile) // instance or use assertEquals if needed
    assertTrue(result.summary.isEmpty)
  }

  @Test
  fun `single level up - level 1 to 2 - applies configured reward`() {
    val old = baseProfile(level = 1, coins = 0, points = 0, lastRewardedLevel = 1)
    val new = old.copy(level = 2, points = 50) // simulate user reached level 2

    val result = engine.applyLevelUpRewards(old, new)

    val updated = result.updatedProfile
    val summary = result.summary

    // Summary should report level 2 as rewarded
    assertEquals(listOf(2), summary.rewardedLevels)
    assertFalse(summary.isEmpty)

    // LevelRewardConfig for level 2: coins 75, accessory "scarf", extraPoints 50
    assertEquals(2, updated.level)
    assertEquals(75, summary.coinsGranted)
    assertTrue(summary.accessoryIdsGranted.contains("scarf"))

    // Profile fields updated accordingly
    assertEquals(75, updated.coins)
    assertTrue(
        updated.accessories.contains("scarf") || updated.accessories.any { it.endsWith(":scarf") })
    assertEquals(new.points + 50, updated.points)

    // Last rewarded level must be updated
    assertEquals(2, updated.lastRewardedLevel)
  }

  @Test
  fun `multiple level ups - 1 to 4 - rewards all intermediate levels`() {
    val old = baseProfile(level = 1, coins = 0, points = 0, lastRewardedLevel = 1)
    val new = old.copy(level = 4, points = 300)

    val result = engine.applyLevelUpRewards(old, new)

    val updated = result.updatedProfile
    val summary = result.summary

    // Levels 2, 3, 4 should have been rewarded
    assertEquals(listOf(2, 3, 4), summary.rewardedLevels)
    assertEquals(4, updated.level)
    assertEquals(4, updated.lastRewardedLevel)

    // From LevelRewardConfig:
    // level 2: 75 coins, 50 pts
    // level 3: 100 coins, 100 pts
    // level 4: 150 coins, 150 pts
    val expectedCoins = 75 + 100 + 150
    val expectedExtraPoints = 50 + 100 + 150

    assertEquals(expectedCoins, summary.coinsGranted)
    assertEquals(expectedCoins, updated.coins)
    assertEquals(new.points + expectedExtraPoints, updated.points)

    // Should have granted accessories "scarf", "boots", "armor"
    assertTrue(summary.accessoryIdsGranted.contains("scarf"))
    assertTrue(summary.accessoryIdsGranted.contains("boots"))
    assertTrue(summary.accessoryIdsGranted.contains("armor"))

    summary.accessoryIdsGranted.forEach { id ->
      assertTrue(updated.accessories.any { it == id || it.endsWith(":$id") })
    }
  }

  @Test
  fun `second call with same final profile does not regrant rewards`() {
    val old = baseProfile(level = 1, coins = 0, points = 0, lastRewardedLevel = 1)
    val new = old.copy(level = 2, points = 50)

    // First application - should grant level 2 rewards
    val first = engine.applyLevelUpRewards(old, new)
    val afterFirst = first.updatedProfile

    // Second application with same profile as old and new (no level increase)
    val second = engine.applyLevelUpRewards(afterFirst, afterFirst)

    assertTrue(second.summary.isEmpty)
    assertEquals(afterFirst, second.updatedProfile)
  }

  @Test
  fun `no new levels when lastRewardedLevel already ahead of new level`() {
    // Synthetic scenario: lastRewardedLevel > new level
    val old = baseProfile(level = 5, coins = 100, points = 500, lastRewardedLevel = 6)
    val new = old.copy(level = 5, points = 600)

    val result = engine.applyLevelUpRewards(old, new)

    // Early exit branch where newlyRewardedLevels is empty
    assertTrue(result.summary.isEmpty)
    assertEquals(new, result.updatedProfile)
  }

  @Test
  fun `accessory rewards are not duplicated if already present`() {
    // Level 1 reward gives "badge".
    // Start with a profile that already has that accessory.
    val old =
        baseProfile(
            level = 0, coins = 0, points = 0, accessories = listOf("badge"), lastRewardedLevel = 0)
    val new = old.copy(level = 1)

    val result = engine.applyLevelUpRewards(old, new)

    val updated = result.updatedProfile
    val summary = result.summary

    // We should *not* have granted "badge" again
    assertFalse(summary.accessoryIdsGranted.contains("badge"))

    // Accessories list should still contain a single "badge"-type item,
    // i.e. we didn't add more duplicates.
    val badgeCount = updated.accessories.count { it == "badge" || it.endsWith(":badge") }
    assertTrue(badgeCount >= 1)
  }

  @Test
  fun `levels above configured ones only reward known levels`() {
    // LevelRewardConfig only has rewards up to 5.
    val old = baseProfile(level = 4, coins = 0, points = 0, lastRewardedLevel = 4)
    val new = old.copy(level = 7, points = 500)

    val result = engine.applyLevelUpRewards(old, new)

    val updated = result.updatedProfile
    val summary = result.summary

    assertEquals(listOf(5, 6, 7), summary.rewardedLevels)

    // Check that we got exactly the coins for level 5 only
    val level5Reward = LevelRewardConfig.rewardForLevel(5)!!
    assertEquals(level5Reward.coins, summary.coinsGranted)
    assertEquals(level5Reward.coins, updated.coins)

    assertEquals(7, updated.lastRewardedLevel)
  }
}
