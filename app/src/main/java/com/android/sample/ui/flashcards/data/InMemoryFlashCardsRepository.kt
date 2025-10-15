package com.android.sample.ui.flashcards.data

import com.android.sample.ui.flashcards.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** In-memory repository for managing flashcard decks. */
object InMemoryFlashcardsRepository {
  private var _decks = MutableStateFlow<List<Deck>>(emptyList())
  val decks: StateFlow<List<Deck>> = _decks

  fun createDeck(title: String, description: String, cards: List<Flashcard>): String {
    val deck = Deck(title = title, description = description, cards = cards.toMutableList())
    _decks.value = _decks.value + deck
    return deck.id
  }

  fun deck(deckId: String): Deck? = _decks.value.find { it.id == deckId }

  fun addCard(deckId: String, card: Flashcard) {
    _decks.value =
        _decks.value.map {
          if (it.id == deckId) it.copy(cards = (it.cards + card).toMutableList()) else it
        }
  }
}
