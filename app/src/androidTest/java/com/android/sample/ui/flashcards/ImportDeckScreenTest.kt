package com.android.sample.ui.flashcards

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.android.sample.ui.flashcards.model.Flashcard
import com.android.sample.ui.flashcards.model.ImportDeckViewModel
import com.android.sample.ui.theme.EduMonTheme
import org.junit.Rule
import org.junit.Test

class ImportDeckScreenTest {

  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  /** Fake repo for UI tests. Behaves like the unit-test fake. */
  private class FakeRepo : com.android.sample.ui.flashcards.data.FlashcardsRepository {
    var nextResult: String? = null

    override fun observeDecks() = throw UnsupportedOperationException()

    override fun observeDeck(deckId: String) = throw UnsupportedOperationException()

    override suspend fun createDeck(title: String, description: String, cards: List<Flashcard>) = ""

    override suspend fun addCard(deckId: String, card: Flashcard) {}

    override suspend fun deleteDeck(deckId: String) {}

    override suspend fun importSharedDeck(token: String): String = nextResult ?: ""
  }

  @Test
  fun errorShown_whenInvalidToken() {
    val repo = FakeRepo().apply { nextResult = "" }
    val vm = ImportDeckViewModel(repo)

    composeRule.setContent {
      EduMonTheme { ImportDeckScreen(onSuccess = {}, onBack = {}, vm = vm) }
    }

    composeRule.onNodeWithText("Import").assertIsNotEnabled()

    composeRule.onNodeWithText("Share Code").performTextInput("abc")
    composeRule.onNodeWithText("Import").performClick()

    composeRule.onNodeWithText("Share code not valid").assertIsDisplayed()
  }

  @Test
  fun successTriggersCallback() {
    val repo = FakeRepo().apply { nextResult = "deck123" }
    val vm = ImportDeckViewModel(repo)

    var succeeded = false

    composeRule.setContent {
      EduMonTheme { ImportDeckScreen(onSuccess = { succeeded = true }, onBack = {}, vm = vm) }
    }

    composeRule.onNodeWithText("Share Code").performTextInput("abc")
    composeRule.onNodeWithText("Import").performClick()

    composeRule.waitUntil(3_000) { succeeded }
    assert(succeeded)
  }
}
