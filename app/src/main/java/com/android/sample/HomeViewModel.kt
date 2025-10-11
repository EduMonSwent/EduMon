package com.android.sample

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ---------- Models ----------
data class Todo(
    val id: String,
    val title: String,
    val due: String,
    val done: Boolean = false,
)

data class CreatureStats(
    val happiness: Int = 85,
    val health: Int = 90,
    val energy: Int = 70,
    val level: Int = 5,
)

data class UserStats(
    val streakDays: Int = 7,
    val points: Int = 1250,
    val studyTodayMin: Int = 45,
    val dailyGoalMin: Int = 180,
)

// ---------- Repository ----------
interface HomeRepository {
  suspend fun fetchTodos(): List<Todo>

  suspend fun fetchCreatureStats(): CreatureStats

  suspend fun fetchUserStats(): UserStats

  fun dailyQuote(nowMillis: Long = System.currentTimeMillis()): String
}

class FakeHomeRepository : HomeRepository {
  private val sampleTodos =
      listOf(
          Todo("1", "CS-101: Finish exercise sheet", "Today 18:00", true),
          Todo("2", "Math review: sequences", "Today 20:00"),
          Todo("3", "Pack lab kit for tomorrow", "Tomorrow"),
      )

  private val quotes =
      listOf(
          "Small consistent steps beat intense sprints.",
          "Study now, thank yourself later.",
          "Progress over perfection, always.",
          "You don't have to do it fast — just do it.",
          "Your future self is watching. Keep going.",
      )

  override suspend fun fetchTodos(): List<Todo> {
    delay(100) // simulate I/O
    return sampleTodos
  }

  override suspend fun fetchCreatureStats(): CreatureStats {
    delay(50)
    return CreatureStats()
  }

  override suspend fun fetchUserStats(): UserStats {
    delay(50)
    return UserStats()
  }

  override fun dailyQuote(nowMillis: Long): String {
    val idx = ((nowMillis / 86_400_000L) % quotes.size).toInt()
    return quotes[idx]
  }
}

// ---------- UI State ----------
data class HomeUiState(
    val isLoading: Boolean = true,
    val todos: List<Todo> = emptyList(),
    val creatureStats: CreatureStats = CreatureStats(),
    val userStats: UserStats = UserStats(),
    val quote: String = "",
)

// ---------- ViewModel ----------
class HomeViewModel(
    private val repository: HomeRepository = FakeHomeRepository(), // swap for DI/Hilt
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
