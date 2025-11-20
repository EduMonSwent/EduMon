package com.android.sample.data

// This code has been written partially using A.I (LLM).

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** In-memory fake implementation of UserStatsRepository for previews/tests. */
class FakeUserStatsRepository(initialStats: UserStats = UserStats()) : UserStatsRepository {

  private companion object {
    private const val DEFAULT_INT_DELTA = 0
  }

  private val _stats = MutableStateFlow(initialStats)
  override val stats: StateFlow<UserStats> = _stats

  override fun start() {
    // No-op for fake repository.
  }

  override suspend fun addStudyMinutes(extraMinutes: Int) {
    if (extraMinutes <= DEFAULT_INT_DELTA) {
      return
    }

    val currentStats = _stats.value
    val updatedStats =
        currentStats.copy(
            totalStudyMinutes = currentStats.totalStudyMinutes + extraMinutes,
            todayStudyMinutes = currentStats.todayStudyMinutes + extraMinutes)
    _stats.value = updatedStats
  }

  override suspend fun updateCoins(delta: Int) {
    if (delta == DEFAULT_INT_DELTA) {
      return
    }

    val currentStats = _stats.value
    val updatedStats = currentStats.copy(coins = currentStats.coins + delta)
    _stats.value = updatedStats
  }

  override suspend fun setWeeklyGoal(goalMinutes: Int) {
    val currentStats = _stats.value
    val updatedStats = currentStats.copy(weeklyGoal = goalMinutes)
    _stats.value = updatedStats
  }

  override suspend fun addPoints(delta: Int) {
    if (delta == DEFAULT_INT_DELTA) {
      return
    }

    val currentStats = _stats.value
    val updatedStats = currentStats.copy(points = currentStats.points + delta)
    _stats.value = updatedStats
  }

  override suspend fun updateCompletedGoals(count: Int) {
    val currentStats = _stats.value
    val updatedStats = currentStats.copy(completedGoals = count)
    _stats.value = updatedStats
  }

  override suspend fun incrementCompletedPomodoros() {
    val currentStats = _stats.value
    val updatedStats =
        currentStats.copy(todayCompletedPomodoros = currentStats.todayCompletedPomodoros + 1)
    _stats.value = updatedStats
  }
}
