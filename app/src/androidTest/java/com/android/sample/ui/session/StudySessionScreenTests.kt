package com.android.sample.ui.session

import android.content.Context
import androidx.compose.ui.test.assertAny
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import com.android.sample.R
import com.android.sample.ui.session.components.SessionStatsPanel
import com.android.sample.ui.session.components.SessionStatsPanelTestTags
import com.android.sample.ui.theme.SampleAppTheme
import org.junit.Rule
import org.junit.Test

// Parts of this code were written using ChatGPT and AndroidStudio Gemini tool.

class StudySessionScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun studySessionScreen_displaysTitleAndComponents() {
    composeTestRule.setContent { SampleAppTheme { StudySessionScreen() } }

    composeTestRule.onNodeWithTag(StudySessionTestTags.TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(StudySessionTestTags.TASK_LIST).assertIsDisplayed()
    composeTestRule.onNodeWithTag(StudySessionTestTags.TIMER_SECTION).assertIsDisplayed()
    composeTestRule.onNodeWithTag(StudySessionTestTags.STATS_PANEL).assertIsDisplayed()
  }

  @Test
  fun statsPanel_displaysValuesCorrectly() {
    // Arrange
    composeTestRule.setContent { SessionStatsPanel(pomodoros = 3, totalMinutes = 75, streak = 5) }

    val context = ApplicationProvider.getApplicationContext<Context>()

    // Assert each stat card displays correct text

    // Pomodoros
    composeTestRule
        .onNodeWithTag(SessionStatsPanelTestTags.POMODOROS)
        .onChildren()
        .assertAny(hasText(context.getString(R.string.pomodoros_completed_txt)))
    composeTestRule
        .onNodeWithTag(SessionStatsPanelTestTags.POMODOROS)
        .onChildren()
        .assertAny(hasText("3"))

    // Study time
    composeTestRule
        .onNodeWithTag(SessionStatsPanelTestTags.TIME)
        .onChildren()
        .assertAny(hasText(context.getString(R.string.pomodoro_time_txt)))
    composeTestRule
        .onNodeWithTag(SessionStatsPanelTestTags.TIME)
        .onChildren()
        .assertAny(hasText("75 " + context.getString(R.string.minute)))

    // Streak
    composeTestRule
        .onNodeWithTag(SessionStatsPanelTestTags.STREAK)
        .onChildren()
        .assertAny(hasText(context.getString(R.string.current_streak)))
    composeTestRule
        .onNodeWithTag(SessionStatsPanelTestTags.STREAK)
        .onChildren()
        .assertAny(hasText("5 " + context.getString(R.string.days)))
  }

  @Test
  fun suggestedTasksList_displaysTasksAndHandlesSelection() {
    val tasks = listOf(Task("Task A"), Task("Task B"), Task("Task C"))
    composeTestRule.setContent {
      com.android.sample.ui.session.components.SuggestedTasksList(
          tasks = tasks, selectedTask = tasks[1], onTaskSelected = {})
    }

    composeTestRule.onNodeWithText("Task A").assertIsDisplayed()
    composeTestRule.onNodeWithText("Task B").assertIsDisplayed()
    composeTestRule.onNodeWithText("Task C").assertIsDisplayed()
  }
}
