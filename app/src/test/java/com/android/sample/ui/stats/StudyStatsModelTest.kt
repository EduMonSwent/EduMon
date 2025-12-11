package com.android.sample.ui.stats

import com.android.sample.ui.stats.model.StudyStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StudyStatsModelTest {

  @Test
  fun `StudyStats can be created with all parameters`() {
    val stats =
        StudyStats(
            totalTimeMin = 150,
            courseTimesMin = mapOf("Math" to 60, "Physics" to 90),
            completedGoals = 3,
            progressByDayMin = listOf(10, 20, 30, 40, 50, 60, 70),
            weeklyGoalMin = 300)

    assertEquals(150, stats.totalTimeMin)
    assertEquals(2, stats.courseTimesMin.size)
    assertEquals(60, stats.courseTimesMin["Math"])
    assertEquals(90, stats.courseTimesMin["Physics"])
    assertEquals(3, stats.completedGoals)
    assertEquals(7, stats.progressByDayMin.size)
    assertEquals(300, stats.weeklyGoalMin)
  }

  @Test
  fun `StudyStats can be created with minimal parameters`() {
    val stats =
        StudyStats(
            totalTimeMin = 0,
            courseTimesMin = emptyMap(),
            completedGoals = 0,
            progressByDayMin = emptyList())

    assertEquals(0, stats.totalTimeMin)
    assertTrue(stats.courseTimesMin.isEmpty())
    assertEquals(0, stats.completedGoals)
    assertTrue(stats.progressByDayMin.isEmpty())
    assertEquals(0, stats.weeklyGoalMin) // Default value
  }

  @Test
  fun `StudyStats copy works correctly`() {
    val original =
        StudyStats(
            totalTimeMin = 100,
            courseTimesMin = mapOf("Math" to 50),
            completedGoals = 2,
            progressByDayMin = listOf(10, 20),
            weeklyGoalMin = 200)

    val modified = original.copy(totalTimeMin = 150)

    assertEquals(150, modified.totalTimeMin)
    assertEquals(50, modified.courseTimesMin["Math"])
    assertEquals(2, modified.completedGoals)
    assertEquals(200, modified.weeklyGoalMin)
  }

  @Test
  fun `StudyStats maintains LinkedHashMap order for courseTimesMin`() {
    val orderedMap = linkedMapOf("First" to 10, "Second" to 20, "Third" to 30)

    val stats =
        StudyStats(
            totalTimeMin = 60,
            courseTimesMin = orderedMap,
            completedGoals = 0,
            progressByDayMin = emptyList())

    val keys = stats.courseTimesMin.keys.toList()
    assertEquals("First", keys[0])
    assertEquals("Second", keys[1])
    assertEquals("Third", keys[2])
  }

  @Test
  fun `StudyStats with 7 days of progress`() {
    val progress = listOf(25, 30, 15, 40, 50, 20, 35)
    val stats =
        StudyStats(
            totalTimeMin = progress.sum(),
            courseTimesMin = emptyMap(),
            completedGoals = 5,
            progressByDayMin = progress)

    assertEquals(7, stats.progressByDayMin.size)
    assertEquals(215, stats.totalTimeMin)
    assertEquals(50, stats.progressByDayMin.maxOrNull())
    assertEquals(15, stats.progressByDayMin.minOrNull())
  }

  @Test
  fun `StudyStats with zero values`() {
    val stats =
        StudyStats(
            totalTimeMin = 0,
            courseTimesMin = mapOf("Math" to 0, "Physics" to 0),
            completedGoals = 0,
            progressByDayMin = listOf(0, 0, 0, 0, 0, 0, 0),
            weeklyGoalMin = 0)

    assertEquals(0, stats.totalTimeMin)
    assertEquals(0, stats.completedGoals)
    assertEquals(0, stats.weeklyGoalMin)
    assertEquals(0, stats.progressByDayMin.sum())
  }

  @Test
  fun `StudyStats with large values`() {
    val stats =
        StudyStats(
            totalTimeMin = 10000,
            courseTimesMin = mapOf("Math" to 5000, "Physics" to 5000),
            completedGoals = 100,
            progressByDayMin = listOf(1500, 1500, 1500, 1500, 1500, 1500, 1000),
            weeklyGoalMin = 5000)

    assertEquals(10000, stats.totalTimeMin)
    assertEquals(100, stats.completedGoals)
    assertEquals(5000, stats.weeklyGoalMin)
    assertTrue(stats.totalTimeMin > stats.weeklyGoalMin)
  }

  @Test
  fun `StudyStats equality works correctly`() {
    val stats1 =
        StudyStats(
            totalTimeMin = 100,
            courseTimesMin = mapOf("Math" to 50),
            completedGoals = 2,
            progressByDayMin = listOf(10, 20),
            weeklyGoalMin = 200)

    val stats2 =
        StudyStats(
            totalTimeMin = 100,
            courseTimesMin = mapOf("Math" to 50),
            completedGoals = 2,
            progressByDayMin = listOf(10, 20),
            weeklyGoalMin = 200)

    assertEquals(stats1, stats2)
  }

  @Test
  fun `StudyStats with empty progressByDayMin`() {
    val stats =
        StudyStats(
            totalTimeMin = 100,
            courseTimesMin = mapOf("Math" to 100),
            completedGoals = 5,
            progressByDayMin = emptyList())

    assertTrue(stats.progressByDayMin.isEmpty())
    assertEquals(100, stats.totalTimeMin)
  }

  @Test
  fun `StudyStats with single course`() {
    val stats =
        StudyStats(
            totalTimeMin = 120,
            courseTimesMin = mapOf("Mathematics" to 120),
            completedGoals = 3,
            progressByDayMin = listOf(20, 20, 20, 20, 20, 20, 0))

    assertEquals(1, stats.courseTimesMin.size)
    assertEquals(120, stats.courseTimesMin["Mathematics"])
    assertEquals(120, stats.totalTimeMin)
  }

  @Test
  fun `StudyStats courseTimesMin sum can differ from totalTimeMin`() {
    // In practice, totalTimeMin might be from a different source (UserStats)
    // while courseTimesMin is from weekly StudyStats
    val stats =
        StudyStats(
            totalTimeMin = 500, // Total from UserStats
            courseTimesMin = mapOf("Math" to 50, "Physics" to 50), // Only 100 min this week
            completedGoals = 2,
            progressByDayMin = listOf(20, 20, 20, 20, 20, 0, 0))

    val weeklySum = stats.courseTimesMin.values.sum()
    assertEquals(100, weeklySum)
    assertTrue(stats.totalTimeMin > weeklySum) // Total includes previous weeks
  }
}
