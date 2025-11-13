package com.android.sample.ui.flashcards.data

import com.android.sample.ui.flashcards.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

/** In-memory repository for managing flashcard decks. */
object InMemoryFlashcardsRepository : FlashcardsRepository {
  private var _decks = MutableStateFlow<List<Deck>>(emptyList())
  val decks: StateFlow<List<Deck>> = _decks

  fun deck(deckId: String): Deck? = _decks.value.find { it.id == deckId }

  override fun observeDecks(): Flow<List<Deck>> = _decks

  override fun observeDeck(deckId: String): Flow<Deck?> =
      _decks.map { list -> list.firstOrNull { it.id == deckId } }

  override suspend fun createDeck(
      title: String,
      description: String,
      cards: List<Flashcard>
  ): String {
    val deck = Deck(title = title, description = description, cards = cards.toMutableList())
    _decks.value = _decks.value + deck
    return deck.id
  }

  override suspend fun addCard(deckId: String, card: Flashcard) {
    _decks.value =
        _decks.value.map {
          if (it.id == deckId) it.copy(cards = (it.cards + card).toMutableList()) else it
        }
  }

  override suspend fun deleteDeck(deckId: String) {
    _decks.value = _decks.value.filterNot { it.id == deckId }
  }
}
