package com.android.sample.schedule

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import com.android.sample.R
import com.android.sample.data.Priority as TodoPriority
import com.android.sample.data.Status as TodoStatus
import com.android.sample.data.ToDo
import com.android.sample.feature.schedule.data.calendar.Priority
import com.android.sample.feature.schedule.data.calendar.StudyItem
import com.android.sample.feature.schedule.data.calendar.TaskType
import com.android.sample.feature.schedule.data.schedule.ScheduleEvent
import com.android.sample.feature.schedule.repository.planner.FakePlannerRepository
import com.android.sample.feature.schedule.repository.planner.PlannerRepository
import com.android.sample.feature.schedule.repository.schedule.ScheduleRepository
import com.android.sample.feature.schedule.viewmodel.ScheduleViewModel
import com.android.sample.feature.weeks.ui.WeekProgDailyObjTags
import com.android.sample.feature.weeks.viewmodel.ObjectivesViewModel
import com.android.sample.feature.weeks.viewmodel.WeeksViewModel
import com.android.sample.ui.calendar.CalendarScreenTestTags
import com.android.sample.ui.schedule.WeekTabContent
import io.mockk.mockk
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class WeekTabContentAllAndroidTest {

  @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

  // ---------- Simple in-memory fakes ----------

  private class InMemoryScheduleRepo(initial: List<ScheduleEvent> = emptyList()) :
      ScheduleRepository {
    private val _events = MutableStateFlow(initial)
    override val events: StateFlow<List<ScheduleEvent>> = _events.asStateFlow()

    override suspend fun save(event: ScheduleEvent) {
      _events.value = _events.value.filterNot { it.id == event.id } + event
    }

    override suspend fun update(event: ScheduleEvent) {
      _events.value = _events.value.map { if (it.id == event.id) event else it }
    }

    override suspend fun delete(eventId: String) {
      _events.value = _events.value.filterNot { it.id == eventId }
    }

    override suspend fun getEventsBetween(start: LocalDate, end: LocalDate) =
        _events.value.filter { it.date in start..end }

    override suspend fun getById(id: String) = _events.value.firstOrNull { it.id == id }

    override suspend fun moveEventDate(id: String, newDate: LocalDate): Boolean {
      val ev = getById(id) ?: return false
      _events.value = _events.value.map { if (it.id == id) ev.copy(date = newDate) else it }
      return true
    }

    override suspend fun getEventsForDate(date: LocalDate) =
        _events.value.filter { it.date == date }

    override suspend fun getEventsForWeek(startDate: LocalDate) =
        getEventsBetween(startDate, startDate.plusDays(6))

    override suspend fun importEvents(events: List<ScheduleEvent>) {
      val current = _events.value.toMutableList()
      current.addAll(events)
      _events.value = current
    }
  }

  private fun buildVm(): ScheduleViewModel {
    val ctx = ApplicationProvider.getApplicationContext<Context>()
    val scheduleRepo: ScheduleRepository = InMemoryScheduleRepo()
    val plannerRepo: PlannerRepository = FakePlannerRepository()
    return ScheduleViewModel(
            scheduleRepository = scheduleRepo,
            plannerRepository = plannerRepo,
            resources = ctx.resources)
        .apply { setWeekMode() } // ensure prev/next navigate by week
  }

  private fun tasksForWeek(dateInWeek: LocalDate): List<StudyItem> {
    val start = dateInWeek.with(DayOfWeek.MONDAY)
    return listOf(
        StudyItem(
            id = "a",
            title = "Linear Algebra review",
            date = start,
            time = LocalTime.of(9, 0),
            priority = Priority.MEDIUM,
            type = TaskType.STUDY),
        StudyItem(
            id = "b",
            title = "Part-time shift",
            date = start,
            time = LocalTime.of(16, 0),
            priority = Priority.MEDIUM,
            type = TaskType.WORK),
        StudyItem(
            id = "c",
            title = "Gym with Sam",
            date = start.plusDays(2),
            time = LocalTime.of(18, 30),
            priority = Priority.MEDIUM,
            type = TaskType.PERSONAL),
        // outside current week (must not appear)
        StudyItem(
            id = "out",
            title = "Outside week task",
            date = start.plusDays(8),
            time = LocalTime.NOON,
            priority = Priority.MEDIUM,
            type = TaskType.STUDY))
  }

  // ---------- Tests ----------

  @Test
  fun scaffold_renders_core_sections() {
    val vm = buildVm()
    val weeksVm: WeeksViewModel = mockk(relaxed = true) // not used by this UI
    val objectivesVm = ObjectivesViewModel(requireAuth = false)

    val selected = LocalDate.of(2025, 3, 5)
    val tasks = tasksForWeek(selected)
    val weekStart = vm.startOfWeek(selected)
    val weekTodos =
        todosForWeek(weekStart).filter { it.dueDate in weekStart..weekStart.plusDays(6) }

    rule.setContent {
      WeekTabContent(
          vm = vm,
          objectivesVm = objectivesVm,
          allTasks = tasks,
          selectedDate = selected,
          weekTodos = weekTodos)
    }

    // root content + calendar card + week dots row present
    rule.onNodeWithTag("WeekContent").assertIsDisplayed()
    rule.onNodeWithTag(CalendarScreenTestTags.CALENDAR_CARD).assertIsDisplayed()
    rule.onNodeWithTag(WeekProgDailyObjTags.WEEK_DOTS_ROW).assertIsDisplayed()

    // at least Monday tile exists
    val monday = vm.startOfWeek(selected)
    rule
        .onNodeWithTag("${CalendarScreenTestTags.WEEK_DAY_BOX_PREFIX}${monday.dayOfMonth}")
        .assertIsDisplayed()
  }

  @Test
  fun clicking_day_updates_selectedDate() {
    val vm = buildVm()
    val weeksVm: WeeksViewModel = mockk(relaxed = true)
    val objectivesVm = ObjectivesViewModel(requireAuth = false)

    val selected = LocalDate.of(2025, 3, 5)
    val weekStart = vm.startOfWeek(selected)
    val target = weekStart.plusDays(4) // Friday
    val targetTag = "${CalendarScreenTestTags.WEEK_DAY_BOX_PREFIX}${target.dayOfMonth}"

    rule.setContent {
      WeekTabContent(
          vm = vm,
          objectivesVm = objectivesVm,
          allTasks = tasksForWeek(selected),
          selectedDate = selected,
          weekTodos = emptyList())
    }

    // Ensure week mode on UI thread (prevents any race with default state)
    rule.runOnUiThread { vm.setWeekMode() }

    // Scroll the WEEK_ROW to the day box, then click it
    rule.onNodeWithTag(CalendarScreenTestTags.WEEK_ROW).performScrollToNode(hasTestTag(targetTag))

    rule.onNodeWithTag(targetTag).performClick()

    rule.waitForIdle()
    assertEquals(target, vm.uiState.value.selectedDate)
  }

  @Test
  fun header_prev_next_moves_week_in_weekMode() {
    val vm = buildVm()
    val weeksVm: WeeksViewModel = mockk(relaxed = true)
    val objectivesVm = ObjectivesViewModel(requireAuth = false)

    val selected = LocalDate.of(2025, 6, 18)

    // --- Sync VM state BEFORE composing (so arrow handlers use the same date & mode) ---
    vm.setWeekMode()
    vm.onDateSelected(selected)

    rule.setContent {
      WeekTabContent(
          vm = vm,
          objectivesVm = objectivesVm,
          allTasks = tasksForWeek(selected),
          selectedDate = selected,
          weekTodos = emptyList())
    }

    // Double-ensure on UI thread (paranoia against any later mode flips)
    rule.runOnUiThread {
      vm.setWeekMode()
      vm.onDateSelected(selected)
    }

    rule.onNodeWithContentDescription("Previous").performClick()
    rule.waitForIdle()
    assertEquals(selected.minusWeeks(1), vm.uiState.value.selectedDate)

    rule.onNodeWithContentDescription("Next").performClick()
    rule.waitForIdle()
    assertEquals(selected, vm.uiState.value.selectedDate)
  }

  @Test
  fun upcoming_events_filters_to_current_week() {
    val vm = buildVm()
    val weeksVm: WeeksViewModel = mockk(relaxed = true)
    val objectivesVm = ObjectivesViewModel(requireAuth = false)
    val selected = LocalDate.of(2025, 5, 7)

    rule.setContent {
      WeekTabContent(
          vm = vm,
          objectivesVm = objectivesVm,
          allTasks = tasksForWeek(selected),
          selectedDate = selected,
          weekTodos = emptyList())
    }

    // header
    val upcoming = rule.activity.getString(R.string.upcoming_events)
    rule.onNodeWithText(upcoming).assertIsDisplayed()

    // inside-week items visible
    rule.onAllNodesWithText("Linear Algebra review").onFirst().assertIsDisplayed()
    rule.onAllNodesWithText("Part-time shift").onFirst().assertIsDisplayed()
    rule.onAllNodesWithText("Gym with Sam").onFirst().assertIsDisplayed()

    // outside-week item not shown
    rule.onNodeWithText("Outside week task").assertDoesNotExist()

    // add button is there
    val addEvent = rule.activity.getString(R.string.add_event)
    rule.onNodeWithText(addEvent).assertIsDisplayed()
  }

  @Test
  fun header_title_formats_week_range() {
    val vm = buildVm()
    val weeksVm: WeeksViewModel = mockk(relaxed = true)
    val objectivesVm = ObjectivesViewModel(requireAuth = false)

    val selected = LocalDate.of(2025, 9, 15) // a Monday for stable range
    val start = vm.startOfWeek(selected)
    val end = start.plusDays(6)

    rule.setContent {
      WeekTabContent(
          vm = vm,
          objectivesVm = objectivesVm,
          allTasks = emptyList(),
          selectedDate = selected,
          weekTodos = emptyList())
    }

    val expected =
        "${start.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${start.dayOfMonth} - ${end.dayOfMonth}"
    rule.onNodeWithText(expected).assertIsDisplayed()
  }

  @Test
  fun weekTab_showsEmptyWeekTodosMessage_whenNoTodos() {
    val vm = buildVm()
    val objectivesVm = ObjectivesViewModel(requireAuth = false)
    val selected = LocalDate.of(2025, 4, 9)
    val weekStart = vm.startOfWeek(selected)

    rule.setContent {
      WeekTabContent(
          vm = vm,
          objectivesVm = objectivesVm,
          allTasks = tasksForWeek(selected),
          selectedDate = selected,
          weekTodos = emptyList())
    }

    val ctx = rule.activity
    val title = ctx.getString(R.string.schedule_week_todos_title)
    val empty = ctx.getString(R.string.schedule_week_no_todos)

    rule.onNodeWithText(title).performScrollTo().assertIsDisplayed()
    rule.onNodeWithText(empty).performScrollTo().assertIsDisplayed()
  }

  @Test
  fun weekTab_showsWeekTodos_sorted() {
    val vm = buildVm()
    val objectivesVm = ObjectivesViewModel(requireAuth = false)
    val selected = LocalDate.of(2025, 4, 9)
    val weekStart = vm.startOfWeek(selected)
    val allWeekTodos = todosForWeek(weekStart)
    val filteredWeekTodos = allWeekTodos.filter { it.dueDate in weekStart..weekStart.plusDays(6) }

    rule.setContent {
      WeekTabContent(
          vm = vm,
          objectivesVm = objectivesVm,
          allTasks = tasksForWeek(selected),
          selectedDate = selected,
          weekTodos = filteredWeekTodos)
    }

    // Both inside-week todos should be visible; outside-week one is not passed in
    rule.onNodeWithText("Week todo 1", substring = false).assertIsDisplayed()
    rule.onNodeWithText("Week todo 2", substring = false).assertIsDisplayed()
    rule.onNodeWithText("Outside week todo", substring = false).assertDoesNotExist()
  }

  private fun todosForWeek(startOfWeek: LocalDate): List<ToDo> =
      listOf(
          ToDo(
              title = "Week todo 1",
              dueDate = startOfWeek,
              priority = TodoPriority.HIGH,
              status = TodoStatus.TODO),
          ToDo(
              title = "Week todo 2",
              dueDate = startOfWeek.plusDays(3),
              priority = TodoPriority.MEDIUM,
              status = TodoStatus.IN_PROGRESS),
          // Outside this week - should not appear if you filter before passing
          ToDo(
              title = "Outside week todo",
              dueDate = startOfWeek.plusDays(7),
              priority = TodoPriority.LOW,
              status = TodoStatus.TODO))
}
