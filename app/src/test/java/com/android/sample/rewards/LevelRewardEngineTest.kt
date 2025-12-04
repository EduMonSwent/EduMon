// app/src/test/java/com/android/sample/rewards/LevelRewardEngineTest.kt
package com.android.sample.rewards

import com.android.sample.data.UserProfile
import com.android.sample.feature.rewards.LevelRewardConfig
import com.android.sample.feature.rewards.LevelRewardEngine
import com.android.sample.ui.stats.model.StudyStats
import org.junit.Assert.*
import org.junit.Test

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
    val new = old.copy(points = 250)

    val result = engine.applyLevelUpRewards(old, new)

    assertSame(new, result.updatedProfile)
    assertTrue(result.summary.isEmpty)
  }

  @Test
  fun `single level up - from 1 to 2 - awards correct reward`() {
    val old = baseProfile(level = 1, coins = 0, points = 0, lastRewardedLevel = 1)
    val new = old.copy(level = 2)

    val result = engine.applyLevelUpRewards(old, new)

    val updated = result.updatedProfile
    val summary = result.summary

    assertEquals(listOf(2), summary.rewardedLevels)
    assertFalse(summary.isEmpty)

    val reward = LevelRewardConfig.rewardForLevel(2)
    assertEquals(4, reward.coins)
    assertEquals(4, summary.coinsGranted)

    assertEquals(4, updated.coins)
    assertTrue(summary.accessoryIdsGranted.contains("hat"))
    assertTrue(updated.accessories.any { it.contains("hat") })

    assertEquals(2, updated.lastRewardedLevel)
  }

  @Test
  fun `multiple level ups - 1 to 6 - rewards only configured levels`() {
    val old = baseProfile(level = 1, coins = 0, lastRewardedLevel = 1)
    val new = old.copy(level = 6)

    val result = engine.applyLevelUpRewards(old, new)

    val summary = result.summary
    val updated = result.updatedProfile

    // Levels rewarded: 2, 3, 4, 5, 6
    assertEquals(listOf(2, 3, 4, 5, 6), summary.rewardedLevels)

    // Coins = sum of (level * 2) for rewarded levels
    val expectedCoins = (2 * 2) + (3 * 2) + (4 * 2) + (5 * 2) + (6 * 2)
    assertEquals(expectedCoins, summary.coinsGranted)
    assertEquals(expectedCoins, updated.coins)

    // Accessories only for 2,4,6
    assertTrue(summary.accessoryIdsGranted.contains("hat"))
    assertTrue(summary.accessoryIdsGranted.contains("glasses"))
    assertTrue(summary.accessoryIdsGranted.contains("scarf"))

    assertEquals(6, updated.lastRewardedLevel)
  }

  @Test
  fun `second call with same profile does not regrant rewards`() {
    val old = baseProfile(level = 1, lastRewardedLevel = 1)
    val new = old.copy(level = 2)

    val first = engine.applyLevelUpRewards(old, new)
    val afterFirst = first.updatedProfile

    val second = engine.applyLevelUpRewards(afterFirst, afterFirst)

    assertTrue(second.summary.isEmpty)
    assertEquals(afterFirst, second.updatedProfile)
  }

  @Test
  fun `no new levels when lastRewardedLevel already ahead`() {
    val old = baseProfile(level = 5, coins = 100, lastRewardedLevel = 6)
    val new = old.copy(points = 600)

    val result = engine.applyLevelUpRewards(old, new)

    assertTrue(result.summary.isEmpty)
    assertEquals(new, result.updatedProfile)
  }

  @Test
  fun `accessory rewards are not duplicated if already owned`() {
    val old = baseProfile(level = 1, accessories = listOf("owned:hat"), lastRewardedLevel = 1)
    val new = old.copy(level = 2)

    val result = engine.applyLevelUpRewards(old, new)

    val updated = result.updatedProfile
    val summary = result.summary

    assertFalse(summary.accessoryIdsGranted.contains("hat"))

    val hatCount = updated.accessories.count { it.contains("hat") }
    assertEquals(1, hatCount)
  }

  @Test
  fun `levels above configured ones still give coin rewards`() {
    val old = baseProfile(level = 4, coins = 0, lastRewardedLevel = 4)
    val new = old.copy(level = 7)

    val result = engine.applyLevelUpRewards(old, new)

    val summary = result.summary
    val updated = result.updatedProfile

    assertEquals(listOf(5, 6, 7), summary.rewardedLevels)

    val expectedCoins = (5 * 2) + (6 * 2) + (7 * 2)
    assertEquals(expectedCoins, summary.coinsGranted)
    assertEquals(expectedCoins, updated.coins)

    assertEquals(7, updated.lastRewardedLevel)
  }
}
