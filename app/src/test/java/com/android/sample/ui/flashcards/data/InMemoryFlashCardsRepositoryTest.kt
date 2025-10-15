package com.android.sample.ui.flashcards.data

import com.android.sample.ui.flashcards.model.Deck
import com.android.sample.ui.flashcards.model.Flashcard
import java.lang.reflect.Field
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.*
import org.junit.Test

class InMemoryFlashcardsRepositoryTest {

  /**
   * Because InMemoryFlashcardsRepository is a singleton (`object`), its state persists between
   * tests. This method resets the repository after each test to ensure they don't interfere with
   * each other.
   */
  @After
  fun tearDown() {
    // Use reflection to access the private '_decks' field and reset it.
    val decksField: Field = InMemoryFlashcardsRepository::class.java.getDeclaredField("_decks")
    decksField.isAccessible = true
    decksField.set(InMemoryFlashcardsRepository, MutableStateFlow<List<Deck>>(emptyList()))
  }

  @Test
  fun `deck returns correct deck when ID exists`() {
    // Arrange
    val deckId = InMemoryFlashcardsRepository.createDeck("Science", "Biology", emptyList())

    // Act
    val foundDeck = InMemoryFlashcardsRepository.deck(deckId)

    // Assert
    assertNotNull(foundDeck)
    assertEquals(deckId, foundDeck!!.id)
    assertEquals("Science", foundDeck.title)
  }

  @Test
  fun `deck returns null when ID does not exist`() {
    // Arrange
    InMemoryFlashcardsRepository.createDeck("History", "WW2", emptyList())

    // Act
    val foundDeck = InMemoryFlashcardsRepository.deck("non-existent-id")

    // Assert
    assertNull(foundDeck)
  }

  @Test
  fun `addCard successfully adds a card to the specified deck`() {
    // Arrange
    val initialCard = Flashcard(id = "card-101", question = "Q1", answer = "A1")
    val deckId = InMemoryFlashcardsRepository.createDeck("Math", "Algebra", listOf(initialCard))

    // Act
    val newCard = Flashcard(id = "card-102", question = "Q2", answer = "A2")
    InMemoryFlashcardsRepository.addCard(deckId, newCard)
    val updatedDeck = InMemoryFlashcardsRepository.deck(deckId)

    // Assert
    assertNotNull(updatedDeck)
    assertEquals(2, updatedDeck!!.cards.size) // Check size is now 2
    // Data class equality will check all fields (id, question, answer)
    assertEquals(initialCard, updatedDeck.cards[0])
    assertEquals(newCard, updatedDeck.cards[1])
  }

  @Test
  fun `addCard does not change anything if deck ID is invalid`() {
    // Arrange
    val deckId = InMemoryFlashcardsRepository.createDeck("Geography", "Countries", emptyList())
    val initialDecksState = InMemoryFlashcardsRepository.decks.value.toList()

    // Act
    InMemoryFlashcardsRepository.addCard(
        "invalid-id", Flashcard(id = "card-temp", question = "Question", answer = "Answer"))
    val finalDecksState = InMemoryFlashcardsRepository.decks.value.toList()

    // Assert
    // The list of decks and their contents should be identical
    assertEquals(initialDecksState, finalDecksState)
    assertTrue(InMemoryFlashcardsRepository.deck(deckId)!!.cards.isEmpty())
  }
}
