package com.android.sample.ui.flashcards

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.android.sample.ui.flashcards.data.FlashcardsRepository
import com.android.sample.ui.flashcards.model.Deck
import com.android.sample.ui.flashcards.model.Flashcard
import com.android.sample.ui.theme.EduMonTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

/**
 * Full coverage for ShareDeckDialog using the REAL DeckListViewModel while injecting a fake
 * repository (no Firebase). Some parts of this code have been written by an LLM(ChatGPT)
 */
class ShareDeckDialogTest {

  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  // ------------------------------------------------------------------
  // Fake repo for testing share toggle + token generation
  // ------------------------------------------------------------------
  class FakeRepo : FlashcardsRepository {

    val decksFlow = MutableStateFlow<List<Deck>>(emptyList())

    override fun observeDecks(): Flow<List<Deck>> = decksFlow

    override fun observeDeck(deckId: String): Flow<Deck?> =
        decksFlow.map { all -> all.firstOrNull { it.id == deckId } }

    override suspend fun createDeck(
        title: String,
        description: String,
        cards: List<Flashcard>
    ): String {
      val deck =
          Deck(
              title = title,
              description = description,
              createdAt = 0L,
              cards = cards.toMutableList())
      decksFlow.value = decksFlow.value + deck
      return deck.id
    }

    override suspend fun addCard(deckId: String, card: Flashcard) {}

    override suspend fun deleteDeck(deckId: String) {}
  }

  private fun testDeck(shareable: Boolean) =
      Deck(
          id = "D1",
          title = "Test Deck",
          description = "desc",
          createdAt = 0L,
          cards = mutableListOf(),
          shareable = shareable)

  // ------------------------------------------------------------------
  // 1) shareable = false → UI should only show toggle, not token UI
  // ------------------------------------------------------------------
  @Test
  fun shareableFalse_onlyToggleVisible() {
    val deck = testDeck(false)
    val repo = FakeRepo().apply { decksFlow.value = listOf(deck) }
    val vm = DeckListViewModel(repo, requireAuth = false)

    composeRule.setContent {
      EduMonTheme {
        ShareDeckDialog(
            deck = deck, // IMPORTANT: same instance!
            vm = vm,
            onDismiss = {})
      }
    }

    composeRule.onNodeWithText("Allow sharing").assertIsDisplayed()
    composeRule.onAllNodesWithText("Generate share link").assertCountEquals(0)
  }

  // ------------------------------------------------------------------
  // 2) Clicking "Generate" produces the token text
  // ------------------------------------------------------------------
  @Test
  fun generateToken_showsToken() = runBlocking {
    val deck = testDeck(true)
    val repo = FakeRepo().apply { decksFlow.value = listOf(deck) }
    val vm = DeckListViewModel(repo, requireAuth = false)

    composeRule.setContent { EduMonTheme { ShareDeckDialog(deck = deck, vm = vm, onDismiss = {}) } }

    // Click generate
    composeRule.onNodeWithText("Generate share link").performClick()

    // Wait for token to appear
    composeRule.waitUntil(3_000) {
      composeRule.onAllNodesWithText("Copy").fetchSemanticsNodes().isNotEmpty()
    }
  }

  // ------------------------------------------------------------------
  // 3) After token appears, the Copy button is visible and clickable
  // ------------------------------------------------------------------
  @Test
  fun copyButton_appearsAndCanBeClicked() = runTest {
    val deck = testDeck(true)
    val repo = FakeRepo().apply { decksFlow.value = listOf(deck) }
    val vm = DeckListViewModel(repo, requireAuth = false)

    composeRule.setContent { EduMonTheme { ShareDeckDialog(deck = deck, vm = vm, onDismiss = {}) } }

    composeRule.onNodeWithText("Generate share link").performClick()

    composeRule.waitUntil(3_000) {
      composeRule.onAllNodesWithText("Copy").fetchSemanticsNodes().isNotEmpty()
    }

    composeRule.onNodeWithText("Copy").performClick()
  }
  // ------------------------------------------------------------------
  // 4) Close button dismisses dialog
  // ------------------------------------------------------------------
  @Test
  fun closeButton_callsOnDismiss() {
    val deck = testDeck(false)
    val repo = FakeRepo().apply { decksFlow.value = listOf(deck) }
    val vm = DeckListViewModel(repo, requireAuth = false)

    var dismissed = false

    composeRule.setContent {
      EduMonTheme { ShareDeckDialog(deck = deck, vm = vm, onDismiss = { dismissed = true }) }
    }

    composeRule.onNodeWithText("Close").performClick()
    composeRule.waitUntil(3_000) { dismissed }
  }
}
