package com.android.sample.ui.stats

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import com.android.sample.R
import com.android.sample.ui.theme.SampleAppTheme
import org.junit.Rule
import org.junit.Test

class StatsChartComponentsTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun barChart_displays_day_labels() {
    composeTestRule.setContent {
      SampleAppTheme {
        MaterialTheme {
          // Create a bar chart with test data
          val testValues = listOf(10, 25, 30, 15, 50, 20, 15)
          // Using internal BarChart7Days function would require making it public
          // Instead we test through StatsScreen
        }
      }
    }
  }

  @Test
  fun pieChart_handles_empty_data() {
    composeTestRule.setContent {
      SampleAppTheme {
        // Test that empty course times don't crash the pie chart
        val stats =
            com.android.sample.ui.stats.model.StudyStats(
                totalTimeMin = 0,
                courseTimesMin = emptyMap(),
                completedGoals = 0,
                progressByDayMin = emptyList())

        StatsScreen(
            stats = stats,
            totalStudyMinutes = 0,
            selectedIndex = 0,
            titles = listOf("Test"),
            onSelectScenario = {})
      }
    }

    composeTestRule.waitForIdle()

    val context = ApplicationProvider.getApplicationContext<Context>()
    composeTestRule
        .onNodeWithText(context.getString(R.string.stats_section_subject_distribution))
        .assertIsDisplayed()
  }

  @Test
  fun pieChart_displays_legend_for_multiple_subjects() {
    composeTestRule.setContent {
      SampleAppTheme {
        val stats =
            com.android.sample.ui.stats.model.StudyStats(
                totalTimeMin = 150,
                courseTimesMin = linkedMapOf("Math" to 60, "Physics" to 50, "Chemistry" to 40),
                completedGoals = 3,
                progressByDayMin = listOf(20, 20, 30, 20, 30, 20, 10))

        StatsScreen(
            stats = stats,
            totalStudyMinutes = 150,
            selectedIndex = 0,
            titles = listOf("Test"),
            onSelectScenario = {})
      }
    }

    // Verify legend entries are displayed
    composeTestRule.onNodeWithText("Math", substring = true).assertIsDisplayed()
    composeTestRule.onNodeWithText("Physics", substring = true).assertIsDisplayed()
    composeTestRule.onNodeWithText("Chemistry", substring = true).assertIsDisplayed()
  }

  @Test
  fun barChart_displays_all_seven_days() {
    composeTestRule.setContent {
      SampleAppTheme {
        val stats =
            com.android.sample.ui.stats.model.StudyStats(
                totalTimeMin = 175,
                courseTimesMin = mapOf("Math" to 175),
                completedGoals = 7,
                progressByDayMin = listOf(25, 25, 25, 25, 25, 25, 25))

        StatsScreen(
            stats = stats,
            totalStudyMinutes = 175,
            selectedIndex = 0,
            titles = listOf("Test"),
            onSelectScenario = {})
      }
    }

    val context = ApplicationProvider.getApplicationContext<Context>()

    // Check all day abbreviations are present
    composeTestRule
        .onNodeWithText(context.getString(R.string.stats_label_day_mon))
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithText(context.getString(R.string.stats_label_day_tue))
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithText(context.getString(R.string.stats_label_day_wed))
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithText(context.getString(R.string.stats_label_day_thu))
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithText(context.getString(R.string.stats_label_day_fri))
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithText(context.getString(R.string.stats_label_day_sat))
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithText(context.getString(R.string.stats_label_day_sun))
        .assertIsDisplayed()
  }

  @Test
  fun summaryCard_formats_time_correctly_under_60_minutes() {
    composeTestRule.setContent {
      SampleAppTheme {
        val stats =
            com.android.sample.ui.stats.model.StudyStats(
                totalTimeMin = 45,
                courseTimesMin = mapOf("Math" to 45),
                completedGoals = 1,
                progressByDayMin = listOf(45, 0, 0, 0, 0, 0, 0))

        StatsScreen(
            stats = stats,
            totalStudyMinutes = 45,
            selectedIndex = 0,
            titles = listOf("Test"),
            onSelectScenario = {})
      }
    }

    // Should display "45m" not "0h 45m"
    composeTestRule.onNodeWithText("45m").assertIsDisplayed()
  }

  @Test
  fun summaryCard_formats_time_correctly_over_60_minutes() {
    composeTestRule.setContent {
      SampleAppTheme {
        val stats =
            com.android.sample.ui.stats.model.StudyStats(
                totalTimeMin = 125,
                courseTimesMin = mapOf("Math" to 125),
                completedGoals = 5,
                progressByDayMin = listOf(125, 0, 0, 0, 0, 0, 0))

        StatsScreen(
            stats = stats,
            totalStudyMinutes = 125,
            selectedIndex = 0,
            titles = listOf("Test"),
            onSelectScenario = {})
      }
    }

    // Should display "2h 5m"
    composeTestRule.onNodeWithText("2h 5m").assertIsDisplayed()
  }

  @Test
  fun barChart_caption_is_displayed() {
    composeTestRule.setContent {
      SampleAppTheme {
        val stats =
            com.android.sample.ui.stats.model.StudyStats(
                totalTimeMin = 100,
                courseTimesMin = mapOf("Math" to 100),
                completedGoals = 3,
                progressByDayMin = listOf(20, 15, 15, 15, 15, 10, 10))

        StatsScreen(
            stats = stats,
            totalStudyMinutes = 100,
            selectedIndex = 0,
            titles = listOf("Test"),
            onSelectScenario = {})
      }
    }

    val context = ApplicationProvider.getApplicationContext<Context>()
    composeTestRule
        .onNodeWithText(context.getString(R.string.stats_bar_chart_caption))
        .assertIsDisplayed()
  }

  @Test
  fun legend_entries_show_percentages() {
    composeTestRule.setContent {
      SampleAppTheme {
        val stats =
            com.android.sample.ui.stats.model.StudyStats(
                totalTimeMin = 100,
                courseTimesMin = linkedMapOf("Math" to 50, "Physics" to 30, "Chemistry" to 20),
                completedGoals = 3,
                progressByDayMin = listOf(20, 15, 15, 15, 15, 10, 10))

        StatsScreen(
            stats = stats,
            totalStudyMinutes = 100,
            selectedIndex = 0,
            titles = listOf("Test"),
            onSelectScenario = {})
      }
    }

    // Legend should show percentages (50%, 30%, 20%)
    composeTestRule.onNodeWithText("50%", substring = true).assertIsDisplayed()
    composeTestRule.onNodeWithText("30%", substring = true).assertIsDisplayed()
    composeTestRule.onNodeWithText("20%", substring = true).assertIsDisplayed()
  }

  @Test
  fun barChart_with_all_zero_values() {
    composeTestRule.setContent {
      SampleAppTheme {
        val stats =
            com.android.sample.ui.stats.model.StudyStats(
                totalTimeMin = 0,
                courseTimesMin = emptyMap(),
                completedGoals = 0,
                progressByDayMin = listOf(0, 0, 0, 0, 0, 0, 0),
                weeklyGoalMin = 300)

        StatsScreen(
            stats = stats,
            totalStudyMinutes = 0,
            selectedIndex = 0,
            titles = listOf("Test"),
            onSelectScenario = {})
      }
    }

    // Should not crash with all zeros
    composeTestRule.waitForIdle()

    val context = ApplicationProvider.getApplicationContext<Context>()
    composeTestRule
        .onNodeWithText(context.getString(R.string.stats_section_progress_7_days))
        .assertIsDisplayed()
  }
}
