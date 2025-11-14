package com.android.sample

// This code has been written partially using A.I (LLM).

import androidx.activity.ComponentActivity
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.android.sample.feature.homeScreen.AppDestination
import com.android.sample.feature.homeScreen.HomeTestTags
import com.android.sample.ui.calendar.CalendarScreenTestTags
import com.android.sample.ui.login.LoginScreen
import com.android.sample.ui.planner.PlannerScreenTestTags
import com.android.sample.ui.session.StudySessionTestTags
import com.android.sample.ui.theme.EduMonTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class HomePlannerCalendarStudyEndToEndTest {

  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  private lateinit var loggedInState: MutableState<Boolean>

  @OptIn(ExperimentalTestApi::class)
  @Test
  fun login_home_planner_calendar_study_flow_works_end_to_end() {
    // Single setContent for the whole flow
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

    composeRule.waitUntilExactlyOneExists(
        hasText("Connect yourself to EduMon."), timeoutMillis = 5_000)
    composeRule.onNodeWithText("Connect yourself to EduMon.").assertExists()
    composeRule.onNodeWithText("Continue with Google").assertExists()

    composeRule.runOnIdle { loggedInState.value = true }

    // 1) HOME + NAV HOST
    composeRule.waitUntilExactlyOneExists(
        hasTestTag(HomeTestTags.MENU_BUTTON), timeoutMillis = 10_000)
    composeRule.waitUntilExactlyOneExists(
        hasTestTag(NavigationTestTags.NAV_HOST), timeoutMillis = 10_000)

    composeRule.onNodeWithTag(HomeTestTags.MENU_BUTTON).assertExists()

    composeRule.onNodeWithTag(HomeTestTags.CHIP_OPEN_PLANNER).assertExists().performClick()

    composeRule.waitUntilExactlyOneExists(
        hasTestTag(PlannerScreenTestTags.PLANNER_SCREEN), timeoutMillis = 5_000)

    composeRule.onNodeWithTag(PlannerScreenTestTags.PLANNER_SCREEN).assertExists()

    composeRule.onNodeWithTag(PlannerScreenTestTags.PET_HEADER).assertExists()
    composeRule.onNodeWithTag(PlannerScreenTestTags.TODAY_CLASSES_SECTION).assertExists()

    composeRule
        .onNodeWithTag(PlannerScreenTestTags.PLANNER_SCREEN)
        .performScrollToNode(hasTestTag(PlannerScreenTestTags.WELLNESS_CAMPUS_SECTION))
    composeRule.onNodeWithTag(PlannerScreenTestTags.WELLNESS_CAMPUS_SECTION).assertExists()

    // 3) Open Add Study Task modal
    composeRule.onNodeWithTag("addTaskFab").assertExists().performClick()

    composeRule.waitUntilExactlyOneExists(
        hasTestTag(PlannerScreenTestTags.ADD_TASK_MODAL), timeoutMillis = 5_000)
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

    composeRule.waitUntil(timeoutMillis = 5_000) {
      composeRule
          .onAllNodesWithTag(PlannerScreenTestTags.ADD_TASK_MODAL)
          .fetchSemanticsNodes()
          .isEmpty()
    }

    composeRule.onNodeWithTag(NavigationTestTags.GO_BACK_BUTTON).assertExists().performClick()

    composeRule.waitUntilExactlyOneExists(
        hasTestTag(HomeTestTags.MENU_BUTTON), timeoutMillis = 5_000)
    composeRule.onNodeWithTag(HomeTestTags.MENU_BUTTON).assertExists()

    // 5) Home -> Calendar via bottom nav
    composeRule
        .onNodeWithTag(HomeTestTags.bottomTag(AppDestination.Calendar.route))
        .assertExists()
        .performClick()

    composeRule.waitUntilExactlyOneExists(
        hasTestTag(CalendarScreenTestTags.CALENDAR_CARD), timeoutMillis = 5_000)

    composeRule.onNodeWithTag(CalendarScreenTestTags.CALENDAR_HEADER).assertExists()
    composeRule.onNodeWithTag(CalendarScreenTestTags.CALENDAR_CARD).assertExists()

    composeRule
        .onNodeWithTag(CalendarScreenTestTags.VIEW_TOGGLE_BUTTON)
        .assertExists()
        .performClick()

    composeRule.onNodeWithTag(CalendarScreenTestTags.CALENDAR_CARD).assertExists()

    composeRule.onNodeWithTag(NavigationTestTags.GO_BACK_BUTTON).assertExists().performClick()

    composeRule.waitForIdle()
    composeRule.mainClock.advanceTimeBy(500)
    composeRule.waitForIdle()

    composeRule.waitUntilExactlyOneExists(
        hasTestTag(HomeTestTags.MENU_BUTTON), timeoutMillis = 20_000)

    // 7) Home -> Study via quick action
    // Scroll the home scrollable content until QUICK_STUDY is in view
    composeRule.onNode(hasScrollAction()).performScrollToNode(hasTestTag(HomeTestTags.QUICK_STUDY))

    composeRule.onNodeWithTag(HomeTestTags.QUICK_STUDY).assertExists().performClick()

    composeRule.waitUntilExactlyOneExists(
        hasTestTag(NavigationTestTags.TOP_BAR_TITLE), timeoutMillis = 5_000)

    composeRule
        .onNode(hasTestTag(NavigationTestTags.TOP_BAR_TITLE) and hasText("Study"))
        .assertExists()

    // Study session core UI
    composeRule.onNodeWithTag(StudySessionTestTags.TIMER_SECTION).assertExists()
    composeRule.onNodeWithTag(StudySessionTestTags.STATS_PANEL).assertExists()

    // 8) Study -> Home
    composeRule.onNodeWithTag(NavigationTestTags.GO_BACK_BUTTON).assertExists().performClick()

    composeRule.waitUntilExactlyOneExists(
        hasTestTag(HomeTestTags.MENU_BUTTON), timeoutMillis = 5_000)
    composeRule.onNodeWithTag(HomeTestTags.MENU_BUTTON).assertExists()
  }
}
