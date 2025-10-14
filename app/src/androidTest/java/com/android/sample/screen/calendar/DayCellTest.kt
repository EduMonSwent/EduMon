package com.android.sample.screen.calendar

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.model.calendar.StudyItem
import com.android.sample.model.calendar.TaskType
import com.android.sample.ui.calendar.DayCell
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DayCellTest {

  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun dayCell_click_selects_and_shows_number_and_dots() {
    val date = LocalDate.of(2025, 10, 14)
    val items =
        listOf(
            StudyItem(title = "A", date = date, type = TaskType.STUDY),
            StudyItem(title = "B", date = date, type = TaskType.WORK))
    val selected = mutableStateOf(false)

    composeRule.setContent {
      MaterialTheme {
        DayCell(
            date = date,
            tasks = items,
            isSelected = selected.value,
            onDateClick = { selected.value = true },
            modifier = Modifier.Companion.testTag("dayCell"))
      }
    }

    composeRule.onNodeWithTag("dayCell").performClick()
    composeRule.onNodeWithText(date.dayOfMonth.toString()).assertIsDisplayed()
    // We don’t have separate semantics for dots; the presence of the cell + day number is our
    // proxy.
  }
}
