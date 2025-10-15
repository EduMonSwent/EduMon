package com.android.sample.ui.viewmodel

import com.android.sample.feature.weeks.model.DayStatus
import com.android.sample.feature.weeks.model.WeekProgressItem
import com.android.sample.feature.weeks.viewmodel.WeeksViewModel
import com.android.sample.testing.MainDispatcherRule
import java.time.DayOfWeek
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WeeksAndObjectivesViewModelsTest {

  @get:Rule val mainDispatcherRule = MainDispatcherRule()

  // ---------------- WeeksViewModel ----------------

  @Test
  fun weeks_initial_and_selection_updates_headerPercent() = runTest {
    val vm = WeeksViewModel()
    // Wait for init refresh coroutine
    advanceUntilIdle()

    vm.setWeeks(
        listOf(
            WeekProgressItem("Week 1", 100),
            WeekProgressItem("Week 2", 55),
            WeekProgressItem("Week 3", 10)),
        selectedIndex = 1)
    advanceUntilIdle()
    vm.setProgress(55)

    val s = vm.uiState.first()
    assertEquals(55, s.weekProgressPercent)
    assertEquals(1, s.selectedWeekIndex)

    vm.selectWeek(0)
    advanceUntilIdle()
    val s2 = vm.uiState.first()
    assertEquals(0, s2.selectedWeekIndex)
    assertEquals(100, s2.weekProgressPercent)
  }

  @Test
  fun updateWeekPercent_syncs_header_when_selected() = runTest {
    val vm = WeeksViewModel()
    advanceUntilIdle()

    vm.setWeeks(
        listOf(WeekProgressItem("Week 1", 100), WeekProgressItem("Week 2", 55)), selectedIndex = 1)
    advanceUntilIdle()
    vm.updateWeekPercent(1, 60)
    advanceUntilIdle()

    val s = vm.uiState.first()
    assertEquals(60, s.weeks[1].percent)
    assertEquals(60, s.weekProgressPercent)
  }

  @Test
  fun next_prev_clamped() = runTest {
    val vm = WeeksViewModel()
    advanceUntilIdle()

    vm.setWeeks(
        listOf(
            WeekProgressItem("Week 1", 100),
            WeekProgressItem("Week 2", 55),
            WeekProgressItem("Week 3", 10)),
        selectedIndex = 1)
    advanceUntilIdle()

    vm.selectNextWeek()
    advanceUntilIdle()
    assertEquals(2, vm.uiState.first().selectedWeekIndex)

    vm.selectNextWeek()
    advanceUntilIdle()
    vm.selectNextWeek()
    advanceUntilIdle()
    assertEquals(2, vm.uiState.first().selectedWeekIndex)

    vm.selectPreviousWeek()
    advanceUntilIdle()
    vm.selectPreviousWeek()
    advanceUntilIdle()
    vm.selectPreviousWeek()
    advanceUntilIdle()
    assertEquals(0, vm.uiState.first().selectedWeekIndex)
  }

  @Test
  fun dayStatuses_toggle_only_target_day() = runTest {
    val vm = WeeksViewModel()
    advanceUntilIdle()

    vm.setDayStatuses(
        listOf(DayStatus(DayOfWeek.MONDAY, false), DayStatus(DayOfWeek.TUESDAY, false)))
    advanceUntilIdle()
    vm.toggleDayMet(DayOfWeek.MONDAY)
    advanceUntilIdle()
    val s = vm.uiState.first()
    assertTrue(s.dayStatuses.first { it.dayOfWeek == DayOfWeek.MONDAY }.metTarget)
    assertFalse(s.dayStatuses.first { it.dayOfWeek == DayOfWeek.TUESDAY }.metTarget)
  }
}
