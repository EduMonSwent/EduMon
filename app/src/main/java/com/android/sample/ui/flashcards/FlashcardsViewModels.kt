// app/src/main/java/com/android/sample/ui/flashcards/FlashcardsModule.kt
@file:Suppress("MemberVisibilityCanBePrivate")

package com.android.sample.ui.flashcards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.sample.data.Priority
import com.android.sample.data.Status
import com.android.sample.data.ToDo
import com.android.sample.repositories.ToDoRepository
import com.android.sample.repositories.ToDoRepositoryProvider
import com.android.sample.ui.flashcards.data.FirestoreFlashcardsRepoProvider
import com.android.sample.ui.flashcards.data.FlashcardsRepository
import com.android.sample.ui.flashcards.model.Confidence
import com.android.sample.ui.flashcards.model.Deck
import com.android.sample.ui.flashcards.model.Flashcard
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Ensures FirebaseAuth has a user (anonymous) when required. Use requireAuth=false in tests to skip
 * hitting Firebase.
 */
object FlashcardsAuth {
  suspend fun ensureSignedInIfRequired(
      requireAuth: Boolean = true,
      auth: FirebaseAuth = Firebase.auth
  ) {
    if (!requireAuth) return
    if (auth.currentUser == null) {
      auth.signInAnonymously().await()
    }
  }
}

/** ViewModel that exposes the list of decks from the repository. */
class DeckListViewModel(
    private val repo: FlashcardsRepository = FirestoreFlashcardsRepoProvider.get(),
    private val requireAuth: Boolean = true
) : ViewModel() {

  constructor(repo: FlashcardsRepository) : this(repo, requireAuth = false)

  // Use WhileSubscribed to keep the flow alive during navigation
  val decks: StateFlow<List<Deck>> =
      flow {
            FlashcardsAuth.ensureSignedInIfRequired(requireAuth)
            emitAll(repo.observeDecks())
          }
          .stateIn(
              scope = viewModelScope, started = SharingStarted.Lazily, initialValue = emptyList())

  fun deleteDeck(id: String) =
      viewModelScope.launch {
        FlashcardsAuth.ensureSignedInIfRequired(requireAuth)
        repo.deleteDeck(id)
      }

  fun toggleShareable(deckId: String, enabled: Boolean) =
      viewModelScope.launch {
        FlashcardsAuth.ensureSignedInIfRequired(requireAuth)
        repo.setDeckShareable(deckId, enabled)
      }

  // NOSONAR – Exposing a suspend function is intentional for coroutine-based UI.
  suspend fun createShareToken(deckId: String): String {
    FlashcardsAuth.ensureSignedInIfRequired(requireAuth)
    return repo.createShareToken(deckId)
  }
}

/** ViewModel for creating a new deck. */
class CreateDeckViewModel(
    private val toDoRepo: ToDoRepository = ToDoRepositoryProvider.repository,
    private val repo: FlashcardsRepository = FirestoreFlashcardsRepoProvider.get(),
    private val requireAuth: Boolean = true
) : ViewModel() {

  constructor(
      toDoRepo: ToDoRepository,
      repo: FlashcardsRepository
  ) : this(toDoRepo, repo, requireAuth = false)

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
        FlashcardsAuth.ensureSignedInIfRequired(requireAuth)

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
    private val repo: FlashcardsRepository = FirestoreFlashcardsRepoProvider.get(),
    private val requireAuth: Boolean = true
) : ViewModel() {
  constructor(deckId: String, repo: FlashcardsRepository) : this(deckId, repo, requireAuth = false)

  private val _state = MutableStateFlow(StudyState())
  val state: StateFlow<StudyState> = _state

  init {
    viewModelScope.launch {
      try {
        FlashcardsAuth.ensureSignedInIfRequired(requireAuth)
        repo.observeDeck(deckId).collect { deck ->
          _state.value =
              if (deck == null) {
                StudyState(error = "Deck not found")
              } else {
                StudyState(deck = deck, index = 0, showingAnswer = false)
              }
        }
      } catch (t: Throwable) {
        _state.value = StudyState(error = "Failed to load deck: ${t.message}")
      }
    }
  }

  fun flip() = _state.update { it.copy(showingAnswer = !it.showingAnswer) }

  fun next() =
      _state.update { s ->
        if (s.total == 0) s
        else s.copy(index = (s.index + 1).coerceAtMost(s.total - 1), showingAnswer = false)
      }

  fun prev() =
      _state.update { s ->
        if (s.total == 0) s
        else s.copy(index = (s.index - 1).coerceAtLeast(0), showingAnswer = false)
      }

  fun record(confidence: Confidence) {
    if (!state.value.showingAnswer) return
    next() // TODO: plug in SRS grading later
  }
}
