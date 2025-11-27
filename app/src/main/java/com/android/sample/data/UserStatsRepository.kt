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
}
