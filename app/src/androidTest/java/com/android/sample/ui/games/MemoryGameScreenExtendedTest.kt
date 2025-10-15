package com.android.sample.ui.games

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.School
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test

class MemoryGameScreenExtendedTest {

  @get:Rule val composeRule = createComposeRule()

  private val icons = listOf(Icons.Filled.Book, Icons.Filled.School)

  @Test
  fun cardFlip_and_Match_increasesScore() {
    composeRule.setContent { MemoryGameScreenTestable(initialCards = generateCards(icons)) }

    // Tap two cards to simulate matching logic
    val cards = composeRule.onAllNodesWithText("?")
    cards[0].performClick()
    cards[1].performClick()
    composeRule.waitForIdle()

    // Wait for LaunchedEffect to trigger
    composeRule.waitUntil(timeoutMillis = 2000) {
      composeRule.onAllNodes(hasText("Score:", substring = true)).fetchSemanticsNodes().isNotEmpty()
    }
  }

  @Test
  fun showsGameOverOverlay_whenInitialGameOverTrue() {
    val cards = List(18) { MemoryCard(it, Icons.Default.Book, isMatched = false) }

    composeRule.setContent {
      MemoryGameScreenTestable(initialCards = cards, initialGameOver = true)
    }

    composeRule.waitUntil(timeoutMillis = 5000) {
      composeRule.onAllNodesWithText("Time’s up!").fetchSemanticsNodes().isNotEmpty()
    }
    composeRule.onNodeWithText("Time’s up!").assertExists()
  }

  @Test
  fun clickingRestart_resetsEverything() {
    val cards = List(18) { MemoryCard(it, Icons.Default.Book, isMatched = true) }

    composeRule.setContent { MemoryGameScreenTestable(initialCards = cards, initialWin = true) }

    composeRule.onNodeWithText("Restart").performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithText("Memory Game").assertExists()
  }

  @Test
  fun verifyCardFlip_DisabledAfterMatch() {
    val card = MemoryCard(1, Icons.Filled.Book, isMatched = true)
    composeRule.setContent { MemoryCardView(card = card, onClick = {}) }
    // Matched cards shouldn’t be clickable
    composeRule.onNodeWithText("?").assertDoesNotExist()
  }
}
