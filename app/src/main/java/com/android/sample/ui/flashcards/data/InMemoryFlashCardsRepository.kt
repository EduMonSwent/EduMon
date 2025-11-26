package com.android.sample.ui.flashcards.data

import com.android.sample.ui.flashcards.model.*
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory repository for managing flashcard decks. */
object InMemoryFlashcardsRepository : FlashcardsRepository {

  private val _decks = MutableStateFlow<List<Deck>>(emptyList())

  override fun observeDecks(): Flow<List<Deck>> = _decks

  override fun observeDeck(deckId: String): Flow<Deck?> =
      _decks.map { list -> list.firstOrNull { it.id == deckId } }

  override suspend fun createDeck(
      title: String,
      description: String,
      cards: List<Flashcard>
  ): String {
    val deck =
        Deck(
            id = UUID.randomUUID().toString(),
            title = title.ifBlank { "New deck" },
            description = description,
            cards = cards.toMutableList())
    _decks.value = _decks.value + deck
    return deck.id
  }

  override suspend fun addCard(deckId: String, card: Flashcard) {
    _decks.value =
        _decks.value.map { d ->
          if (d.id == deckId) d.copy(cards = (d.cards + card).toMutableList()) else d
        }
  }

  override suspend fun deleteDeck(deckId: String) {
    _decks.value = _decks.value.filterNot { it.id == deckId }
  }

  override suspend fun importSharedDeck(token: String): String {
    // In-memory repo cannot import shared decks.
    // Returning "" tells the VM the token is invalid.
    return ""
  }

  /** Utility for tests & previews */
  fun clear() {
    _decks.value = emptyList()
  }
}
