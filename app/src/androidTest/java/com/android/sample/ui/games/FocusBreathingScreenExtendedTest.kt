package com.android.sample.ui.games

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test

class FocusBreathingScreenExtendedTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun breathingScreen_isStableDuringRecomposition() {
    composeRule.setContent { FocusBreathingScreen() }

    // On simule plusieurs recompositions forcées
    repeat(3) { composeRule.runOnIdle { composeRule.mainClock.advanceTimeBy(2000) } }

    // L’écran doit toujours afficher une des phases
    composeRule
        .onAllNodes(
            hasText("Inhale...", substring = true)
                .or(hasText("Exhale...", substring = true))
                .or(hasText("Hold...", substring = true)))
        .onFirst()
        .assertExists()
  }
}
