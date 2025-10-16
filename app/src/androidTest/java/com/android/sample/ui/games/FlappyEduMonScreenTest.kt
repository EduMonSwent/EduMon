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
  fun restartButton_displaysAfterGameOverState() {
    composeRule.setContent { FlappyEduMonScreen() }

    composeRule.onAllNodes(hasText("Score:", substring = true)).onFirst().assertExists()
  }
}
