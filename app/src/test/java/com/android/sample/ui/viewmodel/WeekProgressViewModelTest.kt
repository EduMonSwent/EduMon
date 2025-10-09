package com.android.sample.ui.viewmodel

import java.time.DayOfWeek
import org.junit.Assert.*
import org.junit.Test

class WeekProgressViewModelTest {

  private fun newVm() = WeekProgressViewModel()

  // --- Initial state ----------------------------------------------------

  @Test
  fun initialState_isExpected() {
    val vm = newVm()
    val s = vm.uiState.value

    assertEquals(55, s.weekProgressPercent)
    assertEquals(1, s.selectedWeekIndex)
    assertEquals(4, s.weeks.size)
    assertEquals(7, s.dayStatuses.size)
    assertEquals(3, s.objectives.size)
    assertTrue(s.showWhy)
  }

  // --- selectWeek / clamping / sync header percent ----------------------

  @Test
  fun selectWeek_inRange_updatesHeaderPercent() {
    val vm = newVm()
    vm.selectWeek(0)
    val s = vm.uiState.value
    assertEquals(0, s.selectedWeekIndex)
    assertEquals(100, s.weekProgressPercent) // Week 1 percent
  }

  @Test
  fun selectWeek_negativeIndex_clampsToZero() {
    val vm = newVm()
    vm.selectWeek(-5)
    val s = vm.uiState.value
    assertEquals(0, s.selectedWeekIndex)
    assertEquals(100, s.weekProgressPercent)
  }

  @Test
  fun selectWeek_tooLarge_clampsToLast() {
    val vm = newVm()
    vm.selectWeek(999)
    val s = vm.uiState.value
    assertEquals(3, s.selectedWeekIndex) // last index
    assertEquals(0, s.weekProgressPercent) // Week 4 percent
  }

  // --- setProgress (global header percent) ------------------------------

  @Test
  fun setProgress_isClampedTo0_100() {
    val vm = newVm()
    vm.setProgress(-10)
    assertEquals(0, vm.uiState.value.weekProgressPercent)
    vm.setProgress(110)
    assertEquals(100, vm.uiState.value.weekProgressPercent)
    vm.setProgress(42)
    assertEquals(42, vm.uiState.value.weekProgressPercent)
  }

  // --- setWeeks ----------------------------------------------------------

  @Test
  fun setWeeks_replacesList_andOptionallySelectsIndex() {
    val vm = newVm()
    val weeks =
        listOf(WeekProgressItem("W1", 10), WeekProgressItem("W2", 20), WeekProgressItem("W3", 30))

    vm.setWeeks(weeks, selectedIndex = 2)
    vm.uiState.value.let { s ->
      assertEquals(3, s.weeks.size)
      assertEquals(2, s.selectedWeekIndex)
      assertEquals(30, s.weekProgressPercent)
    }

    // no selectedIndex -> preserves (and clamps if needed)
    vm.setWeeks(weeks)
    assertEquals(2, vm.uiState.value.selectedWeekIndex)
  }

  @Test
  fun setWeeks_emptyList_keepsHeaderPercent_andClampsSelection() {
    val vm = newVm()
    val originalPct = vm.uiState.value.weekProgressPercent
    vm.setWeeks(emptyList())
    vm.uiState.value.let { s ->
      assertEquals(0, s.weeks.size)
      assertEquals(0, s.selectedWeekIndex) // clamped to 0 even if list is empty
      assertEquals(originalPct, s.weekProgressPercent) // unchanged
    }
  }

  // --- updateWeekPercent -------------------------------------------------

  @Test
  fun updateWeekPercent_updatesSelected_andSyncsHeader() {
    val vm = newVm()
    // selected is 1 by default ("Week 2" -> 55)
    vm.updateWeekPercent(index = 1, percent = 77)
    vm.uiState.value.let { s ->
      assertEquals(77, s.weekProgressPercent)
      assertEquals(77, s.weeks[1].percent)
    }
  }

  @Test
  fun updateWeekPercent_nonSelected_doesNotChangeHeader() {
    val vm = newVm()
    val before = vm.uiState.value.weekProgressPercent // 55
    vm.updateWeekPercent(index = 0, percent = 1) // update Week 1 (non-selected)
    vm.uiState.value.let { s ->
      assertEquals(before, s.weekProgressPercent) // header unchanged
      assertEquals(1, s.weeks[0].percent)
    }
  }

  @Test
  fun updateWeekPercent_outOfBounds_noOp() {
    val vm = newVm()
    val before = vm.uiState.value
    vm.updateWeekPercent(index = 99, percent = 60)
    assertEquals(before, vm.uiState.value)
  }

  // --- Navigation helpers -----------------------------------------------

  @Test
  fun selectNextWeek_movesForward_andClampsAtEnd() {
    val vm = newVm()
    // from 1 -> 2
    vm.selectNextWeek()
    assertEquals(2, vm.uiState.value.selectedWeekIndex)
    assertEquals(10, vm.uiState.value.weekProgressPercent) // Week 3 percent

    // to end and stay there
    vm.selectNextWeek() // -> 3
    vm.selectNextWeek() // stay at 3
    assertEquals(3, vm.uiState.value.selectedWeekIndex)
    assertEquals(0, vm.uiState.value.weekProgressPercent)
  }

  @Test
  fun selectPreviousWeek_movesBackward_andClampsAtZero() {
    val vm = newVm()
    vm.selectWeek(0)
    vm.selectPreviousWeek() // stays at 0
    assertEquals(0, vm.uiState.value.selectedWeekIndex)
    assertEquals(100, vm.uiState.value.weekProgressPercent)
  }

  @Test
  fun navigation_withEmptyWeeks_doesNotCrash_andClampsSelection() {
    val vm = newVm()
    vm.setWeeks(emptyList())
    val beforePct = vm.uiState.value.weekProgressPercent
    vm.selectNextWeek()
    vm.selectPreviousWeek()
    vm.uiState.value.let { s ->
      assertEquals(0, s.selectedWeekIndex)
      assertEquals(0, s.weeks.size)
      assertEquals(beforePct, s.weekProgressPercent)
    }
  }

  // --- Day statuses ------------------------------------------------------

  @Test
  fun setDayStatuses_replacesList() {
    val vm = newVm()
    val statuses = DayOfWeek.values().map { DayStatus(it, false) }.toList()
    vm.setDayStatuses(statuses)
    assertEquals(7, vm.uiState.value.dayStatuses.size)
    assertTrue(vm.uiState.value.dayStatuses.all { !it.metTarget })
  }

  @Test
  fun toggleDayMet_togglesOnlyTargetDay() {
    val vm = newVm()
    vm.setDayStatuses(
        listOf(DayStatus(DayOfWeek.MONDAY, false), DayStatus(DayOfWeek.WEDNESDAY, false)))
    vm.toggleDayMet(DayOfWeek.WEDNESDAY)
    val map = vm.uiState.value.dayStatuses.associateBy { it.dayOfWeek }
    assertFalse(map[DayOfWeek.MONDAY]!!.metTarget)
    assertTrue(map[DayOfWeek.WEDNESDAY]!!.metTarget)
  }

  @Test
  fun toggleDayMet_dayNotPresent_isNoOp() {
    val vm = newVm()
    vm.setDayStatuses(listOf(DayStatus(DayOfWeek.MONDAY, true)))
    val before = vm.uiState.value.dayStatuses
    vm.toggleDayMet(DayOfWeek.WEDNESDAY)
    assertEquals(before, vm.uiState.value.dayStatuses)
  }

  // --- Objectives --------------------------------------------------------

  @Test
  fun setAddUpdateRemoveMoveObjective_worksAndClamps() {
    val vm = newVm()

    // replace
    val base =
        listOf(
            Objective("A", "C1", 10, "r1"),
            Objective("B", "C2", 20, "r2"),
            Objective("C", "C3", 30, "r3"))
    vm.setObjectives(base)
    assertEquals(3, vm.uiState.value.objectives.size)

    // add
    vm.addObjective(Objective("D", "C4", 40, "r4"))
    assertEquals(4, vm.uiState.value.objectives.size)
    assertEquals("D", vm.uiState.value.objectives.last().title)

    // update in-bounds
    vm.updateObjective(1, Objective("B2", "C2", 25, "r2b"))
    assertEquals("B2", vm.uiState.value.objectives[1].title)

    // update out-of-bounds -> no-op
    val beforeUpdate = vm.uiState.value.objectives
    vm.updateObjective(99, Objective("X", "C", 0, ""))
    assertEquals(beforeUpdate, vm.uiState.value.objectives)

    // remove in-bounds
    vm.removeObjective(0)
    assertEquals(3, vm.uiState.value.objectives.size)
    assertEquals("B2", vm.uiState.value.objectives[0].title)

    // remove out-of-bounds -> no-op
    val beforeRemove = vm.uiState.value.objectives
    vm.removeObjective(99)
    assertEquals(beforeRemove, vm.uiState.value.objectives)

    // move with clamping: move index 0 to end
    vm.moveObjective(fromIndex = 0, toIndex = 999)
    val objsAfterMove = vm.uiState.value.objectives
    assertEquals(3, objsAfterMove.size)
    assertEquals("C", objsAfterMove.first().title) // formerly index 2
    assertEquals("B2", objsAfterMove.last().title) // moved to end
  }

  @Test
  fun moveObjective_onEmptyList_isNoOp() {
    val vm = newVm()
    vm.setObjectives(emptyList())
    val before = vm.uiState.value.objectives
    vm.moveObjective(0, 1)
    assertEquals(before, vm.uiState.value.objectives)
  }

  // --- Flags & hooks -----------------------------------------------------

  @Test
  fun setShowWhy_updatesFlag() {
    val vm = newVm()
    vm.setShowWhy(false)
    assertFalse(vm.uiState.value.showWhy)
    vm.setShowWhy(true)
    assertTrue(vm.uiState.value.showWhy)
  }

  @Test
  fun startObjective_handlesInvalidIndex_noCrash() {
    val vm = newVm()
    vm.startObjective(-1)
    vm.startObjective(999)
    // nothing to assert; just ensuring no exception and line is covered
    assertTrue(true)
  }
}
