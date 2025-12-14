package com.android.sample.ui.flashcards

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.android.sample.R
import com.android.sample.ui.flashcards.model.Flashcard
import com.android.sample.ui.flashcards.model.ImportDeckViewModel
import com.android.sample.ui.flashcards.util.ConnectivityDeps
import com.android.sample.ui.flashcards.util.ConnectivityObserver
import com.android.sample.ui.flashcards.util.DisposableHandle
import com.android.sample.ui.flashcards.util.OnlineChecker
import com.android.sample.ui.theme.EduMonTheme
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

// Some parts of this code have been written by an LLM(ChatGPT)
class ImportDeckScreenTest {

  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  private lateinit var previousOnlineChecker: OnlineChecker
  private lateinit var previousObserver: ConnectivityObserver

  private class FakeOnlineChecker(private val online: Boolean) : OnlineChecker {
    override fun isOnline(context: android.content.Context): Boolean = online
  }

  private class NoOpObserver : ConnectivityObserver {
    override fun observe(
        context: android.content.Context,
        onOnlineChanged: (Boolean) -> Unit
    ): DisposableHandle {
      return DisposableHandle {}
    }
  }

  @Before
  fun saveAndOverrideConnectivity() {
    previousOnlineChecker = ConnectivityDeps.onlineChecker
    previousObserver = ConnectivityDeps.observer

    // Default for existing tests: deterministic ONLINE.
    ConnectivityDeps.onlineChecker = FakeOnlineChecker(true)
    ConnectivityDeps.observer = NoOpObserver()
  }

  @After
  fun restoreConnectivity() {
    ConnectivityDeps.onlineChecker = previousOnlineChecker
    ConnectivityDeps.observer = previousObserver
  }

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

  @Test
  fun offline_disablesImport_andShowsMessage() {
    // Force OFFLINE deterministically for this test.
    ConnectivityDeps.onlineChecker = FakeOnlineChecker(false)

    val repo = FakeRepo().apply { nextResult = "deck123" }
    val vm = ImportDeckViewModel(repo)

    composeRule.setContent {
      EduMonTheme { ImportDeckScreen(onSuccess = {}, onBack = {}, vm = vm) }
    }

    composeRule.onNodeWithText("Share Code").performTextInput("abc")
    composeRule.onNodeWithText("Import").assertIsNotEnabled()

    val offlineText = composeRule.activity.getString(R.string.flashcards_offline_import_deck)
    composeRule.onNodeWithText(offlineText).assertIsDisplayed()
  }
}
