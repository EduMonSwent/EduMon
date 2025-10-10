package com.android.sample.ui.viewmodel

import androidx.lifecycle.ViewModel
import java.time.DayOfWeek
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// Data models --------------------------------------------------------------

data class Objective(
    val title: String,
    val course: String,
    val estimateMinutes: Int = 0,
    val reason: String = ""
)

data class DayStatus(val dayOfWeek: DayOfWeek, val metTarget: Boolean)

data class WeekProgressItem(
    val label: String,
    val percent: Int // 0..100
)

// UI State -----------------------------------------------------------------

data class WeekProgressUiState(
    val weekProgressPercent: Int = 0,
    val dayStatuses: List<DayStatus> = emptyList(),
    val objectives: List<Objective> = emptyList(),
    val showWhy: Boolean = true,
    val weeks: List<WeekProgressItem> = emptyList(),
    val selectedWeekIndex: Int = 0
)

// ViewModel ----------------------------------------------------------------

class WeekProgressViewModel : ViewModel() {

  private val _uiState =
      MutableStateFlow(
          WeekProgressUiState(
              weekProgressPercent = 55,
              dayStatuses =
                  listOf(
                      DayStatus(DayOfWeek.MONDAY, true),
                      DayStatus(DayOfWeek.TUESDAY, true),
                      DayStatus(DayOfWeek.WEDNESDAY, false),
                      DayStatus(DayOfWeek.THURSDAY, true),
                      DayStatus(DayOfWeek.FRIDAY, false),
                      DayStatus(DayOfWeek.SATURDAY, false),
                      DayStatus(DayOfWeek.SUNDAY, false),
                  ),
              objectives =
                  listOf(
                      Objective(
                          title = "Finish Quiz 3",
                          course = "CS101",
                          estimateMinutes = 30,
                          reason = "Due tomorrow • high impact"),
                      Objective(
                          title = "Read Chapter 5",
                          course = "Math201",
                          estimateMinutes = 25,
                          reason = "Prereq for next lecture"),
                      Objective(
                          title = "Flashcards Review",
                          course = "Bio110",
                          estimateMinutes = 15,
                          reason = "Spaced repetition")),
              weeks =
                  listOf(
                      WeekProgressItem("Week 1", 100),
                      WeekProgressItem("Week 2", 55),
                      WeekProgressItem("Week 3", 10),
                      WeekProgressItem("Week 4", 0)),
              selectedWeekIndex = 1))
  val uiState = _uiState.asStateFlow()

  // --- Actions the UI can call ---
  @Suppress("UNUSED_PARAMETER")
  fun startObjective(index: Int = 0) {
    /* hook for analytics / repo later */
  }

  /** Selects a week and syncs the top progress percent with that week. */
  fun selectWeek(index: Int) {
    _uiState.update { current ->
      val safe = index.coerceIn(0, current.weeks.lastIndex.coerceAtLeast(0))
      val newPct = current.weeks.getOrNull(safe)?.percent ?: current.weekProgressPercent
      current.copy(selectedWeekIndex = safe, weekProgressPercent = newPct)
    }
  }

  /** Sets the overall week progress percent (0..100). */
  fun setProgress(pct: Int) {
    _uiState.update { it.copy(weekProgressPercent = pct.coerceIn(0, 100)) }
  }

  // --- Weeks --------------------------------------------------------------

  /** Replaces the full weeks list. Optionally changes selection. */
  fun setWeeks(weeks: List<WeekProgressItem>, selectedIndex: Int? = null) {
    _uiState.update { current ->
      val newSelected =
          selectedIndex?.coerceIn(0, weeks.lastIndex.coerceAtLeast(0))
              ?: current.selectedWeekIndex.coerceIn(0, weeks.lastIndex.coerceAtLeast(0))
      val newPct = weeks.getOrNull(newSelected)?.percent ?: current.weekProgressPercent
      current.copy(weeks = weeks, selectedWeekIndex = newSelected, weekProgressPercent = newPct)
    }
  }

  /** Updates the percent for a given week; also syncs header percent if selected. */
  fun updateWeekPercent(index: Int, percent: Int) {
    _uiState.update { current ->
      if (index !in current.weeks.indices) return@update current
      val clamped = percent.coerceIn(0, 100)
      val updatedWeeks =
          current.weeks.toMutableList().also { list ->
            val item = list[index]
            list[index] = item.copy(percent = clamped)
          }
      val newHeaderPct =
          if (index == current.selectedWeekIndex) clamped else current.weekProgressPercent
      current.copy(weeks = updatedWeeks, weekProgressPercent = newHeaderPct)
    }
  }

  /** Convenience to go to the next week (clamped at last index). */
  fun selectNextWeek() {
    _uiState.update { cur ->
      val next = (cur.selectedWeekIndex + 1).coerceAtMost(cur.weeks.lastIndex.coerceAtLeast(0))
      val pct = cur.weeks.getOrNull(next)?.percent ?: cur.weekProgressPercent
      cur.copy(selectedWeekIndex = next, weekProgressPercent = pct)
    }
  }

  /** Convenience to go to the previous week (clamped at 0). */
  fun selectPreviousWeek() {
    _uiState.update { cur ->
      val prev = (cur.selectedWeekIndex - 1).coerceAtLeast(0)
      val pct = cur.weeks.getOrNull(prev)?.percent ?: cur.weekProgressPercent
      cur.copy(selectedWeekIndex = prev, weekProgressPercent = pct)
    }
  }

  // --- Day statuses -------------------------------------------------------

  /** Replaces the full day status list. */
  fun setDayStatuses(statuses: List<DayStatus>) {
    _uiState.update { it.copy(dayStatuses = statuses) }
  }

  /** Toggles the metTarget flag for a given day of week. */
  fun toggleDayMet(day: DayOfWeek) {
    _uiState.update { cur ->
      val updated =
          cur.dayStatuses.map {
            if (it.dayOfWeek == day) it.copy(metTarget = !it.metTarget) else it
          }
      cur.copy(dayStatuses = updated)
    }
  }

  // --- Objectives ---------------------------------------------------------

  /** Replaces the full objectives list. */
  fun setObjectives(objs: List<Objective>) {
    _uiState.update { it.copy(objectives = objs) }
  }

  /** Appends an objective. */
  fun addObjective(obj: Objective) {
    _uiState.update { it.copy(objectives = it.objectives + obj) }
  }

  /** Updates an objective by index. No-op if out of bounds. */
  fun updateObjective(index: Int, obj: Objective) {
    _uiState.update { cur ->
      if (index !in cur.objectives.indices) return@update cur
      val list = cur.objectives.toMutableList()
      list[index] = obj
      cur.copy(objectives = list)
    }
  }

  /** Removes an objective by index. No-op if out of bounds. */
  fun removeObjective(index: Int) {
    _uiState.update { cur ->
      if (index !in cur.objectives.indices) return@update cur
      cur.copy(objectives = cur.objectives.filterIndexed { i, _ -> i != index })
    }
  }

  /** Reorders an objective position. Safely clamps indices. */
  fun moveObjective(fromIndex: Int, toIndex: Int) {
    _uiState.update { cur ->
      if (cur.objectives.isEmpty()) return@update cur
      val from = fromIndex.coerceIn(0, cur.objectives.lastIndex)
      val to = toIndex.coerceIn(0, cur.objectives.lastIndex)
      if (from == to) return@update cur
      val list = cur.objectives.toMutableList()
      val item = list.removeAt(from)
      list.add(to, item)
      cur.copy(objectives = list)
    }
  }

  // --- Flags --------------------------------------------------------------

  /** Controls whether to show the objective reason text in UI. */
  fun setShowWhy(show: Boolean) {
    _uiState.update { it.copy(showWhy = show) }
  }
}
