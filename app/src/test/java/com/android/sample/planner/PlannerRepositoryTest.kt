package com.android.sample.planner

import com.android.sample.model.planner.*
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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
  fun setup() {
    repository = PlannerRepository()
  }

  @Test
  fun `getTodayClassesFlow should return 3 sorted classes`() = runTest {
    val classes = repository.getTodayClassesFlow().first()
    assertEquals(3, classes.size)
    assertEquals("Algorithms", classes[0].courseName)
    assertEquals("Data Structures", classes[1].courseName)
    assertEquals("Computer Networks", classes[2].courseName)
  }

  @Test
  fun `saveAttendance should store and retrieve attendance`() = runTest {
    val attendance =
        ClassAttendance(
            classId = "1",
            date = LocalDate.now(),
            attendance = AttendanceStatus.YES,
            completion = CompletionStatus.YES)

    repository.saveAttendance(attendance)
    val retrieved = repository.getAttendanceForClass("1").first()

    assertNotNull(retrieved)
    assertEquals(AttendanceStatus.YES, retrieved?.attendance)
    assertEquals(CompletionStatus.YES, retrieved?.completion)
  }

  @Test
  fun `saveAttendance should overwrite same-day record`() = runTest {
    val first = ClassAttendance("1", LocalDate.now(), AttendanceStatus.NO, CompletionStatus.NO)
    val updated = ClassAttendance("1", LocalDate.now(), AttendanceStatus.YES, CompletionStatus.YES)

    repository.saveAttendance(first)
    repository.saveAttendance(updated)

    val result = repository.getAttendanceForClass("1").first()
    assertEquals(AttendanceStatus.YES, result?.attendance)
    assertEquals(CompletionStatus.YES, result?.completion)
  }

  @Test
  fun `getTodayAttendanceFlow should filter only today's records`() = runTest {
    val today = LocalDate.now()
    val yesterday = today.minusDays(1)

    repository.saveAttendance(
        ClassAttendance("1", today, AttendanceStatus.YES, CompletionStatus.YES))
    repository.saveAttendance(
        ClassAttendance("2", yesterday, AttendanceStatus.NO, CompletionStatus.NO))

    val todayRecords = repository.getTodayAttendanceFlow().first()
    assertEquals(1, todayRecords.size)
    assertEquals("1", todayRecords.first().classId)
  }
}
