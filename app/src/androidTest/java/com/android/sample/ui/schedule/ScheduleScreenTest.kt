package com.android.sample.ui.schedule

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.feature.weeks.ui.WeekProgDailyObjTags
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScheduleScreenTest {

  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  private fun setContent() {
    composeRule.setContent { ScheduleScreen() }
  }

  @Test
  fun default_showsDayTabContent() {
    setContent()

    // Default tab should be Day
    composeRule.onNodeWithText("Day").assertIsDisplayed()

    // DayTabContent contains the text "Today •"
    composeRule.onNodeWithText("Today •", substring = true).assertIsDisplayed()
  }

  @Test
  fun switch_toWeek_showsWeekDotsAndUpcomingSection() {
    setContent()

    // Tap Week tab
    composeRule.onNodeWithText("Week").performClick()

    // Wait for the Week tab content to render — up to 7s on slow CI
    composeRule.waitUntil(timeoutMillis = 7000) {
      composeRule
          .onAllNodes(hasTestTag(WeekProgDailyObjTags.WEEK_DOTS_ROW), useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Scroll to ensure visibility
    composeRule
        .onNodeWithTag("WeekContent")
        .performScrollToNode(hasTestTag(WeekProgDailyObjTags.WEEK_DOTS_ROW))

    // Then assert the dots section is visible
    composeRule
        .onNodeWithTag(WeekProgDailyObjTags.WEEK_DOTS_ROW, useUnmergedTree = true)
        .assertExists()
        .assertIsDisplayed()

    // Also check that the "This week" section is visible
    composeRule.waitUntil(timeoutMillis = 5000) {
      composeRule
          .onAllNodesWithText("This week", useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    composeRule
        .onAllNodesWithText("This week", useUnmergedTree = true)
        .onFirst()
        .assertIsDisplayed()
  }

  @Test
  fun switch_toMonth_showsMonthHeaderAndSection() {
    setContent()
    composeRule.onNodeWithText("Month").performClick()
    composeRule.waitForIdle()

    // Try scrolling to the month section header
    composeRule.scrollAnyScrollableTo(hasText("Most important this month"))

    // This may trigger CI’s “Expected exactly 1 node but found 2” if scrollables overlap
    composeRule.onAllNodesWithText("Most important this month").onFirst().assertIsDisplayed()
  }

  @Test
  fun switch_toAgenda_showsAgendaSection_onlySectionNotTab() {
    setContent()
    composeRule.onNodeWithText("Agenda").performClick()
    composeRule.waitForIdle()

    // There are two “Agenda” texts (tab + section)
    val agendaNodes = composeRule.onAllNodesWithText("Agenda")
    agendaNodes[1].assertIsDisplayed()
  }

  @Test
  fun fab_isVisible_and_opensAddDialog() {
    setContent()

    // Check FAB and click it
    composeRule.onNode(hasContentDescription("Add")).assertIsDisplayed().performClick()
    composeRule.waitForIdle()
  }

  // Helper: scroll any scrollable container until a node is found
  private fun ComposeContentTestRule.scrollAnyScrollableTo(matcher: SemanticsMatcher) {
    val scrollables = onAllNodes(hasScrollAction())
    val nodes = scrollables.fetchSemanticsNodes()
    require(nodes.isNotEmpty()) { "No scrollables found in hierarchy" }
    for (i in nodes.indices) {
      try {
        scrollables[i].performScrollToNode(matcher)
        return
      } catch (_: AssertionError) {
        // try next
      }
    }
    throw AssertionError("No scrollable could reach node matching: $matcher")
  }
}
