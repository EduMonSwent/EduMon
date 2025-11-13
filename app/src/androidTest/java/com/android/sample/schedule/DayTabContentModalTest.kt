package com.android.sample.schedule

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import com.android.sample.R
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
      DayTabContent(vm = vm, state = state, objectivesVm = ObjectivesViewModel(requireAuth = false))
    }

    val dateText = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMM d"))
    rule.onNodeWithText(ctx.getString(R.string.today_title_fmt, dateText)).assertIsDisplayed()

    // "Algorithms (Lecture)"
    rule.onNodeWithText("Algorithms (Lecture)", useUnmergedTree = true).assertIsDisplayed()
    // "BC02 • Dr. Smith"
    rule.onNodeWithText("BC02 • Dr. Smith", useUnmergedTree = true).assertIsDisplayed()
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
      DayTabContent(vm = vm, state = state, objectivesVm = ObjectivesViewModel(requireAuth = false))
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
}
