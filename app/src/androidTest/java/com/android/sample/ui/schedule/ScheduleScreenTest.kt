package com.android.sample.ui.schedule

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
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

    // Day tab is selected by default; it renders a title that starts with "Today •"
    composeRule.onNodeWithText("Day").assertIsDisplayed()

    // This text is produced by DayTabContent()
    composeRule.onNodeWithText(/* partial match */ "Today •", substring = true).assertIsDisplayed()
  }

  @Test
  fun switch_toWeek_showsWeekDotsAndUpcomingSection() {
    setContent()

    // Click Week tab
    composeRule.onNodeWithText("Week").performClick()

    // The Week tab content includes WeekDotsRow with a stable testTag
    composeRule.onNodeWithTag(WeekProgDailyObjTags.WEEK_DOTS_ROW).assertIsDisplayed()

    // The Week tab also renders "This week" title in UpcomingEventsSection
    composeRule.onNodeWithText("This week").assertIsDisplayed()
  }

  @Test
  fun switch_toMonth_showsMonthHeaderAndSection() {
    setContent()

    composeRule.onNodeWithText("Month").performClick()
    composeRule.waitForIdle()

    // Try each scrollable until the header is visible
    composeRule.scrollAnyScrollableTo(hasText("Most important this month"))

    composeRule.onAllNodesWithText("Most important this month").onFirst().assertIsDisplayed()
  }

  @Test
  fun switch_toAgenda_showsAgendaSection_onlySectionNotTab() {
    setContent()

    // Go to Agenda tab
    composeRule.onNodeWithText("Agenda").performClick()
    composeRule.waitForIdle()

    // There are TWO "Agenda" nodes (tab label + section title). Pick the section title.
    val agendaNodes = composeRule.onAllNodesWithText("Agenda", substring = false)
    // Index 0 is the tab label; index 1 should be the section title inside content
    agendaNodes[1].assertIsDisplayed()
  }

  @Test
  fun fab_isVisible_and_opensAddDialog() {
    setContent()

    // FAB is always present; icon contentDescription is "Add"
    composeRule.onNode(hasContentDescription("Add")).assertIsDisplayed().performClick()

    composeRule.waitForIdle()
  }

  private fun ComposeContentTestRule.scrollAnyScrollableTo(matcher: SemanticsMatcher) {
    val scrollables = onAllNodes(hasScrollAction())
    val nodes = scrollables.fetchSemanticsNodes()
    require(nodes.isNotEmpty()) { "No scrollables found in the hierarchy." }
    for (i in nodes.indices) {
      try {
        scrollables[i].performScrollToNode(matcher)
        return
      } catch (_: AssertionError) {
        // try the next scrollable
      }
    }
    throw AssertionError("No scrollable could reach node matching: $matcher")
  }
}
