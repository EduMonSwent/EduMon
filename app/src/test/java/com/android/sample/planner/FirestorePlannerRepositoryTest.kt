package com.android.sample.planner

import com.android.sample.feature.schedule.data.planner.AttendanceStatus
import com.android.sample.feature.schedule.data.planner.ClassAttendance
import com.android.sample.feature.schedule.data.planner.CompletionStatus
import com.android.sample.feature.schedule.repository.planner.FirestorePlannerRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class FirestorePlannerRepositoryTest {

    private fun repoWithNoUser(): FirestorePlannerRepository {
        val db: FirebaseFirestore = mock()
        val auth: FirebaseAuth = mock()
        whenever(auth.currentUser).thenReturn(null) // unsigned path
        return FirestorePlannerRepository(db, auth)
    }

    @Test
    fun getTodayClassesFlow_unsigned_falls_back_to_base_demo_classes() = runBlocking {
        val repo = repoWithNoUser()

        val classes = repo.getTodayClassesFlow().first()

        // Base PlannerRepository demo classes: Algorithms, Data Structures, Computer Networks
        assertEquals(3, classes.size)
        assertEquals(listOf("Algorithms", "Data Structures", "Computer Networks"),
            classes.map { it.courseName })
    }

    @Test
    fun getTodayAttendanceFlow_unsigned_initially_empty() = runBlocking {
        val repo = repoWithNoUser()

        val attendance = repo.getTodayAttendanceFlow().first()

        assertTrue(attendance.isEmpty())
    }

    @Test
    fun saveAttendance_unsigned_updates_local_attendance_flow() = runBlocking {
        val repo = repoWithNoUser()
        val today = LocalDate.now()
        val record = ClassAttendance(
            classId = "1",
            date = today,
            attendance = AttendanceStatus.YES,
            completion = CompletionStatus.YES
        )

        val result = repo.saveAttendance(record)
        assertTrue(result.isSuccess)

        val attendance = repo.getTodayAttendanceFlow().first()
        assertEquals(1, attendance.size)
        assertEquals("1", attendance[0].classId)
        assertEquals(today, attendance[0].date)
        assertEquals(AttendanceStatus.YES, attendance[0].attendance)
        assertEquals(CompletionStatus.YES, attendance[0].completion)
    }
}
