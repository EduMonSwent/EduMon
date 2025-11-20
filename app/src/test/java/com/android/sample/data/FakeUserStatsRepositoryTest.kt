package com.android.sample.data

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class FakeUserStatsRepositoryTest {

  private lateinit var repo: FakeUserStatsRepository

  @Before
  fun setUp() {
    repo = FakeUserStatsRepository()
  }

  @Test
  fun `addStudyMinutes updates total and today minutes`() = runTest {
    repo.addStudyMinutes(30)
    val stats = repo.stats.value
    assertEquals(30, stats.totalStudyMinutes)
    assertEquals(30, stats.todayStudyMinutes)

    repo.addStudyMinutes(15)
    val updatedStats = repo.stats.value
    assertEquals(45, updatedStats.totalStudyMinutes)
    assertEquals(45, updatedStats.todayStudyMinutes)
  }

  @Test
  fun `updateCoins modifies coin balance`() = runTest {
    repo.updateCoins(100)
    assertEquals(100, repo.stats.value.coins)

    repo.updateCoins(-50)
    assertEquals(50, repo.stats.value.coins)
  }

  @Test
  fun `setWeeklyGoal updates goal`() = runTest {
    repo.setWeeklyGoal(600)
    assertEquals(600, repo.stats.value.weeklyGoal)
  }

  @Test
  fun `addPoints increases points`() = runTest {
    repo.addPoints(10)
    assertEquals(10, repo.stats.value.points)
  }

  @Test
  fun `updateCompletedGoals sets goal count`() = runTest {
    repo.updateCompletedGoals(5)
    assertEquals(5, repo.stats.value.completedGoals)
  }

  @Test
  fun `incrementCompletedPomodoros increases count`() = runTest {
    repo.incrementCompletedPomodoros()
    assertEquals(1, repo.stats.value.todayCompletedPomodoros)

    repo.incrementCompletedPomodoros()
    assertEquals(2, repo.stats.value.todayCompletedPomodoros)
  }
}
