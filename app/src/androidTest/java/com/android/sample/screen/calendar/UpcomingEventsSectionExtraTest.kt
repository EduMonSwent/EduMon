package com.android.sample.screen.calendar


import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.android.sample.model.calendar.StudyItem
import com.android.sample.model.calendar.TaskType
import com.android.sample.ui.calendar.UpcomingEventsSection
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class UpcomingEventsSectionExtraTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun shows_no_events_message_when_list_empty() {
        composeTestRule.setContent {
            UpcomingEventsSection(
                tasks = emptyList(),
                selectedDate = LocalDate.now(),
                onAddTaskClick = {},
                onTaskClick = {},
                title = "Upcoming Events"
            )
        }

        composeTestRule.onNodeWithText("No upcoming events", ignoreCase = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Add event", ignoreCase = true).assertExists()
    }

    @Test
    fun renders_multiple_events_correctly() {
        val today = LocalDate.now()
        val tasks = listOf(
            StudyItem(
                id = "1",
                title = "Study Math",
                date = today,
                time = LocalTime.of(10, 0),
                type = TaskType.STUDY
            ),
            StudyItem(
                id = "2",
                title = "Team meeting",
                date = today.plusDays(1),
                type = TaskType.WORK
            )
        )

        composeTestRule.setContent {
            UpcomingEventsSection(
                tasks = tasks,
                selectedDate = today,
                onAddTaskClick = {},
                onTaskClick = {},
                title = "Upcoming Events"
            )
        }

        composeTestRule.onNodeWithText("Study Math").assertIsDisplayed()
        composeTestRule.onNodeWithText("Team meeting").assertIsDisplayed()
        composeTestRule.onNodeWithText("Add event", ignoreCase = true).performClick()
    }
}
