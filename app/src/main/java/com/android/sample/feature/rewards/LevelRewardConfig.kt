package com.android.sample.feature.rewards

private const val COINS_PER_LEVEL_UPGRADE = 2

private const val HAT = "hat"

private const val GLASSES = "glasses"

private const val SCARF = "scarf"

private const val CAPE = "cape"

private const val WINGS = "wings"

private const val AURA = "aura"

/**
 * Provides the mapping between level and rewards.
 *
 * Later, we can load this from remote config / Firestore instead of hardcoding it.
 */
object LevelRewardConfig {
  val accessoriesByLevel: Map<Int, List<String>> =
      mapOf(
          COINS_PER_LEVEL_UPGRADE to listOf(HAT),
          4 to listOf(GLASSES),
          6 to listOf(SCARF),
          8 to listOf(CAPE),
          10 to listOf(WINGS),
          20 to listOf(AURA))

  private fun coinsForLevel(level: Int): Int = level * COINS_PER_LEVEL_UPGRADE

  fun rewardForLevel(level: Int): LevelReward {
    val accessories = accessoriesByLevel[level] ?: emptyList()
    val coins = coinsForLevel(level)
    return LevelReward(level = level, coins = coins, accessoryIds = accessories)
  }
}
