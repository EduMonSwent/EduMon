package com.android.sample.feature.rewards

/** Domain model describing what is granted when a user reaches a given level. */
data class LevelReward(
    val level: Int,
    val coins: Int = 0,
    val accessoryIds: List<String> = emptyList(),
    val extraPoints: Int = 0,
    val extraStudyTimeMin: Int = 0
)

/**
 * Summary of what was actually granted when applying rewards for one or more levels. Useful for UI
 * feedback.
 */
data class GrantedRewardsSummary(
    val rewardedLevels: List<Int> = emptyList(),
    val coinsGranted: Int = 0,
    val accessoryIdsGranted: List<String> = emptyList(),
    val extraPointsGranted: Int = 0,
    val extraStudyTimeMinGranted: Int = 0
) {
  val isEmpty: Boolean
    get() =
        rewardedLevels.isEmpty() &&
            coinsGranted == 0 &&
            accessoryIdsGranted.isEmpty() &&
            extraPointsGranted == 0 &&
            extraStudyTimeMinGranted == 0
}
