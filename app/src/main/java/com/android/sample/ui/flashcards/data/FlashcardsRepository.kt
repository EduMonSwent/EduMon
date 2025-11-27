package com.android.sample.ui.flashcards.data

import com.android.sample.ui.flashcards.model.Deck
import com.android.sample.ui.flashcards.model.Flashcard
import kotlinx.coroutines.flow.Flow

interface FlashcardsRepository {
  fun observeDecks(): Flow<List<Deck>>

  fun observeDeck(deckId: String): Flow<Deck?>

  suspend fun createDeck(title: String, description: String, cards: List<Flashcard>): String

  suspend fun addCard(deckId: String, card: Flashcard)

  suspend fun deleteDeck(deckId: String)

  suspend fun importSharedDeck(token: String): String = ""

  suspend fun setDeckShareable(deckId: String, enabled: Boolean) {}

  suspend fun createShareToken(deckId: String): String = ""
}
