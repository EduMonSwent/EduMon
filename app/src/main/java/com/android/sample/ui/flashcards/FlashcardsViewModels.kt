package com.android.sample.ui.flashcards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.sample.data.Priority
import com.android.sample.data.Status
import com.android.sample.data.ToDo
import com.android.sample.repositories.ToDoRepository
import com.android.sample.repositories.ToDoRepositoryProvider
import com.android.sample.ui.flashcards.data.FlashcardsRepository
import com.android.sample.ui.flashcards.data.FlashcardsRepositoryProvider
import com.android.sample.ui.flashcards.model.*
import java.time.LocalDate
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/** ViewModel that exposes the list of decks from the repository. */
class DeckListViewModel(
    private val repo: FlashcardsRepository = FlashcardsRepositoryProvider.repository
) : ViewModel() {
  val decks = repo.observeDecks()

  fun deleteDeck(id: String) = viewModelScope.launch { repo.deleteDeck(id) }
}

/** ViewModel for creating a new deck. */
class CreateDeckViewModel(
    private val toDoRepo: ToDoRepository = ToDoRepositoryProvider.repository,
    private val repo: FlashcardsRepository = FlashcardsRepositoryProvider.repository
) : ViewModel() {

  private val _title = MutableStateFlow("")
  private val _description = MutableStateFlow("")
  private val _cards = MutableStateFlow<List<Flashcard>>(emptyList())

  val title = _title.asStateFlow()
  val description = _description.asStateFlow()
  val cards = _cards.asStateFlow()

  fun setTitle(t: String) {
    _title.value = t
  }

  fun setDescription(d: String) {
    _description.value = d
  }

  fun addEmptyCard() {
    _cards.value = _cards.value + Flashcard(question = "", answer = "")
  }

  fun updateCard(index: Int, question: String? = null, answer: String? = null) {
    _cards.value =
        _cards.value.mapIndexed { i, c ->
          if (i == index) c.copy(question = question ?: c.question, answer = answer ?: c.answer)
          else c
        }
  }

  fun removeCard(index: Int) {
    _cards.value = _cards.value.toMutableList().also { if (index in it.indices) it.removeAt(index) }
  }

  fun save(onSaved: (String) -> Unit) =
      viewModelScope.launch {
        val id =
            repo.createDeck(
                title = _title.value.trim(),
                description = _description.value.trim(),
                cards = _cards.value.filter { it.question.isNotBlank() && it.answer.isNotBlank() })

        val deckTitle = _title.value.trim().ifBlank { "New deck" }
        toDoRepo.add(
            ToDo(
                title = "Study: $deckTitle",
                dueDate = LocalDate.now(),
                priority = Priority.MEDIUM,
                status = Status.TODO,
                links = listOf("flashcard://deck/$id"),
                note = _description.value.takeIf { it.isNotBlank() }))
        onSaved(id)
      }
}
/**
 * Immutable UI state for studying a deck. Tracks current index and whether the answer is visible.
 */
data class StudyState(
    val deck: Deck? = null,
    val index: Int = 0,
    val showingAnswer: Boolean = false,
    val error: String? = null
) {
  val total: Int
    get() = deck?.cards?.size ?: 0

  val isFirst: Boolean
    get() = index <= 0

  val isLast: Boolean
    get() = total == 0 || index >= total - 1

  val currentOrNull: Flashcard?
    get() = if (total == 0 || index !in 0 until total) null else deck!!.cards[index]
}

class StudyViewModel(
    private val deckId: String,
    private val repo: FlashcardsRepository = FlashcardsRepositoryProvider.repository
) : ViewModel() {

  private val _state = MutableStateFlow(StudyState())
  val state: StateFlow<StudyState> = _state

  init {
    viewModelScope.launch {
      runCatching {
            repo.observeDeck(deckId).collect { deck ->
              if (deck == null) {
                _state.value = StudyState(error = "Deck not found")
              } else {
                _state.value = StudyState(deck = deck, index = 0, showingAnswer = false)
              }
            }
          }
          .onFailure { e -> _state.value = StudyState(error = "Failed to load deck: ${e.message}") }
    }
  }

  fun flip() = _state.update { it.copy(showingAnswer = !it.showingAnswer) }

  fun next() =
      _state.update { s ->
        val total = s.total
        if (total == 0) s
        else {
          val newIndex = (s.index + 1).coerceAtMost(total - 1)
          s.copy(index = newIndex, showingAnswer = false)
        }
      }

  fun prev() =
      _state.update { s ->
        val total = s.total
        if (total == 0) s
        else {
          val newIndex = (s.index - 1).coerceAtLeast(0)
          s.copy(index = newIndex, showingAnswer = false)
        }
      }

  // StudyViewModel.kt
  fun record(confidence: Confidence) {
    // Don’t allow answering unless the answer is visible
    val canGrade = state.value.showingAnswer
    if (!canGrade) return

    // TODO: apply SRS here later
    next()
  }
}
