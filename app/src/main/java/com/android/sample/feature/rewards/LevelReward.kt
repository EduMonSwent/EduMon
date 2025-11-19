package com.android.sample.feature.rewards

/** Domain model describing what is granted when a user reaches a given level. */
data class LevelReward(
    val level: Int,
    val coins: Int = 0,
    val accessoryIds: List<String> = emptyList(),
    val extraPoints: Int = 0
)

/**
 * Summary of what was actually granted when applying rewards for one or more levels. It's useful
 * for UI feedback.
 */
data class GrantedRewardsSummary(
    val rewardedLevels: List<Int> = emptyList(),
    val coinsGranted: Int = 0,
    val accessoryIdsGranted: List<String> = emptyList(),
    val extraPointsGranted: Int = 0
) {
  /**
   * Returns true when this summary represents “no rewards granted”. When all of these fields are
   * empty/zero, it means the level-up resulted in no actual rewards. UI components can use this to
   * avoid showing a reward snackbar in these cases.
   */
  val isEmpty: Boolean
    get() =
        rewardedLevels.isEmpty() &&
            coinsGranted == 0 &&
            accessoryIdsGranted.isEmpty() &&
            extraPointsGranted == 0
}
