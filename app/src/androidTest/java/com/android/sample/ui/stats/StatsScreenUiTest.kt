// app/src/androidTest/java/com/android/sample/ui/stats/StatsScreenUiTest.kt
package com.android.sample.ui.stats

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.android.sample.ui.stats.repository.FakeStatsRepository
import com.android.sample.ui.theme.EduMonTheme
import org.junit.Rule
import org.junit.Test

class StatsScreenUiTest {

  @get:Rule val rule = createComposeRule()

  @Test
  fun renders_stats_title_and_sections() {
    val repo = FakeStatsRepository()
    val stats = repo.stats.value
    val titles = repo.titles
    val selected = repo.selectedIndex.value

    rule.setContent {
      EduMonTheme {
        StatsScreen(stats = stats, selectedIndex = selected, titles = titles, onSelectScenario = {})
      }
    }

    rule.onNodeWithText("Tes statistiques de la semaine").assertIsDisplayed()
    rule.onNodeWithText("Répartition par cours").assertIsDisplayed()
    rule.onNodeWithText("Progression sur 7 jours").assertIsDisplayed()
  }
}
