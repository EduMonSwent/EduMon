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
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.android.sample.feature.homeScreen.HomeTestTags
import com.android.sample.repos_providors.AppRepositories
import com.android.sample.repos_providors.FakeRepositoriesProvider
import com.android.sample.ui.login.LoginScreen
import com.android.sample.ui.schedule.ScheduleScreenTestTags
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
    waitForHome()

    // ---------- 2) HOME -> SCHEDULE (via "Open Schedule" chip) ----------
    ensureHomeChildVisible(HomeTestTags.CHIP_OPEN_PLANNER)

    composeRule
        .onNodeWithTag(HomeTestTags.CHIP_OPEN_PLANNER, useUnmergedTree = true)
        .assertExists()
        .performClick()

    // Wait for Schedule screen to appear
    composeRule.waitUntilExactlyOneExists(
        hasTestTag(ScheduleScreenTestTags.ROOT),
        timeoutMillis = 20_000,
    )

    composeRule.onNodeWithTag(ScheduleScreenTestTags.ROOT).assertExists()
    composeRule.onNodeWithTag(ScheduleScreenTestTags.TAB_ROW).assertExists()
    composeRule.onNodeWithTag(ScheduleScreenTestTags.FAB_ADD).assertExists()

    // Toggle Day -> Week -> Month
    composeRule.onNodeWithTag(ScheduleScreenTestTags.CONTENT_DAY).assertExists()
    composeRule.onNodeWithText("Week").assertExists().performClick()
    composeRule.onNodeWithTag(ScheduleScreenTestTags.CONTENT_WEEK).assertExists()
    composeRule.onNodeWithText("Month").assertExists().performClick()
    composeRule.onNodeWithTag(ScheduleScreenTestTags.CONTENT_MONTH).assertExists()

    // Back to Home from Schedule
    goBackToHome()

    // ---------- 3) HOME -> STUDY (via Quick Action) ----------
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

    // Back Home from Study
    goBackToHome()

    // Final sanity: Home still reachable
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
}
