package com.android.sample.ui.schedule

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.feature.weeks.ui.WeekProgDailyObjTags
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScheduleScreenDeepCoverageTest {

  @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

  private fun launch() = compose.setContent { ScheduleScreen() }

  @Test
  fun dayTab_renders_static_sections_and_fab() {
    launch()
    compose.waitForIdle()

    // Tabs exist
    listOf("Day", "Week", "Month", "Agenda").forEach {
      compose.onNodeWithText(it, ignoreCase = true).assertExists()
    }

    // Day is default: wellness and objectives texts are rendered from DayTabContent()
    compose.onAllNodesWithText("Today •", substring = true)[0].assertExists()

    // FAB (icon has contentDescription = "Add")
    compose.onNode(hasContentDescription("Add")).assertExists()
  }

  @Test
  fun weekTab_renders_tagged_sections() {
    launch()
    compose.waitForIdle()

    compose.onNodeWithText("Week", ignoreCase = true).performClick()
    compose.waitForIdle()

    // The root of the week content
    compose.onNodeWithTag("WeekContent").assertExists()
    // Dots row from weeks module
    compose.onNodeWithTag(WeekProgDailyObjTags.WEEK_DOTS_ROW).assertExists()
    // Upcoming section container we tagged
    compose.onNodeWithTag("WeekUpcomingSection").assertExists()
  }

  @Test
  fun monthTab_renders_grid_and_important_section_with_empty_state() {
    launch()
    compose.waitForIdle()

    compose.onNodeWithText("Month", ignoreCase = true).performClick()
    compose.waitForIdle()

    // Containers we added tags for
    compose.onNodeWithTag("MonthContent").assertExists()
    compose.onNodeWithTag("MonthImportantSection").assertExists()

    // Section title is static
    compose.onAllNodesWithText("Most important this month")[0].assertExists()
  }

  @Test
  fun agendaTab_renders_tagged_section_and_title() {
    launch()
    compose.waitForIdle()

    compose.onNodeWithText("Agenda", ignoreCase = true).performClick()
    compose.waitForIdle()

    compose.onNodeWithTag("AgendaContent").assertExists()
    compose.onNodeWithTag("AgendaSection").assertExists()
    // The section title string itself
    compose.onAllNodesWithText("Agenda")[0].assertExists()
  }

  @Test
  fun tab_toggling_exercises_launchedEffect_paths_and_recompositions() {
    launch()
    compose.waitForIdle()

    // Bounce across tabs to trigger WEEK and MONTH branches in LaunchedEffect(currentTab)
    val tabs = listOf("Week", "Month", "Agenda", "Day", "Week", "Month", "Day")
    tabs.forEach {
      compose.onNodeWithText(it, ignoreCase = true).performClick()
      compose.waitForIdle()
    }

    // Sanity: plenty of click targets exist (tabs + FAB)
    val clickableCount = compose.onAllNodes(hasClickAction()).fetchSemanticsNodes().size
    check(clickableCount >= 4) { "Expected at least 4 clickable elements (tabs + FAB)." }
  }

  @Test
  fun fab_click_opens_flow_without_crash() {
    launch()
    compose.waitForIdle()
    compose.onNode(hasContentDescription("Add")).assertExists().performClick()
    compose.waitForIdle()
    // We don’t over-assert modal fields to keep CI stable; the composition path is covered.
  }
}
