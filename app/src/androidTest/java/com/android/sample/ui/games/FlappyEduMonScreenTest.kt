package com.android.sample.ui.games

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test

// The assistance of an AI tool (ChatGPT) was solicited in writing this test file.

class FlappyEduMonScreenTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun displaysInitialUIElements() {
    composeRule.setContent { FlappyEduMonScreen() }

    composeRule.onNodeWithText("Tap anywhere to start").assertExists()
    composeRule.onNodeWithText("Score: 0").assertExists()
  }

  @Test
  fun tapStartsGame_displaysScore() {
    composeRule.setContent { FlappyEduMonScreen() }

    composeRule.onNodeWithText("Tap anywhere to start").performClick()
    composeRule.onAllNodes(hasText("Score:", substring = true)).onFirst().assertExists()
  }

  @Test
  fun tapStartsGame_hidesStartMessage() {
    composeRule.setContent { FlappyEduMonScreen() }

    // Initially shows start message
    composeRule.onNodeWithText("Tap anywhere to start").assertExists()

    // Tap to start
    composeRule.onNodeWithText("Tap anywhere to start").performClick()

    // Start message should disappear
    composeRule.onNodeWithText("Tap anywhere to start").assertDoesNotExist()
  }

  @Test
  fun gameOver_displaysGameOverScreen() {
    composeRule.setContent { FlappyEduMonScreen() }

    // Start the game
    composeRule.onNodeWithText("Tap anywhere to start").performClick()

    // Wait for game over (player will eventually hit pipes or go out of bounds)
    composeRule.waitUntil(timeoutMillis = 10_000) {
      composeRule.onAllNodesWithText("Game Over").fetchSemanticsNodes().isNotEmpty()
    }

    // Verify game over screen elements
    composeRule.onNodeWithText("Game Over").assertExists()
    composeRule.onNodeWithText("Restart").assertExists()
    composeRule.onAllNodes(hasText("Score:", substring = true)).onFirst().assertExists()
  }

  @Test
  fun gameOver_restartButton_resetsGame() {
    composeRule.setContent { FlappyEduMonScreen() }

    // Start the game
    composeRule.onNodeWithText("Tap anywhere to start").performClick()

    // Wait for game over
    composeRule.waitUntil(timeoutMillis = 10_000) {
      composeRule.onAllNodesWithText("Game Over").fetchSemanticsNodes().isNotEmpty()
    }

    // Verify game over screen is shown
    composeRule.onNodeWithText("Game Over").assertExists()
    composeRule.onNodeWithText("Restart").assertExists()

    // Click restart button
    composeRule.onNodeWithText("Restart").performClick()

    // After restart, game over screen should disappear
    composeRule.onNodeWithText("Game Over").assertDoesNotExist()

    // Should show start message again
    composeRule.onNodeWithText("Tap anywhere to start").assertExists()

    // Score should reset to 0
    composeRule.onNodeWithText("Score: 0").assertExists()
  }

  @Test
  fun gameOver_displaysScoreOnGameOverScreen() {
    composeRule.setContent { FlappyEduMonScreen() }

    // Start the game
    composeRule.onNodeWithText("Tap anywhere to start").performClick()

    // Wait for game over
    composeRule.waitUntil(timeoutMillis = 10_000) {
      composeRule.onAllNodesWithText("Game Over").fetchSemanticsNodes().isNotEmpty()
    }

    // Verify that score is displayed on game over screen (there will be 2 score texts)
    val scoreNodes = composeRule.onAllNodes(hasText("Score:", substring = true))
    scoreNodes.assertCountEquals(2) // One at top, one in game over dialog
  }

  @Test
  fun onExit_callback_isProvided() {
    var exitCalled = false
    composeRule.setContent { FlappyEduMonScreen(onExit = { exitCalled = true }) }

    // Just verify the screen can be created with an onExit callback
    composeRule.onNodeWithText("Tap anywhere to start").assertExists()

    // Note: The current implementation doesn't have an exit button in the UI
    // This test just verifies the callback parameter is accepted
  }

  @Test
  fun initialState_gameNotStarted_scoreIsZero() {
    composeRule.setContent { FlappyEduMonScreen() }

    // Before starting, score should be 0
    composeRule.onNodeWithText("Score: 0").assertExists()

    // Start message should be visible
    composeRule.onNodeWithText("Tap anywhere to start").assertExists()
  }
}
