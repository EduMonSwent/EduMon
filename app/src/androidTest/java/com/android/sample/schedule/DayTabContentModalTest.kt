package com.android.sample.schedule

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import com.android.sample.R
import com.android.sample.data.Priority as TodoPriority
import com.android.sample.data.Status as TodoStatus
import com.android.sample.data.ToDo
import com.android.sample.feature.schedule.data.planner.AttendanceStatus
import com.android.sample.feature.schedule.data.planner.ClassType
import com.android.sample.feature.schedule.data.planner.CompletionStatus
import com.android.sample.feature.schedule.viewmodel.ScheduleUiState
import com.android.sample.feature.weeks.viewmodel.ObjectivesViewModel
import com.android.sample.ui.schedule.DayTabContent
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import org.junit.Rule
import org.junit.Test

class DayTabContentAllAndroidTest {

  @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
  private val ctx
    get() = rule.activity

  // ---- Test new branch: allClassesFinished ----
  @Test
  fun showsFinishedClasses_when_allClassesFinishedTrue() {
    val vm = buildScheduleVM(ctx)

    val state =
        ScheduleUiState(
            todayClasses = listOf(), // irrelevant
            attendanceRecords = emptyList(),
            allClassesFinished = true)

    rule.setContent {
      DayTabContent(vm = vm, state = state, objectivesVm = ObjectivesViewModel(requireAuth = false))
    }

    rule.onNodeWithText(ctx.getString(R.string.finished_classes)).assertIsDisplayed()
  }

  // ---- Test original empty-classes branch ----
  @Test
  fun showsNoClassesMessage_whenEmpty_andNotFinished() {
    val vm = buildScheduleVM(ctx)

    val state =
        ScheduleUiState(
            todayClasses = emptyList(), attendanceRecords = emptyList(), allClassesFinished = false)

    rule.setContent {
      DayTabContent(vm = vm, state = state, objectivesVm = ObjectivesViewModel(requireAuth = false))
    }

    rule.onNodeWithText(ctx.getString(R.string.no_classes_today)).assertIsDisplayed()
  }

  // ---- Attendance YES/YES ----
  @Test
  fun showsAttendanceAndCompletionChips_yes_yes() {
    val vm = buildScheduleVM(ctx)
    val clazz = fakeClass(id = "c1", name = "Algorithms")
    val record = fakeAttendance("c1") // default YES/YES

    val state = ScheduleUiState(todayClasses = listOf(clazz), attendanceRecords = listOf(record))

    rule.setContent { DayTabContent(vm, state, ObjectivesViewModel(requireAuth = false)) }

    rule.onNodeWithText(ctx.getString(R.string.attendance_attended)).assertIsDisplayed()
    rule.onNodeWithText(ctx.getString(R.string.completion_done)).assertIsDisplayed()
  }

  // ---- Attendance: ARRIVED_LATE / PARTIALLY ----
  @Test
  fun showsLateAndPartialStatuses() {
    val vm = buildScheduleVM(ctx)
    val clazz = fakeClass(id = "c2", name = "Databases")

    val latePartial =
        fakeAttendance(
            classId = "c2",
            attendance = AttendanceStatus.ARRIVED_LATE,
            completion = CompletionStatus.PARTIALLY)

    val state =
        ScheduleUiState(todayClasses = listOf(clazz), attendanceRecords = listOf(latePartial))

    rule.setContent { DayTabContent(vm, state, ObjectivesViewModel(requireAuth = false)) }

    rule.onNodeWithText(ctx.getString(R.string.attendance_arrived_late)).assertIsDisplayed()
    rule.onNodeWithText(ctx.getString(R.string.completion_partially)).assertIsDisplayed()
  }

  // ---- Attendance: NO / NO ----
  @Test
  fun showsMissedAndNotDoneStatuses() {
    val vm = buildScheduleVM(ctx)
    val clazz = fakeClass(id = "c3", name = "OS")

    val missedNotDone =
        fakeAttendance(
            classId = "c3", attendance = AttendanceStatus.NO, completion = CompletionStatus.NO)

    val state =
        ScheduleUiState(todayClasses = listOf(clazz), attendanceRecords = listOf(missedNotDone))

    rule.setContent { DayTabContent(vm, state, ObjectivesViewModel(requireAuth = false)) }

    rule.onNodeWithText(ctx.getString(R.string.attendance_missed)).assertIsDisplayed()
    rule.onNodeWithText(ctx.getString(R.string.completion_not_done)).assertIsDisplayed()
  }

  // ---- Wellness block ----
  @Test
  fun rendersWellnessEvents() {
    val vm = buildScheduleVM(ctx)
    val clazz = fakeClass("c9", "AI")
    val state = ScheduleUiState(todayClasses = listOf(clazz))

    rule.setContent {
      Column(Modifier.verticalScroll(rememberScrollState())) {
        DayTabContent(vm, state, ObjectivesViewModel(requireAuth = false))
      }
    }

    rule
        .onNodeWithText(ctx.getString(R.string.wellness_events_label))
        .performScrollTo()
        .assertIsDisplayed()

    rule
        .onNodeWithText(ctx.getString(R.string.wellness_event_yoga_title))
        .performScrollTo()
        .assertIsDisplayed()
    rule
        .onNodeWithText(ctx.getString(R.string.wellness_event_lecture_title))
        .performScrollTo()
        .assertIsDisplayed()
  }

  @Test
  fun showsEmptyTodosMessage() {
    val vm = buildScheduleVM(ctx)

    val state =
        ScheduleUiState(
            todayClasses = emptyList(), attendanceRecords = emptyList(), todos = emptyList())

    rule.setContent {
      Column(Modifier.verticalScroll(rememberScrollState())) {
        DayTabContent(vm, state, ObjectivesViewModel(requireAuth = false))
      }
    }

    rule
        .onNodeWithText(ctx.getString(R.string.schedule_day_todos_title))
        .performScrollTo()
        .assertIsDisplayed()

    rule
        .onNodeWithText(ctx.getString(R.string.schedule_day_todos_empty))
        .performScrollTo()
        .assertIsDisplayed()
  }

  // ---- Modal branch ----
  @Test
  fun attendanceModal_isShownWhenStateFlagTrue() {
    val ctx = appContext()
    val vm = buildScheduleVM(ctx)
    val clazz = fakeClass(id = "c1")

    val state =
        ScheduleUiState(
            todayClasses = listOf(clazz),
            attendanceRecords = emptyList(),
            showAttendanceModal = true,
            selectedClass = clazz,
        )

    rule.setContent {
      DayTabContent(vm = vm, state = state, objectivesVm = ObjectivesViewModel(requireAuth = false))
    }

    rule.onNodeWithText(ctx.getString(R.string.confirm_attendance_completion)).assertIsDisplayed()
  }

  // ---- Header & class details ----
  @Test
  fun showsTodayHeader_andClassDetails() {
    val ctx = rule.activity
    val clazz =
        fakeClass(
            name = "Algorithms",
            location = "BC02",
            instructor = "Dr. Smith",
            type = ClassType.LECTURE)
    val state = ScheduleUiState(todayClasses = listOf(clazz))
    val vm = buildScheduleVM(ctx)

    rule.setContent {
      Column(Modifier.verticalScroll(rememberScrollState())) {
        DayTabContent(
            vm = vm, state = state, objectivesVm = ObjectivesViewModel(requireAuth = false))
      }
    }

    val dateText = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMM d"))
    val todayHeader = ctx.getString(R.string.today_title_fmt, dateText)
    val lectureLabel = ctx.getString(R.string.lecture_type)

    // Header
    rule.onNodeWithText(todayHeader).performScrollTo().assertIsDisplayed()

    // "Algorithms"
    rule.onNodeWithText("Algorithms", useUnmergedTree = true).performScrollTo().assertIsDisplayed()

    // "(Lecture)" — exactly how CompactClassRow renders it
    rule
        .onNodeWithText("($lectureLabel)", useUnmergedTree = true)
        .performScrollTo()
        .assertIsDisplayed()

    // "BC02 • Dr. Smith"
    rule
        .onNodeWithText("BC02 • Dr. Smith", useUnmergedTree = true)
        .performScrollTo()
        .assertIsDisplayed()
  }

  // ---- Empty classes branch ----
  @Test
  fun showsNoClassesMessage_whenEmpty() {
    val ctx = rule.activity
    val vm = buildScheduleVM(ctx)
    val state = ScheduleUiState(todayClasses = emptyList())

    rule.setContent {
      DayTabContent(vm = vm, state = state, objectivesVm = ObjectivesViewModel(requireAuth = false))
    }

    rule.onNodeWithText(ctx.getString(R.string.no_classes_today)).assertIsDisplayed()
  }

  // ---- Attendance/Completion chips: YES/YES ----
  @Test
  fun showsAttendanceAndCompletionChips_whenRecordPresent_yes_yes() {
    val ctx = appContext()
    val vm = buildScheduleVM(ctx)
    val clazz = fakeClass(id = "c1", name = "Algorithms")
    val record = fakeAttendance(classId = "c1") // default YES/YES in your helpers

    val state = ScheduleUiState(todayClasses = listOf(clazz), attendanceRecords = listOf(record))

    rule.setContent {
      DayTabContent(vm = vm, state = state, objectivesVm = ObjectivesViewModel(requireAuth = false))
    }

    rule.onNodeWithText(ctx.getString(R.string.attendance_attended)).assertIsDisplayed()
    rule.onNodeWithText(ctx.getString(R.string.completion_done)).assertIsDisplayed()
  }

  // ---- Attendance/Completion chips: ARRIVED_LATE/PARTIALLY ----
  @Test
  fun showsLateAndPartialStatuses_whenRecordPresent() {
    val ctx = appContext()
    val vm = buildScheduleVM(ctx)
    val clazz = fakeClass(id = "c2", name = "Databases")

    val latePartial =
        fakeAttendance(
            classId = "c2",
            attendance = AttendanceStatus.ARRIVED_LATE,
            completion = CompletionStatus.PARTIALLY)

    val state =
        ScheduleUiState(todayClasses = listOf(clazz), attendanceRecords = listOf(latePartial))

    rule.setContent {
      DayTabContent(vm = vm, state = state, objectivesVm = ObjectivesViewModel(requireAuth = false))
    }

    rule.onNodeWithText(ctx.getString(R.string.attendance_arrived_late)).assertIsDisplayed()
    rule.onNodeWithText(ctx.getString(R.string.completion_partially)).assertIsDisplayed()
  }

  // ---- Attendance/Completion chips: NO/NO ----
  @Test
  fun showsMissedAndNotDoneStatuses_whenRecordPresent() {
    val ctx = appContext()
    val vm = buildScheduleVM(ctx)
    val clazz = fakeClass(id = "c3", name = "OS")

    val missedNotDone =
        fakeAttendance(
            classId = "c3", attendance = AttendanceStatus.NO, completion = CompletionStatus.NO)

    val state =
        ScheduleUiState(todayClasses = listOf(clazz), attendanceRecords = listOf(missedNotDone))

    rule.setContent {
      DayTabContent(vm = vm, state = state, objectivesVm = ObjectivesViewModel(requireAuth = false))
    }

    rule.onNodeWithText(ctx.getString(R.string.attendance_missed)).assertIsDisplayed()
    rule.onNodeWithText(ctx.getString(R.string.completion_not_done)).assertIsDisplayed()
  }

  // ---- Wellness events block texts ----
  @Test
  fun rendersWellnessEvents_withCorrectTexts() {
    val ctx = rule.activity
    val vm = buildScheduleVM(ctx)
    val clazz = fakeClass(id = "c9", name = "AI") // any class to render the card

    val state = ScheduleUiState(todayClasses = listOf(clazz))

    rule.setContent {
      Column(Modifier.verticalScroll(rememberScrollState())) {
        DayTabContent(
            vm = vm, state = state, objectivesVm = ObjectivesViewModel(requireAuth = false))
      }
    }

    // Scroll to the Wellness section header first
    rule
        .onNodeWithText(ctx.getString(R.string.wellness_events_label))
        .performScrollTo()
        .assertIsDisplayed()

    // Yoga item
    rule
        .onNodeWithText(ctx.getString(R.string.wellness_event_yoga_title))
        .performScrollTo()
        .assertIsDisplayed()
    rule
        .onNodeWithText(ctx.getString(R.string.wellness_event_yoga_time))
        .performScrollTo()
        .assertIsDisplayed()
    rule
        .onNodeWithText(ctx.getString(R.string.wellness_event_yoga_description))
        .performScrollTo()
        .assertIsDisplayed()

    // Lecture item
    rule
        .onNodeWithText(ctx.getString(R.string.wellness_event_lecture_title))
        .performScrollTo()
        .assertIsDisplayed()
    rule
        .onNodeWithText(ctx.getString(R.string.wellness_event_lecture_time))
        .performScrollTo()
        .assertIsDisplayed()
    rule
        .onNodeWithText(ctx.getString(R.string.wellness_event_lecture_description))
        .performScrollTo()
        .assertIsDisplayed()
  }

  @Test
  fun dayTab_showsEmptyTodosMessage_whenNoTodosForToday() {
    val ctx = rule.activity
    val vm = buildScheduleVM(ctx) // your existing helper
    val state =
        ScheduleUiState(
            todayClasses = emptyList(), attendanceRecords = emptyList(), todos = emptyList())

    rule.setContent {
      Column(Modifier.verticalScroll(rememberScrollState())) {
        DayTabContent(
            vm = vm, state = state, objectivesVm = ObjectivesViewModel(requireAuth = false))
      }
    }

    val title = ctx.getString(R.string.schedule_day_todos_title)
    val empty = ctx.getString(R.string.schedule_day_todos_empty)

    // Scroll down to the title/section
    rule.onNodeWithText(title).performScrollTo().assertIsDisplayed()
    rule.onNodeWithText(empty).performScrollTo().assertIsDisplayed()
  }

  @Test
  fun dayTab_showsOnlyTodayTodos() {
    val ctx = rule.activity
    val vm = buildScheduleVM(ctx)
    val today = LocalDate.now()
    val tomorrow = today.plusDays(1)

    val todayTodo =
        ToDo(
            title = "Today task",
            dueDate = today,
            priority = TodoPriority.MEDIUM,
            status = TodoStatus.TODO)
    val tomorrowTodo =
        ToDo(
            title = "Tomorrow task",
            dueDate = tomorrow,
            priority = TodoPriority.MEDIUM,
            status = TodoStatus.TODO)

    val state =
        ScheduleUiState(
            todayClasses = emptyList(),
            attendanceRecords = emptyList(),
            todos = listOf(todayTodo, tomorrowTodo))

    rule.setContent {
      Column(Modifier.verticalScroll(rememberScrollState())) {
        DayTabContent(
            vm = vm, state = state, objectivesVm = ObjectivesViewModel(requireAuth = false))
      }
    }

    // Only today's task should appear in the "Today's To-Dos" section
    rule.onNodeWithText("Today task", substring = false).performScrollTo().assertIsDisplayed()
    rule.onNodeWithText("Tomorrow task").assertDoesNotExist()
  }
}
