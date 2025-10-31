// src/test/java/com/android/sample/schedule/ScheduleViewModelTest.kt
@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.android.sample.schedule

import com.android.sample.model.schedule.EventKind
import com.android.sample.model.schedule.ScheduleEvent
import com.android.sample.model.schedule.ScheduleRepository
import com.android.sample.testing.MainDispatcherRule
import com.android.sample.ui.schedule.ScheduleViewModel
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

private class FakeScheduleRepository : ScheduleRepository {
  private val _events = MutableStateFlow<List<ScheduleEvent>>(emptyList())
  override val events: StateFlow<List<ScheduleEvent>>
    get() = _events

  fun emit(vararg ev: ScheduleEvent) {
    _events.value = ev.toList()
  }

  val moved = mutableListOf<Pair<String, LocalDate>>()
  val saved = mutableListOf<ScheduleEvent>()
  val deleted = mutableListOf<String>()
  val updated = mutableListOf<ScheduleEvent>()

  override suspend fun save(event: ScheduleEvent) {
    saved.add(event)
  }

  override suspend fun update(event: ScheduleEvent) {
    updated.add(event)
  }

  override suspend fun delete(eventId: String) {
    deleted.add(eventId)
  }

  override suspend fun getEventsBetween(start: LocalDate, end: LocalDate) =
      _events.value.filter { it.date in start..end }

  override suspend fun getById(id: String) = _events.value.firstOrNull { it.id == id }

  override suspend fun moveEventDate(id: String, newDate: LocalDate): Boolean {
    moved.add(id to newDate)
    return true
  }

  override suspend fun getEventsForDate(date: LocalDate) = _events.value.filter { it.date == date }

  override suspend fun getEventsForWeek(startDate: LocalDate) =
      _events.value.filter { it.date in startDate..startDate.plusDays(6) }
}

class ScheduleViewModelTest {

  @get:Rule val mainDispatcherRule = MainDispatcherRule()

  @Test
  fun monthWeekNavigation_and_filters() = runTest {
    val repo = FakeScheduleRepository()
    val vm = ScheduleViewModel(repo)

    // defaults
    assertEquals(LocalDate.now().month, vm.selectedDate.value.month)
    assertEquals(YearMonth.now(), vm.currentDisplayMonth.value)

    // toggle
    vm.setWeekMode()
    assertFalse(vm.isMonthView.value)
    vm.setMonthMode()
    assertTrue(vm.isMonthView.value)

    // month navigation keeps date in month
    val before = vm.currentDisplayMonth.value
    vm.onPreviousMonthWeekClicked()
    assertEquals(before.minusMonths(1), vm.currentDisplayMonth.value)
    vm.onNextMonthWeekClicked()
    vm.onNextMonthWeekClicked()
    assertEquals(before.plusMonths(1), vm.currentDisplayMonth.value)

    // filters
    vm.setFilters(setOf(EventKind.STUDY))
    assertEquals(setOf(EventKind.STUDY), vm.activeKinds.value)
  }

  @Test
  fun adjustWeeklyPlan_distributesPulledTask_toLeastLoadedRemainingDay_andSetsLastAdjustment() =
      runTest {
        val repo = FakeScheduleRepository()
        val vm = ScheduleViewModel(repo)

        val today = LocalDate.of(2025, 3, 6) // Thu
        val weekStart = vm.startOfWeek(today) // Mon
        val weekEnd = weekStart.plusDays(6) // Sun

        // Current week load: Thu has 2 tasks, Fri has 1, Sat/Sun 0 → earliest min is Sat
        val thuTask1 =
            ScheduleEvent(id = "cw1", title = "thu-1", date = today, kind = EventKind.STUDY)
        val thuTask2 =
            ScheduleEvent(id = "cw2", title = "thu-2", date = today, kind = EventKind.STUDY)
        val friTask =
            ScheduleEvent(
                id = "cw3", title = "fri-1", date = today.plusDays(1), kind = EventKind.STUDY)

        // Completed early (date > today inside current week) → triggers pull
        val completedEarly =
            ScheduleEvent(
                id = "done",
                title = "done early",
                date = today.plusDays(1),
                isCompleted = true,
                kind = EventKind.STUDY)

        // Next week candidate to pull
        val nextWeekCandidate =
            ScheduleEvent(
                id = "n1",
                title = "next-week",
                date = weekStart.plusWeeks(1),
                kind = EventKind.STUDY)

        // Emit all events (VM reads via repository queries)
        repo.emit(thuTask1, thuTask2, friTask, completedEarly, nextWeekCandidate)

        // Run adjust
        vm.adjustWeeklyPlan(today)
        advanceUntilIdle()

        // Should place candidate on the earliest least-loaded remaining day (Sat)
        val expectedTarget = today.plusDays(2) // Thu + 2 = Sat
        assertTrue(repo.moved.contains("n1" to expectedTarget))

        // State flags updated
        assertFalse(vm.isAdjustingPlan.value)
        assertEquals(today, vm.lastAdjustment.value)
      }

  @Test
  fun adjustWeeklyPlan_reentrancyGuard_onlyRunsOnce_whenCalledTwiceImmediately() = runTest {
    val repo = FakeScheduleRepository()
    val vm = ScheduleViewModel(repo)

    val today = LocalDate.of(2025, 3, 6)
    val weekStart = vm.startOfWeek(today)

    val completedEarly =
        ScheduleEvent(
            id = "c1",
            title = "done",
            date = today.plusDays(1),
            isCompleted = true,
            kind = EventKind.STUDY)
    val candidate =
        ScheduleEvent(
            id = "n1", title = "next-week", date = weekStart.plusWeeks(1), kind = EventKind.STUDY)
    repo.emit(completedEarly, candidate)

    // Call twice before yielding
    vm.adjustWeeklyPlan(today)
    vm.adjustWeeklyPlan(today)
    advanceUntilIdle()

    // Only one move should have been recorded
    val movesForN1 = repo.moved.filter { it.first == "n1" }
    assertEquals(1, movesForN1.size)
  }

  @Test
  fun filters_and_modes_and_completion_false_dontAdjust() = runTest {
    val repo = FakeScheduleRepository()
    val vm = ScheduleViewModel(repo)

    vm.setWeekMode()
    assertFalse(vm.isMonthView.value)
    vm.setMonthMode()
    assertTrue(vm.isMonthView.value)

    vm.setFilters(setOf(EventKind.STUDY))
    assertEquals(setOf(EventKind.STUDY), vm.activeKinds.value)

    // completion=false → update only, no adjust call path required
    val e = ScheduleEvent(id = "x", title = "t", date = LocalDate.now(), kind = EventKind.STUDY)
    repo.emit(e)
    vm.onTaskCompletionChanged("x", completed = false)
    advanceUntilIdle()
    assertTrue(repo.updated.any { it.id == "x" && !it.isCompleted })
  }
}
