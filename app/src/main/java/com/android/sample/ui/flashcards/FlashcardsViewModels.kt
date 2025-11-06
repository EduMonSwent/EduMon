package com.android.sample.ui.flashcards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.sample.data.Priority
import com.android.sample.data.Status
import com.android.sample.data.ToDo
import com.android.sample.repos_providors.AppRepositories
import com.android.sample.repositories.ToDoRepository
import com.android.sample.ui.flashcards.data.InMemoryFlashcardsRepository
import com.android.sample.ui.flashcards.model.*
import java.time.LocalDate
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/** ViewModel that exposes the list of decks from the repository. */
class DeckListViewModel : ViewModel() {
  val decks = InMemoryFlashcardsRepository.decks
}

/** ViewModel for creating a new deck. */
class CreateDeckViewModel(private val toDoRepo: ToDoRepository = AppRepositories.toDoRepository) :
    ViewModel() {
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
            InMemoryFlashcardsRepository.createDeck(
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
data class StudyState(val deck: Deck, val index: Int = 0, val showingAnswer: Boolean = false) {
  val total: Int
    get() = deck.cards.size

  val current: Flashcard
    get() = deck.cards[index]

  val isFirst: Boolean
    get() = index == 0

  val isLast: Boolean
    get() = index == total - 1
}
/** ViewModel that drives the study flow (flip/next/prev/record). */
class StudyViewModel(private val deckId: String) : ViewModel() {
  private val deck = requireNotNull(InMemoryFlashcardsRepository.deck(deckId)) { "Deck not found" }
  private val _state = MutableStateFlow(StudyState(deck))
  val state: StateFlow<StudyState> = _state

  fun flip() {
    _state.update { it.copy(showingAnswer = !it.showingAnswer) }
  }

  fun next() {
    _state.update { s -> if (!s.isLast) s.copy(index = s.index + 1, showingAnswer = false) else s }
  }

  fun prev() {
    _state.update { s -> if (!s.isFirst) s.copy(index = s.index - 1, showingAnswer = false) else s }
  }

  fun record(confidence: Confidence) {
    // Here you could persist spaced-repetition data; we just advance.
    next()
  }
}
