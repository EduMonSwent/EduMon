package com.android.sample.ui.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// Holds only objective-related UI state
data class ObjectivesUiState(
    val objectives: List<Objective> = emptyList(),
    val showWhy: Boolean = true,
)

class ObjectivesViewModel : ViewModel() {
  private val _uiState = MutableStateFlow(ObjectivesUiState())
  val uiState = _uiState.asStateFlow()

  // ---- Mutations ----
  fun setObjectives(objs: List<Objective>) {
    _uiState.update { it.copy(objectives = objs) }
  }

  fun addObjective(obj: Objective) {
    _uiState.update { it.copy(objectives = it.objectives + obj) }
  }

  fun updateObjective(index: Int, obj: Objective) {
    _uiState.update { cur ->
      if (index !in cur.objectives.indices) return@update cur
      val list = cur.objectives.toMutableList()
      list[index] = obj
      cur.copy(objectives = list)
    }
  }

  fun removeObjective(index: Int) {
    _uiState.update { cur ->
      if (index !in cur.objectives.indices) return@update cur
      cur.copy(objectives = cur.objectives.filterIndexed { i, _ -> i != index })
    }
  }

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

  fun setShowWhy(show: Boolean) {
    _uiState.update { it.copy(showWhy = show) }
  }

  fun startObjective(index: Int = 0) {
    // hook for analytics / navigation later
  }
}
