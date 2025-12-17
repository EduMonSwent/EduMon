package com.android.sample.data

// This code has been written partially using A.I (LLM).

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class FakeUserStatsRepositoryTest {

  @Test
  fun initial_state_is_default() = runTest {
    val repo = FakeUserStatsRepository()

    val stats = repo.stats.value

    assertEquals(0, stats.totalStudyMinutes)
    assertEquals(0, stats.todayStudyMinutes)
    assertEquals(0, stats.coins)
    assertEquals(0, stats.points)
    assertEquals(0, stats.weeklyGoal)
    assertEquals(0, stats.streak)
    // lastStudyDateEpochDay should be null by default
    assertEquals(null, stats.lastStudyDateEpochDay)
  }

  @Test
  fun addStudyMinutes_positive_updates_total_and_today() = runTest {
    val repo = FakeUserStatsRepository()

    repo.addStudyMinutes(30)

    val stats = repo.stats.value
    assertEquals(30, stats.totalStudyMinutes)
    assertEquals(30, stats.todayStudyMinutes)
  }

  @Test
  fun addStudyMinutes_nonPositive_does_not_change_state() = runTest {
    val repo = FakeUserStatsRepository(UserStats(totalStudyMinutes = 10, todayStudyMinutes = 5))

    repo.addStudyMinutes(0)
    repo.addStudyMinutes(-10)

    val stats = repo.stats.value
    assertEquals(10, stats.totalStudyMinutes)
    assertEquals(5, stats.todayStudyMinutes)
  }

  @Test
  fun updateCoins_updates_only_coins_when_nonZero() = runTest {
    val repo = FakeUserStatsRepository(UserStats(coins = 4))

    repo.updateCoins(3)

    val stats = repo.stats.value
    assertEquals(7, stats.coins)
  }

  @Test
  fun updateCoins_zero_does_nothing() = runTest {
    val repo = FakeUserStatsRepository(UserStats(coins = 4))

    repo.updateCoins(0)

    val stats = repo.stats.value
    assertEquals(4, stats.coins)
  }

  @Test
  fun setWeeklyGoal_updates_weeklyGoal() = runTest {
    val repo = FakeUserStatsRepository(UserStats(weeklyGoal = 100))

    repo.setWeeklyGoal(250)

    val stats = repo.stats.value
    assertEquals(250, stats.weeklyGoal)
  }

  @Test
  fun addPoints_updates_points_when_nonZero() = runTest {
    val repo = FakeUserStatsRepository(UserStats(points = 10))

    repo.addPoints(5)

    val stats = repo.stats.value
    assertEquals(15, stats.points)
  }

  @Test
  fun addPoints_zero_does_not_change_points() = runTest {
    val repo = FakeUserStatsRepository(UserStats(points = 10))

    repo.addPoints(0)

    val stats = repo.stats.value
    assertEquals(10, stats.points)
  }

  @Test
  fun start_does_not_throw_and_keeps_state() = runTest {
    val initial = UserStats(totalStudyMinutes = 42)
    val repo = FakeUserStatsRepository(initial)

    repo.start()

    val stats = repo.stats.value
    assertEquals(42, stats.totalStudyMinutes)
  }

  // ========== Tests for applyDailyRollover coverage ==========

  @Test
  fun addStudyMinutes_firstTimeToday_incrementsStreak() = runTest {
    val today = java.time.LocalDate.now()
    val yesterday = today.minusDays(1)

    val initial =
        UserStats(
            totalStudyMinutes = 50,
            todayStudyMinutes = 30, // Had study yesterday
            streak = 5,
            lastStudyDateEpochDay = yesterday.toEpochDay())
    val repo = FakeUserStatsRepository(initial)

    // First study of today should increment streak
    repo.addStudyMinutes(20)

    val stats = repo.stats.value
    assertEquals(70, stats.totalStudyMinutes)
    assertEquals(20, stats.todayStudyMinutes) // Reset to today's minutes
    assertEquals(6, stats.streak) // Incremented
    assertEquals(today.toEpochDay(), stats.lastStudyDateEpochDay)
  }

  @Test
  fun addStudyMinutes_sameDay_doesNotIncrementStreak() = runTest {
    val today = java.time.LocalDate.now()

    val initial =
        UserStats(
            totalStudyMinutes = 50,
            todayStudyMinutes = 30,
            streak = 5,
            lastStudyDateEpochDay = today.toEpochDay())
    val repo = FakeUserStatsRepository(initial)

    // Adding more minutes same day should NOT increment streak
    repo.addStudyMinutes(20)

    val stats = repo.stats.value
    assertEquals(70, stats.totalStudyMinutes)
    assertEquals(50, stats.todayStudyMinutes) // 30 + 20
    assertEquals(5, stats.streak) // NOT incremented
    assertEquals(today.toEpochDay(), stats.lastStudyDateEpochDay)
  }

  @Test
  fun addStudyMinutes_afterGap_breaksStreak() = runTest {
    val today = java.time.LocalDate.now()
    val threeDaysAgo = today.minusDays(3)

    val initial =
        UserStats(
            totalStudyMinutes = 100,
            todayStudyMinutes = 50,
            streak = 10,
            lastStudyDateEpochDay = threeDaysAgo.toEpochDay())
    val repo = FakeUserStatsRepository(initial)

    // Gap of 3 days should break the streak
    repo.addStudyMinutes(30)

    val stats = repo.stats.value
    assertEquals(130, stats.totalStudyMinutes)
    assertEquals(30, stats.todayStudyMinutes) // Reset to today's minutes
    assertEquals(1, stats.streak) // Streak broken and restarted at 1
    assertEquals(today.toEpochDay(), stats.lastStudyDateEpochDay)
  }

  @Test
  fun addStudyMinutes_withNullLastStudyDate_setsInitialDate() = runTest {
    val today = java.time.LocalDate.now()

    val initial =
        UserStats(
            totalStudyMinutes = 0, todayStudyMinutes = 0, streak = 0, lastStudyDateEpochDay = null)
    val repo = FakeUserStatsRepository(initial)

    repo.addStudyMinutes(15)

    val stats = repo.stats.value
    assertEquals(15, stats.totalStudyMinutes)
    assertEquals(15, stats.todayStudyMinutes)
    assertEquals(1, stats.streak) // First study starts streak
    assertEquals(today.toEpochDay(), stats.lastStudyDateEpochDay)
  }

  @Test
  fun addStudyMinutes_noStudyYesterday_breaksStreak() = runTest {
    val today = java.time.LocalDate.now()
    val yesterday = today.minusDays(1)

    val initial =
        UserStats(
            totalStudyMinutes = 50,
            todayStudyMinutes = 0, // No study yesterday
            streak = 7,
            lastStudyDateEpochDay = yesterday.toEpochDay())
    val repo = FakeUserStatsRepository(initial)

    // If no study yesterday (todayStudyMinutes = 0), streak breaks
    repo.addStudyMinutes(25)

    val stats = repo.stats.value
    assertEquals(75, stats.totalStudyMinutes)
    assertEquals(25, stats.todayStudyMinutes)
    assertEquals(1, stats.streak) // Streak broken, restarted
    assertEquals(today.toEpochDay(), stats.lastStudyDateEpochDay)
  }

  @Test
  fun applyDailyRollover_sameDay_noChanges() = runTest {
    val today = java.time.LocalDate.now()

    // Set initial state with study today
    val initial =
        UserStats(
            totalStudyMinutes = 30,
            todayStudyMinutes = 30,
            streak = 3,
            lastStudyDateEpochDay = today.toEpochDay())
    val repo = FakeUserStatsRepository(initial)

    // Add more minutes on same day
    repo.addStudyMinutes(10)

    val stats = repo.stats.value
    // Rollover shouldn't happen, just add to existing
    assertEquals(40, stats.totalStudyMinutes)
    assertEquals(40, stats.todayStudyMinutes)
    assertEquals(3, stats.streak) // Same streak
  }

  @Test
  fun applyDailyRollover_multipleDayGap_resetsStreakToOne() = runTest {
    val today = java.time.LocalDate.now()
    val fiveDaysAgo = today.minusDays(5)

    val initial =
        UserStats(
            totalStudyMinutes = 200,
            todayStudyMinutes = 40,
            streak = 15,
            lastStudyDateEpochDay = fiveDaysAgo.toEpochDay())
    val repo = FakeUserStatsRepository(initial)

    // After 5 days gap, streak should reset
    repo.addStudyMinutes(35)

    val stats = repo.stats.value
    assertEquals(235, stats.totalStudyMinutes)
    assertEquals(35, stats.todayStudyMinutes)
    assertEquals(1, stats.streak) // Reset to 1
    assertEquals(today.toEpochDay(), stats.lastStudyDateEpochDay)
  }
}
