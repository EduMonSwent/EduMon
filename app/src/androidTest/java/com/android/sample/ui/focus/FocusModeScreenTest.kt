package com.android.sample.ui.focus

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.viewmodel.compose.viewModel
import org.junit.Rule
import org.junit.Test

class FocusModeScreenTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun showsStartButtonInitially() {
    composeRule.setContent { FocusModeScreen(viewModel()) }
    composeRule.onNodeWithText("Start").assertExists()
  }

  @Test
  fun togglesToStopAfterStartClick() {
    val viewModel = FocusModeViewModel()
    composeRule.setContent { FocusModeScreen(viewModel) }
    composeRule.onNodeWithText("Start").performClick()
    composeRule.waitUntil(2000) { viewModel.isRunning.value }
    composeRule.onNodeWithText("Stop").assertExists()
  }
}
