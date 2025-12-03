package com.android.sample.schedule

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import com.android.sample.R
import com.android.sample.feature.schedule.data.calendar.Priority
import com.android.sample.feature.schedule.data.calendar.StudyItem
import com.android.sample.feature.schedule.data.calendar.TaskType
import com.android.sample.feature.schedule.data.schedule.ScheduleEvent
import com.android.sample.feature.schedule.repository.planner.FakePlannerRepository
import com.android.sample.feature.schedule.repository.schedule.ScheduleRepository
import com.android.sample.feature.schedule.viewmodel.ScheduleViewModel
import com.android.sample.ui.calendar.CalendarScreenTestTags
import com.android.sample.ui.schedule.MONTH_TAB_CONTENT
import com.android.sample.ui.schedule.MonthTabContent
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Rule
import org.junit.Test

/**
 * Instrumented tests for MonthTabContent.
 *
 * File: app/src/androidTest/java/com/android/sample/schedule/MonthTabContentAllAndroidTest.kt
 */
class MonthTabContentAllAndroidTest {

  @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

  // ────────────────────── Fakes / helpers ──────────────────────

  private class FakeScheduleRepository(initial: List<ScheduleEvent> = emptyList()) :
      ScheduleRepository {

    private val _events = MutableStateFlow(initial)
    override val events: StateFlow<List<ScheduleEvent>> = _events

    override suspend fun save(event: ScheduleEvent) {
      _events.value = _events.value.filterNot { it.id == event.id } + event
    }

    override suspend fun update(event: ScheduleEvent) {
      _events.value = _events.value.map { if (it.id == event.id) event else it }
    }

    override suspend fun delete(eventId: String) {
      _events.value = _events.value.filterNot { it.id == eventId }
    }

    override suspend fun getEventsBetween(start: LocalDate, end: LocalDate): List<ScheduleEvent> =
        _events.value.filter { it.date in start..end }

    override suspend fun getById(id: String): ScheduleEvent? =
        _events.value.firstOrNull { it.id == id }

    override suspend fun moveEventDate(id: String, newDate: LocalDate): Boolean {
      val ev = getById(id) ?: return false
      _events.value = _events.value.map { if (it.id == id) ev.copy(date = newDate) else it }
      return true
    }

    override suspend fun getEventsForDate(date: LocalDate): List<ScheduleEvent> =
        _events.value.filter { it.date == date }

    override suspend fun getEventsForWeek(startDate: LocalDate): List<ScheduleEvent> =
        getEventsBetween(startDate, startDate.plusDays(6))

    override suspend fun importEvents(events: List<ScheduleEvent>) {
      val current = _events.value.toMutableList()
      current.addAll(events)
      _events.value = current
    }
  }

  private fun buildScheduleVm(ctx: Context): ScheduleViewModel {
    val scheduleRepo = FakeScheduleRepository()
    val plannerRepo = FakePlannerRepository() // from main code, safe for tests
    return ScheduleViewModel(
        scheduleRepository = scheduleRepo,
        plannerRepository = plannerRepo,
        resources = ctx.resources)
  }

  private fun sampleMonthTasks(month: YearMonth): List<StudyItem> {
    val mid = month.atDay(10)
    val later = month.atDay(20)
    val outside = month.plusMonths(1).atDay(5)

    return listOf(
        StudyItem(
            id = "h1",
            title = "Important exam",
            date = mid,
            time = LocalTime.of(10, 0),
            type = TaskType.STUDY,
            priority = Priority.HIGH),
        StudyItem(
            id = "h2",
            title = "Major project deadline",
            date = later,
            time = LocalTime.of(17, 0),
            type = TaskType.STUDY,
            priority = Priority.HIGH),
        StudyItem(
            id = "m1",
            title = "Normal homework",
            date = mid.plusDays(1),
            time = LocalTime.of(14, 0),
            type = TaskType.STUDY,
            priority = Priority.MEDIUM),
        // HIGH but outside this month – should NOT appear in important section
        StudyItem(
            id = "h_out",
            title = "Next month milestone",
            date = outside,
            time = LocalTime.NOON,
            type = TaskType.STUDY,
            priority = Priority.HIGH))
  }

  // ────────────────────── Tests ──────────────────────

  @Test
  fun month_tab_renders_header_and_calendar_card() {
    val ctx = ApplicationProvider.getApplicationContext<Context>()
    val vm = buildScheduleVm(ctx)

    val month = YearMonth.of(2025, 9)
    val selected = month.atDay(15)
    val tasks = sampleMonthTasks(month)

    val monthName = month.month.name.lowercase().replaceFirstChar { it.uppercase() }
    val headerTitle = ctx.getString(R.string.calendar_month_year, monthName, month.year)

    rule.setContent {
      MonthTabContent(
          allTasks = tasks,
          selectedDate = selected,
          currentMonth = month,
          onPreviousMonthClick = {},
          onNextMonthClick = {},
          onDateSelected = { _ -> })
    }

    // Root month content
    rule.onNodeWithTag(MONTH_TAB_CONTENT, useUnmergedTree = true).assertExists()

    // Calendar card (MonthGrid container)
    rule.onNodeWithTag(CalendarScreenTestTags.CALENDAR_CARD, useUnmergedTree = true).assertExists()

    // Header title for the month
    rule.onNodeWithText(headerTitle).assertIsDisplayed()
  }

  @Test
  fun month_tab_shows_only_high_priority_tasks_in_important_section() {
    val ctx = ApplicationProvider.getApplicationContext<Context>()
    val vm = buildScheduleVm(ctx)

    val month = YearMonth.of(2025, 11)
    val selected = month.atDay(3)
    val tasks = sampleMonthTasks(month)

    rule.setContent {
      MonthTabContent(
          allTasks = tasks,
          selectedDate = selected,
          currentMonth = month,
          onPreviousMonthClick = {},
          onNextMonthClick = {},
          onDateSelected = { _ -> })
    }

    // Section title: "Most important this month" (or your localized string)
    val importantTitle = ctx.getString(R.string.schedule_month_important_title)
    rule.onNodeWithText(importantTitle).assertIsDisplayed()

    // HIGH priority items inside the month should be visible
    rule.onNodeWithText("Important exam").assertIsDisplayed()
    rule.onNodeWithText("Major project deadline").assertIsDisplayed()

    // Medium-priority item from same month should NOT appear in the important list
    rule.onNodeWithText("Normal homework").assertDoesNotExist()

    // HIGH priority but outside current month should NOT appear either
    rule.onNodeWithText("Next month milestone").assertDoesNotExist()
  }

  @Test
  fun month_tab_shows_noImportantMessage_when_no_high_priority() {
    val ctx = ApplicationProvider.getApplicationContext<Context>()
    val vm = buildScheduleVm(ctx)

    val month = YearMonth.of(2025, 4)
    val selected = month.atDay(8)

    // Only MEDIUM / LOW priority tasks in this month
    val tasks =
        listOf(
            StudyItem(
                id = "m1",
                title = "Regular revision",
                date = month.atDay(5),
                time = LocalTime.of(9, 0),
                type = TaskType.STUDY,
                priority = Priority.MEDIUM),
            StudyItem(
                id = "m2",
                title = "Gym with friend",
                date = month.atDay(12),
                time = LocalTime.of(18, 0),
                type = TaskType.PERSONAL,
                priority = Priority.LOW))

    rule.setContent {
      MonthTabContent(
          allTasks = tasks,
          selectedDate = selected,
          currentMonth = month,
          onPreviousMonthClick = {},
          onNextMonthClick = {},
          onDateSelected = { _ -> })
    }

    val noImportantText = ctx.getString(R.string.schedule_month_no_important)

    // When there is no HIGH priority task in the month, the "no important" text is shown
    rule.onNodeWithText(noImportantText).assertIsDisplayed()
  }
}
