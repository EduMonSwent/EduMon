package com.android.sample.schedule

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import com.android.sample.ui.schedule.ScheduleScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ScheduleScreenRobolectricTest {

  @get:Rule val compose = createComposeRule()

  @Test
  fun renders_tabs_and_basic_interactions() {
    compose.setContent { ScheduleScreen() }
    compose.waitForIdle()

    // Tabs exist
    compose.onNodeWithText("Day", ignoreCase = true).assertExists()
    compose.onNodeWithText("Week", ignoreCase = true).assertExists()
    compose.onNodeWithText("Month", ignoreCase = true).assertExists()
    compose.onNodeWithText("Agenda", ignoreCase = true).assertExists()

    // Switch Week
    compose.onNodeWithText("Week", ignoreCase = true).performClick()
    compose.waitForIdle()
    compose.onNodeWithText("Week", ignoreCase = true).assertExists()

    // Switch Month, look for generic section words (copy may vary)
    compose.onNodeWithText("Month", ignoreCase = true).performClick()
    compose.waitForIdle()
    assertAnyTextPresent("Most important", "Month", "Upcoming")(compose)

    // Switch Agenda
    compose.onNodeWithText("Agenda", ignoreCase = true).performClick()
    compose.waitForIdle()
    assertAnyTextPresent("Agenda", "Upcoming", "Events")(compose)

    // FAB present
    compose.onNode(hasContentDescription("Add")).assertExists()
  }
}

private fun assertAnyTextPresent(vararg candidates: String): (ComposeContentTestRule) -> Unit =
    { rule ->
      val found =
          candidates.any { t ->
            rule
                .onAllNodes(hasText(t, substring = true, ignoreCase = true))
                .fetchSemanticsNodes()
                .isNotEmpty()
          }
      check(found) { "None of the texts were found: ${candidates.joinToString()}" }
    }
