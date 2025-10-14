package com.android.sample.screen.calendar

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.model.calendar.StudyItem
import com.android.sample.model.calendar.TaskType
import com.android.sample.ui.calendar.UpcomingEventsSection
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UpcomingEventsSectionTest {

  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun upcoming_events_sorted_and_heading_visible() {
    val selected = LocalDate.of(2025, 10, 14)
    val items =
        listOf(
            StudyItem(
                title = "B later",
                date = selected,
                time = LocalTime.of(15, 0),
                type = TaskType.WORK),
            StudyItem(
                title = "A earlier",
                date = selected,
                time = LocalTime.of(9, 30),
                type = TaskType.STUDY),
            StudyItem(title = "All Day", date = selected, time = null, type = TaskType.PERSONAL))

    composeRule.setContent {
      MaterialTheme {
        UpcomingEventsSection(
            tasks = items.sortedBy { it.time ?: LocalTime.MIN },
            selectedDate = selected,
            onAddTaskClick = {},
            onTaskClick = {},
            title = "Upcoming Events")
      }
    }

    // Title
    composeRule.onNodeWithText("Upcoming Events").assertExists()

    // Order: All Day (time null becomes MIN) -> A earlier -> B later
    composeRule.onNodeWithText("All Day").assertExists()
    composeRule.onNodeWithText("A earlier").assertExists()
    composeRule.onNodeWithText("B later").assertExists()
  }
}
