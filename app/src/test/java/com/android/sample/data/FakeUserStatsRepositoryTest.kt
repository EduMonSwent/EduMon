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
}
