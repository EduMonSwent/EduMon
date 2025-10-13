package com.android.sample.ui.viewmodel

import java.time.DayOfWeek
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WeeksAndObjectivesViewModelsTest {

  // ---------------- WeeksViewModel ----------------

  @Test
  fun weeks_initial_and_selection_updates_headerPercent() = runTest {
    val vm = WeeksViewModel()
    vm.setWeeks(
        listOf(
            WeekProgressItem("Week 1", 100),
            WeekProgressItem("Week 2", 55),
            WeekProgressItem("Week 3", 10)),
        selectedIndex = 1)
    vm.setProgress(55)

    val s = vm.uiState.first()
    assertEquals(55, s.weekProgressPercent)
    assertEquals(1, s.selectedWeekIndex)

    vm.selectWeek(0)
    val s2 = vm.uiState.first()
    assertEquals(0, s2.selectedWeekIndex)
    assertEquals(100, s2.weekProgressPercent)
  }

  @Test
  fun updateWeekPercent_syncs_header_when_selected() = runTest {
    val vm = WeeksViewModel()
    vm.setWeeks(
        listOf(WeekProgressItem("Week 1", 100), WeekProgressItem("Week 2", 55)), selectedIndex = 1)
    vm.updateWeekPercent(1, 60)
    val s = vm.uiState.first()
    assertEquals(60, s.weeks[1].percent)
    assertEquals(60, s.weekProgressPercent)
  }

  @Test
  fun next_prev_clamped() = runTest {
    val vm = WeeksViewModel()
    vm.setWeeks(
        listOf(
            WeekProgressItem("Week 1", 100),
            WeekProgressItem("Week 2", 55),
            WeekProgressItem("Week 3", 10)),
        selectedIndex = 1)

    vm.selectNextWeek()
    assertEquals(2, vm.uiState.first().selectedWeekIndex)

    vm.selectNextWeek()
    vm.selectNextWeek()
    assertEquals(2, vm.uiState.first().selectedWeekIndex)

    vm.selectPreviousWeek()
    vm.selectPreviousWeek()
    vm.selectPreviousWeek()
    assertEquals(0, vm.uiState.first().selectedWeekIndex)
  }

  @Test
  fun dayStatuses_toggle_only_target_day() = runTest {
    val vm = WeeksViewModel()
    vm.setDayStatuses(
        listOf(DayStatus(DayOfWeek.MONDAY, false), DayStatus(DayOfWeek.TUESDAY, false)))
    vm.toggleDayMet(DayOfWeek.MONDAY)
    val s = vm.uiState.first()
    assertTrue(s.dayStatuses.first { it.dayOfWeek == DayOfWeek.MONDAY }.metTarget)
    assertFalse(s.dayStatuses.first { it.dayOfWeek == DayOfWeek.TUESDAY }.metTarget)
  }

  // ---------------- ObjectivesViewModel ----------------

  @Test
  fun objectives_crud_and_reorder_and_flag() = runTest {
    val vm = ObjectivesViewModel()
    val obj = Objective("Task", "Course", 5, "Reason")

    vm.addObjective(obj)
    assertEquals(1, vm.uiState.first().objectives.size)

    vm.updateObjective(0, obj.copy(title = "Task2"))
    assertEquals("Task2", vm.uiState.first().objectives[0].title)

    vm.addObjective(obj.copy(title = "B"))
    vm.moveObjective(1, 0)
    assertEquals("B", vm.uiState.first().objectives[0].title)

    vm.removeObjective(0)
    assertEquals(1, vm.uiState.first().objectives.size)

    assertTrue(vm.uiState.first().showWhy)
    vm.setShowWhy(false)
    assertFalse(vm.uiState.first().showWhy)
  }
}
