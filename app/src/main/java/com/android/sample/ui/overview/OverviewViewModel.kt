package com.android.sample.ui.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.sample.model.todo.ToDo
import com.android.sample.model.todo.ToDosRepository
import com.android.sample.model.todo.ToDosRepositoryProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Represents the UI state for the Overview screen.
 *
 * @property todos A list of `ToDo` items to be displayed in the Overview screen. Defaults to an
 *   empty list if no items are available.
 */
data class OverviewUIState(
    val todos: List<ToDo> = emptyList(),
)

/**
 * ViewModel for the Overview screen.
 *
 * Responsible for managing the UI state, by fetching and providing ToDo items via the
 * [ToDosRepository].
 *
 * @property todoRepository The repository used to fetch and manage ToDo items.
 */
class OverviewViewModel(
    private val todoRepository: ToDosRepository = ToDosRepositoryProvider.repository,
) : ViewModel() {

  private val _uiState = MutableStateFlow(OverviewUIState())
  val uiState: StateFlow<OverviewUIState> = _uiState.asStateFlow()

  init {
    getAllTodos()
  }

  /** Refreshes the UI state by fetching all ToDo items from the repository. */
  fun refreshUIState() {
    getAllTodos()
  }

  /** Fetches all todos from the repository and updates the UI state. */
  private fun getAllTodos() {
    viewModelScope.launch {
      val todos = todoRepository.getAllTodos()
      _uiState.value = OverviewUIState(todos = todos)
    }
  }
}
