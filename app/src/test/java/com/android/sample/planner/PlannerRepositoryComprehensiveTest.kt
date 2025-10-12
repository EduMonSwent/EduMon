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
class PlannerRepositoryComprehensiveTest {

  private lateinit var repository: PlannerRepository

  @Before
  fun setUp() {
    repository = PlannerRepository()
  }

  @Test
  fun `getTodayClasses should return non-empty list`() = runTest {
    // When
    val classes = repository.getTodayClasses()

    // Then
    assertTrue(classes.isNotEmpty())
    assertEquals(3, classes.size)
  }

  @Test
  fun `getTodayClasses should return classes sorted by start time`() = runTest {
    // When
    val classes = repository.getTodayClasses()

    // Then
    assertEquals("Algorithms", classes[0].courseName)
    assertEquals("Data Structures", classes[1].courseName)
    assertEquals("Computer Networks", classes[2].courseName)

    // Verify start times are in ascending order
    assertTrue(classes[0].startTime.isBefore(classes[1].startTime))
    assertTrue(classes[1].startTime.isBefore(classes[2].startTime))
  }

  @Test
  fun `getTodayClasses should have correct class properties`() = runTest {
    // When
    val classes = repository.getTodayClasses()

    // Then
    val algorithmClass = classes[0]
    assertEquals("1", algorithmClass.id)
    assertEquals("Algorithms", algorithmClass.courseName)
    assertEquals(ClassType.LECTURE, algorithmClass.type)
    assertEquals("INM 202", algorithmClass.location)
    assertEquals("Prof. Smith", algorithmClass.instructor)

    val dataStructuresClass = classes[1]
    assertEquals("2", dataStructuresClass.id)
    assertEquals("Data Structures", dataStructuresClass.courseName)
    assertEquals(ClassType.EXERCISE, dataStructuresClass.type)
    assertEquals("BC 101", dataStructuresClass.location)
    assertEquals("Dr. Johnson", dataStructuresClass.instructor)

    val networksClass = classes[2]
    assertEquals("3", networksClass.id)
    assertEquals("Computer Networks", networksClass.courseName)
    assertEquals(ClassType.LAB, networksClass.type)
    assertEquals("Lab A", networksClass.location)
    assertEquals("Prof. Davis", networksClass.instructor)
  }

  @Test
  fun `saveAttendance should store new attendance record`() = runTest {
    // Given
    val testAttendance =
        ClassAttendance(
            classId = "test-class-1",
            date = LocalDate.now(),
            attendance = AttendanceStatus.YES,
            completion = CompletionStatus.PARTIALLY)

    // When
    repository.saveAttendance(testAttendance)

    // Then
    val retrieved = repository.getAttendanceForClass("test-class-1")
    assertNotNull(retrieved)
    assertEquals("test-class-1", retrieved?.classId)
    assertEquals(AttendanceStatus.YES, retrieved?.attendance)
    assertEquals(CompletionStatus.PARTIALLY, retrieved?.completion)
    assertEquals(LocalDate.now(), retrieved?.date)
  }

  @Test
  fun `saveAttendance should overwrite existing record for same class and date`() = runTest {
    // Given
    val initialAttendance =
        ClassAttendance(
            classId = "test-class-1",
            date = LocalDate.now(),
            attendance = AttendanceStatus.NO,
            completion = CompletionStatus.NO)

    val updatedAttendance =
        ClassAttendance(
            classId = "test-class-1",
            date = LocalDate.now(),
            attendance = AttendanceStatus.YES,
            completion = CompletionStatus.YES)

    // When
    repository.saveAttendance(initialAttendance)
    repository.saveAttendance(updatedAttendance)

    // Then
    val retrieved = repository.getAttendanceForClass("test-class-1")
    assertNotNull(retrieved)
    assertEquals(AttendanceStatus.YES, retrieved?.attendance)
    assertEquals(CompletionStatus.YES, retrieved?.completion)
  }

  @Test
  fun `getTodayAttendanceRecords should return only today's records`() = runTest {
    // Given
    val todayAttendance =
        ClassAttendance(
            classId = "today-class",
            date = LocalDate.now(),
            attendance = AttendanceStatus.YES,
            completion = CompletionStatus.YES)

    val yesterdayAttendance =
        ClassAttendance(
            classId = "yesterday-class",
            date = LocalDate.now().minusDays(1),
            attendance = AttendanceStatus.YES,
            completion = CompletionStatus.YES)

    // When
    repository.saveAttendance(todayAttendance)
    repository.saveAttendance(yesterdayAttendance)

    // Then
    val todayRecords = repository.getTodayAttendanceRecords()
    assertEquals(1, todayRecords.size)
    assertEquals("today-class", todayRecords[0].classId)
  }

  @Test
  fun `getAttendanceForClass should return null for non-existent class`() = runTest {
    // When
    val result = repository.getAttendanceForClass("non-existent-class")

    // Then
    assertNull(result)
  }

  @Test
  fun `getAttendanceForClass should return correct record for existing class`() = runTest {
    // Given
    val testAttendance =
        ClassAttendance(
            classId = "specific-class",
            date = LocalDate.now(),
            attendance = AttendanceStatus.ARRIVED_LATE,
            completion = CompletionStatus.PARTIALLY)
    repository.saveAttendance(testAttendance)

    // When
    val result = repository.getAttendanceForClass("specific-class")

    // Then
    assertNotNull(result)
    assertEquals("specific-class", result?.classId)
    assertEquals(AttendanceStatus.ARRIVED_LATE, result?.attendance)
    assertEquals(CompletionStatus.PARTIALLY, result?.completion)
  }

  @Test
  fun `multiple attendance records for different classes`() = runTest {
    // Given
    val attendance1 =
        ClassAttendance(
            classId = "class-1",
            date = LocalDate.now(),
            attendance = AttendanceStatus.YES,
            completion = CompletionStatus.YES)

    val attendance2 =
        ClassAttendance(
            classId = "class-2",
            date = LocalDate.now(),
            attendance = AttendanceStatus.NO,
            completion = CompletionStatus.NO)

    val attendance3 =
        ClassAttendance(
            classId = "class-3",
            date = LocalDate.now(),
            attendance = AttendanceStatus.ARRIVED_LATE,
            completion = CompletionStatus.PARTIALLY)

    // When
    repository.saveAttendance(attendance1)
    repository.saveAttendance(attendance2)
    repository.saveAttendance(attendance3)

    // Then
    val todayRecords = repository.getTodayAttendanceRecords()
    assertEquals(3, todayRecords.size)

    val record1 = repository.getAttendanceForClass("class-1")
    assertNotNull(record1)
    assertEquals(AttendanceStatus.YES, record1?.attendance)

    val record2 = repository.getAttendanceForClass("class-2")
    assertNotNull(record2)
    assertEquals(AttendanceStatus.NO, record2?.attendance)

    val record3 = repository.getAttendanceForClass("class-3")
    assertNotNull(record3)
    assertEquals(AttendanceStatus.ARRIVED_LATE, record3?.attendance)
  }

  @Test
  fun `attendance records with different dates should be stored separately`() = runTest {
    // Given
    val todayAttendance =
        ClassAttendance(
            classId = "same-class",
            date = LocalDate.now(),
            attendance = AttendanceStatus.YES,
            completion = CompletionStatus.YES)

    val tomorrowAttendance =
        ClassAttendance(
            classId = "same-class",
            date = LocalDate.now().plusDays(1),
            attendance = AttendanceStatus.NO,
            completion = CompletionStatus.NO)

    // When
    repository.saveAttendance(todayAttendance)
    repository.saveAttendance(tomorrowAttendance)

    // Then
    val todayRecord = repository.getAttendanceForClass("same-class")
    assertNotNull(todayRecord)
    assertEquals(AttendanceStatus.YES, todayRecord?.attendance)
    assertEquals(LocalDate.now(), todayRecord?.date)

    // Note: getTodayAttendanceRecords only returns today's records
    val todayRecords = repository.getTodayAttendanceRecords()
    assertEquals(1, todayRecords.size)
    assertEquals(AttendanceStatus.YES, todayRecords[0].attendance)
  }
}
