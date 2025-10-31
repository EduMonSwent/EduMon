package com.android.sample.ui.schedule

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScheduleScreenSmokePathsTest {

  @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun month_then_agenda_then_day_paths_render_texts() {
    compose.setContent { ScheduleScreen() }
    compose.waitForIdle()

    compose.onNodeWithText("Month", ignoreCase = true).performClick()
    compose.waitForIdle()
    compose.onAllNodesWithText("Most important this month")[0].assertExists()

    compose.onNodeWithText("Agenda", ignoreCase = true).performClick()
    compose.waitForIdle()
    compose.onAllNodesWithText("Agenda")[0].assertExists()

    compose.onNodeWithText("Day", ignoreCase = true).performClick()
    compose.waitForIdle()
    compose.onAllNodesWithText("Today •", substring = true)[0].assertExists()
  }
}
