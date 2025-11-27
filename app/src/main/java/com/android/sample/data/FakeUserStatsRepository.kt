package com.android.sample.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** In-memory fake implementation of UserStatsRepository for tests and previews. */
class FakeUserStatsRepository(initial: UserStats = UserStats()) : UserStatsRepository {

  private val _stats = MutableStateFlow(initial)
  override val stats: StateFlow<UserStats> = _stats

  override suspend fun start() {
    // no-op for fakes
  }

  override suspend fun addStudyMinutes(extraMinutes: Int) {
    if (extraMinutes <= 0) return

    val now = java.time.LocalDate.now()
    val current = _stats.value

    // apply simple rollover in the fake as well so UI behaves like real repo
    val rolled = applyDailyRollover(current, now)

    val firstToday = rolled.todayStudyMinutes == 0
    val updated =
        rolled.copy(
            totalStudyMinutes = rolled.totalStudyMinutes + extraMinutes,
            todayStudyMinutes = rolled.todayStudyMinutes + extraMinutes,
            streak = rolled.streak + if (firstToday) 1 else 0,
            lastStudyDateEpochDay = now.toEpochDay())

    _stats.value = updated
  }

  override suspend fun updateCoins(delta: Int) {
    if (delta == 0) return
    val c = _stats.value
    _stats.value = c.copy(coins = (c.coins + delta).coerceAtLeast(0))
  }

  override suspend fun setWeeklyGoal(goalMinutes: Int) {
    val c = _stats.value
    _stats.value = c.copy(weeklyGoal = goalMinutes.coerceAtLeast(0))
  }

  override suspend fun addPoints(delta: Int) {
    if (delta == 0) return
    val c = _stats.value
    _stats.value = c.copy(points = (c.points + delta).coerceAtLeast(0))
  }

  private fun applyDailyRollover(stats: UserStats, today: java.time.LocalDate): UserStats {
    val lastEpoch =
        stats.lastStudyDateEpochDay ?: return stats.copy(lastStudyDateEpochDay = today.toEpochDay())
    val last = java.time.LocalDate.ofEpochDay(lastEpoch)

    if (last.isEqual(today)) return stats

    val gap = java.time.temporal.ChronoUnit.DAYS.between(last, today)

    val hadStudyThatDay = stats.todayStudyMinutes > 0
    val newStreak =
        if (!hadStudyThatDay || gap > 1L) 0
        else stats.streak // break streak if we skipped ≥1 full day

    return stats.copy(
        todayStudyMinutes = 0, streak = newStreak, lastStudyDateEpochDay = today.toEpochDay())
  }
}
