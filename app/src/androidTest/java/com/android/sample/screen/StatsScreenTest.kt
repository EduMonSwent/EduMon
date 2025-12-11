package com.android.sample.screen

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.android.sample.R
import com.android.sample.ui.stats.StatsScreen
import com.android.sample.ui.stats.model.StudyStats
import com.android.sample.ui.theme.SampleAppTheme
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
  fun statsScreen_displays_all_components() {
    composeTestRule.setContent {
      SampleAppTheme {
        StatsScreen(
            stats = defaultStats(),
            totalStudyMinutes = 145,
            selectedIndex = 0,
            titles = listOf("Week 1", "Week 2"),
            onSelectScenario = {})
      }
    }

    val context = ApplicationProvider.getApplicationContext<Context>()

    // Check title is displayed
    composeTestRule.onNodeWithText(context.getString(R.string.stats_title_week)).assertExists()

    // Check summary cards exist
    composeTestRule.onNodeWithText("2h 25m").assertExists()
    composeTestRule.onNodeWithText("5").assertExists()
    composeTestRule.onNodeWithText("5h 0m").assertExists()
  }

  @Test
  fun statsScreen_displays_all_summary_cards_with_correct_labels() {
    composeTestRule.setContent {
      SampleAppTheme {
        StatsScreen(
            stats = defaultStats(),
            totalStudyMinutes = 145,
            selectedIndex = 0,
            titles = listOf("Week 1"),
            onSelectScenario = {})
      }
    }

    val context = ApplicationProvider.getApplicationContext<Context>()

    // Total study time card
    composeTestRule
        .onNodeWithText(context.getString(R.string.stats_summary_total_study))
        .assertExists()

    // Completed goals card
    composeTestRule
        .onNodeWithText(context.getString(R.string.stats_summary_completed_goals))
        .assertExists()

    // Weekly goal card
    composeTestRule
        .onNodeWithText(context.getString(R.string.stats_summary_weekly_goal))
        .assertExists()
  }

  @Test
  fun statsScreen_displays_scenario_selector_when_multiple_titles() {
    composeTestRule.setContent {
      SampleAppTheme {
        StatsScreen(
            stats = defaultStats(),
            totalStudyMinutes = 145,
            selectedIndex = 0,
            titles = listOf("Week 1", "Week 2", "Week 3"),
            onSelectScenario = {})
      }
    }

    composeTestRule.onNodeWithText("Week 1").assertExists()
    composeTestRule.onNodeWithText("Week 2").assertExists()
    composeTestRule.onNodeWithText("Week 3").assertExists()
  }

  @Test
  fun statsScreen_hides_scenario_selector_when_single_title() {
    composeTestRule.setContent {
      SampleAppTheme {
        StatsScreen(
            stats = defaultStats(),
            totalStudyMinutes = 145,
            selectedIndex = 0,
            titles = listOf("Week 1"),
            onSelectScenario = {})
      }
    }

    // Scenario selector should not be present (only shown when titles.size > 1)
    composeTestRule.onNodeWithText("Week 1").assertDoesNotExist()
  }

  @Test
  fun statsScreen_scenario_selection_triggers_callback() {
    var selectedIndex = 0
    composeTestRule.setContent {
      SampleAppTheme {
        StatsScreen(
            stats = defaultStats(),
            totalStudyMinutes = 145,
            selectedIndex = selectedIndex,
            titles = listOf("Week 1", "Week 2"),
            onSelectScenario = { selectedIndex = it })
      }
    }

    composeTestRule.onNodeWithText("Week 2").performClick()
    composeTestRule.waitForIdle()

    assert(selectedIndex == 1)
  }

  @Test
  fun statsScreen_displays_subject_distribution_section() {
    composeTestRule.setContent {
      SampleAppTheme {
        StatsScreen(
            stats = defaultStats(),
            totalStudyMinutes = 145,
            selectedIndex = 0,
            titles = listOf("Week 1"),
            onSelectScenario = {})
      }
    }

    val context = ApplicationProvider.getApplicationContext<Context>()

    composeTestRule
        .onNodeWithText(context.getString(R.string.stats_section_subject_distribution))
        .assertExists()

    composeTestRule
        .onNodeWithText(context.getString(R.string.stats_subjects_this_week_hint))
        .assertExists()
  }

  @Test
  fun statsScreen_displays_progress_7_days_section() {
    composeTestRule.setContent {
      SampleAppTheme {
        StatsScreen(
            stats = defaultStats(),
            totalStudyMinutes = 145,
            selectedIndex = 0,
            titles = listOf("Week 1"),
            onSelectScenario = {})
      }
    }

    val context = ApplicationProvider.getApplicationContext<Context>()

    composeTestRule
        .onNodeWithText(context.getString(R.string.stats_section_progress_7_days))
        .assertExists()
  }

  @Test
  fun statsScreen_displays_legend_entries_for_subjects() {
    composeTestRule.setContent {
      SampleAppTheme {
        StatsScreen(
            stats = defaultStats(),
            totalStudyMinutes = 145,
            selectedIndex = 0,
            titles = listOf("Week 1"),
            onSelectScenario = {})
      }
    }

    // Check that subject names appear in legend
    composeTestRule.onNodeWithText("Algorithms", substring = true).assertExists()
    composeTestRule.onNodeWithText("Linear Algebra", substring = true).assertExists()
    composeTestRule.onNodeWithText("Physics", substring = true).assertExists()
    composeTestRule.onNodeWithText("Computer Science", substring = true).assertExists()
  }

  @Test
  fun statsScreen_with_empty_course_times_displays_correctly() {
    val stats = defaultStats().copy(courseTimesMin = emptyMap())
    composeTestRule.setContent {
      SampleAppTheme {
        StatsScreen(
            stats = stats,
            totalStudyMinutes = 0,
            selectedIndex = 0,
            titles = listOf("Week 1"),
            onSelectScenario = {})
      }
    }

    composeTestRule.waitForIdle()

    // Screen should display without crashing
    val context = ApplicationProvider.getApplicationContext<Context>()
    composeTestRule.onNodeWithText(context.getString(R.string.stats_title_week)).assertExists()
  }

  @Test
  fun statsScreen_with_zero_weekly_goal_displays_correctly() {
    val stats = defaultStats().copy(weeklyGoalMin = 0)
    composeTestRule.setContent {
      SampleAppTheme {
        StatsScreen(
            stats = stats,
            totalStudyMinutes = 145,
            selectedIndex = 0,
            titles = listOf("Week 1"),
            onSelectScenario = {})
      }
    }

    composeTestRule.onNodeWithText("0m").assertExists()
  }

  @Test
  fun statsScreen_formats_minutes_under_60_correctly() {
    val stats = defaultStats().copy(totalTimeMin = 45)
    composeTestRule.setContent {
      SampleAppTheme {
        StatsScreen(
            stats = stats,
            totalStudyMinutes = 45,
            selectedIndex = 0,
            titles = listOf("Week 1"),
            onSelectScenario = {})
      }
    }

    composeTestRule.onNodeWithText("45m").assertExists()
  }

  @Test
  fun statsScreen_formats_hours_and_minutes_correctly() {
    val stats = defaultStats().copy(totalTimeMin = 125) // 2h 5m
    composeTestRule.setContent {
      SampleAppTheme {
        StatsScreen(
            stats = stats,
            totalStudyMinutes = 125,
            selectedIndex = 0,
            titles = listOf("Week 1"),
            onSelectScenario = {})
      }
    }

    composeTestRule.onNodeWithText("2h 5m").assertExists()
  }

  @Test
  fun statsScreen_displays_day_labels_in_bar_chart() {
    composeTestRule.setContent {
      SampleAppTheme {
        StatsScreen(
            stats = defaultStats(),
            totalStudyMinutes = 145,
            selectedIndex = 0,
            titles = listOf("Week 1"),
            onSelectScenario = {})
      }
    }

    val context = ApplicationProvider.getApplicationContext<Context>()

    // Check day labels
    composeTestRule.onNodeWithText(context.getString(R.string.stats_label_day_mon)).assertExists()
    composeTestRule.onNodeWithText(context.getString(R.string.stats_label_day_tue)).assertExists()
    composeTestRule.onNodeWithText(context.getString(R.string.stats_label_day_wed)).assertExists()
    composeTestRule.onNodeWithText(context.getString(R.string.stats_label_day_thu)).assertExists()
    composeTestRule.onNodeWithText(context.getString(R.string.stats_label_day_fri)).assertExists()
    composeTestRule.onNodeWithText(context.getString(R.string.stats_label_day_sat)).assertExists()
    composeTestRule.onNodeWithText(context.getString(R.string.stats_label_day_sun)).assertExists()
  }

  @Test
  fun statsScreen_uses_totalStudyMinutes_parameter_for_summary() {
    // Test that totalStudyMinutes parameter is used (not stats.totalTimeMin)
    val stats = defaultStats().copy(totalTimeMin = 100)
    composeTestRule.setContent {
      SampleAppTheme {
        StatsScreen(
            stats = stats,
            totalStudyMinutes = 200, // Different from stats.totalTimeMin
            selectedIndex = 0,
            titles = listOf("Week 1"),
            onSelectScenario = {})
      }
    }

    // Should display 200 minutes (3h 20m), not 100 minutes
    composeTestRule.onNodeWithText("3h 20m").assertExists()
  }

  @Test
  fun statsScreen_with_zero_progress_values_displays_correctly() {
    val stats = defaultStats().copy(progressByDayMin = listOf(0, 0, 0, 0, 0, 0, 0))
    composeTestRule.setContent {
      SampleAppTheme {
        StatsScreen(
            stats = stats,
            totalStudyMinutes = 0,
            selectedIndex = 0,
            titles = listOf("Week 1"),
            onSelectScenario = {})
      }
    }

    composeTestRule.waitForIdle()

    val context = ApplicationProvider.getApplicationContext<Context>()
    composeTestRule
        .onNodeWithText(context.getString(R.string.stats_section_progress_7_days))
        .assertExists()
  }
}
