package com.android.sample.planner

import com.android.sample.model.planner.*
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.*
import org.junit.Test

class ModelClassesComprehensiveTest {

  @Test
  fun `class data class should have correct properties and equality`() {
    // Given
    val time1 = LocalTime.of(9, 0)
    val time2 = LocalTime.of(10, 0)
    val class1 =
        Class(
            id = "test-id",
            courseName = "Test Course",
            startTime = time1,
            endTime = time2,
            type = ClassType.LECTURE,
            location = "Test Room",
            instructor = "Test Instructor")

    val class2 =
        Class(
            id = "test-id",
            courseName = "Test Course",
            startTime = time1,
            endTime = time2,
            type = ClassType.LECTURE,
            location = "Test Room",
            instructor = "Test Instructor")

    // Then
    assertEquals("test-id", class1.id)
    assertEquals("Test Course", class1.courseName)
    assertEquals(time1, class1.startTime)
    assertEquals(time2, class1.endTime)
    assertEquals(ClassType.LECTURE, class1.type)
    assertEquals("Test Room", class1.location)
    assertEquals("Test Instructor", class1.instructor)

    // Test equality
    assertEquals(class1, class2)
    assertEquals(class1.hashCode(), class2.hashCode())
  }

  @Test
  fun `class with default parameters`() {
    // Given
    val classWithDefaults =
        Class(
            id = "test-id",
            courseName = "Test Course",
            startTime = LocalTime.of(9, 0),
            endTime = LocalTime.of(10, 0),
            type = ClassType.LECTURE)

    // Then
    assertEquals("", classWithDefaults.location)
    assertEquals("", classWithDefaults.instructor)
  }

  @Test
  fun `class attendance data class should have correct properties`() {
    // Given
    val timestamp = Instant.now()
    val date = LocalDate.of(2024, 1, 15)
    val attendance =
        ClassAttendance(
            classId = "class-1",
            date = date,
            attendance = AttendanceStatus.ARRIVED_LATE,
            completion = CompletionStatus.PARTIALLY,
            timestamp = timestamp)

    // Then
    assertEquals("class-1", attendance.classId)
    assertEquals(date, attendance.date)
    assertEquals(AttendanceStatus.ARRIVED_LATE, attendance.attendance)
    assertEquals(CompletionStatus.PARTIALLY, attendance.completion)
    assertEquals(timestamp, attendance.timestamp)
  }

  @Test
  fun `class attendance with default timestamp`() {
    // Given
    val attendance =
        ClassAttendance(
            classId = "class-1",
            date = LocalDate.now(),
            attendance = AttendanceStatus.YES,
            completion = CompletionStatus.YES)

    // Then
    assertNotNull(attendance.timestamp)
  }

  @Test
  fun `class type enum should have all expected values`() {
    // When
    val values = ClassType.values()

    // Then
    assertEquals(3, values.size)
    assertTrue(values.contains(ClassType.LECTURE))
    assertTrue(values.contains(ClassType.EXERCISE))
    assertTrue(values.contains(ClassType.LAB))
  }

  @Test
  fun `attendance status enum should have all expected values`() {
    // When
    val values = AttendanceStatus.values()

    // Then
    assertEquals(3, values.size)
    assertTrue(values.contains(AttendanceStatus.YES))
    assertTrue(values.contains(AttendanceStatus.NO))
    assertTrue(values.contains(AttendanceStatus.ARRIVED_LATE))
  }

  @Test
  fun `completion status enum should have all expected values`() {
    // When
    val values = CompletionStatus.values()

    // Then
    assertEquals(3, values.size)
    assertTrue(values.contains(CompletionStatus.YES))
    assertTrue(values.contains(CompletionStatus.NO))
    assertTrue(values.contains(CompletionStatus.PARTIALLY))
  }

  @Test
  fun `wellness event type enum properties should be accessible`() {
    // Then
    WellnessEventType.values().forEach { type ->
      assertNotNull(type.iconRes)
      assertNotNull(type.primaryColor)
    }
  }

  @Test
  fun `wellness event type fromTitle should correctly identify types`() {
    // Test cases for each type
    assertEquals(WellnessEventType.YOGA, WellnessEventType.fromTitle("Yoga Class"))
    assertEquals(WellnessEventType.YOGA, WellnessEventType.fromTitle("morning yoga"))
    assertEquals(WellnessEventType.YOGA, WellnessEventType.fromTitle("YOGA session"))

    assertEquals(WellnessEventType.LECTURE, WellnessEventType.fromTitle("Physics Lecture"))
    assertEquals(WellnessEventType.LECTURE, WellnessEventType.fromTitle("Guest Talk"))
    assertEquals(WellnessEventType.LECTURE, WellnessEventType.fromTitle("LECTURE on AI"))

    assertEquals(WellnessEventType.SPORTS, WellnessEventType.fromTitle("Sports Day"))
    assertEquals(WellnessEventType.SPORTS, WellnessEventType.fromTitle("Fitness Training"))
    assertEquals(WellnessEventType.SPORTS, WellnessEventType.fromTitle("SPORTS event"))

    assertEquals(WellnessEventType.SOCIAL, WellnessEventType.fromTitle("Social Gathering"))
    assertEquals(WellnessEventType.SOCIAL, WellnessEventType.fromTitle("Campus Party"))
    assertEquals(WellnessEventType.SOCIAL, WellnessEventType.fromTitle("SOCIAL event"))

    assertEquals(WellnessEventType.MUSIC, WellnessEventType.fromTitle("Music Concert"))
    assertEquals(WellnessEventType.MUSIC, WellnessEventType.fromTitle("Live Concert"))
    assertEquals(WellnessEventType.MUSIC, WellnessEventType.fromTitle("MUSIC festival"))

    assertEquals(WellnessEventType.DEFAULT, WellnessEventType.fromTitle("Unknown Event"))
    assertEquals(WellnessEventType.DEFAULT, WellnessEventType.fromTitle("Regular Meeting"))
    assertEquals(WellnessEventType.DEFAULT, WellnessEventType.fromTitle(""))
  }

  @Test
  fun `wellness event type fromTitle should be case insensitive`() {
    assertEquals(WellnessEventType.YOGA, WellnessEventType.fromTitle("yoga"))
    assertEquals(WellnessEventType.YOGA, WellnessEventType.fromTitle("YOGA"))
    assertEquals(WellnessEventType.YOGA, WellnessEventType.fromTitle("Yoga"))
  }

  @Test
  fun `enum valueOf should work correctly`() {
    // Test valueOf for each enum
    assertEquals(ClassType.LECTURE, ClassType.valueOf("LECTURE"))
    assertEquals(ClassType.EXERCISE, ClassType.valueOf("EXERCISE"))
    assertEquals(ClassType.LAB, ClassType.valueOf("LAB"))

    assertEquals(AttendanceStatus.YES, AttendanceStatus.valueOf("YES"))
    assertEquals(AttendanceStatus.NO, AttendanceStatus.valueOf("NO"))
    assertEquals(AttendanceStatus.ARRIVED_LATE, AttendanceStatus.valueOf("ARRIVED_LATE"))

    assertEquals(CompletionStatus.YES, CompletionStatus.valueOf("YES"))
    assertEquals(CompletionStatus.NO, CompletionStatus.valueOf("NO"))
    assertEquals(CompletionStatus.PARTIALLY, CompletionStatus.valueOf("PARTIALLY"))
  }
}
