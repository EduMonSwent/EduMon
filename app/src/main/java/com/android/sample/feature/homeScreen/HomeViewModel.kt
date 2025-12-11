package com.android.sample.feature.homeScreen

// This code has been written partially using A.I (LLM).

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.sample.data.ToDo
import com.android.sample.data.UserStats
import com.android.sample.data.UserStatsRepository
import com.android.sample.repos_providors.AppRepositories
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ---------- UI State ----------
data class HomeUiState(
    val isLoading: Boolean = true,
    val todos: List<ToDo> = emptyList(),
    // creatureStats removed as requested
    val userStats: UserStats = UserStats(),
    val quote: String = "",
    val userLevel: Int = 1,
)

// ---------- ViewModel ----------
class HomeViewModel(
    private val repository: HomeRepository = AppRepositories.homeRepository,
    private val userStatsRepository: UserStatsRepository = AppRepositories.userStatsRepository,
) : ViewModel() {

  private val _uiState = MutableStateFlow(HomeUiState())
  val uiState: StateFlow<HomeUiState> = _uiState

  init {
    // Start unified stats listener
    viewModelScope.launch { userStatsRepository.start() }

    // Keep Home screen in sync with unified stats
    viewModelScope.launch {
      userStatsRepository.stats.collect { stats -> _uiState.update { it.copy(userStats = stats) } }
    }

    refresh()
  }

  fun refresh() {
    _uiState.update { it.copy(isLoading = true) }
    viewModelScope.launch {
      val todos = repository.fetchTodos()
      // creatureStats fetch removed
      val quote = repository.dailyQuote()
      val userProfile = repository.fetchUserStats()

      _uiState.update {
        it.copy(isLoading = false, todos = todos, quote = quote, userLevel = userProfile.level)
      }
    }
  }
}
