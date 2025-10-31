package com.android.sample.feature.weeks.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.sample.feature.weeks.model.DayStatus
import com.android.sample.feature.weeks.model.WeekContent
import com.android.sample.feature.weeks.model.WeekProgressItem
import com.android.sample.feature.weeks.repository.FirestoreWeeksRepository
import com.android.sample.feature.weeks.repository.WeeksRepository
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
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
    val currentWeekContent: WeekContent = WeekContent(emptyList(), emptyList()),
    val isLoading: Boolean = true,
)

class WeeksViewModel(
    private val repository: WeeksRepository =
        FirestoreWeeksRepository(Firebase.firestore, Firebase.auth),
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
      val content = repository.getWeekContent(selected)
      _uiState.update {
        it.copy(
            isLoading = false,
            weeks = weeks,
            dayStatuses = statuses,
            selectedWeekIndex = selected,
            weekProgressPercent = headerPct,
            currentWeekContent = content,
        )
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
    // Also refresh content for selection
    val idx = selectedIndex ?: uiState.value.selectedWeekIndex
    loadWeekContent(idx.coerceIn(0, weeks.lastIndex.coerceAtLeast(0)))
  }

  fun updateWeekPercent(index: Int, percent: Int) {
    // Delegate to repository, then sync UI state
    viewModelScope.launch {
      val updatedWeeks = repository.updateWeekPercent(index, percent)
      val sel = uiState.value.selectedWeekIndex.coerceIn(0, updatedWeeks.lastIndex.coerceAtLeast(0))
      val header = updatedWeeks.getOrNull(sel)?.percent ?: uiState.value.weekProgressPercent
      val content = repository.getWeekContent(sel)
      _uiState.update { cur ->
        cur.copy(weeks = updatedWeeks, weekProgressPercent = header, currentWeekContent = content)
      }
    }
  }

  fun selectWeek(index: Int) {
    val safeIndex = index.coerceIn(0, uiState.value.weeks.lastIndex.coerceAtLeast(0))
    _uiState.update { current ->
      val newPct = current.weeks.getOrNull(safeIndex)?.percent ?: current.weekProgressPercent
      current.copy(selectedWeekIndex = safeIndex, weekProgressPercent = newPct)
    }
    // Load content for the selected week
    loadWeekContent(safeIndex)
  }

  fun selectNextWeek() {
    val next =
        (uiState.value.selectedWeekIndex + 1).coerceAtMost(
            uiState.value.weeks.lastIndex.coerceAtLeast(0))
    selectWeek(next)
  }

  fun selectPreviousWeek() {
    val prev = (uiState.value.selectedWeekIndex - 1).coerceAtLeast(0)
    selectWeek(prev)
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

  // ---- Content helpers ----
  private fun loadWeekContent(index: Int) {
    viewModelScope.launch {
      val content = repository.getWeekContent(index)
      _uiState.update { it.copy(currentWeekContent = content) }
    }
  }

  fun markExerciseDone(exerciseId: String, done: Boolean) {
    val idx = uiState.value.selectedWeekIndex
    viewModelScope.launch {
      val updatedWeeks = repository.markExerciseDone(idx, exerciseId, done)
      val header = updatedWeeks.getOrNull(idx)?.percent ?: uiState.value.weekProgressPercent
      val content = repository.getWeekContent(idx)
      _uiState.update {
        it.copy(weeks = updatedWeeks, weekProgressPercent = header, currentWeekContent = content)
      }
    }
  }

  fun markCourseRead(courseId: String, read: Boolean) {
    val idx = uiState.value.selectedWeekIndex
    viewModelScope.launch {
      val updatedWeeks = repository.markCourseRead(idx, courseId, read)
      val header = updatedWeeks.getOrNull(idx)?.percent ?: uiState.value.weekProgressPercent
      val content = repository.getWeekContent(idx)
      _uiState.update {
        it.copy(weeks = updatedWeeks, weekProgressPercent = header, currentWeekContent = content)
      }
    }
  }
}
