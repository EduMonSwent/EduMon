package com.android.sample.feature.homeScreen

// This code has been written partially using A.I (LLM).

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.sample.data.CreatureStats
import com.android.sample.data.ToDo
import com.android.sample.data.UserStats
import com.android.sample.data.UserStatsRepository
import com.android.sample.feature.weeks.model.Objective
import com.android.sample.repos_providors.AppRepositories
import com.android.sample.repositories.ToDoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ---------- UI State ----------
data class HomeUiState(
    val isLoading: Boolean = true,
    val todos: List<ToDo> = emptyList(),
    val objectives: List<Objective> = emptyList(),
    val creatureStats: CreatureStats = CreatureStats(),
    val userStats: UserStats = UserStats(),
    val quote: String = "",
)

// ---------- ViewModel ----------
class HomeViewModel(
    private val repository: HomeRepository = AppRepositories.homeRepository,
    private val userStatsRepository: UserStatsRepository = AppRepositories.userStatsRepository,
    private val toDoRepository: ToDoRepository = AppRepositories.toDoRepository, // ✅ shared repo
) : ViewModel() {

  private val _uiState = MutableStateFlow(HomeUiState())
  val uiState: StateFlow<HomeUiState> = _uiState

  init {
    initializeStatsListener()
    refresh()
  }

  private fun initializeStatsListener() {
    // Start unified stats listener
    viewModelScope.launch {
      try {
        userStatsRepository.start()
      } catch (e: Exception) {
        // User might not be logged in yet, will retry on refresh
      }
    }

    // Keep Home screen in sync with unified stats
    viewModelScope.launch {
      userStatsRepository.stats.collect { stats -> _uiState.update { it.copy(userStats = stats) } }
    }

    // ✅ Keep Home screen in sync with REAL todos (same repo as Todo screen)
    viewModelScope.launch {
      // Assumes your ToDoRepository exposes a Flow/StateFlow called "todos"
      toDoRepository.todos.collect { todos -> _uiState.update { it.copy(todos = todos) } }
    }

    refresh()
  }

  fun refresh() {
    _uiState.update { it.copy(isLoading = true) }
    viewModelScope.launch {
      // Ensure stats listener is running for current user
      try {
        userStatsRepository.start()
      } catch (e: Exception) {
        // Handle case where user is not logged in
      }

      val todos = repository.fetchTodos()
      val objectives = repository.fetchObjectives()
      val creature = repository.fetchCreatureStats()
      val quote = repository.dailyQuote()

      _uiState.update {
        it.copy(
            isLoading = false,
            creatureStats = creature,
            quote = quote,
            objectives = objectives,
        )
      }
    }
  }
}
