package com.android.sample.ui.viewmodel

import androidx.lifecycle.ViewModel
import java.time.DayOfWeek
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class WeekDotsUiState(
    val dayStatuses: List<DayStatus> = emptyList(),
)

class WeekDotsViewModel : ViewModel() {
  private val _uiState = MutableStateFlow(WeekDotsUiState())
  val uiState = _uiState.asStateFlow()

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
