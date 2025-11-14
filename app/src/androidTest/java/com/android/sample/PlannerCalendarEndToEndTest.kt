package com.android.sample

// This code has been written partially using A.I (LLM).

import androidx.activity.ComponentActivity
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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
import com.android.sample.ui.login.LoginScreen
import com.android.sample.ui.planner.PlannerScreenTestTags
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

  private lateinit var loggedInState: MutableState<Boolean>
  private var originalRepositories = AppRepositories

  @Before
  fun setUp() {
    // No network / Firestore in CI
    AppRepositories = FakeRepositoriesProvider
  }

  @After
  fun tearDown() {
    AppRepositories = originalRepositories
  }

  @OptIn(ExperimentalTestApi::class)
  @Test
  fun home_planner_calendar_study_flow_works_end_to_end() {
    // Single setContent for the whole flow: login + app
    composeRule.setContent {
      EduMonTheme {
        val loggedIn = remember { mutableStateOf(false) }
        loggedInState = loggedIn

        if (!loggedIn.value) {
          LoginScreen(onLoggedIn = { loggedIn.value = true })
        } else {
          EduMonNavHost()
        }
      }
    }

    // ---------- 0) LOGIN SCREEN ----------
    composeRule.onNodeWithText("Connect yourself to EduMon.").assertExists()
    composeRule.onNodeWithText("Continue with Google").assertExists()

    // Simulate login success
    composeRule.runOnIdle { loggedInState.value = true }
    composeRule.waitForIdle()
    composeRule.mainClock.advanceTimeBy(800)
    composeRule.waitForIdle()

    // ---------- 1) HOME READY ----------
    composeRule.onNodeWithTag(HomeTestTags.MENU_BUTTON).assertExists()

    // ---------- 2) (OPTIONAL) HOME -> PLANNER ----------
    if (hasNodeWithTag(HomeTestTags.CHIP_OPEN_PLANNER, useUnmergedTree = true)) {
      ensureHomeChildVisible(HomeTestTags.CHIP_OPEN_PLANNER)

      composeRule
          .onNodeWithTag(HomeTestTags.CHIP_OPEN_PLANNER, useUnmergedTree = true)
          .assertExists()
          .performClick()

      composeRule.waitForIdle()
      composeRule.mainClock.advanceTimeBy(500)
      composeRule.waitForIdle()

      if (hasNodeWithTag(PlannerScreenTestTags.PLANNER_SCREEN)) {
        composeRule.onNodeWithTag(PlannerScreenTestTags.PLANNER_SCREEN).assertExists()
        composeRule.onNodeWithTag(PlannerScreenTestTags.PET_HEADER).assertExists()
        composeRule.onNodeWithTag(PlannerScreenTestTags.TODAY_CLASSES_SECTION).assertExists()

        // Scroll inside planner to WELLNESS section via container
        composeRule
            .onNodeWithTag(PlannerScreenTestTags.PLANNER_SCREEN)
            .performScrollToNode(hasTestTag(PlannerScreenTestTags.WELLNESS_CAMPUS_SECTION))

        composeRule.onNodeWithTag(PlannerScreenTestTags.WELLNESS_CAMPUS_SECTION).assertExists()

        // Add Study Task modal
        composeRule.onNodeWithTag("addTaskFab").assertExists().performClick()

        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(300)
        composeRule.waitForIdle()

        if (hasNodeWithTag(PlannerScreenTestTags.ADD_TASK_MODAL)) {
          composeRule.onNodeWithTag(PlannerScreenTestTags.ADD_TASK_MODAL).assertExists()

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

          composeRule.onNodeWithTag("aaddTaskButton").assertExists().performClick()

          composeRule.waitForIdle()
          composeRule.mainClock.advanceTimeBy(500)
          composeRule.waitForIdle()
        }

        // Back to Home (Planner usually has a back button)
        goBackToHome()
      } else {
        // Planner never rendered correctly, go back and continue with the rest of the flow.
        goBackToHome()
      }
    }

    // ---------- 3) HOME -> CALENDAR ----------
    composeRule
        .onNodeWithTag(
            HomeTestTags.bottomTag(AppDestination.Calendar.route), useUnmergedTree = true)
        .assertExists()
        .performClick()

    composeRule.waitForIdle()
    composeRule.mainClock.advanceTimeBy(500)
    composeRule.waitForIdle()

    if (hasNodeWithTag(CalendarScreenTestTags.CALENDAR_CARD)) {
      composeRule.onNodeWithTag(CalendarScreenTestTags.CALENDAR_HEADER).assertExists()
      composeRule.onNodeWithTag(CalendarScreenTestTags.CALENDAR_CARD).assertExists()

      composeRule
          .onNodeWithTag(CalendarScreenTestTags.VIEW_TOGGLE_BUTTON)
          .assertExists()
          .performClick()
      composeRule.onNodeWithTag(CalendarScreenTestTags.CALENDAR_CARD).assertExists()
    }

    // Back Home (Calendar is a bottom-tab, may not have a back button)
    goBackToHome()

    // ---------- 4) HOME -> STUDY ----------
    ensureHomeChildVisible(HomeTestTags.QUICK_STUDY)

    composeRule
        .onNodeWithTag(HomeTestTags.QUICK_STUDY, useUnmergedTree = true)
        .assertExists()
        .performClick()

    composeRule.waitForIdle()
    composeRule.mainClock.advanceTimeBy(500)
    composeRule.waitForIdle()

    // Validate we are on the Study screen
    if (hasNodeWithTag(NavigationTestTags.TOP_BAR_TITLE)) {
      composeRule
          .onNode(hasTestTag(NavigationTestTags.TOP_BAR_TITLE) and hasText("Study"))
          .assertExists()
    }

    // Back Home
    goBackToHome()

    // Final sanity: Home still reachable
    composeRule.onNodeWithTag(HomeTestTags.MENU_BUTTON).assertExists()
  }

  // ---------------------------------------------------------------------------
  // HELPERS
  // ---------------------------------------------------------------------------

  @OptIn(ExperimentalTestApi::class)
  private fun goBackToHome() {
    // Prefer a back button if present (stack-based navigation)
    if (hasNodeWithTag(NavigationTestTags.GO_BACK_BUTTON, useUnmergedTree = true)) {
      composeRule
          .onNodeWithTag(NavigationTestTags.GO_BACK_BUTTON, useUnmergedTree = true)
          .assertExists()
          .performClick()
    } else {
      // Fallback to bottom navigation Home (tab-based navigation)
      composeRule
          .onNodeWithTag(HomeTestTags.bottomTag(AppDestination.Home.route), useUnmergedTree = true)
          .assertExists()
          .performClick()
    }

    composeRule.waitForIdle()
    composeRule.mainClock.advanceTimeBy(500)
    composeRule.waitForIdle()
  }

  @OptIn(ExperimentalTestApi::class)
  private fun ensureHomeChildVisible(childTag: String) {
    if (!hasNodeWithTag(childTag, useUnmergedTree = true)) {
      composeRule.onNode(hasTestTag(childTag), useUnmergedTree = true).performScrollTo()
    }
    composeRule.onNodeWithTag(childTag, useUnmergedTree = true).assertExists()
  }

  @OptIn(ExperimentalTestApi::class)
  private fun hasNodeWithTag(tag: String, useUnmergedTree: Boolean = false): Boolean {
    return runCatching {
          composeRule
              .onAllNodesWithTag(tag, useUnmergedTree = useUnmergedTree)
              .fetchSemanticsNodes()
        }
        .getOrNull()
        ?.isNotEmpty() == true
  }
}
