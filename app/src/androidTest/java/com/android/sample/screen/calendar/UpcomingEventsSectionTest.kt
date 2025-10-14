package com.android.sample.ui.calendar

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.android.sample.model.calendar.StudyItem
import com.android.sample.model.calendar.TaskType
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Rule
import org.junit.Test

class UpcomingEventsSectionTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun upcomingEventsSection_displays_tasks_and_addButton() {
    val today = LocalDate.now()
    val tasks =
        listOf(
            StudyItem(
                id = "1",
                title = "Math Revision",
                date = today,
                time = LocalTime.of(10, 0),
                type = TaskType.STUDY),
            StudyItem(
                id = "2",
                title = "Team Meeting",
                date = today.plusDays(1),
                time = LocalTime.of(8, 0),
                type = TaskType.WORK))

    composeTestRule.setContent {
      UpcomingEventsSection(
          tasks = tasks,
          selectedDate = today,
          onAddTaskClick = {},
          onTaskClick = {},
          title = "Upcoming Events")
    }

    composeTestRule.onNodeWithText("Upcoming Events").assertIsDisplayed()

    composeTestRule.onNodeWithText("Math Revision").assertIsDisplayed()
    composeTestRule.onNodeWithText("Team Meeting").assertIsDisplayed()

    composeTestRule.onNodeWithText("Add event", ignoreCase = true).assertExists().performClick()
  }

  @Test
  fun upcomingEventsSection_shows_empty_message_when_no_tasks() {
    composeTestRule.setContent {
      UpcomingEventsSection(
          tasks = emptyList(),
          selectedDate = LocalDate.now(),
          onAddTaskClick = {},
          onTaskClick = {},
          title = "Upcoming Events")
    }

    composeTestRule.onNodeWithText("No upcoming events", ignoreCase = true).assertIsDisplayed()
    composeTestRule.onNodeWithText("Add event", ignoreCase = true).assertExists()
  }
}
