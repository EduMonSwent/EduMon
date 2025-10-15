package com.android.sample.planner

import com.android.sample.model.planner.*
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.*
import org.junit.Test

class PlannerDataTest {

    @Test
    fun `class equality and toString should work correctly`() {
        val c1 = Class("1", "Math", LocalTime.of(8, 0), LocalTime.of(9, 0),
            ClassType.LECTURE, "Room A", "Prof X")
        val c2 = c1.copy()
        assertEquals(c1, c2)
        assertTrue(c1.toString().contains("Math"))
        assertEquals(c1.hashCode(), c2.hashCode())
    }

    @Test
    fun `attendance should support equals and default timestamp`() {
        val today = LocalDate.now()
        val a1 = ClassAttendance("1", today, AttendanceStatus.YES, CompletionStatus.NO)
        val a2 = a1.copy()
        assertEquals(a1, a2)
        assertNotNull(a1.timestamp)
        assertEquals(a1.hashCode(), a2.hashCode())
    }

    @Test
    fun `enum names should be unique`() {
        assertEquals(setOf("LECTURE","EXERCISE","LAB"), ClassType.values().map { it.name }.toSet())
        assertEquals(setOf("YES","NO","ARRIVED_LATE"), AttendanceStatus.values().map { it.name }.toSet())
        assertEquals(setOf("YES","NO","PARTIALLY"), CompletionStatus.values().map { it.name }.toSet())
    }
}
