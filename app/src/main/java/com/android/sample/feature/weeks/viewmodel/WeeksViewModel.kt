package com.android.sample.feature.weeks.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.sample.feature.weeks.model.DayStatus
import com.android.sample.feature.weeks.model.WeekProgressItem
import com.android.sample.feature.weeks.repository.FakeWeeksRepository
import com.android.sample.feature.weeks.repository.WeeksRepository
import java.time.DayOfWeek
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Holds only week-related UI state

data class WeeksUiState(
    val weekProgressPercent: Int = 0,
    val dayStatuses: List<DayStatus> = emptyList(),
    val weeks: List<WeekProgressItem> = emptyList(),
    val selectedWeekIndex: Int = 0,
    val isLoading: Boolean = true,
)

class WeeksViewModel(
    private val repository: WeeksRepository = FakeWeeksRepository(),
) : ViewModel() {
  private val _uiState = MutableStateFlow(WeeksUiState())
  val uiState = _uiState.asStateFlow()

  init {
    refresh()
  }

  fun refresh() {
    _uiState.update { it.copy(isLoading = true) }
    viewModelScope.launch {
      val weeks = repository.getWeeks()
      val statuses = repository.getDayStatuses()
      // default to first week selected
      val selected = 0.coerceIn(0, weeks.lastIndex.coerceAtLeast(0))
      val headerPct = weeks.getOrNull(selected)?.percent ?: 0
      _uiState.update {
        it.copy(
            isLoading = false,
            weeks = weeks,
            dayStatuses = statuses,
            selectedWeekIndex = selected,
            weekProgressPercent = headerPct)
      }
    }
  }

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
    // Delegate to repository, then sync UI state
    viewModelScope.launch {
      val updatedWeeks = repository.updateWeekPercent(index, percent)
      _uiState.update { cur ->
        val sel = cur.selectedWeekIndex.coerceIn(0, updatedWeeks.lastIndex.coerceAtLeast(0))
        val header = updatedWeeks.getOrNull(sel)?.percent ?: cur.weekProgressPercent
        cur.copy(weeks = updatedWeeks, weekProgressPercent = header)
      }
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
