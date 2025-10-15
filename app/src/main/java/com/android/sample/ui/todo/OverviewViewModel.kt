package com.android.sample.ui.todo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.sample.todo.Status
import com.android.sample.todo.ToDo
import com.android.sample.todo.ToDoRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// UI model for the Overview screen
data class OverviewUiState(val items: List<ToDo> = emptyList())

/**
 * ViewModel for the Overview screen.
 * - Observes repository Flow and exposes a StateFlow<OverviewUiState> to the UI
 * - Sorts items so unfinished come first, then by earliest due date
 * - Handles user actions: cycle status, delete
 */
class OverviewViewModel(private val repo: ToDoRepository) : ViewModel() {

  // Convert repo.todos (Flow<List<ToDo>>) -> StateFlow<OverviewUiState> for Compose
  val uiState: StateFlow<OverviewUiState> =
      repo.todos
          .map { list ->
            // Sort: items with DONE last; among the rest, nearest due date first
            val sorted =
                list.sortedWith(
                    compareBy<ToDo> { it.status == Status.DONE } // false < true → non-DONE first
                        .thenBy { it.dueDate } // earlier dates first
                    )
            OverviewUiState(sorted) // wrap in a UI state object
          }
          .stateIn(
              scope = viewModelScope, // lifecycle-aware scope
              started =
                  SharingStarted.WhileSubscribed(5_000), // keep active briefly with no collectors
              initialValue = OverviewUiState() // initial UI state (empty list)
              )

  // Cycle status in order: TODO → IN_PROGRESS → DONE → TODO
  fun cycleStatus(id: String) =
      viewModelScope.launch {
        repo.getById(id)?.let {
          val next =
              when (it.status) {
                Status.TODO -> Status.IN_PROGRESS
                Status.IN_PROGRESS -> Status.DONE
                Status.DONE -> Status.TODO
              }
          repo.update(it.copy(status = next))
        }
      }

  // Delete an item by id
  fun delete(id: String) = viewModelScope.launch { repo.remove(id) }
}
