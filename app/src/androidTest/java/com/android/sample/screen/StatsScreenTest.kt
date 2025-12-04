package com.android.sample.screen

// The help of an LLM has been used to write this test file.
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.android.sample.ui.stats.StatsScreen
import com.android.sample.ui.stats.model.StudyStats
import org.junit.Rule
import org.junit.Test

class StatsScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  private fun defaultStats() =
      StudyStats(
          totalTimeMin = 145,
          courseTimesMin =
              linkedMapOf(
                  "Algorithms" to 60,
                  "Linear Algebra" to 45,
                  "Physics" to 25,
                  "Computer Science" to 15),
          completedGoals = 5,
          progressByDayMin = listOf(10, 25, 30, 15, 50, 20, 15),
          weeklyGoalMin = 300)

  @Test
  fun statsScreen_displays_all_summary_cards() {
    composeTestRule.setContent {
      StatsScreen(
          stats = defaultStats(),
          selectedIndex = 0,
          titles = listOf("Week 1", "Week 2"),
          onSelectScenario = {})
    }

    composeTestRule.onNodeWithText("2h 25m").assertExists()
    composeTestRule.onNodeWithText("5").assertExists()
    composeTestRule.onNodeWithText("5h 0m").assertExists()
  }

  @Test
  fun statsScreen_displays_scenario_selector() {
    composeTestRule.setContent {
      StatsScreen(
          stats = defaultStats(),
          selectedIndex = 0,
          titles = listOf("Week 1", "Week 2", "Week 3"),
          onSelectScenario = {})
    }

    composeTestRule.onNodeWithText("Week 1").assertExists()
    composeTestRule.onNodeWithText("Week 2").assertExists()
    composeTestRule.onNodeWithText("Week 3").assertExists()
  }

  @Test
  fun statsScreen_scenario_selection_triggers_callback() {
    var selectedIndex = 0
    composeTestRule.setContent {
      StatsScreen(
          stats = defaultStats(),
          selectedIndex = selectedIndex,
          titles = listOf("Week 1", "Week 2"),
          onSelectScenario = { selectedIndex = it })
    }

    composeTestRule.onNodeWithText("Week 2").performClick()
    assert(selectedIndex == 1)
  }

  @Test
  fun statsScreen_with_empty_course_times() {
    val stats = defaultStats().copy(courseTimesMin = emptyMap())
    composeTestRule.setContent {
      StatsScreen(
          stats = stats, selectedIndex = 0, titles = listOf("Week 1"), onSelectScenario = {})
    }
    composeTestRule.waitForIdle()
  }

  @Test
  fun statsScreen_with_zero_weekly_goal() {
    val stats = defaultStats().copy(weeklyGoalMin = 0)
    composeTestRule.setContent {
      StatsScreen(
          stats = stats, selectedIndex = 0, titles = listOf("Week 1"), onSelectScenario = {})
    }

    composeTestRule.onNodeWithText("0m").assertExists()
  }

  @Test
  fun statsScreen_formats_minutes_under_60() {
    val stats = defaultStats().copy(totalTimeMin = 45)
    composeTestRule.setContent {
      StatsScreen(
          stats = stats, selectedIndex = 0, titles = listOf("Week 1"), onSelectScenario = {})
    }

    composeTestRule.onNodeWithText("45m").assertExists()
  }
}
