package com.android.sample.ui.flashcards

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FlashcardsFlowTest {

  @get:Rule val composeRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun fullFlow_createDeck_thenStudy_thenBack() {
    // Starts empty
    composeRule.onNodeWithText("Flashcards").assertIsDisplayed()
    composeRule.onAllNodesWithText("Study").assertCountEquals(0)

    // Create a new deck
    composeRule.onNodeWithText("New Deck").performClick()
    composeRule.onNodeWithText("New Deck").assertIsDisplayed()

    composeRule.onNodeWithText("Title").performTextInput("CI Deck")
    composeRule.onNodeWithText("Description").performTextInput("Created from UI test")

    composeRule.onNodeWithText("Add card").performClick()
    composeRule.onNodeWithText("Question").performTextInput("What is binary search?")
    composeRule.onNodeWithText("Answer").performTextInput("O(log n)")

    composeRule.onNodeWithText("Save Deck").performClick()

    // Back to list + shows 1 deck
    composeRule.onNodeWithText("Flashcards").assertIsDisplayed()
    composeRule.onAllNodesWithText("Study").assertCountEquals(1)
    composeRule.onNodeWithText("Study").performClick()

    // Study screen basics
    composeRule.onNodeWithText("Card 1 of 1").assertIsDisplayed()
    composeRule.onNodeWithText("Question").assertIsDisplayed()
    composeRule.onNodeWithText("Tap to reveal answer").assertIsDisplayed()

    // Reveal and mark confidence
    composeRule.onNodeWithText("Reveal").performClick()
    composeRule.onNodeWithText("Answer").assertIsDisplayed()
    composeRule.onNodeWithText("Medium").performClick() // advances (no crash)

    // Go back
    composeRule.onNodeWithText("← Back").performClick()
    composeRule.onNodeWithText("Flashcards").assertIsDisplayed()
  }
}
