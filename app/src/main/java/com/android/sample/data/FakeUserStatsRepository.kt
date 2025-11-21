package com.android.sample.data

// This code has been written partially using A.I (LLM).

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** In-memory fake implementation of UserStatsRepository for tests and previews. */
class FakeUserStatsRepository(initial: UserStats = UserStats()) : UserStatsRepository {

  private companion object {
    private const val DEFAULT_INT_DELTA = 0
  }

  private val _stats = MutableStateFlow(initial)
  override val stats: StateFlow<UserStats> = _stats

  override fun start() {
    // No-op: everything is in memory.
  }

  override suspend fun addStudyMinutes(extraMinutes: Int) {
    if (extraMinutes <= DEFAULT_INT_DELTA) {
      return
    }
    val current = _stats.value
    _stats.value =
        current.copy(
            totalStudyMinutes = current.totalStudyMinutes + extraMinutes,
            todayStudyMinutes = current.todayStudyMinutes + extraMinutes)
  }

  override suspend fun updateCoins(delta: Int) {
    if (delta == DEFAULT_INT_DELTA) {
      return
    }
    val current = _stats.value
    _stats.value = current.copy(coins = current.coins + delta)
  }

  override suspend fun setWeeklyGoal(goalMinutes: Int) {
    val current = _stats.value
    _stats.value = current.copy(weeklyGoal = goalMinutes)
  }

  override suspend fun addPoints(delta: Int) {
    if (delta == DEFAULT_INT_DELTA) {
      return
    }
    val current = _stats.value
    _stats.value = current.copy(points = current.points + delta)
  }
}
