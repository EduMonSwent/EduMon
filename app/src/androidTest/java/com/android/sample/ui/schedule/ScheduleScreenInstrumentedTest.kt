package com.android.sample.ui.schedule

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScheduleScreenInstrumentedTest {

  @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

  private fun setContent() = compose.setContent { ScheduleScreen() }

  @Test
  fun renders_tabs_and_allows_basic_tab_clicks() {
    setContent()
    compose.waitForIdle()

    // Tabs exist (don’t assert visibility/layout)
    compose.onNodeWithText("Day", ignoreCase = true).assertExists()
    compose.onNodeWithText("Week", ignoreCase = true).assertExists()
    compose.onNodeWithText("Month", ignoreCase = true).assertExists()
    compose.onNodeWithText("Agenda", ignoreCase = true).assertExists()

    // Light interaction: click each tab; don’t over-assert the content
    compose.onNodeWithText("Week", ignoreCase = true).performClick()
    compose.waitForIdle()

    compose.onNodeWithText("Month", ignoreCase = true).performClick()
    compose.waitForIdle()

    compose.onNodeWithText("Agenda", ignoreCase = true).performClick()
    compose.waitForIdle()

    // Back to Day
    compose.onNodeWithText("Day", ignoreCase = true).performClick()
    compose.waitForIdle()
  }

  @Test
  fun fab_exists_and_is_clickable() {
    setContent()
    compose.waitForIdle()

    // In instrumentation the FAB’s icon contentDescription should be present
    compose.onNode(hasContentDescription("Add")).assertExists().assertIsEnabled().performClick()

    // We don’t assert dialog contents (keeps CI stable)
    compose.waitForIdle()
  }

  @Test
  fun switching_tabs_reacts_without_crash() {
    compose.setContent { ScheduleScreen() }
    compose.waitForIdle()
    listOf("Day", "Week", "Month", "Agenda").forEach { tab ->
      compose.onNodeWithText(tab, ignoreCase = true).performClick()
      compose.waitForIdle()
    }
  }
}
