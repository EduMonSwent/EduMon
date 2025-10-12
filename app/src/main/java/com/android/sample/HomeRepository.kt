package com.android.sample

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.sample.model.CreatureStats
import com.android.sample.model.Priority
import com.android.sample.model.ToDo
import com.android.sample.model.UserStats
import java.time.LocalDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ---------- Repository ----------
interface HomeRepository {
  suspend fun fetchTodos(): List<ToDo>

  suspend fun fetchCreatureStats(): CreatureStats

  suspend fun fetchUserStats(): UserStats

  fun dailyQuote(nowMillis: Long = System.currentTimeMillis()): String
}

class FakeHomeRepository : HomeRepository {
  private val sampleTodos =
      listOf(
          ToDo(
              title = "CS-101: Finish exercise sheet",
              dueDate = LocalDate.now(),
              priority = Priority.HIGH,
              location = "Library – 2nd floor",
              links = listOf("https://university.example/cs101/sheet-5"),
              notificationsEnabled = true),
          ToDo(
              title = "Math review: sequences",
              dueDate = LocalDate.now(),
              priority = Priority.MEDIUM,
              note = "Focus on convergence tests"),
          ToDo(
              title = "Pack lab kit for tomorrow",
              dueDate = LocalDate.now().plusDays(1),
              priority = Priority.LOW),
      )

  private val quotes =
      listOf(
          "Small consistent steps beat intense sprints.",
          "Study now, thank yourself later.",
          "Progress over perfection, always.",
          "You don't have to do it fast — just do it.",
          "Your future self is watching. Keep going.",
      )

  override suspend fun fetchTodos(): List<ToDo> {
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
    val todos: List<ToDo> = emptyList(),
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
