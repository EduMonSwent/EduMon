package com.android.sample.schedule

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.android.sample.R
import com.android.sample.data.Priority as TodoPriority
import com.android.sample.data.Status as TodoStatus
import com.android.sample.data.ToDo
import com.android.sample.feature.schedule.data.planner.AttendanceStatus
import com.android.sample.feature.schedule.data.planner.ClassType
import com.android.sample.feature.schedule.data.planner.CompletionStatus
import com.android.sample.feature.schedule.data.planner.ScheduleClassItem
import com.android.sample.feature.schedule.data.planner.ScheduleEventItem
import com.android.sample.feature.schedule.data.planner.ScheduleGapItem
import com.android.sample.feature.schedule.data.schedule.EventKind
import com.android.sample.feature.schedule.data.schedule.ScheduleEvent
import com.android.sample.feature.schedule.viewmodel.ScheduleUiState
import com.android.sample.feature.weeks.viewmodel.ObjectivesViewModel
import com.android.sample.ui.schedule.DayTabContent
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import org.junit.Rule
import org.junit.Test

class DayTabContentAllAndroidTest {

  @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
  private val ctx
    get() = rule.activity

  // ---- Test new branch: allClassesFinished ----
  @Test
  fun doesNotShowNoClassesMessage_whenAllClassesFinished() {
    val ctx = rule.activity
    val vm = buildScheduleVM(ctx)

    val state =
        ScheduleUiState(
            todaySchedule = emptyList(), attendanceRecords = emptyList(), allClassesFinished = true)

    rule.setContent {
      DayTabContent(vm = vm, state = state, objectivesVm = ObjectivesViewModel(requireAuth = false))
    }

    // "No classes today" should NOT be shown when allClassesFinished = true
    rule.onNodeWithText(ctx.getString(R.string.no_classes_today)).assertDoesNotExist()
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

    val state =
        ScheduleUiState(
            todaySchedule = listOf(ScheduleClassItem(clazz)), attendanceRecords = listOf(record))

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
        ScheduleUiState(
            todaySchedule = listOf(ScheduleClassItem(clazz)),
            attendanceRecords = listOf(latePartial))

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
        ScheduleUiState(
            todaySchedule = listOf(ScheduleClassItem(clazz)),
            attendanceRecords = listOf(missedNotDone))

    rule.setContent { DayTabContent(vm, state, ObjectivesViewModel(requireAuth = false)) }

    rule.onNodeWithText(ctx.getString(R.string.attendance_missed)).assertIsDisplayed()
    rule.onNodeWithText(ctx.getString(R.string.completion_not_done)).assertIsDisplayed()
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
    val state = ScheduleUiState(todaySchedule = listOf(ScheduleClassItem(clazz)))
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

    val state =
        ScheduleUiState(
            todaySchedule = listOf(ScheduleClassItem(clazz)), attendanceRecords = listOf(record))

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
        ScheduleUiState(
            todaySchedule = listOf(ScheduleClassItem(clazz)),
            attendanceRecords = listOf(latePartial))

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
        ScheduleUiState(
            todaySchedule = listOf(ScheduleClassItem(clazz)),
            attendanceRecords = listOf(missedNotDone))

    rule.setContent {
      DayTabContent(vm = vm, state = state, objectivesVm = ObjectivesViewModel(requireAuth = false))
    }

    rule.onNodeWithText(ctx.getString(R.string.attendance_missed)).assertIsDisplayed()
    rule.onNodeWithText(ctx.getString(R.string.completion_not_done)).assertIsDisplayed()
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

  @Test
  fun showsGapItem_whenScheduleHasGap() {
    val vm = buildScheduleVM(rule.activity)

    val gap = ScheduleGapItem(start = LocalTime.of(10, 0), end = LocalTime.of(10, 30))

    val state = ScheduleUiState(todaySchedule = listOf(gap))

    rule.setContent {
      DayTabContent(vm = vm, state = state, objectivesVm = ObjectivesViewModel(requireAuth = false))
    }

    rule.onNodeWithText("Free Time (30 min)").assertIsDisplayed()
    rule.onNodeWithText("10:00 - 10:30").assertIsDisplayed()
  }

  @Test
  fun gapOptionsModal_isVisible_whenFlagIsTrue() {
    val vm = buildScheduleVM(rule.activity)
    val gap = ScheduleGapItem(LocalTime.of(12, 0), LocalTime.of(12, 20))

    val state = ScheduleUiState(showGapOptionsModal = true, selectedGap = gap)

    rule.setContent {
      DayTabContent(vm = vm, state = state, objectivesVm = ObjectivesViewModel(requireAuth = false))
    }

    rule.onNodeWithText("Free Time Found (20 min)").assertIsDisplayed()
    rule.onNodeWithText("How would you like to use this time?").assertIsDisplayed()

    rule.onNodeWithText("Study").assertIsDisplayed()
    rule.onNodeWithText("Relax").assertIsDisplayed()
  }

  @Test
  fun gapPropositionsModal_showsButtons() {
    val vm = buildScheduleVM(rule.activity)
    val gap = ScheduleGapItem(LocalTime.of(14, 0), LocalTime.of(14, 45))

    val propositions = listOf("Work on Objectives", "Read Course Material")

    val state =
        ScheduleUiState(
            showGapPropositionsModal = true, selectedGap = gap, gapPropositions = propositions)

    rule.setContent {
      DayTabContent(vm = vm, state = state, objectivesVm = ObjectivesViewModel(requireAuth = false))
    }

    rule.onNodeWithText("Suggestions").assertIsDisplayed()
    rule.onNodeWithText("Work on Objectives").assertIsDisplayed()
    rule.onNodeWithText("Read Course Material").assertIsDisplayed()
  }

  @Test
  fun showsEventItemRow_correctly() {
    val vm = buildScheduleVM(rule.activity)

    val event =
        ScheduleEvent(
            id = "e1",
            title = "Review Flashcards",
            date = LocalDate.now(),
            time = LocalTime.of(15, 0),
            durationMinutes = 30,
            kind = EventKind.STUDY)

    val state = ScheduleUiState(todaySchedule = listOf(ScheduleEventItem(event)))

    rule.setContent {
      DayTabContent(vm = vm, state = state, objectivesVm = ObjectivesViewModel(requireAuth = false))
    }

    rule.onNodeWithText("Review Flashcards").assertIsDisplayed()
    rule.onNodeWithText("15:00 - 15:30").assertIsDisplayed()

    // Delete button visible
    rule.onNodeWithContentDescription("Remove event").assertIsDisplayed()
  }

  @Test
  fun mixedSchedule_rendersClassGapEvent() {
    val ctx = rule.activity
    val vm = buildScheduleVM(ctx)

    val clazz = fakeClass(id = "cx", name = "Math")
    val gap = ScheduleGapItem(LocalTime.of(10, 0), LocalTime.of(10, 30))
    val event =
        ScheduleEvent(
            id = "ex",
            title = "Study Session",
            date = LocalDate.now(),
            time = LocalTime.of(11, 0),
            durationMinutes = 60,
            kind = EventKind.STUDY)

    val items = listOf(ScheduleClassItem(clazz), gap, ScheduleEventItem(event))

    val state = ScheduleUiState(todaySchedule = items)

    rule.setContent { DayTabContent(vm, state, ObjectivesViewModel(requireAuth = false)) }

    rule.onNodeWithText("Math").assertIsDisplayed()
    rule.onNodeWithText("Free Time (30 min)").assertIsDisplayed()
    rule.onNodeWithText("Study Session").assertIsDisplayed()
  }

  @Test
  fun clickingEventItem_triggersEventClickCallback() {
    var clicked = false
    val vm = buildScheduleVM(rule.activity)
    val event =
        ScheduleEvent(
            title = "Review Flashcards",
            date = LocalDate.now(),
            time = LocalTime.of(14, 0),
            durationMinutes = 30,
            kind = EventKind.STUDY)
    val state = ScheduleUiState(todaySchedule = listOf(ScheduleEventItem(event)))

    rule.setContent {
      DayTabContent(
          vm = vm,
          state = state,
          objectivesVm = ObjectivesViewModel(requireAuth = false),
          onObjectiveNavigation = {},
          onTodoClicked = {},
      )
    }

    rule.onNodeWithText("Review Flashcards").performClick()
  }

  @Test
  fun clickingDeleteOnEvent_triggersDeleteCallback() {
    val vm = buildScheduleVM(rule.activity)
    val event =
        ScheduleEvent(title = "Review Flashcards", date = LocalDate.now(), kind = EventKind.STUDY)
    val state = ScheduleUiState(todaySchedule = listOf(ScheduleEventItem(event)))

    rule.setContent {
      DayTabContent(vm = vm, state = state, objectivesVm = ObjectivesViewModel(requireAuth = false))
    }

    rule.onNodeWithContentDescription("Remove event").performClick()
  }
}
