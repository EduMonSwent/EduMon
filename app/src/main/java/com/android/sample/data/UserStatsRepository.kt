package com.android.sample.data

import kotlinx.coroutines.flow.StateFlow

interface UserStatsRepository {
  val stats: StateFlow<UserStats>

  /** Idempotent: safe to call multiple times. */
  suspend fun start()

  /** Adds study minutes for “today” and updates streak. */
  suspend fun addStudyMinutes(delta: Int)

  /** Adds points (can be negative). */
  suspend fun addPoints(delta: Int)

  /** Adds coins (can be negative, but never goes below 0). */
  suspend fun updateCoins(delta: Int)

  /** Sets the weekly goal in minutes. */
  suspend fun setWeeklyGoal(minutes: Int)

  /** Adds all pomodoro rewards in a single atomic update. */
  suspend fun addReward(minutes: Int = 0, points: Int = 0, coins: Int = 0) {
    // Default implementation calls individual methods
    // Production repo should override for single atomic write
    if (minutes > 0) addStudyMinutes(minutes)
    if (points != 0) addPoints(points)
    if (coins != 0) updateCoins(coins)
  }
}
