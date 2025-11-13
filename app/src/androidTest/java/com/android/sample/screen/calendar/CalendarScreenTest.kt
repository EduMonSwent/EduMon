package com.android.sample.screen.calendar

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.android.sample.feature.schedule.viewmodel.CalendarViewModel
import com.android.sample.repos_providors.FakeRepositories
import com.android.sample.ui.calendar.CalendarScreen
import com.android.sample.ui.calendar.CalendarScreenTestTags
import org.junit.Rule
import org.junit.Test

class CalendarScreenTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun calendarScreen_displays_toggle_and_calendarCard() {
    composeTestRule.setContent {
      CalendarScreen(vm = CalendarViewModel(FakeRepositories.calendarRepository))
    }

    composeTestRule.onNodeWithTag(CalendarScreenTestTags.CALENDAR_CARD).assertIsDisplayed()
    composeTestRule.onNodeWithTag(CalendarScreenTestTags.VIEW_TOGGLE_BUTTON).performClick()
  }
}
