package com.android.sample.data

// This code has been written partially using A.I (LLM).

import kotlinx.coroutines.flow.StateFlow

/**
 * Repository abstraction for the unified user stats document. This will be used by Home, Profile,
 * StudySession, Stats in PR2.
 */
interface UserStatsRepository {

  val stats: StateFlow<UserStats>

  /** Start listening to Firestore realtime updates for /users/{uid}/stats/stats. */
  fun start()

  /**
   * Add study minutes to the global counters. Implementations must handle daily reset for
   * todayStudyMinutes.
   */
  suspend fun addStudyMinutes(extraMinutes: Int)

  /** Update coin balance by a delta (can be negative). */
  suspend fun updateCoins(delta: Int)

  /** Set the weekly goal (in minutes). */
  suspend fun setWeeklyGoal(goalMinutes: Int)

  /** Add points (e.g. on Pomodoro completion). */
  suspend fun addPoints(delta: Int)
}
