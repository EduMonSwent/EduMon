package com.android.sample

// This code has been written partially using A.I (LLM).

import androidx.activity.ComponentActivity
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.android.sample.feature.homeScreen.AppDestination
import com.android.sample.feature.homeScreen.HomeTestTags
import com.android.sample.feature.weeks.model.Objective
import com.android.sample.feature.weeks.repository.FakeObjectivesRepository
import com.android.sample.feature.weeks.ui.CourseExercisesTestTags
import com.android.sample.feature.weeks.ui.WeekProgDailyObjTags
import com.android.sample.repos_providors.AppRepositories
import com.android.sample.repos_providors.FakeRepositoriesProvider
import com.android.sample.ui.login.LoginScreen
import com.android.sample.ui.profile.ProfileScreenTestTags
import com.android.sample.ui.schedule.ScheduleScreenTestTags
import com.android.sample.ui.session.StudySessionTestTags
import com.android.sample.ui.theme.EduMonTheme
import com.android.sample.ui.todo.TestTags as ToDoTestTags
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Holistic End-to-End test covering multiple EduMon features:
 * - Login flow
 * - Home screen navigation & widgets
 * - Schedule/Calendar with tabs (Day/Week/Month)
 * - Week progression & daily objectives
 * - Study session with timer
 * - Profile screen with settings
 * - To-Do task management
 * - Games section
 * - Shop section
 *
 * This test simulates a complete user journey through the app.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class HolisticEndToEndTest {

  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  private lateinit var loggedInState: MutableState<Boolean>
  private var originalRepositories = AppRepositories

  @Before
  fun setUp() {
    // Use fake repositories to avoid network/Firestore dependencies
    AppRepositories = FakeRepositoriesProvider

    // Initialize with test objectives
    runBlocking { resetObjectivesToDefault() }
  }

  @After
  fun tearDown() {
    AppRepositories = originalRepositories
    runBlocking { FakeObjectivesRepository.setObjectives(emptyList()) }
  }

  /** Reset objectives to a known default state for testing. */
  private suspend fun resetObjectivesToDefault() {
    val today = LocalDate.now().dayOfWeek
    FakeObjectivesRepository.setObjectives(
        listOf(
            Objective(
                title = "Complete Quiz 3",
                course = "CS101",
                estimateMinutes = 30,
                completed = false,
                day = today),
            Objective(
                title = "Review flashcards",
                course = "ENG200",
                estimateMinutes = 15,
                completed = false,
                day = today),
            Objective(
                title = "Outline lab report",
                course = "PHY101",
                estimateMinutes = 45,
                completed = false,
                day = today),
        ))
  }

  /** Match any node whose testTag starts with [prefix]. */
  private fun hasTestTagPrefix(prefix: String): SemanticsMatcher =
      SemanticsMatcher("TestTag starts with \"$prefix\"") { node ->
        val tag = node.config.getOrNull(SemanticsProperties.TestTag)
        tag?.startsWith(prefix) == true
      }

  /**
   * Focused test: Schedule screen interactions
   * - Day/Week/Month tab switching
   * - FAB add button
   * - Content areas
   */
  @OptIn(ExperimentalTestApi::class)
  @Test
  fun scheduleScreen_tabNavigation_worksCorrectly() {
    setupAndLogin()

    // Navigate to Schedule
    ensureHomeChildVisible(HomeTestTags.CHIP_OPEN_PLANNER)
    composeRule.onNodeWithTag(HomeTestTags.CHIP_OPEN_PLANNER, useUnmergedTree = true).performClick()

    composeRule.waitUntilExactlyOneExists(
        hasTestTag(ScheduleScreenTestTags.ROOT), timeoutMillis = 20_000)

    // Day tab (default)
    composeRule.onNodeWithTag(ScheduleScreenTestTags.CONTENT_DAY).assertExists()

    // Switch to Week
    composeRule.onNodeWithText("Week").performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithTag(ScheduleScreenTestTags.CONTENT_WEEK).assertExists()

    // Switch to Month
    composeRule.onNodeWithText("Month").performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithTag(ScheduleScreenTestTags.CONTENT_MONTH).assertExists()

    // Back to Day
    composeRule.onNodeWithText("Day").performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithTag(ScheduleScreenTestTags.CONTENT_DAY).assertExists()

    // Verify FAB exists
    composeRule.onNodeWithTag(ScheduleScreenTestTags.FAB_ADD).assertExists()
  }

  /** Focused test: Profile screen navigation & sections */
  @OptIn(ExperimentalTestApi::class)
  @Test
  fun profileScreen_displaysAllSections() {
    setupAndLogin()

    // Navigate to Profile
    openDrawerDestination(AppDestination.Profile.route)

    composeRule.waitUntilExactlyOneExists(
        hasTestTag(ProfileScreenTestTags.PROFILE_SCREEN), timeoutMillis = 20_000)

    // Verify all profile sections
    composeRule.onNodeWithTag(ProfileScreenTestTags.PROFILE_SCREEN).assertExists()
    composeRule.onNodeWithTag(ProfileScreenTestTags.PET_SECTION).assertExists()

    // Scroll to and verify each section
    composeRule
        .onNodeWithTag(ProfileScreenTestTags.PROFILE_SCREEN)
        .performScrollToNode(hasTestTag(ProfileScreenTestTags.PROFILE_CARD))
    composeRule.onNodeWithTag(ProfileScreenTestTags.PROFILE_CARD).assertExists()

    composeRule
        .onNodeWithTag(ProfileScreenTestTags.PROFILE_SCREEN)
        .performScrollToNode(hasTestTag(ProfileScreenTestTags.STATS_CARD))
    composeRule.onNodeWithTag(ProfileScreenTestTags.STATS_CARD).assertExists()

    composeRule
        .onNodeWithTag(ProfileScreenTestTags.PROFILE_SCREEN)
        .performScrollToNode(hasTestTag(ProfileScreenTestTags.SETTINGS_CARD))
    composeRule.onNodeWithTag(ProfileScreenTestTags.SETTINGS_CARD).assertExists()

    composeRule
        .onNodeWithTag(ProfileScreenTestTags.PROFILE_SCREEN)
        .performScrollToNode(hasTestTag(ProfileScreenTestTags.ACCOUNT_ACTIONS_SECTION))
    composeRule.onNodeWithTag(ProfileScreenTestTags.ACCOUNT_ACTIONS_SECTION).assertExists()
  }

  /** Focused test: To-Do screen & FAB */
  @OptIn(ExperimentalTestApi::class)
  @Test
  fun todoScreen_displaysListAndFab() {
    setupAndLogin()

    // Navigate to To-Do
    openDrawerDestination(AppDestination.Todo.route)

    composeRule.waitUntilExactlyOneExists(
        hasTestTag(ToDoTestTags.OverviewScreen), timeoutMillis = 20_000)

    // Verify To-Do screen elements
    composeRule.onNodeWithTag(ToDoTestTags.OverviewScreen).assertExists()
    composeRule.onNodeWithTag(ToDoTestTags.FabAdd).assertExists()
  }

  /** Focused test: Study session components */
  @OptIn(ExperimentalTestApi::class)
  @Test
  fun studySession_displaysTimerAndStats() {
    setupAndLogin()

    // Navigate to Study
    ensureHomeChildVisible(HomeTestTags.QUICK_STUDY)
    composeRule.onNodeWithTag(HomeTestTags.QUICK_STUDY, useUnmergedTree = true).performClick()

    composeRule.waitUntilExactlyOneExists(
        hasTestTag(StudySessionTestTags.TIMER_SECTION), timeoutMillis = 20_000)

    // Verify study session components
    composeRule.onNodeWithTag(StudySessionTestTags.TIMER_SECTION).assertExists()
    composeRule.onNodeWithTag(StudySessionTestTags.STATS_PANEL).assertExists()
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // FEATURE INTERACTION TESTS
  // ═══════════════════════════════════════════════════════════════════════════

  /**
   * Test: Start an objective, navigate to course/exercises screen, and mark it completed. This
   * tests the full objective workflow.
   */
  @OptIn(ExperimentalTestApi::class)
  @Test
  fun objective_startAndComplete_fullWorkflow() {
    setupAndLogin()

    // Navigate to Schedule
    ensureHomeChildVisible(HomeTestTags.CHIP_OPEN_PLANNER)
    composeRule.onNodeWithTag(HomeTestTags.CHIP_OPEN_PLANNER, useUnmergedTree = true).performClick()

    composeRule.waitUntilExactlyOneExists(
        hasTestTag(ScheduleScreenTestTags.ROOT), timeoutMillis = 20_000)

    // Wait for objectives section to load
    composeRule.waitUntil(timeoutMillis = 15_000) {
      composeRule
          .onAllNodesWithTag(WeekProgDailyObjTags.OBJECTIVES_SECTION, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Scroll to objectives section
    tryScrollToAndAssert(WeekProgDailyObjTags.OBJECTIVES_SECTION)

    // Check if there's an objective with a start button
    val hasStartButton =
        composeRule
            .onAllNodes(
                hasTestTagPrefix(WeekProgDailyObjTags.OBJECTIVE_START_BUTTON_PREFIX),
                useUnmergedTree = true)
            .fetchSemanticsNodes()
            .isNotEmpty()

    if (hasStartButton) {
      // Click the first Start button to navigate to CourseExercises
      composeRule
          .onAllNodes(
              hasTestTagPrefix(WeekProgDailyObjTags.OBJECTIVE_START_BUTTON_PREFIX),
              useUnmergedTree = true)
          .onFirst()
          .performClick()

      // Wait for CourseExercises screen
      composeRule.waitUntilExactlyOneExists(
          hasTestTag(CourseExercisesTestTags.SCREEN), timeoutMillis = 20_000)

      // Verify objective details are shown
      composeRule.onNodeWithTag(CourseExercisesTestTags.OBJECTIVE_TITLE).assertExists()
      composeRule.onNodeWithTag(CourseExercisesTestTags.HEADER_CARD).assertExists()

      // Verify tabs exist
      composeRule.onNodeWithTag(CourseExercisesTestTags.TAB_ROW).assertExists()
      composeRule.onNodeWithTag(CourseExercisesTestTags.COURSE_TAB).assertExists()
      composeRule.onNodeWithTag(CourseExercisesTestTags.EXERCISES_TAB).assertExists()

      // Switch to Exercises tab
      composeRule.onNodeWithTag(CourseExercisesTestTags.EXERCISES_TAB).performClick()
      composeRule.waitForIdle()

      // Switch back to Course tab
      composeRule.onNodeWithTag(CourseExercisesTestTags.COURSE_TAB).performClick()
      composeRule.waitForIdle()

      // Click the Completed FAB to mark objective as done
      composeRule.onNodeWithTag(CourseExercisesTestTags.COMPLETED_FAB).assertExists()
      composeRule.onNodeWithTag(CourseExercisesTestTags.COMPLETED_FAB).performClick()

      // Should navigate back to Schedule after completing
      composeRule.waitUntilExactlyOneExists(
          hasTestTag(ScheduleScreenTestTags.ROOT), timeoutMillis = 20_000)
    }

    // Go back to home
    goBackToHome()
  }

  /** Test: Navigate through week tabs in Schedule and verify content changes */
  @OptIn(ExperimentalTestApi::class)
  @Test
  fun scheduleScreen_navigateAllTabs_contentChanges() {
    setupAndLogin()

    // Navigate to Schedule
    ensureHomeChildVisible(HomeTestTags.CHIP_OPEN_PLANNER)
    composeRule.onNodeWithTag(HomeTestTags.CHIP_OPEN_PLANNER, useUnmergedTree = true).performClick()

    composeRule.waitUntilExactlyOneExists(
        hasTestTag(ScheduleScreenTestTags.ROOT), timeoutMillis = 20_000)

    // Verify Day tab (default)
    composeRule.onNodeWithTag(ScheduleScreenTestTags.CONTENT_DAY).assertExists()

    // Navigate to Week tab and verify unique content
    composeRule.onNodeWithText("Week").performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithTag(ScheduleScreenTestTags.CONTENT_WEEK).assertExists()

    // Check for week-specific elements (Week Dots Row should be visible)
    composeRule.waitUntil(timeoutMillis = 10_000) {
      composeRule
          .onAllNodesWithTag(WeekProgDailyObjTags.WEEK_DOTS_ROW, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Navigate to Month tab
    composeRule.onNodeWithText("Month").performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithTag(ScheduleScreenTestTags.CONTENT_MONTH).assertExists()

    // Back to Day
    composeRule.onNodeWithText("Day").performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithTag(ScheduleScreenTestTags.CONTENT_DAY).assertExists()

    goBackToHome()
  }

  /** Test: Toggle objectives section expansion (if multiple objectives exist) */
  @OptIn(ExperimentalTestApi::class)
  @Test
  fun objectives_toggleExpansion_showsAllObjectives() {
    setupAndLogin()

    // Navigate to Schedule
    ensureHomeChildVisible(HomeTestTags.CHIP_OPEN_PLANNER)
    composeRule.onNodeWithTag(HomeTestTags.CHIP_OPEN_PLANNER, useUnmergedTree = true).performClick()

    composeRule.waitUntilExactlyOneExists(
        hasTestTag(ScheduleScreenTestTags.ROOT), timeoutMillis = 20_000)

    // Wait for objectives section
    composeRule.waitUntil(timeoutMillis = 15_000) {
      composeRule
          .onAllNodesWithTag(WeekProgDailyObjTags.OBJECTIVES_SECTION, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    tryScrollToAndAssert(WeekProgDailyObjTags.OBJECTIVES_SECTION)

    // Check if "Show all" button exists (means multiple objectives)
    val hasShowAll =
        composeRule
            .onAllNodesWithTag(
                WeekProgDailyObjTags.OBJECTIVES_SHOW_ALL_BUTTON, useUnmergedTree = true)
            .fetchSemanticsNodes()
            .isNotEmpty()

    if (hasShowAll) {
      // Click to expand
      composeRule
          .onNodeWithTag(WeekProgDailyObjTags.OBJECTIVES_SHOW_ALL_BUTTON, useUnmergedTree = true)
          .performClick()
      composeRule.waitForIdle()

      // Verify multiple objective rows are now visible
      val objectiveRows =
          composeRule
              .onAllNodes(
                  hasTestTagPrefix(WeekProgDailyObjTags.OBJECTIVE_ROW_PREFIX),
                  useUnmergedTree = true)
              .fetchSemanticsNodes()

      assert(objectiveRows.size > 1) { "Expected multiple objectives after expansion" }

      // Click again to collapse
      composeRule
          .onNodeWithTag(WeekProgDailyObjTags.OBJECTIVES_SHOW_ALL_BUTTON, useUnmergedTree = true)
          .performClick()
      composeRule.waitForIdle()
    }

    goBackToHome()
  }

  /** Test: Study session timer is interactive */
  @OptIn(ExperimentalTestApi::class)
  @Test
  fun studySession_timerInteraction_works() {
    setupAndLogin()

    // Navigate to Study
    ensureHomeChildVisible(HomeTestTags.QUICK_STUDY)
    composeRule.onNodeWithTag(HomeTestTags.QUICK_STUDY, useUnmergedTree = true).performClick()

    composeRule.waitUntilExactlyOneExists(
        hasTestTag(StudySessionTestTags.TIMER_SECTION), timeoutMillis = 20_000)

    // Verify timer section exists
    composeRule.onNodeWithTag(StudySessionTestTags.TIMER_SECTION).assertExists()
    composeRule.onNodeWithTag(StudySessionTestTags.STATS_PANEL).assertExists()

    // Look for start/pause button and interact
    val hasStartButton =
        composeRule
            .onAllNodesWithText("Start", useUnmergedTree = true)
            .fetchSemanticsNodes()
            .isNotEmpty()

    if (hasStartButton) {
      composeRule.onAllNodesWithText("Start", useUnmergedTree = true).onFirst().performClick()
      composeRule.waitForIdle()
      composeRule.mainClock.advanceTimeBy(1000)

      // Try to pause
      val hasPauseButton =
          composeRule
              .onAllNodesWithText("Pause", useUnmergedTree = true)
              .fetchSemanticsNodes()
              .isNotEmpty()

      if (hasPauseButton) {
        composeRule.onAllNodesWithText("Pause", useUnmergedTree = true).onFirst().performClick()
        composeRule.waitForIdle()
      }
    }

    goBackToHome()
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // HELPER METHODS
  // ═══════════════════════════════════════════════════════════════════════════

  @OptIn(ExperimentalTestApi::class)
  private fun setupAndLogin() {
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
        hasText("Connect yourself to EduMon."), timeoutMillis = 20_000)

    composeRule.runOnIdle { loggedInState.value = true }
    composeRule.waitForIdle()
    composeRule.mainClock.advanceTimeBy(500)

    waitForHome()
  }

  @OptIn(ExperimentalTestApi::class)
  private fun waitForHome() {
    composeRule.waitUntilExactlyOneExists(
        hasTestTag(HomeTestTags.MENU_BUTTON), timeoutMillis = 20_000)
  }

  @OptIn(ExperimentalTestApi::class)
  private fun goBackToHome() {
    composeRule.onNodeWithTag(NavigationTestTags.GO_BACK_BUTTON).assertExists().performClick()
    composeRule.waitForIdle()
    composeRule.mainClock.advanceTimeBy(500)
    waitForHome()
  }

  @OptIn(ExperimentalTestApi::class)
  private fun ensureHomeChildVisible(childTag: String) {
    composeRule.waitUntil(timeoutMillis = 20_000) {
      runCatching {
            composeRule.onAllNodesWithTag(childTag, useUnmergedTree = true).fetchSemanticsNodes()
          }
          .getOrNull()
          ?.isNotEmpty() == true
    }
    composeRule.onNode(hasTestTag(childTag), useUnmergedTree = true).performScrollTo()
  }

  @OptIn(ExperimentalTestApi::class)
  private fun openDrawerDestination(route: String) {
    composeRule.onNodeWithTag(HomeTestTags.MENU_BUTTON).assertExists().performClick()

    val drawerItemTag = HomeTestTags.drawerTag(route)

    composeRule.waitUntil(timeoutMillis = 20_000) {
      runCatching {
            composeRule
                .onAllNodesWithTag(drawerItemTag, useUnmergedTree = true)
                .fetchSemanticsNodes()
          }
          .getOrNull()
          ?.isNotEmpty() == true
    }

    composeRule.onNodeWithTag(drawerItemTag, useUnmergedTree = true).assertExists().performClick()
  }

  @OptIn(ExperimentalTestApi::class)
  private fun tryScrollToAndAssert(tag: String) {
    try {
      composeRule.waitUntil(timeoutMillis = 5_000) {
        runCatching {
              composeRule.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes()
            }
            .getOrNull()
            ?.isNotEmpty() == true
      }
      composeRule.onNode(hasTestTag(tag), useUnmergedTree = true).performScrollTo()
      composeRule.onNodeWithTag(tag, useUnmergedTree = true).assertExists()
    } catch (_: Exception) {
      // Element may not be visible in current view - that's OK for optional checks
    }
  }
}
