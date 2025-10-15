package com.android.sample.ui.session

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
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
    composeTestRule.setContent {
      com.android.sample.ui.session.components.SessionStatsPanel(
          pomodoros = 3, totalMinutes = 75, streak = 5)
    }

    composeTestRule.onNodeWithText("Pomodoros Completed: 3").assertIsDisplayed()
    composeTestRule.onNodeWithText("Total Study Time: 75 min").assertIsDisplayed()
    composeTestRule.onNodeWithText("Current Streak: 5 days").assertIsDisplayed()
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
