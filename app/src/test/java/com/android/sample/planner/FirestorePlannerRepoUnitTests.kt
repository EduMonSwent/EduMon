package com.android.sample.planner

import com.android.sample.feature.schedule.data.planner.AttendanceStatus
import com.android.sample.feature.schedule.data.planner.ClassAttendance
import com.android.sample.feature.schedule.data.planner.CompletionStatus
import com.android.sample.feature.schedule.repository.planner.FirestorePlannerRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FirestorePlannerRepositoryUnitTest {

  private lateinit var db: FirebaseFirestore
  private lateinit var auth: FirebaseAuth
  private lateinit var repo: FirestorePlannerRepository

  @Before
  fun setup() {
    db = mockk(relaxed = true)
    auth = mockk(relaxed = true)
    // We create the repo with mocks
    repo = FirestorePlannerRepository(db, auth)
  }

  @Test
  fun `when user is NOT signed in, getTodayClassesFlow delegates to super`() = runBlocking {
    // Given: No user is logged in
    every { auth.currentUser } returns null

    // When
    val classes = repo.getTodayClassesFlow().first()

    // Then: It should return the hardcoded list from PlannerRepository (base class)
    // The base class usually returns a list containing "Algorithms"
    assertTrue(classes.isNotEmpty())
    assertNotNull(classes.find { it.courseName == "Algorithms" })
  }

  @Test
  fun `when user is NOT signed in, getTodayAttendanceFlow delegates to super`() = runBlocking {
    every { auth.currentUser } returns null

    // Save to the *base* repository (in-memory) so we have something to fetch
    // (Note: FirestorePlannerRepository extends PlannerRepository)
    val today = LocalDate.now()
    val att = ClassAttendance("1", today, AttendanceStatus.YES, CompletionStatus.YES)
    repo.saveAttendance(att)

    val records = repo.getTodayAttendanceFlow().first()
    assertEquals(1, records.size)
    assertEquals("1", records[0].classId)
  }

  @Test
  fun `when user is NOT signed in, saveAttendance delegates to super`() = runBlocking {
    every { auth.currentUser } returns null

    val result =
        repo.saveAttendance(
            ClassAttendance("99", LocalDate.now(), AttendanceStatus.NO, CompletionStatus.NO))

    assertTrue(result.isSuccess)
    // Verify it was saved to the in-memory map of the base class
    val record = repo.getAttendanceForClass("99").first()
    assertNotNull(record)
    assertEquals(AttendanceStatus.NO, record?.attendance)
  }

  @Test
  fun `when user is NOT signed in, saveClass returns failure`() = runBlocking {
    every { auth.currentUser } returns null

    val result = repo.saveClasses(emptyList())

    assertTrue(result.isFailure)
    assertEquals("Not logged in", result.exceptionOrNull()?.message)
  }

  @Test
  fun `when user is NOT signed in, saveClasses returns failure`() = runBlocking {
    every { auth.currentUser } returns null

    val result = repo.saveClasses(emptyList())

    assertTrue(result.isFailure)
    assertEquals("Not logged in", result.exceptionOrNull()?.message)
  }

  @Test
  fun `when user is NOT signed in, clearClasses returns failure`() = runBlocking {
    every { auth.currentUser } returns null

    val result = repo.clearClasses()

    assertTrue(result.isFailure)
    assertEquals("Not logged in", result.exceptionOrNull()?.message)
  }
}
