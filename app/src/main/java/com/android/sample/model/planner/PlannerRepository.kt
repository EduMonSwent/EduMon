package com.android.sample.model.planner

import java.time.LocalDate
import java.time.LocalTime

class PlannerRepository {

    // In-memory storage for demo purposes
    private val attendanceRecords = mutableListOf<ClassAttendance>()

    suspend fun getTodayClasses(): List<Class> {
        // Mock data - replace with your actual data source
        return listOf(
            Class(
                id = "1",
                courseName = "Algorithms",
                startTime = LocalTime.of(9, 0),
                endTime = LocalTime.of(10, 0),
                type = ClassType.LECTURE,
                location = "INM 202",
                instructor = "Prof. Smith"
            ),
            Class(
                id = "2",
                courseName = "Data Structures",
                startTime = LocalTime.of(11, 0),
                endTime = LocalTime.of(12, 30),
                type = ClassType.EXERCISE,
                location = "BC 101",
                instructor = "Dr. Johnson"
            ),
            Class(
                id = "3",
                courseName = "Computer Networks",
                startTime = LocalTime.of(14, 0),
                endTime = LocalTime.of(16, 0),
                type = ClassType.LAB,
                location = "Lab A",
                instructor = "Prof. Davis"
            )
        ).sortedBy { it.startTime }
    }

    suspend fun saveAttendance(attendance: ClassAttendance) {
        // Remove existing record for same class and date
        attendanceRecords.removeAll { record ->
            record.classId == attendance.classId && record.date == attendance.date
        }
        // Add new record
        attendanceRecords.add(attendance)
    }

    suspend fun getTodayAttendanceRecords(): List<ClassAttendance> {
        val today = LocalDate.now()
        return attendanceRecords.filter { it.date == today }
    }

    fun getAttendanceForClass(classId: String): ClassAttendance? {
        val today = LocalDate.now()
        return attendanceRecords.find { it.classId == classId && it.date == today }
    }
}