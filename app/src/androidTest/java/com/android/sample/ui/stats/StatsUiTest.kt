package com.android.sample.ui.stats

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.android.sample.ui.stats.repository.FakeStatsRepository
import com.android.sample.ui.theme.EduMonTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class StatsUiTest {

  @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

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

    // Header should be visible at top
    rule.onNodeWithText("Tes statistiques de la semaine").assertIsDisplayed()

    // Sections may be below the fold on small CI devices: assert existence via nodes list
    assertTrue(rule.onAllNodesWithText("Répartition par cours").fetchSemanticsNodes().isNotEmpty())
    assertTrue(
        rule.onAllNodesWithText("Progression sur 7 jours").fetchSemanticsNodes().isNotEmpty())
  }
}
