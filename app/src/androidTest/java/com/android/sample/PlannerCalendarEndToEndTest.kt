package com.android.sample

// This code has been written partially using A.I (LLM).

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.android.sample.feature.homeScreen.AppDestination
import com.android.sample.feature.homeScreen.HomeTestTags
import com.android.sample.repos_providors.AppRepositories
import com.android.sample.repos_providors.FakeRepositoriesProvider
import com.android.sample.ui.calendar.CalendarScreenTestTags
import com.android.sample.ui.planner.PlannerScreenTestTags
import com.android.sample.ui.session.StudySessionTestTags
import com.android.sample.ui.theme.EduMonTheme
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class HomePlannerCalendarStudyEndToEndTest {

  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  private var originalRepositories = AppRepositories

  @Before
  fun setUp() {
    // Use fake repositories so the flow does not depend on network / Firestore
    AppRepositories = FakeRepositoriesProvider

    composeRule.setContent { EduMonTheme { EduMonNavHost() } }

    // Same strategy as EduMonEndToEndTest: wait until Home is actually rendered.
    waitForHomeContent()
  }

  @After
  fun tearDown() {
    AppRepositories = originalRepositories
  }

  @OptIn(ExperimentalTestApi::class)
  @Test
  fun home_planner_calendar_study_flow_works_end_to_end() {
    // 1) Home -> Planner via "Open planner" chip (with scroll if needed)
    ensureHomeChildVisible(HomeTestTags.CHIP_OPEN_PLANNER)

    composeRule
        .onNodeWithTag(HomeTestTags.CHIP_OPEN_PLANNER, useUnmergedTree = true)
        .assertIsDisplayed()
        .performClick()

    // 2) Planner screen appears
    waitUntilNodeAppears(PlannerScreenTestTags.PLANNER_SCREEN)
    composeRule.onNodeWithTag(PlannerScreenTestTags.PLANNER_SCREEN).assertIsDisplayed()

    // Basic structural checks in Planner
    composeRule.onNodeWithTag(PlannerScreenTestTags.PET_HEADER).assertIsDisplayed()
    composeRule.onNodeWithTag(PlannerScreenTestTags.TODAY_CLASSES_SECTION).assertIsDisplayed()

    // Scroll to WELLNESS_CAMPUS_SECTION inside Planner (LazyColumn bottom)
    composeRule
        .onNodeWithTag(PlannerScreenTestTags.PLANNER_SCREEN)
        .performScrollToNode(hasTestTag(PlannerScreenTestTags.WELLNESS_CAMPUS_SECTION))
    composeRule.onNodeWithTag(PlannerScreenTestTags.WELLNESS_CAMPUS_SECTION).assertIsDisplayed()

    // 3) Open Add Study Task modal
    composeRule.onNodeWithTag("addTaskFab").assertIsDisplayed().performClick()

    waitUntilNodeAppears(PlannerScreenTestTags.ADD_TASK_MODAL)
    composeRule.onNodeWithTag(PlannerScreenTestTags.ADD_TASK_MODAL).assertIsDisplayed()

    // Fill form fields
    composeRule.onNodeWithTag(PlannerScreenTestTags.SUBJECT_FIELD).apply {
      performTextClearance()
      performTextInput("Algebra 1")
    }

    composeRule.onNodeWithTag(PlannerScreenTestTags.TASK_TITLE_FIELD).apply {
      performTextClearance()
      performTextInput("Problem set 3")
    }

    composeRule.onNodeWithTag(PlannerScreenTestTags.DURATION_FIELD).apply {
      performTextClearance()
      performTextInput("90")
    }

    composeRule.onNodeWithTag(PlannerScreenTestTags.DEADLINE_FIELD).apply {
      performTextClearance()
      performTextInput("2025-11-20")
    }

    // Submit
    composeRule.onNodeWithTag("aaddTaskButton").assertIsDisplayed().performClick()

    // Wait until modal disappears
    composeRule.waitUntil(timeoutMillis = 10_000) {
      composeRule
          .onAllNodesWithTag(PlannerScreenTestTags.ADD_TASK_MODAL)
          .fetchSemanticsNodes()
          .isEmpty()
    }

    // Back to Home from Planner
    clickGoBack()

    // 4) Home -> Calendar via bottom nav
    composeRule
        .onNodeWithTag(
            HomeTestTags.bottomTag(AppDestination.Calendar.route), useUnmergedTree = true)
        .assertIsDisplayed()
        .performClick()

    waitUntilNodeAppears(CalendarScreenTestTags.CALENDAR_CARD)
    composeRule.onNodeWithTag(CalendarScreenTestTags.CALENDAR_HEADER).assertIsDisplayed()
    composeRule.onNodeWithTag(CalendarScreenTestTags.CALENDAR_CARD).assertIsDisplayed()

    // Toggle view and ensure calendar is still there
    composeRule
        .onNodeWithTag(CalendarScreenTestTags.VIEW_TOGGLE_BUTTON)
        .assertIsDisplayed()
        .performClick()
    composeRule.onNodeWithTag(CalendarScreenTestTags.CALENDAR_CARD).assertIsDisplayed()

    // Back to Home from Calendar
    clickGoBack()

    // 5) Home -> Study via Quick Study action (scroll-safe)
    ensureHomeChildVisible(HomeTestTags.QUICK_STUDY)

    composeRule
        .onNodeWithTag(HomeTestTags.QUICK_STUDY, useUnmergedTree = true)
        .assertIsDisplayed()
        .performClick()

    composeRule.waitUntilExactlyOneExists(
        hasTestTag(NavigationTestTags.TOP_BAR_TITLE), timeoutMillis = 10_000)

    composeRule
        .onNode(hasTestTag(NavigationTestTags.TOP_BAR_TITLE) and hasText("Study"))
        .assertIsDisplayed()

    composeRule.onNodeWithTag(StudySessionTestTags.TIMER_SECTION).assertIsDisplayed()
    composeRule.onNodeWithTag(StudySessionTestTags.STATS_PANEL).assertIsDisplayed()

    // Back to Home from Study
    clickGoBack()

    // Final sanity: Home content is still there
    waitForHomeContent()
  }

  // ----------------- Helpers, aligned with EduMonEndToEndTest style -----------------

  @OptIn(ExperimentalTestApi::class)
  private fun waitForHomeContent() {
    waitUntilNodeAppears(HomeTestTags.TODAY_SEE_ALL)
  }

  @OptIn(ExperimentalTestApi::class)
  private fun waitUntilNodeAppears(tag: String) {
    composeRule.waitUntil(timeoutMillis = 10_000) {
      runCatching {
            composeRule.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes()
          }
          .getOrNull()
          ?.isNotEmpty() == true
    }
  }

  private fun clickGoBack() {
    composeRule.onNodeWithTag(NavigationTestTags.GO_BACK_BUTTON).performClick()
    waitForHomeContent()
  }

  @OptIn(ExperimentalTestApi::class)
  private fun ensureHomeChildVisible(childTag: String) {
    // 1) Wait until the child exists somewhere in the Home semantics tree
    waitUntilNodeAppears(childTag)

    // 2) Find *a* scrollable container on Home
    composeRule.waitUntil(timeoutMillis = 10_000) {
      runCatching {
            composeRule.onAllNodes(hasScrollAction(), useUnmergedTree = true).fetchSemanticsNodes()
          }
          .getOrNull()
          ?.isNotEmpty() == true
    }

    // 3) Scroll the first scrollable container until the child is visible
    composeRule
        .onNode(hasScrollAction(), useUnmergedTree = true)
        .performScrollToNode(hasTestTag(childTag))
  }
}
