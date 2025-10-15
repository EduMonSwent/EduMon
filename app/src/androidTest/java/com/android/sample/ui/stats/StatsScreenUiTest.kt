// app/src/androidTest/java/com/android/sample/ui/stats/StatsScreenUiTest.kt
package com.android.sample.ui.stats

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class StatsScreenUiTest {

  @get:Rule val rule = createComposeRule()

  @Test
  fun renders_stats_title_and_sections() {
    rule.setContent {
      com.android.sample.ui.theme.EduMonTheme {
        StatsScreen(viewModel = StatsViewModel(FakeStatsRepository()))
      }
    }

    rule.onNodeWithText("Tes statistiques de la semaine").assertIsDisplayed()
    rule.onNodeWithText("Répartition par cours").assertIsDisplayed()
    rule.onNodeWithText("Progression sur 7 jours").assertIsDisplayed()
  }
}
