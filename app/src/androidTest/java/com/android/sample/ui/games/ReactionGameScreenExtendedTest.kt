package com.android.sample.ui.games

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test

class ReactionGameScreenExtendedTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun tapToStart_showsWaitMessage() {
    composeRule.setContent { ReactionGameScreen() }
    composeRule.onNodeWithText("Tap to start!").performClick()
    composeRule.onNodeWithText("Wait for green...", substring = true).assertExists()
  }

  @Test
  fun tapTooSoon_showsTooSoonMessage() {
    composeRule.setContent { ReactionGameScreen() }
    composeRule.onNodeWithText("Tap to start!").performClick()
    composeRule.onNodeWithText("Wait for green...", substring = true).performClick()
    composeRule.onNodeWithText("Too soon!", substring = true).assertExists()
  }

  @Test
  fun whenTurnsGreen_thenShowsTapNow_andComputesReaction() {
    composeRule.setContent { ReactionGameScreen() }

    // Start game
    composeRule.onNodeWithText("Tap to start!").performClick()

    // Wait enough time for delay(Random.nextLong(1500, 4000))
    composeRule.mainClock.advanceTimeBy(4500)
    composeRule.waitForIdle()

    // Should now display TAP NOW!
    composeRule.onNodeWithText("TAP NOW!").assertExists()

    // Tap to stop reaction timer
    composeRule.onNodeWithText("TAP NOW!").performClick()

    // Verify reaction time message appears
    composeRule.onNodeWithText("Reaction:", substring = true).assertExists()
  }
}
