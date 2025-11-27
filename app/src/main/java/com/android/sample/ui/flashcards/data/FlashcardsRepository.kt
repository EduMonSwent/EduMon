package com.android.sample.ui.flashcards.data

import com.android.sample.ui.flashcards.model.Deck
import com.android.sample.ui.flashcards.model.Flashcard
import kotlinx.coroutines.flow.Flow

/**
 * Contract for a flashcards data source.
 *
 * The app supports multiple repository implementations:
 * - Firestore-backed repository
 * - In-memory/Fake repositories (used in tests or previews)
 * - DataStore/local-only repository The comments in this file have been written with the help of an
 *   LLM (ChatGPT)
 */
interface FlashcardsRepository {
  /**
   * Observe the list of all decks for the current user. Emits updates whenever the underlying data
   * changes.
   */
  fun observeDecks(): Flow<List<Deck>>
  /** Observe a single deck by ID. Returns null if the deck no longer exists. */
  fun observeDeck(deckId: String): Flow<Deck?>
  /**
   * Create a new deck with the given title, description and list of cards.
   *
   * @return The newly created deck ID.
   */
  suspend fun createDeck(title: String, description: String, cards: List<Flashcard>): String
  /** Add a card to an existing deck. */
  suspend fun addCard(deckId: String, card: Flashcard)
  /** Permanently delete a deck and all its associated cards. */
  suspend fun deleteDeck(deckId: String)
  /**
   * Import a deck that was shared using a share token.
   *
   * DEFAULT BEHAVIOR: Returns an empty string. This ensures that in-memory and simple storage
   * implementations do not need to support deck-sharing.
   *
   * FirestoreRepository overrides this to perform the actual import logic.
   *
   * @return The ID of the newly imported deck, or "" if not supported.
   */
  suspend fun importSharedDeck(token: String): String = ""
  /**
   * Toggle whether a deck can be shared or not.
   *
   * DEFAULT BEHAVIOR: No-op. This is intentional for repositories that do not support sharing, such
   * as in-memory or local-only repositories used in tests.
   *
   * FirestoreRepository overrides this to persist the field.
   */
  suspend fun setDeckShareable(deckId: String, enabled: Boolean) {}
  /**
   * Create a token that can be used to share a deck with another user.
   *
   * DEFAULT BEHAVIOR: Returns an empty string. Only the Firestore implementation supports
   * generating real share tokens.
   *
   * @return a share token or an empty string if unsupported.
   */
  suspend fun createShareToken(deckId: String): String = ""
}
