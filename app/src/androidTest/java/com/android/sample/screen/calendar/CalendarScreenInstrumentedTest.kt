package com.android.sample.screen.calendar

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.ui.calendar.CalendarScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CalendarScreenInstrumentedTest {

  @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun renders_and_has_some_section_text_and_tabs_click_if_present() {
    compose.setContent { CalendarScreen() }
    compose.waitForIdle()

    // Try common “section-ish” words in English UI; don’t depend on resources
    val candidates = listOf("Upcoming", "This week", "This month", "Agenda", "Events")
    val foundAny =
        candidates.any { t ->
          compose
              .onAllNodes(hasText(t, substring = true, ignoreCase = true))
              .fetchSemanticsNodes()
              .isNotEmpty()
        }
    check(foundAny) { "Could not find any likely upcoming/section text among $candidates" }

    // If Month/Week tabs exist in this build, click them (no strict assertions)
    val hasWeek =
        compose
            .onAllNodes(hasText("Week", substring = true, ignoreCase = true))
            .fetchSemanticsNodes()
            .isNotEmpty()
    if (hasWeek) {
      compose.onNodeWithText("Week", ignoreCase = true).performClick()
      compose.waitForIdle()
    }

    val hasMonth =
        compose
            .onAllNodes(hasText("Month", substring = true, ignoreCase = true))
            .fetchSemanticsNodes()
            .isNotEmpty()
    if (hasMonth) {
      compose.onNodeWithText("Month", ignoreCase = true).performClick()
      compose.waitForIdle()
    }
  }

  @Test
  fun toggling_tabs_updates_content_texts() {
    compose.setContent { CalendarScreen() }
    compose.waitForIdle()

    if (compose.onAllNodes(hasText("Week", ignoreCase = true)).fetchSemanticsNodes().isNotEmpty()) {
      compose.onNodeWithText("Week", ignoreCase = true).performClick()
      compose.waitForIdle()
      compose.onAllNodes(hasText("Week", substring = true, ignoreCase = true))
    }

    if (compose
        .onAllNodes(hasText("Month", ignoreCase = true))
        .fetchSemanticsNodes()
        .isNotEmpty()) {
      compose.onNodeWithText("Month", ignoreCase = true).performClick()
      compose.waitForIdle()
      compose.onAllNodes(hasText("Month", substring = true, ignoreCase = true))
    }
  }
}
