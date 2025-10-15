package com.android.sample.screen.calendar

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.android.sample.ui.calendar.CalendarScreen
import org.junit.Rule
import org.junit.Test

class CalendarScreenExtraTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun toggle_between_month_and_week_view() {
    composeTestRule.setContent { CalendarScreen() }

    // Initially should show "Month"
    composeTestRule.onNodeWithText("Month").assertExists().performClick()

    // After click, should toggle to "Week"
    composeTestRule.onNodeWithText("Week").assertExists()
  }
}
