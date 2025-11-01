package com.android.sample.ui.schedule

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.android.sample.feature.weeks.viewmodel.ObjectivesViewModel
import com.android.sample.feature.weeks.viewmodel.WeeksViewModel
import com.android.sample.model.StudyItem
import com.android.sample.model.schedule.ScheduleEvent
import com.android.sample.model.schedule.ScheduleRepository
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Rule
import org.junit.Test

class TabContentTest {

  @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

  // -------- DayTabContent --------
  @Test
  fun dayTab_renders_today_title_and_empty_state() {
    val objectivesVm = ObjectivesViewModel() // if your app needs args, use the real ctor here

    compose.setContent {
      DayTabContent(
          todayClasses = emptyList(), // forces empty-state branch
          objectivesVm = objectivesVm)
    }

    compose.onAllNodesWithText("Today •", substring = true)[0].assertExists()
    compose.onNodeWithText("No classes scheduled today").assertExists()
    compose.onNodeWithText("Yoga Session").assertExists()
    compose.onNodeWithText("Guest Lecture").assertExists()
  }

  // -------- WeekTabContent --------
  @Test
  fun weekTab_renders_week_content_and_upcoming_section() {
    val vm = ScheduleViewModel(FakeScheduleRepository())
    val weeksVm = WeeksViewModel() // if ctor requires args, use real ctor you use in prod
    val objectivesVm = ObjectivesViewModel()

    compose.setContent {
      WeekTabContent(
          vm = vm,
          weeksVm = weeksVm,
          objectivesVm = objectivesVm,
          allTasks = emptyList<StudyItem>(),
          selectedDate = LocalDate.now())
    }

    compose.onNodeWithTag("WeekContent").assertExists()
    compose.onNodeWithTag("WeekUpcomingSection").assertExists()
    compose
        .onNodeWithTag(com.android.sample.feature.weeks.ui.WeekProgDailyObjTags.WEEK_DOTS_ROW)
        .assertExists()
  }

  // -------- MonthTabContent --------
  @Test
  fun monthTab_renders_grid_and_empty_important_section() {
    val vm = ScheduleViewModel(FakeScheduleRepository())

    compose.setContent {
      MonthTabContent(
          vm = vm,
          allTasks = emptyList<StudyItem>(), // triggers "No important events this month"
          selectedDate = LocalDate.now(),
          currentMonth = YearMonth.now())
    }

    compose.onNodeWithTag("MonthContent").assertExists()
    compose.onNodeWithTag("MonthImportantSection").assertExists()
    compose.onAllNodesWithText("Most important this month")[0].assertExists()
    compose.onNodeWithText("No important events this month").assertExists()
  }

  // -------- AgendaTabContent --------
  @Test
  fun agendaTab_renders_container_and_section() {
    val vm = ScheduleViewModel(FakeScheduleRepository())

    compose.setContent {
      AgendaTabContent(vm = vm, allTasks = emptyList<StudyItem>(), selectedDate = LocalDate.now())
    }

    compose.onNodeWithTag("AgendaContent").assertExists()
    compose.onNodeWithTag("AgendaSection").assertExists()
    compose.onAllNodesWithText("Agenda")[0].assertExists()
  }

  // ------------ Fake repo that exactly matches ScheduleRepository ------------
  private class FakeScheduleRepository : ScheduleRepository {
    private val _events = MutableStateFlow<List<ScheduleEvent>>(emptyList())
    override val events: StateFlow<List<ScheduleEvent>>
      get() = _events

    override suspend fun save(event: ScheduleEvent) {
      _events.value = _events.value + event
    }

    override suspend fun update(event: ScheduleEvent) {
      _events.value = _events.value.map { if (it.id == event.id) event else it }
    }

    override suspend fun delete(eventId: String) {
      _events.value = _events.value.filterNot { it.id == eventId }
    }

    override suspend fun getEventsBetween(start: LocalDate, end: LocalDate): List<ScheduleEvent> =
        _events.value.filter { it.date >= start && it.date <= end }

    override suspend fun getById(id: String): ScheduleEvent? =
        _events.value.firstOrNull { it.id == id }

    override suspend fun moveEventDate(id: String, newDate: LocalDate): Boolean {
      val idx = _events.value.indexOfFirst { it.id == id }
      if (idx < 0) return false
      val list = _events.value.toMutableList()
      list[idx] = list[idx].copy(date = newDate)
      _events.value = list
      return true
    }

    override suspend fun getEventsForDate(date: LocalDate): List<ScheduleEvent> =
        _events.value.filter { it.date == date }

    override suspend fun getEventsForWeek(startDate: LocalDate): List<ScheduleEvent> {
      val endDate = startDate.plusDays(6)
      return getEventsBetween(startDate, endDate)
    }
  }
}
