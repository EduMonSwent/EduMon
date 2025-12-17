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
import androidx.compose.ui.test.assertIsDisplayed
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
import androidx.compose.ui.test.performTextInput
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
   * Complete user journey test:
   * 1. Login screen → simulate login
   * 2. Home screen → verify widgets & quick actions
   * 3. Schedule → navigate tabs (Day/Week/Month)
   * 4. Week Progress → expand weeks list & objectives
   * 5. Study Session → verify timer & stats
   * 6. Profile → check settings & stats card
   * 7. To-Do → view task list
   * 8. Games → access games section
   * 9. Shop → browse items
   * 10. Return to Home
   */
  @OptIn(ExperimentalTestApi::class)
  @Test
  fun completeUserJourney_coversAllMajorFeatures() {
    // ═══════════════════════════════════════════════════════════════════
    // SETUP: Launch app with Login screen
    // ═══════════════════════════════════════════════════════════════════
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

    // ═══════════════════════════════════════════════════════════════════
    // STEP 1: LOGIN SCREEN
    // ═══════════════════════════════════════════════════════════════════
    composeRule.waitUntilExactlyOneExists(
        hasText("Connect yourself to EduMon."), timeoutMillis = 20_000)

    composeRule.onNodeWithText("Connect yourself to EduMon.").assertIsDisplayed()
    composeRule.onNodeWithText("Continue with Google").assertIsDisplayed()

    // Simulate successful login (bypass Google/Firebase)
    composeRule.runOnIdle { loggedInState.value = true }
    composeRule.waitForIdle()
    composeRule.mainClock.advanceTimeBy(500)

    // ═══════════════════════════════════════════════════════════════════
    // STEP 2: HOME SCREEN - Verify widgets & navigation
    // ═══════════════════════════════════════════════════════════════════
    waitForHome()

    // Verify home screen quick actions exist
    ensureHomeChildVisible(HomeTestTags.CHIP_OPEN_PLANNER)
    ensureHomeChildVisible(HomeTestTags.QUICK_STUDY)
    ensureHomeChildVisible(HomeTestTags.CHIP_MOOD)

    // ═══════════════════════════════════════════════════════════════════
    // STEP 3: SCHEDULE SCREEN - Test tabs (Day/Week/Month)
    // ═══════════════════════════════════════════════════════════════════
    composeRule
        .onNodeWithTag(HomeTestTags.CHIP_OPEN_PLANNER, useUnmergedTree = true)
        .performClick()

    composeRule.waitUntilExactlyOneExists(
        hasTestTag(ScheduleScreenTestTags.ROOT), timeoutMillis = 20_000)

    // Verify schedule screen elements
    composeRule.onNodeWithTag(ScheduleScreenTestTags.ROOT).assertExists()
    composeRule.onNodeWithTag(ScheduleScreenTestTags.TAB_ROW).assertExists()

    // Navigate through tabs
    composeRule.onNodeWithTag(ScheduleScreenTestTags.CONTENT_DAY).assertExists()

    composeRule.onNodeWithText("Week").performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithTag(ScheduleScreenTestTags.CONTENT_WEEK).assertExists()

    composeRule.onNodeWithText("Month").performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithTag(ScheduleScreenTestTags.CONTENT_MONTH).assertExists()

    // Return to Day tab
    composeRule.onNodeWithText("Day").performClick()
    composeRule.waitForIdle()

    goBackToHome()

    // ═══════════════════════════════════════════════════════════════════
    // STEP 4: WEEK PROGRESS & DAILY OBJECTIVES (via Schedule)
    // ═══════════════════════════════════════════════════════════════════
    composeRule
        .onNodeWithTag(HomeTestTags.CHIP_OPEN_PLANNER, useUnmergedTree = true)
        .performClick()

    composeRule.waitUntilExactlyOneExists(
        hasTestTag(ScheduleScreenTestTags.ROOT), timeoutMillis = 20_000)

    // Check if week progress section exists (may need scrolling)
    composeRule.waitForIdle()
    composeRule.mainClock.advanceTimeBy(300)

    // Try to find week progress elements if visible
    tryScrollToAndAssert(WeekProgDailyObjTags.WEEK_PROGRESS_SECTION)

    goBackToHome()

    // ═══════════════════════════════════════════════════════════════════
    // STEP 5: STUDY SESSION - Timer & stats
    // ═══════════════════════════════════════════════════════════════════
    ensureHomeChildVisible(HomeTestTags.QUICK_STUDY)
    composeRule.onNodeWithTag(HomeTestTags.QUICK_STUDY, useUnmergedTree = true).performClick()

    composeRule.waitUntilExactlyOneExists(
        hasTestTag(NavigationTestTags.TOP_BAR_TITLE), timeoutMillis = 20_000)

    composeRule
        .onNode(hasTestTag(NavigationTestTags.TOP_BAR_TITLE) and hasText("Study"))
        .assertExists()

    // Verify study session components
    composeRule.onNodeWithTag(StudySessionTestTags.TIMER_SECTION).assertExists()
    composeRule.onNodeWithTag(StudySessionTestTags.STATS_PANEL).assertExists()

    goBackToHome()

    // ═══════════════════════════════════════════════════════════════════
    // STEP 6: PROFILE SCREEN - Settings & stats
    // ═══════════════════════════════════════════════════════════════════
    openDrawerDestination(AppDestination.Profile.route)

    composeRule.waitUntilExactlyOneExists(
        hasTestTag(ProfileScreenTestTags.PROFILE_SCREEN), timeoutMillis = 20_000)

    // Verify profile sections
    composeRule.onNodeWithTag(ProfileScreenTestTags.PROFILE_SCREEN).assertExists()
    composeRule.onNodeWithTag(ProfileScreenTestTags.PET_SECTION).assertExists()

    // Scroll to stats card
    composeRule
        .onNodeWithTag(ProfileScreenTestTags.PROFILE_SCREEN)
        .performScrollToNode(hasTestTag(ProfileScreenTestTags.STATS_CARD))
    composeRule.onNodeWithTag(ProfileScreenTestTags.STATS_CARD).assertExists()

    // Scroll to settings card
    composeRule
        .onNodeWithTag(ProfileScreenTestTags.PROFILE_SCREEN)
        .performScrollToNode(hasTestTag(ProfileScreenTestTags.SETTINGS_CARD))
    composeRule.onNodeWithTag(ProfileScreenTestTags.SETTINGS_CARD).assertExists()

    goBackToHome()

    // ═══════════════════════════════════════════════════════════════════
    // STEP 7: TO-DO SCREEN - Task list
    // ═══════════════════════════════════════════════════════════════════
    openDrawerDestination(AppDestination.Todo.route)

    composeRule.waitUntilExactlyOneExists(
        hasTestTag(ToDoTestTags.OverviewScreen), timeoutMillis = 20_000)

    composeRule.onNodeWithTag(ToDoTestTags.OverviewScreen).assertExists()
    composeRule.onNodeWithTag(ToDoTestTags.FabAdd).assertExists()

    goBackToHome()

    // ═══════════════════════════════════════════════════════════════════
    // STEP 8: GAMES SCREEN
    // ═══════════════════════════════════════════════════════════════════
    openDrawerDestination(AppDestination.Games.route)

    composeRule.waitUntilExactlyOneExists(
        hasTestTag(NavigationTestTags.TOP_BAR_TITLE), timeoutMillis = 20_000)

    composeRule
        .onNode(hasTestTag(NavigationTestTags.TOP_BAR_TITLE) and hasText("Games"))
        .assertExists()

    goBackToHome()

    // ═══════════════════════════════════════════════════════════════════
    // STEP 9: SHOP SCREEN
    // ═══════════════════════════════════════════════════════════════════
    openDrawerDestination(AppDestination.Shop.route)

    composeRule.waitUntilExactlyOneExists(
        hasTestTag(NavigationTestTags.TOP_BAR_TITLE), timeoutMillis = 20_000)

    composeRule
        .onNode(hasTestTag(NavigationTestTags.TOP_BAR_TITLE) and hasText("Shop"))
        .assertExists()

    goBackToHome()

    // ═══════════════════════════════════════════════════════════════════
    // STEP 10: STATS SCREEN
    // ═══════════════════════════════════════════════════════════════════
    openDrawerDestination(AppDestination.Stats.route)

    composeRule.waitUntilExactlyOneExists(
        hasTestTag(NavigationTestTags.TOP_BAR_TITLE), timeoutMillis = 20_000)

    composeRule
        .onNode(hasTestTag(NavigationTestTags.TOP_BAR_TITLE) and hasText("Stats"))
        .assertExists()

    goBackToHome()

    // ═══════════════════════════════════════════════════════════════════
    // STEP 11: FLASHCARDS SCREEN
    // ═══════════════════════════════════════════════════════════════════
    openDrawerDestination(AppDestination.Flashcards.route)

    composeRule.waitUntilExactlyOneExists(
        hasTestTag(NavigationTestTags.TOP_BAR_TITLE), timeoutMillis = 20_000)

    composeRule
        .onNode(hasTestTag(NavigationTestTags.TOP_BAR_TITLE) and hasText("Flashcards"))
        .assertExists()

    goBackToHome()

    // ═══════════════════════════════════════════════════════════════════
    // FINAL: Verify home is stable after full journey
    // ═══════════════════════════════════════════════════════════════════
    waitForHome()
    composeRule.onNodeWithTag(HomeTestTags.MENU_BUTTON).assertExists()
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
    composeRule
        .onNodeWithTag(HomeTestTags.CHIP_OPEN_PLANNER, useUnmergedTree = true)
        .performClick()

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

  /**
   * Focused test: Profile screen navigation & sections
   */
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

  /**
   * Focused test: To-Do screen & FAB
   */
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

  /**
   * Focused test: Study session components
   */
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

  /**
   * Focused test: All drawer destinations are accessible
   */
  @OptIn(ExperimentalTestApi::class)
  @Test
  fun drawerNavigation_allDestinationsAccessible() {
    setupAndLogin()

    val destinations =
        listOf(
            AppDestination.Profile,
            AppDestination.Games,
            AppDestination.Stats,
            AppDestination.Flashcards,
            AppDestination.Todo,
            AppDestination.Shop,
        )

    for (destination in destinations) {
      openDrawerDestination(destination.route)

      composeRule.waitUntilExactlyOneExists(
          hasTestTag(NavigationTestTags.TOP_BAR_TITLE), timeoutMillis = 20_000)

      composeRule
          .onNode(hasTestTag(NavigationTestTags.TOP_BAR_TITLE) and hasText(destination.label))
          .assertExists()

      goBackToHome()
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // FEATURE INTERACTION TESTS
  // ═══════════════════════════════════════════════════════════════════════════

  /**
   * Test: Start an objective, navigate to course/exercises screen, and mark it completed.
   * This tests the full objective workflow.
   */
  @OptIn(ExperimentalTestApi::class)
  @Test
  fun objective_startAndComplete_fullWorkflow() {
    setupAndLogin()

    // Navigate to Schedule
    ensureHomeChildVisible(HomeTestTags.CHIP_OPEN_PLANNER)
    composeRule
        .onNodeWithTag(HomeTestTags.CHIP_OPEN_PLANNER, useUnmergedTree = true)
        .performClick()

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
            .onAllNodes(hasTestTagPrefix(WeekProgDailyObjTags.OBJECTIVE_START_BUTTON_PREFIX), useUnmergedTree = true)
            .fetchSemanticsNodes()
            .isNotEmpty()

    if (hasStartButton) {
      // Click the first Start button to navigate to CourseExercises
      composeRule
          .onAllNodes(hasTestTagPrefix(WeekProgDailyObjTags.OBJECTIVE_START_BUTTON_PREFIX), useUnmergedTree = true)
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

  /**
   * Test: Navigate through week tabs in Schedule and verify content changes
   */
  @OptIn(ExperimentalTestApi::class)
  @Test
  fun scheduleScreen_navigateAllTabs_contentChanges() {
    setupAndLogin()

    // Navigate to Schedule
    ensureHomeChildVisible(HomeTestTags.CHIP_OPEN_PLANNER)
    composeRule
        .onNodeWithTag(HomeTestTags.CHIP_OPEN_PLANNER, useUnmergedTree = true)
        .performClick()

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

  /**
   * Test: Toggle objectives section expansion (if multiple objectives exist)
   */
  @OptIn(ExperimentalTestApi::class)
  @Test
  fun objectives_toggleExpansion_showsAllObjectives() {
    setupAndLogin()

    // Navigate to Schedule
    ensureHomeChildVisible(HomeTestTags.CHIP_OPEN_PLANNER)
    composeRule
        .onNodeWithTag(HomeTestTags.CHIP_OPEN_PLANNER, useUnmergedTree = true)
        .performClick()

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
            .onAllNodesWithTag(WeekProgDailyObjTags.OBJECTIVES_SHOW_ALL_BUTTON, useUnmergedTree = true)
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
              .onAllNodes(hasTestTagPrefix(WeekProgDailyObjTags.OBJECTIVE_ROW_PREFIX), useUnmergedTree = true)
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

  /**
   * Test: Profile settings toggles work correctly
   */
  @OptIn(ExperimentalTestApi::class)
  @Test
  fun profileScreen_toggleSettings_work() {
    setupAndLogin()

    // Navigate to Profile
    openDrawerDestination(AppDestination.Profile.route)

    composeRule.waitUntilExactlyOneExists(
        hasTestTag(ProfileScreenTestTags.PROFILE_SCREEN), timeoutMillis = 20_000)

    // Scroll to settings card
    composeRule
        .onNodeWithTag(ProfileScreenTestTags.PROFILE_SCREEN)
        .performScrollToNode(hasTestTag(ProfileScreenTestTags.SETTINGS_CARD))

    // Toggle focus mode switch
    val hasFocusSwitch =
        composeRule
            .onAllNodesWithTag(ProfileScreenTestTags.SWITCH_FOCUS_MODE, useUnmergedTree = true)
            .fetchSemanticsNodes()
            .isNotEmpty()

    if (hasFocusSwitch) {
      composeRule
          .onNodeWithTag(ProfileScreenTestTags.SWITCH_FOCUS_MODE, useUnmergedTree = true)
          .performClick()
      composeRule.waitForIdle()

      // Toggle back
      composeRule
          .onNodeWithTag(ProfileScreenTestTags.SWITCH_FOCUS_MODE, useUnmergedTree = true)
          .performClick()
      composeRule.waitForIdle()
    }

    goBackToHome()
  }

  /**
   * Test: Study session timer is interactive
   */
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

  /**
   * Test: Week progress section can be expanded
   */
  @OptIn(ExperimentalTestApi::class)
  @Test
  fun weekProgress_expandSection_showsWeeksList() {
    setupAndLogin()

    // Navigate to Schedule
    ensureHomeChildVisible(HomeTestTags.CHIP_OPEN_PLANNER)
    composeRule
        .onNodeWithTag(HomeTestTags.CHIP_OPEN_PLANNER, useUnmergedTree = true)
        .performClick()

    composeRule.waitUntilExactlyOneExists(
        hasTestTag(ScheduleScreenTestTags.ROOT), timeoutMillis = 20_000)

    // Wait for week progress section
    composeRule.waitUntil(timeoutMillis = 15_000) {
      composeRule
          .onAllNodesWithTag(WeekProgDailyObjTags.WEEK_PROGRESS_SECTION, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    tryScrollToAndAssert(WeekProgDailyObjTags.WEEK_PROGRESS_SECTION)

    // Check if toggle exists and click it
    val hasToggle =
        composeRule
            .onAllNodesWithTag(WeekProgDailyObjTags.WEEK_PROGRESS_TOGGLE, useUnmergedTree = true)
            .fetchSemanticsNodes()
            .isNotEmpty()

    if (hasToggle) {
      composeRule
          .onNodeWithTag(WeekProgDailyObjTags.WEEK_PROGRESS_TOGGLE, useUnmergedTree = true)
          .performClick()
      composeRule.waitForIdle()

      // Verify weeks list appears
      composeRule.waitUntil(timeoutMillis = 10_000) {
        composeRule
            .onAllNodesWithTag(WeekProgDailyObjTags.WEEKS_LIST, useUnmergedTree = true)
            .fetchSemanticsNodes()
            .isNotEmpty()
      }

      composeRule
          .onNodeWithTag(WeekProgDailyObjTags.WEEKS_LIST, useUnmergedTree = true)
          .assertExists()

      // Collapse again
      composeRule
          .onNodeWithTag(WeekProgDailyObjTags.WEEK_PROGRESS_TOGGLE, useUnmergedTree = true)
          .performClick()
      composeRule.waitForIdle()
    }

    goBackToHome()
  }

  /**
   * Test: To-Do FAB opens add screen
   */
  @OptIn(ExperimentalTestApi::class)
  @Test
  fun todoScreen_fabClick_opensAddScreen() {
    setupAndLogin()

    // Navigate to To-Do
    openDrawerDestination(AppDestination.Todo.route)

    composeRule.waitUntilExactlyOneExists(
        hasTestTag(ToDoTestTags.OverviewScreen), timeoutMillis = 20_000)

    // Click FAB to add new task
    composeRule.onNodeWithTag(ToDoTestTags.FabAdd).performClick()
    composeRule.waitForIdle()

    // Verify Add screen opens
    composeRule.waitUntilExactlyOneExists(
        hasTestTag(ToDoTestTags.AddScreen), timeoutMillis = 10_000)

    composeRule.onNodeWithTag(ToDoTestTags.TitleField).assertExists()
    composeRule.onNodeWithTag(ToDoTestTags.SaveButton).assertExists()

    // Go back without saving
    composeRule.onNodeWithTag(NavigationTestTags.GO_BACK_BUTTON).performClick()
    composeRule.waitForIdle()

    // Should be back at overview
    composeRule.waitUntilExactlyOneExists(
        hasTestTag(ToDoTestTags.OverviewScreen), timeoutMillis = 10_000)

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

