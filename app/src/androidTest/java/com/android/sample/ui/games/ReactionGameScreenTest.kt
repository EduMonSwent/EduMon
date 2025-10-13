package com.android.sample.ui.games

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test

// The assistance of an AI tool (ChatGPT) was solicited in writing this test file.

class ReactionGameScreenTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun showsInitialMessage() {
    composeRule.setContent { ReactionGameScreen() }
    composeRule.onNodeWithText("Tap to start!").assertExists()
  }

  @Test
  fun updatesMessageAfterTap() {
    composeRule.setContent { ReactionGameScreen() }

    composeRule.onNodeWithText("Tap to start!").performClick()
    composeRule.onNodeWithText("Wait for green...", substring = true).assertExists()
  }

  @Test
  fun showsTooSoonMessageIfTappedEarly() {
    composeRule.setContent { ReactionGameScreen() }

    composeRule.onNodeWithText("Tap to start!").performClick()
    composeRule.onNodeWithText("Wait for green...", substring = true).performClick()

    composeRule.onNodeWithText("Too soon!", substring = true).assertExists()
  }
}
