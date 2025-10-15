package com.android.sample.ui.games

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test

class FocusBreathingScreenTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun displaysInitialOrFirstPhaseText() {
    composeRule.setContent { FocusBreathingScreen() }

    composeRule.waitUntil(timeoutMillis = 5000) {
      composeRule
          .onAllNodes(
              hasText("Inhale", substring = true)
                  .or(hasText("Exhale", substring = true))
                  .or(hasText("Hold", substring = true)))
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeRule
        .onAllNodes(
            hasText("Inhale", substring = true)
                .or(hasText("Exhale", substring = true))
                .or(hasText("Hold", substring = true)))
        .onFirst()
        .assertExists()
  }
}
