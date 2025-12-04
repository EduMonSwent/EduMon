package com.android.sample.feature.rewards

/**
 * Provides the mapping between level and rewards.
 *
 * Later, we can load this from remote config / Firestore instead of hardcoding it.
 */
object LevelRewardConfig {
  val accessoriesByLevel: Map<Int, List<String>> =
      mapOf(
          2 to listOf("hat"),
          4 to listOf("glasses"),
          6 to listOf("scarf"),
          8 to listOf("cape"),
          10 to listOf("wings"),
          20 to listOf("aura"))

  private fun coinsForLevel(level: Int): Int = level * 2

  fun rewardForLevel(level: Int): LevelReward {
    val accessories = accessoriesByLevel[level] ?: emptyList()
    val coins = coinsForLevel(level)
    return LevelReward(level = level, coins = coins, accessoryIds = accessories)
  }
}
