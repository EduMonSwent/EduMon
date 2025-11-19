package com.android.sample.feature.rewards

/**
 * Provides the mapping between level and rewards.
 *
 * Later, we can load this from remote config / Firestore instead of hardcoding it.
 */
object LevelRewardConfig {
  val rewardsByLevel: Map<Int, LevelReward> =
      listOf(
              LevelReward(level = 1, coins = 50, accessoryIds = listOf("badge"), extraPoints = 0),
              LevelReward(level = 2, coins = 75, accessoryIds = listOf("scarf"), extraPoints = 50),
              LevelReward(
                  level = 3, coins = 100, accessoryIds = listOf("boots"), extraPoints = 100),
              LevelReward(
                  level = 4, coins = 150, accessoryIds = listOf("armor"), extraPoints = 150),
              LevelReward(
                  level = 5, coins = 200, accessoryIds = listOf("rocket"), extraPoints = 200),
          )
          .associateBy { it.level }

  fun rewardForLevel(level: Int): LevelReward? = rewardsByLevel[level]
}
