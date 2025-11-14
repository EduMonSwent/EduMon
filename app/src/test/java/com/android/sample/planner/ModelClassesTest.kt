package com.android.sample.planner

import com.android.sample.feature.schedule.data.planner.AttendanceStatus
import com.android.sample.feature.schedule.data.planner.Class
import com.android.sample.feature.schedule.data.planner.ClassAttendance
import com.android.sample.feature.schedule.data.planner.ClassType
import com.android.sample.feature.schedule.data.planner.CompletionStatus
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.*
import org.junit.Test

class ModelClassesTest {

  @Test
  fun `class should have correct properties`() {
    // Given
    val testClass =
        Class(
            id = "test-id",
            courseName = "Test Course",
            startTime = LocalTime.of(10, 0),
            endTime = LocalTime.of(11, 0),
            type = ClassType.LECTURE,
            location = "Test Room",
            instructor = "Test Instructor")

    // Then
    assertEquals("test-id", testClass.id)
    assertEquals("Test Course", testClass.courseName)
    assertEquals(LocalTime.of(10, 0), testClass.startTime)
    assertEquals(LocalTime.of(11, 0), testClass.endTime)
    assertEquals(ClassType.LECTURE, testClass.type)
    assertEquals("Test Room", testClass.location)
    assertEquals("Test Instructor", testClass.instructor)
  }

  @Test
  fun `class attendance should have correct properties`() {
    // Given
    val timestamp = Instant.now()
    val attendance =
        ClassAttendance(
            classId = "class-1",
            date = LocalDate.of(2024, 1, 1),
            attendance = AttendanceStatus.ARRIVED_LATE,
            completion = CompletionStatus.PARTIALLY,
            timestamp = timestamp)

    // Then
    assertEquals("class-1", attendance.classId)
    assertEquals(LocalDate.of(2024, 1, 1), attendance.date)
    assertEquals(AttendanceStatus.ARRIVED_LATE, attendance.attendance)
    assertEquals(CompletionStatus.PARTIALLY, attendance.completion)
    assertEquals(timestamp, attendance.timestamp)
  }

  @Test
  fun `enum values should be correct`() {
    // Then
    assertEquals(3, ClassType.values().size)
    assertEquals(3, AttendanceStatus.values().size)
    assertEquals(3, CompletionStatus.values().size)

    // Verify specific enum values
    assertTrue(ClassType.values().contains(ClassType.LECTURE))
    assertTrue(ClassType.values().contains(ClassType.EXERCISE))
    assertTrue(ClassType.values().contains(ClassType.LAB))

    assertTrue(AttendanceStatus.values().contains(AttendanceStatus.YES))
    assertTrue(AttendanceStatus.values().contains(AttendanceStatus.NO))
    assertTrue(AttendanceStatus.values().contains(AttendanceStatus.ARRIVED_LATE))

    assertTrue(CompletionStatus.values().contains(CompletionStatus.YES))
    assertTrue(CompletionStatus.values().contains(CompletionStatus.NO))
    assertTrue(CompletionStatus.values().contains(CompletionStatus.PARTIALLY))
  }
}
