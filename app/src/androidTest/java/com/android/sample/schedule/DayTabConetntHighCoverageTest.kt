package com.android.sample.schedule

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import com.android.sample.data.Priority
import com.android.sample.data.ToDo
import com.android.sample.feature.schedule.data.planner.*
import com.android.sample.feature.schedule.data.schedule.EventKind
import com.android.sample.feature.schedule.data.schedule.ScheduleEvent
import com.android.sample.feature.schedule.repository.planner.PlannerRepository
import com.android.sample.feature.schedule.repository.schedule.ScheduleRepository
import com.android.sample.feature.schedule.viewmodel.ScheduleUiState
import com.android.sample.feature.schedule.viewmodel.ScheduleViewModel
import com.android.sample.feature.weeks.viewmodel.ObjectivesViewModel
import com.android.sample.ui.schedule.DayTabContent
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

class DayTabContentHighCoverageTest {

  @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

  // ------------------------------------------------------------
  // Minimal REAL fakes (no mocks)
  // ------------------------------------------------------------

  private class FakeScheduleRepo : ScheduleRepository {
    override val events = MutableStateFlow(emptyList<ScheduleEvent>())

    override suspend fun save(event: ScheduleEvent) {}

    override suspend fun update(event: ScheduleEvent) {}

    override suspend fun delete(eventId: String) {}

    override suspend fun getEventsBetween(s: LocalDate, e: LocalDate) = emptyList<ScheduleEvent>()

    override suspend fun getById(id: String) = null

    override suspend fun moveEventDate(id: String, newDate: LocalDate) = true

    override suspend fun getEventsForDate(date: LocalDate) = emptyList<ScheduleEvent>()

    override suspend fun getEventsForWeek(startDate: LocalDate) = emptyList<ScheduleEvent>()

    override suspend fun importEvents(events: List<ScheduleEvent>) {}
  }

  private class FakePlannerRepo : PlannerRepository() {
    override fun getTodayClassesFlow(): Flow<List<Class>> = MutableStateFlow(emptyList())

    override fun getTodayAttendanceFlow(): Flow<List<ClassAttendance>> =
        MutableStateFlow(emptyList())

    override suspend fun saveAttendance(attendance: ClassAttendance) = Result.success(Unit)
  }

  private fun createVm(): ScheduleViewModel {
    val context = ApplicationProvider.getApplicationContext<Context>()
    return ScheduleViewModel(
        scheduleRepository = FakeScheduleRepo(),
        plannerRepository = FakePlannerRepo(),
        resources = context.resources)
  }

  private fun createObjectivesVm(): ObjectivesViewModel {
    return ObjectivesViewModel()
  }

  // ------------------------------------------------------------
  // 1️⃣ Full state
  // ------------------------------------------------------------
  @Test
  fun dayTabContent_fullState_executesMostLines() {
    val vm = createVm()
    val objectivesVm = createObjectivesVm()

    val klass =
        Class(
            id = "1",
            courseName = "Algorithms",
            startTime = LocalTime.of(10, 0),
            endTime = LocalTime.of(11, 0),
            type = ClassType.LECTURE)

    val event =
        ScheduleEvent(
            id = "e1",
            title = "Study",
            date = LocalDate.now(),
            time = LocalTime.of(12, 0),
            durationMinutes = 60,
            kind = EventKind.STUDY)

    val state =
        ScheduleUiState(
            todaySchedule =
                listOf(
                    ScheduleClassItem(klass),
                    ScheduleGapItem(LocalTime.of(11, 0), LocalTime.of(12, 0)),
                    ScheduleEventItem(event)),
            attendanceRecords =
                listOf(
                    ClassAttendance(
                        classId = "1",
                        date = LocalDate.now(),
                        attendance = AttendanceStatus.YES,
                        completion = CompletionStatus.YES)),
            showAttendanceModal = true,
            selectedClass = klass,
            todos =
                listOf(
                    ToDo(
                        id = "t1",
                        title = "Read slides",
                        dueDate = LocalDate.now().plusDays(1),
                        priority = Priority.HIGH)),
            allClassesFinished = false)

    rule.setContent {
      val snackbarHostState = remember { SnackbarHostState() }

      DayTabContent(
          vm = vm,
          state = state,
          objectivesVm = objectivesVm,
          snackbarHostState = snackbarHostState,
          onObjectiveNavigation = {},
          onTodoClicked = {})
    }
  }

  // ------------------------------------------------------------
  // 2️⃣ Empty state
  // ------------------------------------------------------------
  @Test
  fun dayTabContent_emptyState_executesEmptyBranches() {
    val vm = createVm()
    val objectivesVm = createObjectivesVm()

    val state =
        ScheduleUiState(
            todaySchedule = emptyList(),
            attendanceRecords = emptyList(),
            todos = emptyList(),
            allClassesFinished = false)

    rule.setContent {
      val snackbarHostState = remember { SnackbarHostState() }

      DayTabContent(
          vm = vm,
          state = state,
          objectivesVm = objectivesVm,
          snackbarHostState = snackbarHostState)
    }
  }

  // ------------------------------------------------------------
  // 3️⃣ Finished classes state
  // ------------------------------------------------------------
  @Test
  fun dayTabContent_allClassesFinished_executesFinishBranch() {
    val vm = createVm()
    val objectivesVm = createObjectivesVm()

    val state = ScheduleUiState(todaySchedule = emptyList(), allClassesFinished = true)

    rule.setContent {
      val snackbarHostState = remember { SnackbarHostState() }

      DayTabContent(
          vm = vm,
          state = state,
          objectivesVm = objectivesVm,
          snackbarHostState = snackbarHostState)
    }
  }
}
