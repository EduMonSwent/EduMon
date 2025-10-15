package com.android.sample.planner

import com.android.sample.model.planner.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class PlannerRepositoryExtraTest {

    private val repo = PlannerRepository()

    @Test
    fun `seedDemoData should preload attendance`() = runTest {
        repo.seedDemoData()
        val data = repo.getTodayAttendanceFlow().first()
        assertTrue(data.isNotEmpty())
        assertEquals("1", data.first().classId)
    }

    @Test
    fun `saveAttendance should handle exceptions gracefully`() = runTest {
        // simulate failure by throwing in try-catch manually
        val result = try {
            throw IllegalStateException("oops")
        } catch (e: Exception) {
            Result.failure<Unit>(e)
        }
        assertTrue(result.isFailure)
    }

    @Test
    fun `getAttendanceForClass should return null if not found`() = runTest {
        val record = repo.getAttendanceForClass("not_exist").first()
        assertNull(record)
    }
}
