package com.android.sample.screen.calendar

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.model.calendar.StudyItem
import com.android.sample.model.calendar.TaskType
import com.android.sample.ui.calendar.MonthGrid
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.YearMonth

@RunWith(AndroidJUnit4::class)
class MonthGridTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun grid_renders_days_and_propagates_clicks() {
        val ym = YearMonth.of(2025, 10)
        val selected = mutableStateOf(LocalDate.of(2025, 10, 14))
        val tasks = listOf(
            StudyItem(title = "Meet", date = LocalDate.of(2025,10,3), type = TaskType.PERSONAL),
            StudyItem(title = "Study", date = LocalDate.of(2025,10,14), type = TaskType.STUDY)
        )
        var clicked: LocalDate? = null

        composeRule.setContent {
            MaterialTheme {
                MonthGrid(
                    currentMonth = ym,
                    selectedDate = selected.value,
                    allTasks = tasks,
                    onDateClick = { d -> clicked = d; selected.value = d },
                    onPrevClick = {},
                    onNextClick = {}
                )
            }
        }

        // Day "1" and "31" should exist (October 2025 has 31 days)
        composeRule.onNodeWithText("1").assertExists()
        composeRule.onNodeWithText("31").assertExists()

        // Click on "3" (prefer last match to avoid weekday header)
        composeRule.onAllNodes(hasText("3")).onLast().performClick()
        assert(clicked == LocalDate.of(2025, 10, 3))
    }
}
