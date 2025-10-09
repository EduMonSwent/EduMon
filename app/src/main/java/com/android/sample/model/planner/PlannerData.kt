package com.android.sample.model.planner

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

data class Class(
    val id: String,
    val courseName: String,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val type: ClassType,
    val location: String = "",
    val instructor: String = ""
)

enum class ClassType {
  LECTURE,
  EXERCISE,
  LAB
}

data class ClassAttendance(
    val classId: String,
    val date: LocalDate,
    val attendance: AttendanceStatus,
    val completion: CompletionStatus,
    val timestamp: Instant = Instant.now()
)

enum class AttendanceStatus {
  YES,
  NO,
  ARRIVED_LATE
}

enum class CompletionStatus {
  YES,
  NO,
  PARTIALLY
}
