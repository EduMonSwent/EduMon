package com.android.sample.screen

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.feature.schedule.data.planner.AttendanceStatus
import com.android.sample.feature.schedule.data.planner.Class
import com.android.sample.feature.schedule.data.planner.ClassAttendance
import com.android.sample.feature.schedule.data.planner.ClassType
import com.android.sample.feature.schedule.data.planner.CompletionStatus
import com.android.sample.feature.schedule.repository.planner.FirestorePlannerRepository
import com.android.sample.util.FirebaseEmulator
import com.google.android.gms.tasks.Tasks
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
  fun saveClasses_and_clearClasses_work_correctly() = runBlocking {
    val c1 =
        Class(
            UUID.randomUUID().toString(),
            "Batch 1",
            LocalTime.now(),
            LocalTime.now(),
            ClassType.LAB)
    val c2 =
        Class(
            UUID.randomUUID().toString(),
            "Batch 2",
            LocalTime.now(),
            LocalTime.now(),
            ClassType.LECTURE)

    // Test Batch Save
    val saveResult = repo.saveClasses(listOf(c1, c2))
    assertTrue(saveResult.isSuccess)

    val loaded = repo.getTodayClassesFlow().first()
    assertTrue(loaded.any { it.courseName == "Batch 1" })
    assertTrue(loaded.any { it.courseName == "Batch 2" })

    // Test Clear
    val clearResult = repo.clearClasses()
    assertTrue(clearResult.isSuccess)

    val loadedAfterClear = repo.getTodayClassesFlow().first()
    assertTrue("Classes should be empty after clear", loadedAfterClear.isEmpty())
  }

  @Test
  fun getTodayClassesFlow_read_from_firestore_when_docs_exist() = runBlocking {
    val db = FirebaseEmulator.firestore
    val uid = FirebaseEmulator.auth.currentUser!!.uid
    val today = LocalDate.now()

    val classesCol = db.collection("users").document(uid).collection("classes")

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

    val classes = repo.getTodayClassesFlow().first()
    assertTrue(classes.any { it.courseName == "Networks" })
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

    val names = classes.map { it.courseName }
    val times = classes.map { it.startTime }

    // We don't assume there are *only* these two classes (to avoid cross-test pollution),
    // but they must be present and Algorithms must be before Networks.
    val idxAlgorithms = names.indexOf("Algorithms")
    val idxNetworks = names.indexOf("Networks")

    assertTrue("Algorithms not found", idxAlgorithms != -1)
    assertTrue("Networks not found", idxNetworks != -1)
    assertTrue("Algorithms should come before Networks", idxAlgorithms < idxNetworks)

    assertEquals(LocalTime.of(9, 0), times[idxAlgorithms])
    assertEquals(LocalTime.of(14, 0), times[idxNetworks])
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

    // Don't assume there is only one record; pick the one we just saved by classId.
    val loaded = records.firstOrNull { it.classId == "42" }
    assertNotNull("Expected an attendance record for classId=42", loaded)

    loaded!!
    assertEquals("42", loaded.classId)
    assertEquals(today, loaded.date)
    assertEquals(AttendanceStatus.ARRIVED_LATE, loaded.attendance)
    assertEquals(CompletionStatus.PARTIALLY, loaded.completion)
  }

  @Test
  fun getTodayClassesFlow_ignores_malformed_firestore_documents() = runBlocking {
    val db = FirebaseEmulator.firestore
    val uid = FirebaseEmulator.auth.currentUser!!.uid

    val classesCol = db.collection("users").document(uid).collection("classes")

    // Valid document
    Tasks.await(
        classesCol.add(
            mapOf(
                "courseName" to "Valid Course",
                "startTime" to LocalTime.of(10, 0).toString(),
                "endTime" to LocalTime.of(12, 0).toString(),
                "type" to ClassType.LECTURE.name,
                "location" to "Room 1",
                "instructor" to "Prof. Valid")))

    // Missing courseName => toPlannerClass() should return null
    Tasks.await(
        classesCol.add(
            mapOf(
                // "courseName" missing on purpose
                "startTime" to LocalTime.of(8, 0).toString(),
                "endTime" to LocalTime.of(9, 0).toString(),
                "type" to ClassType.LECTURE.name,
                "location" to "Room 2",
                "instructor" to "Prof. Missing")))

    // Invalid time format => toPlannerClass() should return null
    Tasks.await(
        classesCol.add(
            mapOf(
                "courseName" to "Broken Time",
                "startTime" to "not-a-time",
                "endTime" to "also-not-a-time",
                "type" to ClassType.LECTURE.name,
                "location" to "Room 3",
                "instructor" to "Prof. Broken")))

    val classes = repo.getTodayClassesFlow().first()

    // We only strictly require that malformed docs do NOT appear in the mapped list.
    assertTrue(classes.none { it.instructor == "Prof. Missing" })
    assertTrue(classes.none { it.courseName == "Broken Time" })

    // If any class is returned, at least one should be the valid one.
    if (classes.isNotEmpty()) {
      assertTrue(classes.any { it.courseName == "Valid Course" })
    }
  }

  @Test
  fun getTodayAttendanceFlow_applies_default_values_and_filters_by_date() = runBlocking {
    val db = FirebaseEmulator.firestore
    val uid = FirebaseEmulator.auth.currentUser!!.uid
    val today = LocalDate.now()
    val yesterday = today.minusDays(1)

    val attendanceCol =
        db.collection("users")
            .document(uid)
            .collection("planner")
            .document("meta")
            .collection("attendance")

    // Document with only mandatory fields -> should use default attendance/completion/timestamp
    Tasks.await(
        attendanceCol.add(
            mapOf(
                "classId" to "default-class", "date" to today.toString()
                // no "attendance", "completion" or "timestamp" fields
                )))

    // Missing classId -> toClassAttendance() returns null, should be ignored
    Tasks.await(
        attendanceCol.add(
            mapOf(
                // "classId" missing on purpose
                "date" to today.toString(),
                "attendance" to AttendanceStatus.NO.name,
                "completion" to CompletionStatus.NO.name)))

    // Different date -> should be filtered out by date check in getTodayAttendanceFlow()
    Tasks.await(
        attendanceCol.add(
            mapOf(
                "classId" to "other-day",
                "date" to yesterday.toString(),
                "attendance" to AttendanceStatus.YES.name,
                "completion" to CompletionStatus.YES.name)))

    val records = repo.getTodayAttendanceFlow().first()

    // Look specifically for the default-class record, do not assume there is only one record.
    val rec = records.firstOrNull { it.classId == "default-class" }
    assertNotNull("Expected an attendance record for classId=default-class", rec)

    rec!!
    assertEquals("default-class", rec.classId)
    assertEquals(today, rec.date)
    // Default values from toClassAttendance()
    assertEquals(AttendanceStatus.YES, rec.attendance)
    assertEquals(CompletionStatus.NO, rec.completion)
    // Timestamp should be "recent" (created via Instant.now() in mapper)
    assertTrue(rec.timestamp.isAfter(Instant.now().minusSeconds(60)))
  }

  @Test
  fun getAttendanceForClass_returns_only_today_record_for_given_class() = runBlocking {
    val today = LocalDate.now()
    val yesterday = today.minusDays(1)

    // Same classId, different dates + another classId
    val todayRecord =
        ClassAttendance(
            classId = "42",
            date = today,
            attendance = AttendanceStatus.YES,
            completion = CompletionStatus.PARTIALLY,
            timestamp = Instant.now())

    val yesterdayRecord =
        ClassAttendance(
            classId = "42",
            date = yesterday,
            attendance = AttendanceStatus.NO,
            completion = CompletionStatus.NO,
            timestamp = Instant.now().minusSeconds(86_400))

    val otherClassToday =
        ClassAttendance(
            classId = "99",
            date = today,
            attendance = AttendanceStatus.NO,
            completion = CompletionStatus.NO,
            timestamp = Instant.now())

    assertTrue(repo.saveAttendance(todayRecord).isSuccess)
    assertTrue(repo.saveAttendance(yesterdayRecord).isSuccess)
    assertTrue(repo.saveAttendance(otherClassToday).isSuccess)

    val loaded = repo.getAttendanceForClass("42").first()

    // Must pick the *today* record for classId 42 according to repository logic
    assertEquals("42", loaded?.classId)
    assertEquals(today, loaded?.date)
    assertEquals(AttendanceStatus.YES, loaded?.attendance)
    assertEquals(CompletionStatus.PARTIALLY, loaded?.completion)
  }

  @Test
  fun saveClass_writes_class_and_is_returned_in_getTodayClassesFlow() = runBlocking {
    val today = LocalDate.now()
    val classItem =
        com.android.sample.feature.schedule.data.planner.Class(
            id = "C1",
            courseName = "Distributed Systems",
            startTime = LocalTime.of(10, 0),
            endTime = LocalTime.of(12, 0),
            type = ClassType.LAB,
            location = "LAB-X",
            instructor = "Prof. Turing")

    val result = repo.saveClasses(listOf(classItem))
    assertTrue("saveClass() should succeed", result.isSuccess)

    // Snapshot listener needs a moment to update
    val loaded = repo.getTodayClassesFlow().first()

    // Look for the class we just saved
    val saved = loaded.firstOrNull { it.id == "C1" }
    assertNotNull("Expected class C1 to be returned by getTodayClassesFlow()", saved)

    saved!!
    assertEquals("Distributed Systems", saved.courseName)
    assertEquals(LocalTime.of(10, 0), saved.startTime)
    assertEquals(LocalTime.of(12, 0), saved.endTime)
    assertEquals(ClassType.LAB, saved.type)
    assertEquals("LAB-X", saved.location)
    assertEquals("Prof. Turing", saved.instructor)
  }
}
