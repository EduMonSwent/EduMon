package com.android.sample.ui.stats

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.android.sample.data.FakeUserStatsRepository
import com.android.sample.ui.stats.model.StudyStats
import com.android.sample.ui.theme.EduMonTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class StatsUiTest {

  @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun renders_stats_title_and_sections() {

    val repo = FakeUserStatsRepository()
    val userStats = repo.stats.value

    // Map UserStats to StudyStats as done in ViewModel
    val stats =
        StudyStats(
            totalTimeMin = userStats.totalStudyMinutes,
            courseTimesMin = userStats.courseTimesMin,
            completedGoals = userStats.completedGoals,
            progressByDayMin = userStats.progressByDayMin,
            dailyGoalMin = userStats.dailyGoal,
            weeklyGoalMin = userStats.weeklyGoal)

    val titles = listOf("Semaine")
    val selected = 0

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
