package com.android.sample.ui.viewmodel

import androidx.lifecycle.ViewModel
import java.time.DayOfWeek
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// Holds only week-related UI state

data class WeeksUiState(
    val weekProgressPercent: Int = 0,
    val dayStatuses: List<DayStatus> = emptyList(),
    val weeks: List<WeekProgressItem> = emptyList(),
    val selectedWeekIndex: Int = 0,
)

class WeeksViewModel : ViewModel() {
  private val _uiState = MutableStateFlow(WeeksUiState())
  val uiState = _uiState.asStateFlow()

  // ---- Mutations ----
  fun setProgress(pct: Int) {
    _uiState.update { it.copy(weekProgressPercent = pct.coerceIn(0, 100)) }
  }

  fun setWeeks(weeks: List<WeekProgressItem>, selectedIndex: Int? = null) {
    _uiState.update { current ->
      val newSelected =
          selectedIndex?.coerceIn(0, weeks.lastIndex.coerceAtLeast(0))
              ?: current.selectedWeekIndex.coerceIn(0, weeks.lastIndex.coerceAtLeast(0))
      val newPct = weeks.getOrNull(newSelected)?.percent ?: current.weekProgressPercent
      current.copy(weeks = weeks, selectedWeekIndex = newSelected, weekProgressPercent = newPct)
    }
  }

  fun updateWeekPercent(index: Int, percent: Int) {
    _uiState.update { current ->
      if (index !in current.weeks.indices) return@update current
      val clamped = percent.coerceIn(0, 100)
      val updatedWeeks =
          current.weeks.toMutableList().also { list ->
            list[index] = list[index].copy(percent = clamped)
          }
      val newHeaderPct =
          if (index == current.selectedWeekIndex) clamped else current.weekProgressPercent
      current.copy(weeks = updatedWeeks, weekProgressPercent = newHeaderPct)
    }
  }

  fun selectWeek(index: Int) {
    _uiState.update { current ->
      val safe = index.coerceIn(0, current.weeks.lastIndex.coerceAtLeast(0))
      val newPct = current.weeks.getOrNull(safe)?.percent ?: current.weekProgressPercent
      current.copy(selectedWeekIndex = safe, weekProgressPercent = newPct)
    }
  }

  fun selectNextWeek() {
    _uiState.update { cur ->
      val next = (cur.selectedWeekIndex + 1).coerceAtMost(cur.weeks.lastIndex.coerceAtLeast(0))
      val pct = cur.weeks.getOrNull(next)?.percent ?: cur.weekProgressPercent
      cur.copy(selectedWeekIndex = next, weekProgressPercent = pct)
    }
  }

  fun selectPreviousWeek() {
    _uiState.update { cur ->
      val prev = (cur.selectedWeekIndex - 1).coerceAtLeast(0)
      val pct = cur.weeks.getOrNull(prev)?.percent ?: cur.weekProgressPercent
      cur.copy(selectedWeekIndex = prev, weekProgressPercent = pct)
    }
  }

  fun setDayStatuses(statuses: List<DayStatus>) {
    _uiState.update { it.copy(dayStatuses = statuses) }
  }

  fun toggleDayMet(day: DayOfWeek) {
    _uiState.update { cur ->
      val updated =
          cur.dayStatuses.map {
            if (it.dayOfWeek == day) it.copy(metTarget = !it.metTarget) else it
          }
      cur.copy(dayStatuses = updated)
    }
  }
}
