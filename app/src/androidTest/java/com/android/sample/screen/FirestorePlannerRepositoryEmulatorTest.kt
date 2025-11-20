package com.android.sample.screen

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.feature.schedule.data.planner.AttendanceStatus
import com.android.sample.feature.schedule.data.planner.ClassAttendance
import com.android.sample.feature.schedule.data.planner.ClassType
import com.android.sample.feature.schedule.data.planner.CompletionStatus
import com.android.sample.feature.schedule.repository.planner.FirestorePlannerRepository
import com.android.sample.util.FirebaseEmulator
import com.google.android.gms.tasks.Tasks
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FirestorePlannerRepositoryEmulatorTest {

  private lateinit var repo: FirestorePlannerRepository

  @Before
  fun setUp() = runBlocking {
    FirebaseEmulator.initIfNeeded(ApplicationProvider.getApplicationContext())
    FirebaseEmulator.connectIfRunning()

    assertTrue(
        "Firebase emulators not reachable. Start with: " +
            "firebase emulators:start --only firestore,auth",
        FirebaseEmulator.isRunning)

    FirebaseEmulator.clearAll()
    Tasks.await(FirebaseEmulator.auth.signInAnonymously())

    repo = FirestorePlannerRepository(FirebaseEmulator.firestore, FirebaseEmulator.auth)
  }

  @After
  fun tearDown() = runBlocking {
    if (FirebaseEmulator.isRunning) {
      FirebaseEmulator.clearAll()
    }
  }

  @Test
  fun getTodayClassesFlow_reads_from_firestore_when_docs_exist() = runBlocking {
    val db = FirebaseEmulator.firestore
    val uid = FirebaseEmulator.auth.currentUser!!.uid
    val today = LocalDate.now()

    val classesCol = db.collection("users").document(uid).collection("classes")

    // Two classes with different times to test sorting
    Tasks.await(
        classesCol.add(
            mapOf(
                "courseName" to "Networks",
                "startTime" to LocalTime.of(14, 0).toString(),
                "endTime" to LocalTime.of(16, 0).toString(),
                "type" to ClassType.LAB.name,
                "location" to "Lab A",
                "instructor" to "Prof. Davis",
                "date" to today.toString())))

    Tasks.await(
        classesCol.add(
            mapOf(
                "courseName" to "Algorithms",
                "startTime" to LocalTime.of(9, 0).toString(),
                "endTime" to LocalTime.of(10, 0).toString(),
                "type" to ClassType.LECTURE.name,
                "location" to "INM 202",
                "instructor" to "Prof. Smith",
                "date" to today.toString())))

    val classes = repo.getTodayClassesFlow().first()

    // Should come sorted by startTime: Algorithms then Networks
    assertEquals(listOf("Algorithms", "Networks"), classes.map { it.courseName })
    assertEquals(listOf(LocalTime.of(9, 0), LocalTime.of(14, 0)), classes.map { it.startTime })
  }

  @Test
  fun saveAttendance_and_getTodayAttendanceFlow_round_trips_through_firestore() = runBlocking {
    val today = LocalDate.now()
    val record =
        ClassAttendance(
            classId = "42",
            date = today,
            attendance = AttendanceStatus.ARRIVED_LATE,
            completion = CompletionStatus.PARTIALLY,
            timestamp = Instant.now())

    val result = repo.saveAttendance(record)
    assertTrue(result.isSuccess)

    val records = repo.getTodayAttendanceFlow().first()
    assertEquals(1, records.size)
    val loaded = records[0]

    assertEquals("42", loaded.classId)
    assertEquals(today, loaded.date)
    assertEquals(AttendanceStatus.ARRIVED_LATE, loaded.attendance)
    assertEquals(CompletionStatus.PARTIALLY, loaded.completion)
  }
}
