package com.android.sample.screen.calendar

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.feature.schedule.data.calendar.StudyItem
import com.android.sample.feature.schedule.data.calendar.TaskType
import com.android.sample.ui.calendar.WeekRow
import java.time.LocalDate
import junit.framework.TestCase.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WeekRowTest {

  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun weekRow_renders_days_and_propagates_clicks() {
    val startOfWeek = LocalDate.of(2025, 3, 3) // Monday 3
    val selected = mutableStateOf(startOfWeek)
    var clicked: LocalDate? = null

    composeRule.setContent {
      MaterialTheme {
        WeekRow(
            startOfWeek = startOfWeek,
            selectedDate = selected.value,
            allTasks = emptyList(),
            onDayClick = { d ->
              clicked = d
              selected.value = d
            })
      }
    }

    val firstDayLabel = startOfWeek.dayOfMonth.toString() // "3"
    val secondDay = startOfWeek.plusDays(1) // "4"

    composeRule.onNodeWithText(firstDayLabel).assertExists()
    composeRule.onNodeWithText(secondDay.dayOfMonth.toString()).assertExists()

    composeRule.onNodeWithText(secondDay.dayOfMonth.toString()).performClick()

    assertEquals(secondDay, clicked)
  }

  @Test
  fun weekRow_shows_plusMore_when_more_than_two_events() {
    val ctx = composeRule.activity
    val startOfWeek = LocalDate.of(2025, 5, 5) // Monday
    val busyDay = startOfWeek.plusDays(2) // Wednesday

    val tasks =
        listOf(
            StudyItem(
                title = "Very long first event title that should be truncated",
                date = busyDay,
                type = TaskType.STUDY),
            StudyItem(title = "Second event", date = busyDay, type = TaskType.STUDY),
            StudyItem(title = "Third event", date = busyDay, type = TaskType.WORK),
            StudyItem(title = "Fourth event", date = busyDay, type = TaskType.PERSONAL))

    composeRule.setContent {
      MaterialTheme {
        WeekRow(
            startOfWeek = startOfWeek,
            selectedDate = startOfWeek,
            allTasks = tasks,
            onDayClick = {})
      }
    }

    composeRule.onNodeWithText("Second event", substring = false).assertExists()

    composeRule.onNodeWithText("Third event", substring = true).assertDoesNotExist()

    val moreText = ctx.getString(com.android.sample.R.string.calendar_more_events, 2)
    composeRule.onNodeWithText(moreText).assertExists()
  }
}
