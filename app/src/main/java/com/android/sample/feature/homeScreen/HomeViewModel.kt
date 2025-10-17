package com.android.sample.feature.homeScreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.sample.data.CreatureStats
import com.android.sample.data.ToDo
import com.android.sample.data.UserStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ---------- UI State ----------
data class HomeUiState(
    val isLoading: Boolean = true,
    val todos: List<ToDo> = emptyList(),
    val creatureStats: CreatureStats = CreatureStats(),
    val userStats: UserStats = UserStats(),
    val quote: String = "",
)

// ---------- ViewModel ----------
class HomeViewModel(
    private val repository: HomeRepository = FakeHomeRepository(), // swap pour DI/Hilt
) : ViewModel() {

  private val _uiState = MutableStateFlow(HomeUiState())
  val uiState: StateFlow<HomeUiState> = _uiState

  init {
    refresh()
  }

  fun refresh() {
    _uiState.update { it.copy(isLoading = true) }
    viewModelScope.launch {
      val todos = repository.fetchTodos()
      val creature = repository.fetchCreatureStats()
      val user = repository.fetchUserStats()
      val quote = repository.dailyQuote()

      _uiState.update {
        it.copy(
            isLoading = false,
            todos = todos,
            creatureStats = creature,
            userStats = user,
            quote = quote)
      }
    }
  }
}
