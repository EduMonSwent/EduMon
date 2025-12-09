package com.android.sample.schedule

// This code has been written partially using A.I (LLM).

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.feature.weeks.model.Objective
import com.android.sample.feature.weeks.repository.FakeObjectivesRepository
import com.android.sample.feature.weeks.ui.CourseExercisesTestTags
import com.android.sample.feature.weeks.ui.WeekProgDailyObjTags
import com.android.sample.repos_providors.AppRepositories
import com.android.sample.repos_providors.FakeRepositoriesProvider
import com.android.sample.repos_providors.RepositoriesProvider
import com.android.sample.ui.schedule.ScheduleScreen
import com.android.sample.ui.schedule.ScheduleScreenTestTags
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests for ScheduleScreen verifying:
 * - Basic rendering and tab navigation
 * - Objectives section display (requires scrolling)
 * - Navigation to CourseExercises screen
 * - CourseExercises interactions (tabs, back, complete)
 */
@RunWith(AndroidJUnit4::class)
class ScheduleScreenCourseExercisesNavigationTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private lateinit var previousRepositories: RepositoriesProvider

  @Before
  fun setUp() {
    // Use in-memory fakes for all repositories during these tests
    previousRepositories = AppRepositories
    AppRepositories = FakeRepositoriesProvider

    runBlocking { resetObjectives() }
  }

  @After
  fun tearDown() {
    AppRepositories = previousRepositories
    runBlocking { FakeObjectivesRepository.setObjectives(emptyList()) }
  }

  /** Reset objectives to default incomplete state. */
  private suspend fun resetObjectives() {
    val today = LocalDate.now().dayOfWeek
    val tomorrow = today.plus(1)
    val afterTomorrow = today.plus(2)

    FakeObjectivesRepository.setObjectives(
        listOf(
            Objective(
                title = "Finish Quiz 3",
                course = "CS101",
                estimateMinutes = 30,
                completed = false,
                day = today),
            Objective(
                title = "Outline lab report",
                course = "CS101",
                estimateMinutes = 20,
                completed = false,
                day = tomorrow),
            Objective(
                title = "Review 15 flashcards",
                course = "ENG200",
                estimateMinutes = 10,
                completed = false,
                day = afterTomorrow),
        ))
  }

  /** Match any node whose testTag starts with [prefix]. */
  private fun hasTestTagPrefix(prefix: String): SemanticsMatcher =
      SemanticsMatcher("TestTag starts with \"$prefix\"") { node ->
        val tag = node.config.getOrNull(SemanticsProperties.TestTag)
        tag?.startsWith(prefix) == true
      }

  // ---------------------------------------------------------------------------
  // Basic rendering
  // ---------------------------------------------------------------------------

  @Test
  fun scheduleScreen_renders_withoutCrashing() {
    composeTestRule.setContent { ScheduleScreen() }

    composeTestRule.onNodeWithTag(ScheduleScreenTestTags.ROOT).assertExists()
  }

  @Test
  fun scheduleScreen_showsTabRow() {
    composeTestRule.setContent { ScheduleScreen() }

    composeTestRule.onNodeWithTag(ScheduleScreenTestTags.TAB_ROW).assertExists()
  }

  @Test
  fun scheduleScreen_showsObjectivesSection() {
    composeTestRule.setContent { ScheduleScreen() }

    composeTestRule.waitUntil(10_000) {
      composeTestRule
          .onAllNodesWithTag(WeekProgDailyObjTags.OBJECTIVES_SECTION)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule.onNodeWithTag(WeekProgDailyObjTags.OBJECTIVES_SECTION).assertExists()
  }

  @Test
  fun scheduleScreen_showsFabButton() {
    composeTestRule.setContent { ScheduleScreen() }

    composeTestRule.waitUntil(10_000) {
      composeTestRule
          .onAllNodesWithTag(ScheduleScreenTestTags.FAB_ADD)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule.onNodeWithTag(ScheduleScreenTestTags.FAB_ADD).assertExists()
  }

  @Test
  fun scheduleScreen_defaultTab_isDay() {
    composeTestRule.setContent { ScheduleScreen() }

    composeTestRule.waitUntil(10_000) {
      composeTestRule
          .onAllNodesWithTag(ScheduleScreenTestTags.CONTENT_DAY, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule
        .onNodeWithTag(ScheduleScreenTestTags.CONTENT_DAY, useUnmergedTree = true)
        .assertExists()
  }

  // ---------------------------------------------------------------------------
  // Objectives section
  // ---------------------------------------------------------------------------

  @Test
  fun objectiveSection_hasAtLeastOneObjective_orEmptyState() {
    composeTestRule.setContent { ScheduleScreen() }

    composeTestRule.waitUntil(10_000) {
      composeTestRule
          .onAllNodesWithTag(WeekProgDailyObjTags.OBJECTIVES_SECTION)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    val hasObjectiveRow =
        composeTestRule
            .onAllNodes(
                hasTestTagPrefix(WeekProgDailyObjTags.OBJECTIVE_ROW_PREFIX), useUnmergedTree = true)
            .fetchSemanticsNodes()
            .isNotEmpty()

    val hasEmptyState =
        composeTestRule
            .onAllNodesWithTag(WeekProgDailyObjTags.OBJECTIVES_EMPTY, useUnmergedTree = true)
            .fetchSemanticsNodes()
            .isNotEmpty()

    assert(hasObjectiveRow || hasEmptyState) {
      "Should have either an objective row or empty state"
    }
  }

  @Test
  fun objectiveWithStartButton_exists() {
    composeTestRule.setContent { ScheduleScreen() }
    composeTestRule.waitForIdle()

    composeTestRule.waitUntil(10_000) {
      composeTestRule
          .onAllNodes(
              hasTestTagPrefix(WeekProgDailyObjTags.OBJECTIVE_START_BUTTON_PREFIX),
              useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule
        .onAllNodes(
            hasTestTagPrefix(WeekProgDailyObjTags.OBJECTIVE_START_BUTTON_PREFIX),
            useUnmergedTree = true)
        .onFirst()
        .assertExists()
  }

  // ---------------------------------------------------------------------------
  // Tab switching
  // ---------------------------------------------------------------------------

  @Test
  fun scheduleScreen_canSwitchBackToDayTab() {
    composeTestRule.setContent { ScheduleScreen() }

    // First, switch to Week tab
    composeTestRule.onAllNodesWithText("Week")[0].performClick()

    composeTestRule.waitUntil(10_000) {
      composeTestRule
          .onAllNodesWithTag(ScheduleScreenTestTags.CONTENT_WEEK, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Now switch back to Day tab
    composeTestRule.onAllNodesWithText("Day")[0].performClick()

    composeTestRule.waitUntil(10_000) {
      composeTestRule
          .onAllNodesWithTag(ScheduleScreenTestTags.CONTENT_DAY, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule
        .onNodeWithTag(ScheduleScreenTestTags.CONTENT_DAY, useUnmergedTree = true)
        .assertExists()
  }

  // ========== Objectives Section Tests (requires scrolling) ==========

  @Test
  fun scheduleScreen_switchBetweenTabs_maintainsState() {
    composeTestRule.setContent { ScheduleScreen() }

    composeTestRule.waitUntil(10_000) {
      composeTestRule
          .onAllNodesWithTag(WeekProgDailyObjTags.OBJECTIVES_SECTION)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    composeTestRule.onNodeWithTag(WeekProgDailyObjTags.OBJECTIVES_SECTION).performScrollTo()

    composeTestRule.onAllNodesWithText("Week")[0].performClick()
    composeTestRule.onAllNodesWithText("Day")[0].performClick()

    composeTestRule
        .onNodeWithTag(
            WeekProgDailyObjTags.OBJECTIVE_START_BUTTON_PREFIX + 0, useUnmergedTree = true)
        .assertExists()
  }

  // ---------------------------------------------------------------------------
  // Navigation to CourseExercises
  // ---------------------------------------------------------------------------

  @Test
  fun clickingStartButton_showsCourseExercisesScreen() {
    composeTestRule.setContent { ScheduleScreen() }
    composeTestRule.waitForIdle()

    composeTestRule.waitUntil(10_000) {
      composeTestRule
          .onAllNodes(
              hasTestTagPrefix(WeekProgDailyObjTags.OBJECTIVE_START_BUTTON_PREFIX),
              useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule
        .onAllNodes(
            hasTestTagPrefix(WeekProgDailyObjTags.OBJECTIVE_START_BUTTON_PREFIX),
            useUnmergedTree = true)
        .onFirst()
        .performScrollTo()
        .performClick()

    composeTestRule.mainClock.advanceTimeBy(1_000)
    composeTestRule.waitUntil(15_000) {
      composeTestRule
          .onAllNodesWithTag(CourseExercisesTestTags.SCREEN, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule
        .onNodeWithTag(CourseExercisesTestTags.SCREEN, useUnmergedTree = true)
        .assertExists()
  }

  // ========== CourseExercises Screen Tests ==========

  @Test
  fun courseExercises_displaysObjectiveDetails() {
    composeTestRule.setContent { ScheduleScreen() }

    composeTestRule.waitUntil(10_000) {
      composeTestRule
          .onAllNodes(
              hasTestTagPrefix(WeekProgDailyObjTags.OBJECTIVE_START_BUTTON_PREFIX),
              useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule
        .onAllNodes(
            hasTestTagPrefix(WeekProgDailyObjTags.OBJECTIVE_START_BUTTON_PREFIX),
            useUnmergedTree = true)
        .onFirst()
        .performScrollTo()
        .performClick()

    composeTestRule.mainClock.advanceTimeBy(1_000)

    composeTestRule.waitUntil(15_000) {
      composeTestRule
          .onAllNodesWithTag(CourseExercisesTestTags.OBJECTIVE_TITLE, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule
        .onNodeWithTag(CourseExercisesTestTags.OBJECTIVE_TITLE, useUnmergedTree = true)
        .assertExists()
        .assertTextContains("Finish Quiz 3")
  }

  @Test
  fun courseExercises_backButton_returnsToSchedule() {
    composeTestRule.setContent { ScheduleScreen() }
    composeTestRule.waitForIdle()

    composeTestRule.waitUntil(10_000) {
      composeTestRule
          .onAllNodes(
              hasTestTagPrefix(WeekProgDailyObjTags.OBJECTIVE_START_BUTTON_PREFIX),
              useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule
        .onAllNodes(
            hasTestTagPrefix(WeekProgDailyObjTags.OBJECTIVE_START_BUTTON_PREFIX),
            useUnmergedTree = true)
        .onFirst()
        .performScrollTo()
        .performClick()

    composeTestRule.mainClock.advanceTimeBy(1_000)

    composeTestRule.waitUntil(15_000) {
      composeTestRule
          .onAllNodesWithTag(CourseExercisesTestTags.BACK_BUTTON, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule
        .onNodeWithTag(CourseExercisesTestTags.BACK_BUTTON, useUnmergedTree = true)
        .performClick()

    composeTestRule.mainClock.advanceTimeBy(1_000)

    composeTestRule.waitUntil(15_000) {
      composeTestRule
          .onAllNodesWithTag(ScheduleScreenTestTags.ROOT)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule.onNodeWithTag(ScheduleScreenTestTags.ROOT).assertExists()
  }

  @Test
  fun courseExercises_completedButton_marksObjectiveComplete() {
    composeTestRule.setContent { ScheduleScreen() }

    composeTestRule.waitUntil(10_000) {
      composeTestRule
          .onAllNodes(
              hasTestTagPrefix(WeekProgDailyObjTags.OBJECTIVE_START_BUTTON_PREFIX),
              useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule
        .onAllNodes(
            hasTestTagPrefix(WeekProgDailyObjTags.OBJECTIVE_START_BUTTON_PREFIX),
            useUnmergedTree = true)
        .onFirst()
        .performScrollTo()
        .performClick()

    composeTestRule.mainClock.advanceTimeBy(1_000)

    composeTestRule.waitUntil(15_000) {
      composeTestRule
          .onAllNodesWithTag(CourseExercisesTestTags.COMPLETED_FAB, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule
        .onNodeWithTag(CourseExercisesTestTags.COMPLETED_FAB, useUnmergedTree = true)
        .performClick()

    composeTestRule.mainClock.advanceTimeBy(1_000)

    composeTestRule.waitUntil(15_000) {
      composeTestRule
          .onAllNodesWithTag(ScheduleScreenTestTags.ROOT)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule
        .onNodeWithTag(ScheduleScreenTestTags.ROOT, useUnmergedTree = true)
        .assertExists()

    // Start button for the first objective should no longer exist
    composeTestRule
        .onAllNodes(
            hasTestTagPrefix(WeekProgDailyObjTags.OBJECTIVE_START_BUTTON_PREFIX),
            useUnmergedTree = true)
        .assertCountEquals(0)
  }

  @Test
  fun courseExercises_tabRow_exists() {
    composeTestRule.setContent { ScheduleScreen() }

    composeTestRule.waitUntil(10_000) {
      composeTestRule
          .onAllNodes(
              hasTestTagPrefix(WeekProgDailyObjTags.OBJECTIVE_START_BUTTON_PREFIX),
              useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule
        .onAllNodes(
            hasTestTagPrefix(WeekProgDailyObjTags.OBJECTIVE_START_BUTTON_PREFIX),
            useUnmergedTree = true)
        .onFirst()
        .performScrollTo()
        .performClick()

    composeTestRule.mainClock.advanceTimeBy(1_000)

    composeTestRule.waitUntil(15_000) {
      composeTestRule
          .onAllNodesWithTag(CourseExercisesTestTags.SCREEN, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule
        .onNodeWithTag(CourseExercisesTestTags.TAB_ROW, useUnmergedTree = true)
        .assertExists()
    composeTestRule
        .onNodeWithTag(CourseExercisesTestTags.COURSE_TAB, useUnmergedTree = true)
        .assertExists()
    composeTestRule
        .onNodeWithTag(CourseExercisesTestTags.EXERCISES_TAB, useUnmergedTree = true)
        .assertExists()
  }
}
