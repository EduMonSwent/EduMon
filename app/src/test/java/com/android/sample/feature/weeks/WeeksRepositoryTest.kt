package com.android.sample.feature.weeks

import com.android.sample.feature.weeks.repository.FakeWeeksRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WeeksRepositoryTest {

  @Test
  fun updateByIndex_clampsAndUpdates() = runTest {
    val repo = FakeWeeksRepository()
    val before = repo.getWeeks()
    assertTrue(before.isNotEmpty())

    val updated = repo.updateWeekPercent(index = 1, percent = 250) // clamp to 100
    assertEquals(100, updated[1].percent)

    val updated2 = repo.updateWeekPercent(index = 1, percent = -10) // clamp to 0
    assertEquals(0, updated2[1].percent)
  }

  @Test
  fun updateByIndex_updatesCorrectWeek() = runTest {
    val repo = FakeWeeksRepository()
    val updated = repo.updateWeekPercent(index = 1, percent = 42)
    // Verify Week 2 is 42, others unchanged initial values (100 for Week 1, 10 for Week 3)
    assertEquals(100, updated[0].percent)
    assertEquals(42, updated[1].percent)
    assertEquals(10, updated[2].percent)
  }

  @Test
  fun updateByIndex_nonExistent_returnsUnchanged() = runTest {
    val repo = FakeWeeksRepository()
    val before = repo.getWeeks()
    val after = repo.updateWeekPercent(index = 42, percent = 77) // out-of-range index
    assertEquals(before, after)
  }

  @Test
  fun getDayStatuses_returnsSevenWithAlternatingPattern() = runTest {
    val repo = FakeWeeksRepository()
    val statuses = repo.getDayStatuses()
    assertEquals(7, statuses.size)
    // Pattern in Fake: idx % 2 == 0 -> metTarget true
    statuses.forEachIndexed { i, ds -> assertEquals(i % 2 == 0, ds.metTarget) }
  }

  @Test
  fun markExerciseDone_updatesExerciseStatus() = runTest {
    val repo = FakeWeeksRepository()
    val weekIndex = 1 // Week 2
    val exerciseId = "e3" // "Build layout challenge" - initially false

    // Verify initial state
    val initialContent = repo.getWeekContent(weekIndex)
    val initialExercise = initialContent.exercises.find { it.id == exerciseId }
    assertEquals(false, initialExercise?.done)

    // Mark exercise as done
    val updated = repo.markExerciseDone(weekIndex, exerciseId, done = true)

    // Verify exercise is marked done
    val updatedContent = repo.getWeekContent(weekIndex)
    val updatedExercise = updatedContent.exercises.find { it.id == exerciseId }
    assertEquals(true, updatedExercise?.done)

    // Verify the percent was recomputed (Week 2 has 2 courses + 1 exercise = 3 items)
    // Initially: 1 course read (c4) + 0 exercises = 1/3 = 33%
    // After marking e3 done: 1 course read + 1 exercise = 2/3 = 66%
    assertTrue(updated[weekIndex].percent >= 60) // Should be around 66%
  }

  @Test
  fun markExerciseDone_canMarkAsDoneAndUndone() = runTest {
    val repo = FakeWeeksRepository()
    val weekIndex = 0 // Week 1
    val exerciseId = "e1" // Initially done = true

    // Mark as undone
    repo.markExerciseDone(weekIndex, exerciseId, done = false)
    val content1 = repo.getWeekContent(weekIndex)
    assertEquals(false, content1.exercises.find { it.id == exerciseId }?.done)

    // Mark as done again
    repo.markExerciseDone(weekIndex, exerciseId, done = true)
    val content2 = repo.getWeekContent(weekIndex)
    assertEquals(true, content2.exercises.find { it.id == exerciseId }?.done)
  }

  @Test
  fun markExerciseDone_invalidWeekIndex_returnsUnchanged() = runTest {
    val repo = FakeWeeksRepository()
    val before = repo.getWeeks()

    val after = repo.markExerciseDone(weekIndex = 999, exerciseId = "e1", done = true)

    assertEquals(before, after)
  }

  @Test
  fun markExerciseDone_invalidExerciseId_leavesWeekUnchanged() = runTest {
    val repo = FakeWeeksRepository()
    val weekIndex = 1
    val before = repo.getWeeks()

    // Try to mark non-existent exercise
    val after = repo.markExerciseDone(weekIndex, exerciseId = "nonexistent", done = true)

    // Percent should remain the same since no exercise was found
    assertEquals(before[weekIndex].percent, after[weekIndex].percent)
  }

  @Test
  fun markExerciseDone_recomputesPercentCorrectly() = runTest {
    val repo = FakeWeeksRepository()
    val weekIndex = 2 // Week 3 has 1 course + 1 exercise = 2 items

    // Initially both are false (done=false, read=false), so percent should be 0
    val initial = repo.getWeeks()
    assertEquals(0, initial[weekIndex].percent)

    // Mark exercise as done (1 out of 2 = 50%)
    val updated1 = repo.markExerciseDone(weekIndex, exerciseId = "e4", done = true)
    assertEquals(50, updated1[weekIndex].percent)

    // Mark exercise as undone again (0 out of 2 = 0%)
    val updated2 = repo.markExerciseDone(weekIndex, exerciseId = "e4", done = false)
    assertEquals(0, updated2[weekIndex].percent)
  }

  @Test
  fun markCourseRead_updatesCourseStatus() = runTest {
    val repo = FakeWeeksRepository()
    val weekIndex = 1 // Week 2
    val courseId = "c3" // "Compose Layouts" - initially false

    // Verify initial state
    val initialContent = repo.getWeekContent(weekIndex)
    val initialCourse = initialContent.courses.find { it.id == courseId }
    assertEquals(false, initialCourse?.read)

    // Mark course as read
    val updated = repo.markCourseRead(weekIndex, courseId, read = true)

    // Verify course is marked read
    val updatedContent = repo.getWeekContent(weekIndex)
    val updatedCourse = updatedContent.courses.find { it.id == courseId }
    assertEquals(true, updatedCourse?.read)

    // Verify the percent was recomputed
    // Week 2 has 2 courses + 1 exercise = 3 items
    // After marking c3 and c4 read: 2 courses read + 0 exercises = 2/3 = 66%
    assertTrue(updated[weekIndex].percent >= 60)
  }

  @Test
  fun markCourseRead_canMarkAsReadAndUnread() = runTest {
    val repo = FakeWeeksRepository()
    val weekIndex = 0 // Week 1
    val courseId = "c1" // Initially read = true

    // Mark as unread
    repo.markCourseRead(weekIndex, courseId, read = false)
    val content1 = repo.getWeekContent(weekIndex)
    assertEquals(false, content1.courses.find { it.id == courseId }?.read)

    // Mark as read again
    repo.markCourseRead(weekIndex, courseId, read = true)
    val content2 = repo.getWeekContent(weekIndex)
    assertEquals(true, content2.courses.find { it.id == courseId }?.read)
  }

  @Test
  fun markCourseRead_invalidWeekIndex_returnsUnchanged() = runTest {
    val repo = FakeWeeksRepository()
    val before = repo.getWeeks()

    val after = repo.markCourseRead(weekIndex = 999, courseId = "c1", read = true)

    assertEquals(before, after)
  }

  @Test
  fun markCourseRead_invalidCourseId_leavesWeekUnchanged() = runTest {
    val repo = FakeWeeksRepository()
    val weekIndex = 1
    val before = repo.getWeeks()

    // Try to mark non-existent course
    val after = repo.markCourseRead(weekIndex, courseId = "nonexistent", read = true)

    // Percent should remain the same since no course was found
    assertEquals(before[weekIndex].percent, after[weekIndex].percent)
  }

  @Test
  fun markCourseRead_recomputesPercentCorrectly() = runTest {
    val repo = FakeWeeksRepository()
    val weekIndex = 2 // Week 3 has 1 course + 1 exercise = 2 items

    // Initially both are false (done=false, read=false), so percent should be 0
    val initial = repo.getWeeks()
    assertEquals(0, initial[weekIndex].percent)

    // Mark course as read (1 out of 2 = 50%)
    val updated1 = repo.markCourseRead(weekIndex, courseId = "c5", read = true)
    assertEquals(50, updated1[weekIndex].percent)

    // Mark course as unread again (0 out of 2 = 0%)
    val updated2 = repo.markCourseRead(weekIndex, courseId = "c5", read = false)
    assertEquals(0, updated2[weekIndex].percent)
  }

  @Test
  fun markCourseAndExercise_bothUpdatePercent() = runTest {
    val repo = FakeWeeksRepository()
    val weekIndex = 2 // Week 3 has 1 course + 1 exercise = 2 items

    // Initially both false (0%)
    val initial = repo.getWeeks()
    assertEquals(0, initial[weekIndex].percent)

    // Mark course as read (50%)
    val updated1 = repo.markCourseRead(weekIndex, courseId = "c5", read = true)
    assertEquals(50, updated1[weekIndex].percent)

    // Mark exercise as done (100%)
    val updated2 = repo.markExerciseDone(weekIndex, exerciseId = "e4", done = true)
    assertEquals(100, updated2[weekIndex].percent)
  }

  @Test
  fun getWeekContent_returnsCorrectContent() = runTest {
    val repo = FakeWeeksRepository()

    // Test Week 1 content
    val week1Content = repo.getWeekContent(0)
    assertEquals(2, week1Content.courses.size)
    assertEquals(2, week1Content.exercises.size)
    assertEquals("Intro to Android", week1Content.courses[0].title)
    assertEquals("Set up environment", week1Content.exercises[0].title)

    // Test Week 2 content
    val week2Content = repo.getWeekContent(1)
    assertEquals(2, week2Content.courses.size)
    assertEquals(1, week2Content.exercises.size)
    assertEquals("Compose Layouts", week2Content.courses[0].title)
  }

  @Test
  fun getWeekContent_invalidIndex_returnsEmpty() = runTest {
    val repo = FakeWeeksRepository()

    val content = repo.getWeekContent(999)

    assertTrue(content.courses.isEmpty())
    assertTrue(content.exercises.isEmpty())
  }
}
