package com.android.sample.ui.flashcards

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.ui.flashcards.data.InMemoryFlashcardsRepository
import com.android.sample.ui.flashcards.model.Flashcard
import com.android.sample.ui.theme.EduMonTheme
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

  @Test
  fun studyFlow_withPrepopulatedDeck_navigatesAndShowsContent() {
    InMemoryFlashcardsRepository.createDeck(
      title = "CI Deck",
      description = "Preloaded",
      cards = listOf(Flashcard("","What is binary search?", "O(log n)")))

    composeRule.setContent { EduMonTheme { FlashcardsApp() } }

    composeRule.onNodeWithText("Flashcards").assertIsDisplayed()
    composeRule.onAllNodesWithText("Study").assertCountEquals(1)

    composeRule.onNodeWithText("Study").performClick()

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