package com.android.sample.feature.weeks.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.sample.feature.weeks.model.Objective
import com.android.sample.feature.weeks.repository.FakeObjectivesRepository
import com.android.sample.feature.weeks.repository.ObjectivesRepository
import java.time.DayOfWeek
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Holds only objective-related UI state
data class ObjectivesUiState(
    val objectives: List<Objective> = emptyList(),
    val showWhy: Boolean = true,
)

class ObjectivesViewModel(
    private val repository: ObjectivesRepository = FakeObjectivesRepository,
) : ViewModel() {
  private val _uiState = MutableStateFlow(ObjectivesUiState())
  val uiState = _uiState.asStateFlow()

  // Removed auto-refresh to avoid races in tests; call refresh() from the screen if needed.
  init {
      refresh()
  }
  // Loads from repository
  fun refresh() {
    viewModelScope.launch {
      val objs = repository.getObjectives()
      _uiState.update { it.copy(objectives = objs) }
    }
  }

  // --- Query API for WeekDots ---
  fun isObjectivesOfDayCompleted(day: DayOfWeek): Boolean {
    val dayList = _uiState.value.objectives.filter { it.day == day }
    return dayList.isNotEmpty() && dayList.all { it.completed }
  }

  // ---- Mutations ----
  fun setObjectives(objs: List<Objective>) {
    _uiState.update { it.copy(objectives = objs) }
  }

  fun addObjective(obj: Objective) {
    viewModelScope.launch {
      val list = repository.addObjective(obj)
      _uiState.update { it.copy(objectives = list) }
    }
  }

  fun updateObjective(index: Int, obj: Objective) {
    viewModelScope.launch {
      val list = repository.updateObjective(index, obj)
      _uiState.update { it.copy(objectives = list) }
    }
  }

  fun removeObjective(index: Int) {
    viewModelScope.launch {
      val list = repository.removeObjective(index)
      _uiState.update { it.copy(objectives = list) }
    }
  }

  fun moveObjective(fromIndex: Int, toIndex: Int) {
    viewModelScope.launch {
      val list = repository.moveObjective(fromIndex, toIndex)
      _uiState.update { it.copy(objectives = list) }
    }
  }

  fun setShowWhy(show: Boolean) {
    _uiState.update { it.copy(showWhy = show) }
  }

  @Suppress("UNUSED_PARAMETER")
  fun startObjective(index: Int = 0) {
    // hook for analytics / navigation later
  }
}
