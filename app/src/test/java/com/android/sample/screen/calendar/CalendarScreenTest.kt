package com.android.sample.screen.calendar

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import com.android.sample.ui.calendar.CalendarScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CalendarScreenRobolectricTest {

  @get:Rule val compose = createComposeRule()

  @Test
  fun calendarScreen_renders_and_shows_any_upcoming_like_title() {
    compose.setContent { CalendarScreen() }
    compose.waitForIdle()

    // Avoid getString(); just check generic labels likely on both modes
    assertAnyTextPresent("Upcoming", "This week", "This month", "Agenda")(compose)
  }

  @Test
  fun calendarScreen_can_toggle_week_or_month_if_tabs_exist() {
    compose.setContent { CalendarScreen() }
    compose.waitForIdle()

    val weekNodes =
        compose
            .onAllNodes(hasText("Week", substring = true, ignoreCase = true))
            .fetchSemanticsNodes()
    val monthNodes =
        compose
            .onAllNodes(hasText("Month", substring = true, ignoreCase = true))
            .fetchSemanticsNodes()

    if (weekNodes.isNotEmpty()) {
      compose.onNodeWithText("Week", ignoreCase = true).performClick()
      compose.waitForIdle()
      assertAnyTextPresent("This week", "Upcoming")(compose)
    } else if (monthNodes.isNotEmpty()) {
      compose.onNodeWithText("Month", ignoreCase = true).performClick()
      compose.waitForIdle()
      assertAnyTextPresent("Most important", "This month", "Upcoming")(compose)
    } else {
      // No toggle in this build; ensure screen is alive:
      assertAnyTextPresent("Upcoming", "Agenda", "Events")(compose)
    }
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
