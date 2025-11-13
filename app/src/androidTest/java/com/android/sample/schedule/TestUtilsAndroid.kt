package com.android.sample.schedule

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.android.sample.feature.schedule.data.planner.AttendanceStatus
import com.android.sample.feature.schedule.data.planner.Class
import com.android.sample.feature.schedule.data.planner.ClassAttendance
import com.android.sample.feature.schedule.data.planner.ClassType
import com.android.sample.feature.schedule.data.planner.CompletionStatus
import com.android.sample.feature.schedule.repository.calendar.CalendarRepositoryImpl
import com.android.sample.feature.schedule.repository.planner.FakePlannerRepository
import com.android.sample.feature.schedule.repository.schedule.ScheduleRepositoryImpl
import com.android.sample.feature.schedule.viewmodel.ScheduleViewModel
import java.time.LocalDate
import java.time.LocalTime

fun appContext(): Context = ApplicationProvider.getApplicationContext()

fun buildScheduleVM(context: Context = appContext()): ScheduleViewModel {
  val tasks = CalendarRepositoryImpl()
  val classes = FakePlannerRepository()
  val repo = ScheduleRepositoryImpl(tasks, classes, context.resources)
  return ScheduleViewModel(repo, classes, context.resources)
}

fun fakeClass(
    id: String = "c1",
    name: String = "Algorithms",
    type: ClassType = ClassType.LECTURE,
    start: LocalTime = LocalTime.of(10, 0),
    end: LocalTime = LocalTime.of(12, 0),
    location: String = "BC02",
    instructor: String = "Dr. Smith",
    date: LocalDate = LocalDate.now()
) =
    Class(
        id = id,
        courseName = name,
        type = type,
        startTime = start,
        endTime = end,
        location = location,
        instructor = instructor)

fun fakeAttendance(
    classId: String = "c1",
    attendance: AttendanceStatus = AttendanceStatus.YES,
    completion: CompletionStatus = CompletionStatus.YES
) =
    ClassAttendance(
        classId = classId, date = LocalDate.now(), attendance = attendance, completion = completion)
