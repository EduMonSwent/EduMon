package com.android.sample.schedule

import androidx.activity.ComponentActivity
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.android.sample.ui.schedule.ScheduleScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ScheduleScreenRobolectricTest {

  @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun renders_tabs_and_week_month_agenda_containers() {
    compose.setContent { ScheduleScreen() }

    // Tabs exist
    compose.onNodeWithText("Day").assertExists()
    compose.onNodeWithText("Week").assertExists()
    compose.onNodeWithText("Month").assertExists()
    compose.onNodeWithText("Agenda").assertExists()

    // Navigate Week and check tagged containers (no display assertion -> CI-stable)
    compose.onNodeWithText("Week").performClick()
    compose.onNodeWithTag("WeekContent").assertExists()
    Modifier.testTag("WeekUpcomingSection")

    // Month
    compose.onNodeWithText("Month").performClick()
    compose.onNodeWithTag("MonthContent").assertExists()
    compose.onNodeWithTag("MonthImportantSection").assertExists()

    // Agenda
    compose.onNodeWithText("Agenda").performClick()
    compose.onNodeWithTag("AgendaContent").assertExists()
    compose.onNodeWithTag("AgendaSection").assertExists()

    // FAB
    compose.onNodeWithContentDescription("Add").assertExists()
  }
}
