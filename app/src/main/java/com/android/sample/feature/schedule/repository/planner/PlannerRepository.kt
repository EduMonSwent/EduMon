package com.android.sample.feature.schedule.repository.planner

import com.android.sample.feature.schedule.data.planner.AttendanceStatus
import com.android.sample.feature.schedule.data.planner.Class
import com.android.sample.feature.schedule.data.planner.ClassAttendance
import com.android.sample.feature.schedule.data.planner.ClassType
import com.android.sample.feature.schedule.data.planner.CompletionStatus
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

open class PlannerRepository {
  private val _attendanceRecords = MutableStateFlow<List<ClassAttendance>>(emptyList())
  private val attendanceRecords = _attendanceRecords.asStateFlow()

  open fun getTodayClassesFlow(): Flow<List<Class>> =
      attendanceRecords.map {
        listOf(
                Class(
                    id = "1",
                    courseName = "Algorithms",
                    startTime = LocalTime.of(9, 0),
                    endTime = LocalTime.of(10, 0),
                    type = ClassType.LECTURE,
                    location = "INM 202",
                    instructor = "Prof. Smith"),
                Class(
                    id = "2",
                    courseName = "Data Structures",
                    startTime = LocalTime.of(11, 0),
                    endTime = LocalTime.of(12, 30),
                    type = ClassType.EXERCISE,
                    location = "BC 101",
                    instructor = "Dr. Johnson"),
                Class(
                    id = "3",
                    courseName = "Computer Networks",
                    startTime = LocalTime.of(14, 0),
                    endTime = LocalTime.of(16, 0),
                    type = ClassType.LAB,
                    location = "Lab A",
                    instructor = "Prof. Davis"))
            .sortedBy { it.startTime }
      }

  open suspend fun saveAttendance(attendance: ClassAttendance): Result<Unit> {
    return try {
      val updated =
          _attendanceRecords.value.toMutableList().apply {
            removeAll { record ->
              record.classId == attendance.classId && record.date == attendance.date
            }
            add(attendance)
          }
      _attendanceRecords.value = updated
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  open fun getTodayAttendanceFlow(): Flow<List<ClassAttendance>> =
      attendanceRecords.map { records ->
        val today = LocalDate.now()
        records.filter { it.date == today }
      }

  open fun getAttendanceForClass(classId: String): Flow<ClassAttendance?> =
      attendanceRecords.map { records ->
        val today = LocalDate.now()
        records.find { it.classId == classId && it.date == today }
      }

  open suspend fun seedDemoData() {
    val today = LocalDate.now()
    _attendanceRecords.value =
        listOf(
            ClassAttendance(
                classId = "1",
                date = today,
                attendance = AttendanceStatus.YES,
                completion = CompletionStatus.YES,
                timestamp = Instant.now()))
  }

  open suspend fun saveClass(classItem: Class): Result<Unit> {
    return Result.failure(Exception("Not implemented"))
  }
}
