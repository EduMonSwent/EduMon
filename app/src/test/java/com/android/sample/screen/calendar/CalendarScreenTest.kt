package com.android.sample.screen.calendar

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.SemanticsNodeInteractionCollection
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.android.sample.R
import com.android.sample.ui.calendar.CalendarScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CalendarScreenRobolectricTest {

  @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun calendarScreen_renders_and_showsUpcomingTitle_inEitherMode() {
    compose.setContent { CalendarScreen() }
    compose.waitForIdle()

    val upcomingMonth = compose.activity.getString(R.string.upcoming_events)
    val upcomingWeek = compose.activity.getString(R.string.upcoming_events_week)

    val foundMonth =
        compose
            .onAllNodes(hasText(upcomingMonth, substring = true, ignoreCase = true))
            .fetchSemanticsNodes()
            .isNotEmpty()

    val foundWeek =
        compose
            .onAllNodes(hasText(upcomingWeek, substring = true, ignoreCase = true))
            .fetchSemanticsNodes()
            .isNotEmpty()

    check(foundMonth || foundWeek) {
      "Could not find any upcoming section title. Tried localized strings: " +
          "[$upcomingMonth, $upcomingWeek]"
    }
  }

  @Test
  fun calendarScreen_can_toggle_mode_if_toggle_exists() {
    compose.setContent { CalendarScreen() }
    compose.waitForIdle()

    val upcomingMonth = compose.activity.getString(R.string.upcoming_events)
    val upcomingWeek = compose.activity.getString(R.string.upcoming_events_week)

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
      compose
          .onAllNodes(hasText(upcomingWeek, substring = true, ignoreCase = true))
          .assertCountGreaterThan(0)
    } else if (monthNodes.isNotEmpty()) {
      compose.onNodeWithText("Month", ignoreCase = true).performClick()
      compose.waitForIdle()
      compose
          .onAllNodes(hasText(upcomingMonth, substring = true, ignoreCase = true))
          .assertCountGreaterThan(0)
    } else {
      // No toggle in this build; do not fail.
    }
  }
}

/** ---- helpers ---- */
private fun SemanticsNodeInteractionCollection.assertCountGreaterThan(min: Int) {
  val count = this.fetchSemanticsNodes().size
  check(count > min) { "Expected more than $min matching nodes, but found $count." }
}
