package com.android.sample.data

import org.junit.Assert.assertEquals
import org.junit.Test

class UserStatsTest {

  @Test
  fun `default values are correct`() {
    val stats = UserStats()
    assertEquals(0, stats.totalStudyMinutes)
    assertEquals(0, stats.todayStudyMinutes)
    assertEquals(0, stats.streak)
    assertEquals(0, stats.weeklyGoal)
    assertEquals(20, stats.dailyGoal)
    assertEquals(0, stats.completedGoals)
    assertEquals(0, stats.todayCompletedPomodoros)
    assertEquals(0, stats.coins)
    assertEquals(0, stats.points)
    assertEquals(0L, stats.lastUpdated)
    assertEquals(0L, stats.lastStudyDate)
    assertEquals(0, stats.courseTimesMin.size)
    assertEquals(7, stats.progressByDayMin.size)
  }

  @Test
  fun `copy works as expected`() {
    val stats = UserStats()
    val copy = stats.copy(totalStudyMinutes = 100)
    assertEquals(100, copy.totalStudyMinutes)
    assertEquals(0, copy.todayStudyMinutes)
  }
}
