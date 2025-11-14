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

  private lateinit var loggedInState: MutableState<Boolean>
  private var originalRepositories = AppRepositories

  @Before
  fun setUp() {
    // Use fake repositories to avoid any network / Firestore dependency in CI
    AppRepositories = FakeRepositoriesProvider
  }

  @After
  fun tearDown() {
    AppRepositories = originalRepositories
  }

  @OptIn(ExperimentalTestApi::class)
  @Test
  fun home_planner_calendar_study_flow_works_end_to_end() {
    // Single setContent for the entire flow: login + app
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

    // 0) LOGIN SCREEN
    composeRule.waitUntilExactlyOneExists(
        hasText("Connect yourself to EduMon."),
        timeoutMillis = 20_000,
    )
    composeRule.onNodeWithText("Connect yourself to EduMon.").assertExists()
    composeRule.onNodeWithText("Continue with Google").assertExists()

    // Simulate login success without hitting Google / Firebase
    composeRule.runOnIdle { loggedInState.value = true }
    composeRule.waitForIdle()
    composeRule.mainClock.advanceTimeBy(500)
    composeRule.waitForIdle()

    // 1) HOME + NAV HOST READY
    waitForHome()

    // 2) HOME -> PLANNER VIA CHIP (with scroll)
    ensureHomeChildVisible(HomeTestTags.CHIP_OPEN_PLANNER)

    composeRule
        .onNodeWithTag(HomeTestTags.CHIP_OPEN_PLANNER, useUnmergedTree = true)
        .assertExists()
        .performClick()

    // Planner screen visible
    waitUntilNodeAppears(PlannerScreenTestTags.PLANNER_SCREEN)
    composeRule.onNodeWithTag(PlannerScreenTestTags.PLANNER_SCREEN).assertExists()

    // Basic Planner structure
    composeRule.onNodeWithTag(PlannerScreenTestTags.PET_HEADER).assertExists()
    composeRule.onNodeWithTag(PlannerScreenTestTags.TODAY_CLASSES_SECTION).assertExists()

    // Scroll to WELLNESS section via the Planner scroll container (IMPORTANT)
    composeRule
        .onNodeWithTag(PlannerScreenTestTags.PLANNER_SCREEN)
        .performScrollToNode(hasTestTag(PlannerScreenTestTags.WELLNESS_CAMPUS_SECTION))

    composeRule.onNodeWithTag(PlannerScreenTestTags.WELLNESS_CAMPUS_SECTION).assertExists()

    // 3) OPEN ADD STUDY TASK MODAL
    composeRule.onNodeWithTag("addTaskFab").assertExists().performClick()

    waitUntilNodeAppears(PlannerScreenTestTags.ADD_TASK_MODAL)
    composeRule.onNodeWithTag(PlannerScreenTestTags.ADD_TASK_MODAL).assertExists()

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
    composeRule.onNodeWithTag("aaddTaskButton").assertExists().performClick()

    // Wait until modal disappears
    composeRule.waitUntil(timeoutMillis = 20_000) {
      composeRule
          .onAllNodesWithTag(PlannerScreenTestTags.ADD_TASK_MODAL)
          .fetchSemanticsNodes()
          .isEmpty()
    }

    // Back to Home from Planner
    goBackToHome()

    // 3) HOME -> CALENDAR VIA BOTTOM NAV
    composeRule
        .onNodeWithTag(
            HomeTestTags.bottomTag(AppDestination.Calendar.route), useUnmergedTree = true)
        .assertExists()
        .performClick()

    composeRule.waitUntilExactlyOneExists(
        hasTestTag(CalendarScreenTestTags.CALENDAR_CARD),
        timeoutMillis = 20_000,
    )

    composeRule.onNodeWithTag(CalendarScreenTestTags.CALENDAR_HEADER).assertExists()
    composeRule.onNodeWithTag(CalendarScreenTestTags.CALENDAR_CARD).assertExists()

    // Toggle view
    composeRule
        .onNodeWithTag(CalendarScreenTestTags.VIEW_TOGGLE_BUTTON)
        .assertExists()
        .performClick()
    composeRule.onNodeWithTag(CalendarScreenTestTags.CALENDAR_CARD).assertExists()

    // Back to Home
    goBackToHome()

    // 4) HOME -> STUDY SESSION VIA QUICK ACTION
    ensureHomeChildVisible(HomeTestTags.QUICK_STUDY)

    composeRule
        .onNodeWithTag(HomeTestTags.QUICK_STUDY, useUnmergedTree = true)
        .assertExists()
        .performClick()

    composeRule.waitUntilExactlyOneExists(
        hasTestTag(NavigationTestTags.TOP_BAR_TITLE),
        timeoutMillis = 20_000,
    )

    composeRule
        .onNode(hasTestTag(NavigationTestTags.TOP_BAR_TITLE) and hasText("Study"))
        .assertExists()

    composeRule.onNodeWithTag(StudySessionTestTags.TIMER_SECTION).assertExists()
    composeRule.onNodeWithTag(StudySessionTestTags.STATS_PANEL).assertExists()

    // Back to Home from Study
    goBackToHome()

    // Final check: Home is still reachable
    waitForHome()
  }

  // ---------------------------------------------------------------------------
  // HELPERS
  // ---------------------------------------------------------------------------

  @OptIn(ExperimentalTestApi::class)
  private fun waitForHome() {
    composeRule.waitUntilExactlyOneExists(
        hasTestTag(HomeTestTags.MENU_BUTTON),
        timeoutMillis = 20_000,
    )
  }

  @OptIn(ExperimentalTestApi::class)
  private fun goBackToHome() {
    composeRule.onNodeWithTag(NavigationTestTags.GO_BACK_BUTTON).assertExists().performClick()

    composeRule.waitForIdle()
    composeRule.mainClock.advanceTimeBy(500)
    composeRule.waitForIdle()

    waitForHome()
  }

  @OptIn(ExperimentalTestApi::class)
  private fun ensureHomeChildVisible(childTag: String) {
    // 1) Wait until *some* node with that tag exists in the semantics tree
    composeRule.waitUntil(timeoutMillis = 20_000) {
      runCatching {
            composeRule.onAllNodesWithTag(childTag, useUnmergedTree = true).fetchSemanticsNodes()
          }
          .getOrNull()
          ?.isNotEmpty() == true
    }

    // 2) Scroll directly to the child. This will scroll its scrollable parent.
    composeRule.onNode(hasTestTag(childTag), useUnmergedTree = true).performScrollTo()
  }

  @OptIn(ExperimentalTestApi::class)
  private fun waitUntilNodeAppears(tag: String) {
    composeRule.waitUntil(timeoutMillis = 20_000) {
      runCatching {
            composeRule.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes()
          }
          .getOrNull()
          ?.isNotEmpty() == true
    }
  }
}
