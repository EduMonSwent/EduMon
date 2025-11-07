package com.android.sample.ui.flashcards

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.ui.flashcards.data.FlashcardsRepositoryProvider
import com.android.sample.ui.flashcards.model.Flashcard
import com.android.sample.ui.theme.EduMonTheme
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Flashcards UI flow that does NOT touch MainActivity. It hosts FlashcardsApp directly and
 * pre-populates the repo to avoid performTextInput (works around Semantics getIsEditable crash).
 */
@RunWith(AndroidJUnit4::class)
class FlashcardsFlowTest {

  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  @Before
  fun setUp() {
    val ctx = ApplicationProvider.getApplicationContext<Context>()
    FlashcardsRepositoryProvider.init(ctx)

    // optional: clear DataStore between tests
    // runBlocking { clearFlashcardsStore(ctx) }
  }

  @Test
  fun studyFlow_withPrepopulatedDeck_navigatesAndShowsContent() {
    // Seed the SAME repo the app uses
    val repo = FlashcardsRepositoryProvider.repository
    runBlocking {
      repo.createDeck(
          title = "CI Deck",
          description = "Preloaded",
          cards = listOf(Flashcard(question = "What is binary search?", answer = "O(log n)")))
    }

    composeRule.setContent { EduMonTheme { FlashcardsApp() } }

    composeRule.onNodeWithText("Flashcards").assertIsDisplayed()

    // Wait for at least one Study button, then click the first
    val studyNodes = composeRule.onAllNodesWithText("Study")
    composeRule.waitUntil(3_000) { studyNodes.fetchSemanticsNodes().isNotEmpty() }
    studyNodes[0].performClick()

    composeRule.onNodeWithText("Card 1 of 1").assertIsDisplayed()
    composeRule.onNodeWithText("Question").assertIsDisplayed()
    composeRule.onNodeWithText("Tap to reveal answer").assertIsDisplayed()

    composeRule.onNodeWithText("Reveal").performClick()
    composeRule.onNodeWithText("Answer").assertIsDisplayed()
    composeRule.onNodeWithText("Medium").performClick()

    composeRule.onNodeWithText("← Back").performClick()
    composeRule.onNodeWithText("Flashcards").assertIsDisplayed()
  }
}
