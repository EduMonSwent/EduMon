package com.android.sample.ui.flashcards

import com.android.sample.ui.flashcards.data.InMemoryFlashcardsRepository
import com.android.sample.ui.flashcards.model.Flashcard
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FlashcardsViewModelsTest {

  private val mainDispatcher = StandardTestDispatcher()

  @Before
  fun setUp() {
    // ViewModel uses viewModelScope (Main dispatcher); bind a test dispatcher.
    kotlinx.coroutines.Dispatchers.setMain(mainDispatcher)
  }

  @After
  fun tearDown() {
    kotlinx.coroutines.Dispatchers.resetMain()
  }

  @Test
  fun deckListViewModel_exposesDecks_andReflectsRepoChanges() = runTest {
    val vm = DeckListViewModel()

    val initial = vm.decks.first()
    val initialCount = initial.size

    val newId =
        InMemoryFlashcardsRepository.createDeck(
            title = "DeckListVM Deck",
            description = "desc",
            cards = listOf(Flashcard(question = "Q", answer = "A")))

    val after = vm.decks.first()
    assertEquals(initialCount + 1, after.size)
    assertTrue(after.any { it.id == newId && it.title == "DeckListVM Deck" })
  }

  @Test
  fun createDeckViewModel_fullFlow_addUpdateRemove_save_addsDeck() = runTest {
    val vm = CreateDeckViewModel()

    // initial state flows
    assertEquals("", vm.title.value)
    assertEquals("", vm.description.value)
    assertTrue(vm.cards.value.isEmpty())

    // set title/desc
    vm.setTitle("Algebra")
    vm.setDescription("Basics")
    assertEquals("Algebra", vm.title.value)
    assertEquals("Basics", vm.description.value)

    // add two empties
    vm.addEmptyCard()
    vm.addEmptyCard()
    assertEquals(2, vm.cards.value.size)

    // update both, then remove one
    vm.updateCard(index = 0, question = "What is a vector?", answer = "Magnitude and direction.")
    vm.updateCard(
        index = 1, question = "Matrix determinant?", answer = "Volume scale / invertibility.")
    assertEquals("What is a vector?", vm.cards.value[0].question)
    assertEquals("Matrix determinant?", vm.cards.value[1].question)

    vm.removeCard(1)
    assertEquals(1, vm.cards.value.size)

    // save -> repo should get a new deck; wait for coroutine to finish
    var savedId: String? = null
    vm.save { id -> savedId = id }
    advanceUntilIdle()

    assertFalse(savedId.isNullOrBlank())

    val decks = InMemoryFlashcardsRepository.decks.first()
    val saved = decks.first { it.id == savedId }
    assertEquals("Algebra", saved.title)
    assertEquals("Basics", saved.description)
    assertEquals(1, saved.cards.size)
    assertEquals("What is a vector?", saved.cards.first().question)
    assertEquals("Magnitude and direction.", saved.cards.first().answer)
  }

  @Test
  fun studyViewModel_flip_next_prev_record_updatesState() =
      runTest(UnconfinedTestDispatcher()) {
        // Seed a deck we control
        val deckId =
            InMemoryFlashcardsRepository.createDeck(
                title = "Study Deck",
                description = "d",
                cards =
                    listOf(
                        Flashcard(question = "Q1", answer = "A1"),
                        Flashcard(question = "Q2", answer = "A2"),
                        Flashcard(question = "Q3", answer = "A3"),
                    ))

        val vm = StudyViewModel(deckId)

        // initial state (covers StudyState getters too)
        var s = vm.state.value
        assertEquals(3, s.total)
        assertEquals("Q1", s.current.question)
        assertTrue(s.isFirst)
        assertFalse(s.isLast)
        assertFalse(s.showingAnswer)

        // flip
        vm.flip()
        s = vm.state.value
        assertTrue(s.showingAnswer)

        // record(…) advances to next
        vm.record(com.android.sample.ui.flashcards.model.Confidence.MEDIUM)
        s = vm.state.value
        assertEquals(1, s.index)
        assertEquals("Q2", s.current.question)
        assertFalse(s.showingAnswer)

        // next to last
        vm.next()
        s = vm.state.value
        assertEquals(2, s.index)
        assertTrue(s.isLast)

        // next at end: stays at last
        vm.next()
        s = vm.state.value
        assertEquals(2, s.index)

        // prev back to middle
        vm.prev()
        s = vm.state.value
        assertEquals(1, s.index)
        assertFalse(s.isFirst)
        assertFalse(s.isLast)
      }
}
