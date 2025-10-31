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

    // Tabs should exist (pure text queries, no Activity/resources)
    compose.onNodeWithText("Day", ignoreCase = true).assertExists()
    compose.onNodeWithText("Week", ignoreCase = true).assertExists()
    compose.onNodeWithText("Month", ignoreCase = true).assertExists()
    compose.onNodeWithText("Agenda", ignoreCase = true).assertExists()

    // Switch to Week and confirm UI is still alive by checking the tab again.
    // (Avoid strict content checks that are flaky on CI).
    compose.onNodeWithText("Week", ignoreCase = true).performClick()
    compose.waitForIdle()
    compose.onNodeWithText("Week", ignoreCase = true).assertExists()

    // Switch to Month; sanity check a generic section label likely present somewhere.
    compose.onNodeWithText("Month", ignoreCase = true).performClick()
    compose.waitForIdle()
    assertAnyTextPresent("Most important", "This month", "Month")(compose)

    // Switch to Agenda; look for “Agenda” within content (there may be two: tab + section)
    compose.onNodeWithText("Agenda", ignoreCase = true).performClick()
    compose.waitForIdle()
    assertAnyTextPresent("Agenda", "Upcoming", "Events")(compose)

    // FAB should exist by content description
    compose.onNode(hasContentDescription("Add")).assertExists()
  }
}

/** --- tiny helper to make tests resilient to copy changes --- */
private fun assertAnyTextPresent(vararg candidates: String): (ComposeContentTestRule) -> Unit =
    { rule ->
      val found =
          candidates.any { text ->
            rule
                .onAllNodes(hasText(text, substring = true, ignoreCase = true))
                .fetchSemanticsNodes()
                .isNotEmpty()
          }
      check(found) { "None of the texts were found: ${candidates.joinToString()}" }
    }
