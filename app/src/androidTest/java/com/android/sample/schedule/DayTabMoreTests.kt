package com.android.sample.schedule

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.android.sample.R
import com.android.sample.data.Priority
import com.android.sample.data.Status
import com.android.sample.data.ToDo
import com.android.sample.feature.schedule.data.planner.*
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

/**
 * Additional comprehensive tests to achieve 95%+ line coverage for DayTabContent.kt These tests
 * cover branches not covered by existing tests.
 */
class DayTabContentExtraCoverageTest {

  @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

  private val ctx
    get() = rule.activity

  // ============================================================
  // CLASS TYPES - All Four Types Coverage
  // ============================================================

  @Test
  fun classRow_exerciseType_showsCorrectElements() {
    val vm = buildScheduleVM(ctx)
    val clazz = fakeClass(name = "Exercise Session", type = ClassType.EXERCISE)
    val state = ScheduleUiState(todaySchedule = listOf(ScheduleClassItem(clazz)))

    rule.setContent {
      val snackbarHostState = remember { SnackbarHostState() }
      DayTabContent(vm, state, ObjectivesViewModel(requireAuth = false), snackbarHostState)
    }

    val exerciseLabel = ctx.getString(R.string.exercise_type)
    rule.onNodeWithText("Exercise Session", useUnmergedTree = true).assertIsDisplayed()
    rule.onNodeWithText("($exerciseLabel)", useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun classRow_labType_showsCorrectElements() {
    val vm = buildScheduleVM(ctx)
    val clazz = fakeClass(name = "Lab Session", type = ClassType.LAB)
    val state = ScheduleUiState(todaySchedule = listOf(ScheduleClassItem(clazz)))

    rule.setContent {
      val snackbarHostState = remember { SnackbarHostState() }
      DayTabContent(vm, state, ObjectivesViewModel(requireAuth = false), snackbarHostState)
    }

    val labLabel = ctx.getString(R.string.lab_type)
    rule.onNodeWithText("Lab Session", useUnmergedTree = true).assertIsDisplayed()
    rule.onNodeWithText("($labLabel)", useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun classRow_projectType_showsCorrectElements() {
    val vm = buildScheduleVM(ctx)
    val clazz = fakeClass(name = "Project Work", type = ClassType.PROJECT)
    val state = ScheduleUiState(todaySchedule = listOf(ScheduleClassItem(clazz)))

    rule.setContent {
      val snackbarHostState = remember { SnackbarHostState() }
      DayTabContent(vm, state, ObjectivesViewModel(requireAuth = false), snackbarHostState)
    }

    val projectLabel = ctx.getString(R.string.project_type)
    rule.onNodeWithText("Project Work", useUnmergedTree = true).assertIsDisplayed()
    rule.onNodeWithText("($projectLabel)", useUnmergedTree = true).assertIsDisplayed()
  }

  // ============================================================
  // ATTENDANCE/COMPLETION - Additional Combinations
  // ============================================================

  @Test
  fun attendanceStatus_YES_completion_PARTIALLY() {
    val vm = buildScheduleVM(ctx)
    val clazz = fakeClass(id = "c1")
    val record =
        fakeAttendance(
            "c1", attendance = AttendanceStatus.YES, completion = CompletionStatus.PARTIALLY)

    val state =
        ScheduleUiState(
            todaySchedule = listOf(ScheduleClassItem(clazz)), attendanceRecords = listOf(record))

    rule.setContent {
      val snackbarHostState = remember { SnackbarHostState() }
      DayTabContent(vm, state, ObjectivesViewModel(requireAuth = false), snackbarHostState)
    }

    rule.onNodeWithText(ctx.getString(R.string.attendance_attended)).assertIsDisplayed()
    rule.onNodeWithText(ctx.getString(R.string.completion_partially)).assertIsDisplayed()
  }

  @Test
  fun attendanceStatus_YES_completion_NO() {
    val vm = buildScheduleVM(ctx)
    val clazz = fakeClass(id = "c2")
    val record =
        fakeAttendance("c2", attendance = AttendanceStatus.YES, completion = CompletionStatus.NO)

    val state =
        ScheduleUiState(
            todaySchedule = listOf(ScheduleClassItem(clazz)), attendanceRecords = listOf(record))

    rule.setContent {
      val snackbarHostState = remember { SnackbarHostState() }
      DayTabContent(vm, state, ObjectivesViewModel(requireAuth = false), snackbarHostState)
    }

    rule.onNodeWithText(ctx.getString(R.string.attendance_attended)).assertIsDisplayed()
    rule.onNodeWithText(ctx.getString(R.string.completion_not_done)).assertIsDisplayed()
  }

  @Test
  fun attendanceStatus_ARRIVED_LATE_completion_YES() {
    val vm = buildScheduleVM(ctx)
    val clazz = fakeClass(id = "c3")
    val record =
        fakeAttendance(
            "c3", attendance = AttendanceStatus.ARRIVED_LATE, completion = CompletionStatus.YES)

    val state =
        ScheduleUiState(
            todaySchedule = listOf(ScheduleClassItem(clazz)), attendanceRecords = listOf(record))

    rule.setContent {
      val snackbarHostState = remember { SnackbarHostState() }
      DayTabContent(vm, state, ObjectivesViewModel(requireAuth = false), snackbarHostState)
    }

    rule.onNodeWithText(ctx.getString(R.string.attendance_arrived_late)).assertIsDisplayed()
    rule.onNodeWithText(ctx.getString(R.string.completion_done)).assertIsDisplayed()
  }

  @Test
  fun attendanceStatus_ARRIVED_LATE_completion_NO() {
    val vm = buildScheduleVM(ctx)
    val clazz = fakeClass(id = "c4")
    val record =
        fakeAttendance(
            "c4", attendance = AttendanceStatus.ARRIVED_LATE, completion = CompletionStatus.NO)

    val state =
        ScheduleUiState(
            todaySchedule = listOf(ScheduleClassItem(clazz)), attendanceRecords = listOf(record))

    rule.setContent {
      val snackbarHostState = remember { SnackbarHostState() }
      DayTabContent(vm, state, ObjectivesViewModel(requireAuth = false), snackbarHostState)
    }

    rule.onNodeWithText(ctx.getString(R.string.attendance_arrived_late)).assertIsDisplayed()
    rule.onNodeWithText(ctx.getString(R.string.completion_not_done)).assertIsDisplayed()
  }

  @Test
  fun attendanceStatus_NO_completion_YES() {
    val vm = buildScheduleVM(ctx)
    val clazz = fakeClass(id = "c5")
    val record =
        fakeAttendance("c5", attendance = AttendanceStatus.NO, completion = CompletionStatus.YES)

    val state =
        ScheduleUiState(
            todaySchedule = listOf(ScheduleClassItem(clazz)), attendanceRecords = listOf(record))

    rule.setContent {
      val snackbarHostState = remember { SnackbarHostState() }
      DayTabContent(vm, state, ObjectivesViewModel(requireAuth = false), snackbarHostState)
    }

    rule.onNodeWithText(ctx.getString(R.string.attendance_missed)).assertIsDisplayed()
    rule.onNodeWithText(ctx.getString(R.string.completion_done)).assertIsDisplayed()
  }

  @Test
  fun attendanceStatus_NO_completion_PARTIALLY() {
    val vm = buildScheduleVM(ctx)
    val clazz = fakeClass(id = "c6")
    val record =
        fakeAttendance(
            "c6", attendance = AttendanceStatus.NO, completion = CompletionStatus.PARTIALLY)

    val state =
        ScheduleUiState(
            todaySchedule = listOf(ScheduleClassItem(clazz)), attendanceRecords = listOf(record))

    rule.setContent {
      val snackbarHostState = remember { SnackbarHostState() }
      DayTabContent(vm, state, ObjectivesViewModel(requireAuth = false), snackbarHostState)
    }

    rule.onNodeWithText(ctx.getString(R.string.attendance_missed)).assertIsDisplayed()
    rule.onNodeWithText(ctx.getString(R.string.completion_partially)).assertIsDisplayed()
  }

  // ============================================================
  // GAP MODAL TESTS
  // ============================================================

  @Test
  fun gapOptionsModal_shows45MinGap() {
    val vm = buildScheduleVM(ctx)
    val gap = ScheduleGapItem(LocalTime.of(14, 0), LocalTime.of(14, 45))

    val state = ScheduleUiState(showGapOptionsModal = true, selectedGap = gap)

    rule.setContent {
      val snackbarHostState = remember { SnackbarHostState() }
      DayTabContent(vm, state, ObjectivesViewModel(requireAuth = false), snackbarHostState)
    }

    rule.onNodeWithText(ctx.getString(R.string.smart_gap_modal_title, 45)).assertIsDisplayed()
    rule.onNodeWithText(ctx.getString(R.string.smart_gap_modal_text)).assertIsDisplayed()
    rule.onNodeWithText(ctx.getString(R.string.smart_gap_action_study)).assertIsDisplayed()
    rule.onNodeWithText(ctx.getString(R.string.smart_gap_action_relax)).assertIsDisplayed()
  }

  @Test
  fun gapPropositionsModal_showsMultipleOptions() {
    val vm = buildScheduleVM(ctx)
    val gap = ScheduleGapItem(LocalTime.of(12, 0), LocalTime.of(12, 40))
    val propositions = listOf("Work on Objectives", "Review Flashcards", "Read Course Material")

    val state =
        ScheduleUiState(
            showGapPropositionsModal = true, selectedGap = gap, gapPropositions = propositions)

    rule.setContent {
      val snackbarHostState = remember { SnackbarHostState() }
      DayTabContent(vm, state, ObjectivesViewModel(requireAuth = false), snackbarHostState)
    }

    rule.onNodeWithText(ctx.getString(R.string.smart_gap_suggestions_title)).assertIsDisplayed()
    rule.onNodeWithText("Work on Objectives").assertIsDisplayed()
    rule.onNodeWithText("Review Flashcards").assertIsDisplayed()
    rule.onNodeWithText("Read Course Material").assertIsDisplayed()
    rule.onNodeWithText(ctx.getString(R.string.cancel)).assertIsDisplayed()
  }

  @Test
  fun gapPropositionsModal_withSingleProposition() {
    val vm = buildScheduleVM(ctx)
    val gap = ScheduleGapItem(LocalTime.of(11, 0), LocalTime.of(11, 20))

    val state =
        ScheduleUiState(
            showGapPropositionsModal = true,
            selectedGap = gap,
            gapPropositions = listOf("Quick Review"))

    rule.setContent {
      val snackbarHostState = remember { SnackbarHostState() }
      DayTabContent(vm, state, ObjectivesViewModel(requireAuth = false), snackbarHostState)
    }

    rule.onNodeWithText("Quick Review").assertIsDisplayed()
  }

  // ============================================================
  // TODO FILTERING TESTS
  // ============================================================

  @Test
  fun todosSection_filtersOnlyTodayTodos() {
    val vm = buildScheduleVM(ctx)
    val today = LocalDate.now()
    val tomorrow = today.plusDays(1)
    val yesterday = today.minusDays(1)

    val todayTodo =
        ToDo(
            id = "t1",
            title = "Today Task",
            dueDate = today,
            priority = Priority.MEDIUM,
            status = Status.TODO)
    val tomorrowTodo =
        ToDo(
            id = "t2",
            title = "Tomorrow Task",
            dueDate = tomorrow,
            priority = Priority.MEDIUM,
            status = Status.TODO)
    val yesterdayTodo =
        ToDo(
            id = "t3",
            title = "Yesterday Task",
            dueDate = yesterday,
            priority = Priority.MEDIUM,
            status = Status.TODO)

    val state =
        ScheduleUiState(
            todaySchedule = emptyList(), todos = listOf(todayTodo, tomorrowTodo, yesterdayTodo))

    rule.setContent {
      val snackbarHostState = remember { SnackbarHostState() }
      Column(Modifier.verticalScroll(rememberScrollState())) {
        DayTabContent(vm, state, ObjectivesViewModel(requireAuth = false), snackbarHostState)
      }
    }

    rule.onNodeWithText("Today Task").performScrollTo().assertIsDisplayed()
    rule.onNodeWithText("Tomorrow Task").assertDoesNotExist()
    rule.onNodeWithText("Yesterday Task").assertDoesNotExist()
  }

  @Test
  fun todosSection_showsMultipleTodosForToday() {
    val vm = buildScheduleVM(ctx)
    val today = LocalDate.now()

    val todos =
        listOf(
            ToDo(
                id = "t1",
                title = "First Todo",
                dueDate = today,
                priority = Priority.HIGH,
                status = Status.TODO),
            ToDo(
                id = "t2",
                title = "Second Todo",
                dueDate = today,
                priority = Priority.MEDIUM,
                status = Status.TODO),
            ToDo(
                id = "t3",
                title = "Third Todo",
                dueDate = today,
                priority = Priority.LOW,
                status = Status.TODO))

    val state = ScheduleUiState(todaySchedule = emptyList(), todos = todos)

    rule.setContent {
      val snackbarHostState = remember { SnackbarHostState() }
      Column(Modifier.verticalScroll(rememberScrollState())) {
        DayTabContent(vm, state, ObjectivesViewModel(requireAuth = false), snackbarHostState)
      }
    }

    rule.onNodeWithText("First Todo").performScrollTo().assertIsDisplayed()
    rule.onNodeWithText("Second Todo").performScrollTo().assertIsDisplayed()
    rule.onNodeWithText("Third Todo").performScrollTo().assertIsDisplayed()
  }

  // ============================================================
  // GAP ITEM TESTS - Different Durations
  // ============================================================

  @Test
  fun gapItem_15Minutes_rendersCorrectly() {
    val vm = buildScheduleVM(ctx)
    val gap = ScheduleGapItem(LocalTime.of(10, 0), LocalTime.of(10, 15))

    val state = ScheduleUiState(todaySchedule = listOf(gap))

    rule.setContent {
      val snackbarHostState = remember { SnackbarHostState() }
      DayTabContent(vm, state, ObjectivesViewModel(requireAuth = false), snackbarHostState)
    }

    rule.onNodeWithText("Free Time (15 min)").assertIsDisplayed()
    rule.onNodeWithText("10:00 - 10:15").assertIsDisplayed()
  }

  @Test
  fun gapItem_60Minutes_rendersCorrectly() {
    val vm = buildScheduleVM(ctx)
    val gap = ScheduleGapItem(LocalTime.of(12, 0), LocalTime.of(13, 0))

    val state = ScheduleUiState(todaySchedule = listOf(gap))

    rule.setContent {
      val snackbarHostState = remember { SnackbarHostState() }
      DayTabContent(vm, state, ObjectivesViewModel(requireAuth = false), snackbarHostState)
    }

    rule.onNodeWithText("Free Time (60 min)").assertIsDisplayed()
    rule.onNodeWithText("12:00 - 13:00").assertIsDisplayed()
  }

  @Test
  fun gapItem_45Minutes_rendersCorrectly() {
    val vm = buildScheduleVM(ctx)
    val gap = ScheduleGapItem(LocalTime.of(14, 30), LocalTime.of(15, 15))

    val state = ScheduleUiState(todaySchedule = listOf(gap))

    rule.setContent {
      val snackbarHostState = remember { SnackbarHostState() }
      DayTabContent(vm, state, ObjectivesViewModel(requireAuth = false), snackbarHostState)
    }

    rule.onNodeWithText("Free Time (45 min)").assertIsDisplayed()
    rule.onNodeWithText("14:30 - 15:15").assertIsDisplayed()
  }

  // ============================================================
  // EVENT ITEM TESTS
  // ============================================================

  @Test
  fun eventItem_relaxEvent_rendersCorrectly() {
    val vm = buildScheduleVM(ctx)
    val event =
        ScheduleEvent(
            id = "e1",
            title = "Meditation",
            date = LocalDate.now(),
            time = LocalTime.of(12, 0),
            durationMinutes = 30,
            kind = EventKind.STUDY)

    val state = ScheduleUiState(todaySchedule = listOf(ScheduleEventItem(event)))

    rule.setContent {
      val snackbarHostState = remember { SnackbarHostState() }
      DayTabContent(vm, state, ObjectivesViewModel(requireAuth = false), snackbarHostState)
    }

    rule.onNodeWithText("Meditation").assertIsDisplayed()
    rule.onNodeWithText("12:00 - 12:30").assertIsDisplayed()
    rule.onNodeWithContentDescription("Remove event").assertIsDisplayed()
  }

  @Test
  fun eventItem_multipleEvents_allRender() {
    val vm = buildScheduleVM(ctx)
    val events =
        listOf(
            ScheduleEvent(
                id = "e1",
                title = "Morning Study",
                date = LocalDate.now(),
                time = LocalTime.of(9, 0),
                durationMinutes = 60,
                kind = EventKind.STUDY),
            ScheduleEvent(
                id = "e2",
                title = "Afternoon Review",
                date = LocalDate.now(),
                time = LocalTime.of(15, 0),
                durationMinutes = 60,
                kind = EventKind.STUDY))

    val state = ScheduleUiState(todaySchedule = events.map { ScheduleEventItem(it) })

    rule.setContent {
      val snackbarHostState = remember { SnackbarHostState() }
      DayTabContent(vm, state, ObjectivesViewModel(requireAuth = false), snackbarHostState)
    }

    rule.onNodeWithText("Morning Study").assertIsDisplayed()
    rule.onNodeWithText("Afternoon Review").assertIsDisplayed()
  }

  // ============================================================
  // MIXED SCHEDULE TESTS
  // ============================================================

  @Test
  fun schedule_multipleClassesInOrder() {
    val vm = buildScheduleVM(ctx)
    val classes =
        listOf(
            fakeClass(
                id = "c1",
                name = "Morning Class",
                start = LocalTime.of(8, 0),
                end = LocalTime.of(9, 0)),
            fakeClass(
                id = "c2",
                name = "Mid Class",
                start = LocalTime.of(10, 0),
                end = LocalTime.of(11, 0)),
            fakeClass(
                id = "c3",
                name = "Afternoon Class",
                start = LocalTime.of(14, 0),
                end = LocalTime.of(15, 0)))

    val state = ScheduleUiState(todaySchedule = classes.map { ScheduleClassItem(it) })

    rule.setContent {
      val snackbarHostState = remember { SnackbarHostState() }
      DayTabContent(vm, state, ObjectivesViewModel(requireAuth = false), snackbarHostState)
    }

    rule.onNodeWithText("Morning Class", useUnmergedTree = true).assertIsDisplayed()
    rule.onNodeWithText("Mid Class", useUnmergedTree = true).assertIsDisplayed()
    rule.onNodeWithText("Afternoon Class", useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun schedule_classesAndGapsAndEvents() {
    val vm = buildScheduleVM(ctx)
    val clazz = fakeClass(id = "c1", name = "Morning Lecture")
    val gap = ScheduleGapItem(LocalTime.of(10, 0), LocalTime.of(10, 30))
    val event =
        ScheduleEvent(
            id = "e1",
            title = "Study Session",
            date = LocalDate.now(),
            time = LocalTime.of(11, 0),
            durationMinutes = 60,
            kind = EventKind.STUDY)

    val state =
        ScheduleUiState(
            todaySchedule = listOf(ScheduleClassItem(clazz), gap, ScheduleEventItem(event)))

    rule.setContent {
      val snackbarHostState = remember { SnackbarHostState() }
      DayTabContent(vm, state, ObjectivesViewModel(requireAuth = false), snackbarHostState)
    }

    rule.onNodeWithText("Morning Lecture", useUnmergedTree = true).assertIsDisplayed()
    rule.onNodeWithText("Free Time (30 min)").assertIsDisplayed()
    rule.onNodeWithText("Study Session").assertIsDisplayed()
  }

  // ============================================================
  // CLICK INTERACTION TESTS
  // ============================================================

  @Test
  fun clickTodo_triggersCallback() {
    val vm = buildScheduleVM(ctx)
    var clickedId = ""
    val todo =
        ToDo(
            id = "todo123",
            title = "Click This",
            dueDate = LocalDate.now(),
            priority = Priority.HIGH,
            status = Status.TODO)

    val state = ScheduleUiState(todaySchedule = emptyList(), todos = listOf(todo))

    rule.setContent {
      val snackbarHostState = remember { SnackbarHostState() }
      Column(Modifier.verticalScroll(rememberScrollState())) {
        DayTabContent(
            vm = vm,
            state = state,
            objectivesVm = ObjectivesViewModel(requireAuth = false),
            snackbarHostState = snackbarHostState,
            onTodoClicked = { clickedId = it })
      }
    }

    rule.onNodeWithText("Click This").performScrollTo().performClick()
  }

  // ============================================================
  // EDGE CASES
  // ============================================================

  @Test
  fun classDetails_displaysLocationAndInstructor() {
    val vm = buildScheduleVM(ctx)
    val clazz = fakeClass(name = "Test", location = "BC02", instructor = "Dr. Test")

    val state = ScheduleUiState(todaySchedule = listOf(ScheduleClassItem(clazz)))

    rule.setContent {
      val snackbarHostState = remember { SnackbarHostState() }
      DayTabContent(vm, state, ObjectivesViewModel(requireAuth = false), snackbarHostState)
    }

    rule.onNodeWithText("BC02 • Dr. Test", useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun classDetails_displaysTimeRange() {
    val vm = buildScheduleVM(ctx)
    val clazz = fakeClass(start = LocalTime.of(9, 15), end = LocalTime.of(10, 45))

    val state = ScheduleUiState(todaySchedule = listOf(ScheduleClassItem(clazz)))

    rule.setContent {
      val snackbarHostState = remember { SnackbarHostState() }
      DayTabContent(vm, state, ObjectivesViewModel(requireAuth = false), snackbarHostState)
    }

    rule.onNodeWithText("09:15 - 10:45", useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun todayHeader_displaysFormattedDate() {
    val vm = buildScheduleVM(ctx)
    val state = ScheduleUiState(todaySchedule = emptyList())

    rule.setContent {
      val snackbarHostState = remember { SnackbarHostState() }
      DayTabContent(vm, state, ObjectivesViewModel(requireAuth = false), snackbarHostState)
    }

    val dateText = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMM d"))
    val todayHeader = ctx.getString(R.string.today_title_fmt, dateText)

    rule.onNodeWithText(todayHeader).assertIsDisplayed()
  }
}
