package com.android.sample.data

// This code has been written partially using A.I (LLM).

import kotlinx.coroutines.flow.StateFlow

/**
 * Repository abstraction for the unified user stats stored in Firestore. All screens (Home,
 * Profile, Stats, StudySession) must go through this.
 */
interface UserStatsRepository {

  val stats: StateFlow<UserStats>

  /**
   * Start listening to Firestore changes for /users/{uid}/stats/stats. This should be called once
   * per app lifecycle (e.g. in AppRepositories).
   */
  fun start()

  /**
   * Add study minutes to the global counters. Handles daily reset for todayStudyMinutes based on
   * lastUpdated.
   */
  suspend fun addStudyMinutes(extraMinutes: Int)

  /** Update coin balance by a delta (can be negative). */
  suspend fun updateCoins(delta: Int)

  /** Set the weekly goal in minutes. */
  suspend fun setWeeklyGoal(goalMinutes: Int)

  /** Add points (e.g. reward on Pomodoro completion). */
  suspend fun addPoints(delta: Int)

  /** Update the number of completed goals. */
  suspend fun updateCompletedGoals(count: Int)

  /** Increments the number of completed pomodoros for today. */
  suspend fun incrementCompletedPomodoros()
}
