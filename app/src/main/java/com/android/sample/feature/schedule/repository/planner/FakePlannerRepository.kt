package com.android.sample.feature.schedule.repository.planner

import com.android.sample.feature.schedule.data.planner.Class
import com.android.sample.feature.schedule.data.planner.ClassAttendance
import com.android.sample.feature.schedule.data.planner.ClassType
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class FakePlannerRepository : PlannerRepository() {

  override fun getTodayClassesFlow(): Flow<List<Class>> = flow {
    emit(
        listOf(
            Class(
                "1",
                "Algorithms",
                LocalTime.of(9, 0),
                LocalTime.of(10, 0),
                ClassType.LECTURE,
                "INM202",
                "Prof. Smith"),
            Class(
                "2",
                "Data Structures",
                LocalTime.of(11, 0),
                LocalTime.of(12, 30),
                ClassType.EXERCISE,
                "BC101",
                "Dr. Johnson"),
            Class(
                "3",
                "Networks",
                LocalTime.of(14, 0),
                LocalTime.of(16, 0),
                ClassType.LAB,
                "LabA",
                "Prof. Davis")))
  }

  override fun getTodayAttendanceFlow(): Flow<List<ClassAttendance>> = flow {
    emit(emptyList()) // initially no attendance
  }

  override suspend fun saveAttendance(attendance: ClassAttendance): Result<Unit> {
    // succeed immediately
    return Result.success(Unit)
  }
}
