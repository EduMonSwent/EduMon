package com.android.sample.ui.viewmodel

import java.time.DayOfWeek
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WeekProgressViewModelTest {

  @Test
  fun initial_state_matches_defaults() = runTest {
    val vm = WeekProgressViewModel()
    val s = vm.uiState.first()

    assertEquals(55, s.weekProgressPercent)
    assertEquals(4, s.weeks.size)
    assertEquals(1, s.selectedWeekIndex)
    assertEquals(7, s.dayStatuses.size)
    assertTrue(s.objectives.isNotEmpty())
  }

  @Test
  fun selectWeek_updates_selectedIndex_and_headerPercent() = runTest {
    val vm = WeekProgressViewModel()
    vm.selectWeek(0)
    val s = vm.uiState.first()
    assertEquals(0, s.selectedWeekIndex)
    assertEquals(100, s.weekProgressPercent)
  }

  @Test
  fun setProgress_clamps_range() = runTest {
    val vm = WeekProgressViewModel()
    vm.setProgress(200)
    assertEquals(100, vm.uiState.first().weekProgressPercent)
    vm.setProgress(-1)
    assertEquals(0, vm.uiState.first().weekProgressPercent)
  }

  @Test
  fun updateWeekPercent_updates_list_and_header_when_selected() = runTest {
    val vm = WeekProgressViewModel()
    // change selected week (default 1) to 60
    vm.updateWeekPercent(1, 60)
    val s1 = vm.uiState.first()
    assertEquals(60, s1.weeks[1].percent)
    assertEquals(60, s1.weekProgressPercent)

    // change non-selected week does not affect header
    vm.selectWeek(0)
    vm.updateWeekPercent(1, 30)
    val s2 = vm.uiState.first()
    assertEquals(30, s2.weeks[1].percent)
    assertEquals(100, s2.weekProgressPercent) // still selected week 0
  }

  @Test
  fun setWeeks_replaces_and_clamps_selection() = runTest {
    val vm = WeekProgressViewModel()
    vm.setWeeks(listOf(WeekProgressItem("Only Week", 42)), selectedIndex = 10)
    val s = vm.uiState.first()
    assertEquals(0, s.selectedWeekIndex)
    assertEquals(42, s.weekProgressPercent)
    assertEquals(1, s.weeks.size)
  }

  @Test
  fun next_prev_week_helpers_move_selection_safely() = runTest {
    val vm = WeekProgressViewModel()
    val initial = vm.uiState.first()
    assertEquals(1, initial.selectedWeekIndex)

    vm.selectNextWeek()
    assertEquals(2, vm.uiState.first().selectedWeekIndex)

    // Go beyond bounds safely
    vm.selectNextWeek()
    vm.selectNextWeek()
    vm.selectNextWeek()
    assertEquals(vm.uiState.first().weeks.lastIndex, vm.uiState.first().selectedWeekIndex)

    vm.selectPreviousWeek()
    vm.selectPreviousWeek()
    vm.selectPreviousWeek()
    assertEquals(0, vm.uiState.first().selectedWeekIndex)
  }

  @Test
  fun setDayStatuses_and_toggleDayMet_work() = runTest {
    val vm = WeekProgressViewModel()
    val allFalse = DayOfWeek.values().map { DayStatus(it, false) }
    vm.setDayStatuses(allFalse)
    assertTrue(vm.uiState.first().dayStatuses.all { !it.metTarget })

    vm.toggleDayMet(DayOfWeek.MONDAY)
    val s = vm.uiState.first()
    assertTrue(s.dayStatuses.first { it.dayOfWeek == DayOfWeek.MONDAY }.metTarget)
  }

  @Test
  fun objectives_crud_and_reorder() = runTest {
    val vm = WeekProgressViewModel()
    val baseCount = vm.uiState.first().objectives.size

    val newObj = Objective("New", "X", 5, "Because")
    vm.addObjective(newObj)
    assertEquals(baseCount + 1, vm.uiState.first().objectives.size)

    vm.updateObjective(baseCount, newObj.copy(title = "New2"))
    assertEquals("New2", vm.uiState.first().objectives[baseCount].title)

    vm.moveObjective(baseCount, 0)
    assertEquals("New2", vm.uiState.first().objectives[0].title)

    vm.removeObjective(0)
    assertEquals(baseCount, vm.uiState.first().objectives.size)

    vm.setObjectives(emptyList())
    assertTrue(vm.uiState.first().objectives.isEmpty())
  }

  @Test
  fun setShowWhy_updates_flag() = runTest {
    val vm = WeekProgressViewModel()
    vm.setShowWhy(false)
    assertFalse(vm.uiState.first().showWhy)
  }
}
