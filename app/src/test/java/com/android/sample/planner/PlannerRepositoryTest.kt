package com.android.sample.planner

import com.android.sample.model.planner.*
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class PlannerRepositoryTest {

  private lateinit var repository: PlannerRepository

  @Before
  fun setUp() {
    repository = PlannerRepository()
  }

  @Test
  fun `getTodayClasses should return sorted list of classes`() = runTest {
    val classes = repository.getTodayClasses()

    assertEquals(3, classes.size)
    // Should be sorted by start time
    assertEquals("Algorithms", classes[0].courseName)
    assertEquals("Data Structures", classes[1].courseName)
    assertEquals("Computer Networks", classes[2].courseName)
  }

  @Test
  fun `saveAttendance should store and retrieve attendance record`() = runTest {
    val testAttendance =
        ClassAttendance(
            classId = "test1",
            date = LocalDate.now(),
            attendance = AttendanceStatus.YES,
            completion = CompletionStatus.PARTIALLY)

    repository.saveAttendance(testAttendance)

    val retrieved = repository.getAttendanceForClass("test1")
    assertNotNull(retrieved)
    assertEquals(AttendanceStatus.YES, retrieved?.attendance)
    assertEquals(CompletionStatus.PARTIALLY, retrieved?.completion)
  }

  @Test
  fun `saveAttendance should update existing record for same class and date`() = runTest {
    val initialAttendance =
        ClassAttendance(
            classId = "test1",
            date = LocalDate.now(),
            attendance = AttendanceStatus.NO,
            completion = CompletionStatus.NO)

    val updatedAttendance =
        ClassAttendance(
            classId = "test1",
            date = LocalDate.now(),
            attendance = AttendanceStatus.YES,
            completion = CompletionStatus.YES)

    repository.saveAttendance(initialAttendance)
    repository.saveAttendance(updatedAttendance)

    val retrieved = repository.getAttendanceForClass("test1")
    assertNotNull(retrieved)
    assertEquals(AttendanceStatus.YES, retrieved?.attendance)
    assertEquals(CompletionStatus.YES, retrieved?.completion)
  }

  @Test
  fun `getTodayAttendanceRecords should return only today's records`() = runTest {
    val todayAttendance =
        ClassAttendance(
            classId = "test1",
            date = LocalDate.now(),
            attendance = AttendanceStatus.YES,
            completion = CompletionStatus.YES)

    repository.saveAttendance(todayAttendance)

    val todayRecords = repository.getTodayAttendanceRecords()
    assertEquals(1, todayRecords.size)
    assertEquals("test1", todayRecords[0].classId)
  }
}
