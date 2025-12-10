package com.android.sample.planner

import com.android.sample.feature.schedule.data.planner.AttendanceStatus
import com.android.sample.feature.schedule.data.planner.Class
import com.android.sample.feature.schedule.data.planner.ClassAttendance
import com.android.sample.feature.schedule.data.planner.ClassType
import com.android.sample.feature.schedule.data.planner.CompletionStatus
import com.android.sample.feature.schedule.repository.planner.PlannerRepository
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class MorePlannerRepositoryTest {

  private lateinit var repo: PlannerRepository

  @Before
  fun setup() {
    repo = PlannerRepository()
  }

  @Test
  fun `getTodayClassesFlow emits default hardcoded classes`() = runBlocking {
    val classes = repo.getTodayClassesFlow().first()
    assertFalse(classes.isEmpty())

    // Verify one of the default classes exists
    val algo = classes.find { it.courseName == "Algorithms" }
    assertNotNull(algo)
    assertEquals(ClassType.LECTURE, algo?.type)
  }

  @Test
  fun `saveAttendance updates internal state and reflects in flows`() = runBlocking {
    val today = LocalDate.now()
    val att =
        ClassAttendance(
            classId = "1",
            date = today,
            attendance = AttendanceStatus.ARRIVED_LATE,
            completion = CompletionStatus.PARTIALLY,
            timestamp = Instant.now())

    val result = repo.saveAttendance(att)
    assertTrue(result.isSuccess)

    // Check getTodayAttendanceFlow
    val records = repo.getTodayAttendanceFlow().first()
    val saved = records.find { it.classId == "1" }
    assertNotNull(saved)
    assertEquals(AttendanceStatus.ARRIVED_LATE, saved?.attendance)

    // Check getAttendanceForClass
    val specific = repo.getAttendanceForClass("1").first()
    assertNotNull(specific)
    assertEquals(CompletionStatus.PARTIALLY, specific?.completion)
  }

  @Test
  fun `saveAttendance overwrites existing record for same day and class`() = runBlocking {
    val today = LocalDate.now()
    val att1 = ClassAttendance("1", today, AttendanceStatus.NO, CompletionStatus.NO)
    val att2 = ClassAttendance("1", today, AttendanceStatus.YES, CompletionStatus.YES)

    repo.saveAttendance(att1)
    repo.saveAttendance(att2)

    val records = repo.getTodayAttendanceFlow().first()
    assertEquals(1, records.size)
    assertEquals(AttendanceStatus.YES, records[0].attendance)
  }

  @Test
  fun `seedDemoData populates attendance`() = runBlocking {
    repo.seedDemoData()
    val records = repo.getTodayAttendanceFlow().first()
    assertTrue(records.isNotEmpty())
    assertEquals("1", records[0].classId)
  }

  @Test
  fun `base implementation stubs return expected results`() = runBlocking {
    // saveClasses returns Success(Unit)
    val dummyClass = Class("id", "Name", LocalTime.now(), LocalTime.now(), ClassType.LECTURE)
    val saveListRes = repo.saveClasses(listOf(dummyClass))
    assertTrue(saveListRes.isSuccess)

    // clearClasses returns Success(Unit)
    val clearRes = repo.clearClasses()
    assertTrue(clearRes.isSuccess)
  }
}
