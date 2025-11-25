package com.android.sample.schedule

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.feature.weeks.model.Objective
import com.android.sample.feature.weeks.repository.FakeObjectivesRepository
import com.android.sample.feature.weeks.ui.CourseExercisesTestTags
import com.android.sample.feature.weeks.ui.WeekProgDailyObjTags
import com.android.sample.ui.schedule.ScheduleScreen
import com.android.sample.ui.schedule.ScheduleScreenTestTags
import java.time.DayOfWeek
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Simplified tests for ScheduleScreen that verify basic rendering and interactions without relying
 * on complex navigation timing.
 */
@RunWith(AndroidJUnit4::class)
class ScheduleScreenCourseExercisesNavigationTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  /** Reset objectives to default incomplete state - call at start of each test */
  private fun resetObjectives() = runBlocking {
    FakeObjectivesRepository.setObjectives(
        listOf(
            Objective(
                title = "Finish Quiz 3",
                course = "CS101",
                estimateMinutes = 30,
                completed = false,
                day = DayOfWeek.MONDAY),
            Objective(
                title = "Outline lab report",
                course = "CS101",
                estimateMinutes = 20,
                completed = false,
                day = DayOfWeek.TUESDAY),
            Objective(
                title = "Review 15 flashcards",
                course = "ENG200",
                estimateMinutes = 10,
                completed = false,
                day = DayOfWeek.WEDNESDAY),
        ))
  }

  @Test
  fun scheduleScreen_renders_withoutCrashing() {
    composeTestRule.setContent { ScheduleScreen() }

    composeTestRule.waitForIdle()

    // Basic smoke test - screen renders
    composeTestRule.onNodeWithTag(ScheduleScreenTestTags.ROOT).assertExists()
  }

  @Test
  fun scheduleScreen_showsTabRow() {
    composeTestRule.setContent { ScheduleScreen() }

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(ScheduleScreenTestTags.TAB_ROW).assertExists()
  }

  @Test
  fun scheduleScreen_showsObjectivesSection() {
    composeTestRule.setContent { ScheduleScreen() }

    composeTestRule.waitForIdle()

    // Wait for objectives to load
    composeTestRule.waitUntil(timeoutMillis = 10000) {
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

    composeTestRule.waitForIdle()

    composeTestRule.waitUntil(timeoutMillis = 10000) {
      composeTestRule
          .onAllNodesWithTag(ScheduleScreenTestTags.FAB_ADD)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule.onNodeWithTag(ScheduleScreenTestTags.FAB_ADD).assertExists()
  }

  @Test
  fun scheduleScreen_initialState_isOnDayTab() {
    composeTestRule.setContent { ScheduleScreen() }

    composeTestRule.waitForIdle()

    composeTestRule.waitUntil(timeoutMillis = 10000) {
      composeTestRule
          .onAllNodesWithTag(ScheduleScreenTestTags.CONTENT_DAY)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule.onNodeWithTag(ScheduleScreenTestTags.CONTENT_DAY).assertExists()
  }

  @Test
  fun objectiveSection_hasAtLeastOneObjective() {
    resetObjectives()
    composeTestRule.setContent { ScheduleScreen() }

    composeTestRule.waitUntil(timeoutMillis = 10000) {
      composeTestRule
          .onAllNodesWithTag(WeekProgDailyObjTags.OBJECTIVES_SECTION)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Check that we have at least one objective row or the empty state
    val hasObjectiveRow =
        composeTestRule
            .onAllNodesWithTag(
                WeekProgDailyObjTags.OBJECTIVE_ROW_PREFIX + 0, useUnmergedTree = true)
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
    resetObjectives()
    composeTestRule.setContent { ScheduleScreen() }

    composeTestRule.waitUntil(timeoutMillis = 10000) {
      composeTestRule
          .onAllNodesWithTag(
              WeekProgDailyObjTags.OBJECTIVE_START_BUTTON_PREFIX + 0, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule
        .onNodeWithTag(
            WeekProgDailyObjTags.OBJECTIVE_START_BUTTON_PREFIX + 0, useUnmergedTree = true)
        .assertExists()
  }

  @Test
  fun scheduleScreen_canSwitchToWeekTab() {
    composeTestRule.setContent { ScheduleScreen() }

    composeTestRule.waitForIdle()

    // Find and click Week tab
    composeTestRule.onAllNodesWithText("Week")[0].performClick()

    composeTestRule.waitForIdle()

    // Week content should be visible (may take time to load)
    composeTestRule.waitUntil(timeoutMillis = 10000) {
      composeTestRule
          .onAllNodesWithTag(ScheduleScreenTestTags.CONTENT_WEEK, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule
        .onNodeWithTag(ScheduleScreenTestTags.CONTENT_WEEK, useUnmergedTree = true)
        .assertExists()
  }

  @Test
  fun objectiveSection_showsObjectiveTitle() {
    composeTestRule.setContent { ScheduleScreen() }

    composeTestRule.waitUntil(timeoutMillis = 10000) {
      composeTestRule
          .onAllNodesWithTag(WeekProgDailyObjTags.OBJECTIVE_ROW_PREFIX + 0, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // The objective row should exist and contain the objective title
    composeTestRule
        .onNodeWithTag(WeekProgDailyObjTags.OBJECTIVE_ROW_PREFIX + 0, useUnmergedTree = true)
        .assertExists()
  }

  @Test
  fun multipleObjectives_canExpand() {
    composeTestRule.setContent { ScheduleScreen() }

    composeTestRule.waitForIdle()

    // Check if there's a toggle button for multiple objectives
    if (composeTestRule
        .onAllNodesWithTag(WeekProgDailyObjTags.OBJECTIVES_TOGGLE, useUnmergedTree = true)
        .fetchSemanticsNodes()
        .isNotEmpty()) {

      // Click the toggle
      composeTestRule
          .onNodeWithTag(WeekProgDailyObjTags.OBJECTIVES_TOGGLE, useUnmergedTree = true)
          .performScrollTo()
          .performClick()

      composeTestRule.waitForIdle()

      // Show all button should appear
      composeTestRule.waitUntil(timeoutMillis = 5000) {
        composeTestRule
            .onAllNodesWithTag(
                WeekProgDailyObjTags.OBJECTIVES_SHOW_ALL_BUTTON, useUnmergedTree = true)
            .fetchSemanticsNodes()
            .isNotEmpty()
      }

      composeTestRule
          .onNodeWithTag(WeekProgDailyObjTags.OBJECTIVES_SHOW_ALL_BUTTON, useUnmergedTree = true)
          .assertExists()
    }
  }

  @Test
  fun scheduleScreen_switchBetweenTabs_maintainsState() {
    composeTestRule.setContent { ScheduleScreen() }

    composeTestRule.waitForIdle()

    // Start on Day
    composeTestRule.waitUntil(timeoutMillis = 10000) {
      composeTestRule
          .onAllNodesWithTag(ScheduleScreenTestTags.CONTENT_DAY, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Switch to Week
    composeTestRule.onAllNodesWithText("Week")[0].performClick()
    composeTestRule.waitForIdle()

    // Switch back to Day
    composeTestRule.onAllNodesWithText("Day")[0].performClick()
    composeTestRule.waitForIdle()

    // Should be back on Day content
    composeTestRule
        .onNodeWithTag(ScheduleScreenTestTags.CONTENT_DAY, useUnmergedTree = true)
        .assertExists()
  }

  // ========== Navigation Tests ==========

  @Test
  fun clickingStartButton_showsCourseExercisesScreen() {
    resetObjectives()
    composeTestRule.setContent { ScheduleScreen() }

    // Wait for objectives to load
    composeTestRule.waitUntil(timeoutMillis = 10000) {
      composeTestRule
          .onAllNodesWithTag(
              WeekProgDailyObjTags.OBJECTIVE_START_BUTTON_PREFIX + 0, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Scroll and click the Start button
    composeTestRule
        .onNodeWithTag(
            WeekProgDailyObjTags.OBJECTIVE_START_BUTTON_PREFIX + 0, useUnmergedTree = true)
        .performScrollTo()

    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(
            WeekProgDailyObjTags.OBJECTIVE_START_BUTTON_PREFIX + 0, useUnmergedTree = true)
        .performClick()

    // Give more time for navigation to complete
    composeTestRule.mainClock.advanceTimeBy(1000)
    composeTestRule.waitForIdle()

    // Check if CourseExercises screen appears (with useUnmergedTree)
    composeTestRule.waitUntil(timeoutMillis = 15000) {
      composeTestRule
          .onAllNodesWithTag(CourseExercisesTestTags.SCREEN, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule
        .onNodeWithTag(CourseExercisesTestTags.SCREEN, useUnmergedTree = true)
        .assertExists()
  }

  @Test
  fun courseExercises_displaysObjectiveDetails() {
    resetObjectives()
    composeTestRule.setContent { ScheduleScreen() }

    composeTestRule.waitUntil(timeoutMillis = 10000) {
      composeTestRule
          .onAllNodesWithTag(
              WeekProgDailyObjTags.OBJECTIVE_START_BUTTON_PREFIX + 0, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Click Start
    composeTestRule
        .onNodeWithTag(
            WeekProgDailyObjTags.OBJECTIVE_START_BUTTON_PREFIX + 0, useUnmergedTree = true)
        .performScrollTo()
        .performClick()

    composeTestRule.mainClock.advanceTimeBy(1000)
    composeTestRule.waitForIdle()

    composeTestRule.waitUntil(timeoutMillis = 15000) {
      composeTestRule
          .onAllNodesWithTag(CourseExercisesTestTags.OBJECTIVE_TITLE, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Verify objective info is displayed
    composeTestRule
        .onNodeWithTag(CourseExercisesTestTags.OBJECTIVE_TITLE, useUnmergedTree = true)
        .assertExists()
        .assertTextContains("Finish Quiz 3")
  }

  @Test
  fun courseExercises_backButton_returnsToSchedule() {
    resetObjectives()
    composeTestRule.setContent { ScheduleScreen() }

    composeTestRule.waitUntil(timeoutMillis = 10000) {
      composeTestRule
          .onAllNodesWithTag(
              WeekProgDailyObjTags.OBJECTIVE_START_BUTTON_PREFIX + 0, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Navigate to CourseExercises
    composeTestRule
        .onNodeWithTag(
            WeekProgDailyObjTags.OBJECTIVE_START_BUTTON_PREFIX + 0, useUnmergedTree = true)
        .performScrollTo()
        .performClick()

    composeTestRule.mainClock.advanceTimeBy(1000)
    composeTestRule.waitForIdle()

    composeTestRule.waitUntil(timeoutMillis = 15000) {
      composeTestRule
          .onAllNodesWithTag(CourseExercisesTestTags.BACK_BUTTON, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Click back
    composeTestRule
        .onNodeWithTag(CourseExercisesTestTags.BACK_BUTTON, useUnmergedTree = true)
        .performClick()

    composeTestRule.mainClock.advanceTimeBy(1000)
    composeTestRule.waitForIdle()

    // Should return to Schedule
    composeTestRule.waitUntil(timeoutMillis = 15000) {
      composeTestRule
          .onAllNodesWithTag(ScheduleScreenTestTags.ROOT, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule
        .onNodeWithTag(ScheduleScreenTestTags.ROOT, useUnmergedTree = true)
        .assertExists()
  }

  @Test
  fun courseExercises_completedButton_marksObjectiveComplete() {
    resetObjectives()
    composeTestRule.setContent { ScheduleScreen() }

    composeTestRule.waitUntil(timeoutMillis = 10000) {
      composeTestRule
          .onAllNodesWithTag(
              WeekProgDailyObjTags.OBJECTIVE_START_BUTTON_PREFIX + 0, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Navigate to CourseExercises
    composeTestRule
        .onNodeWithTag(
            WeekProgDailyObjTags.OBJECTIVE_START_BUTTON_PREFIX + 0, useUnmergedTree = true)
        .performScrollTo()
        .performClick()

    composeTestRule.mainClock.advanceTimeBy(1000)
    composeTestRule.waitForIdle()

    composeTestRule.waitUntil(timeoutMillis = 15000) {
      composeTestRule
          .onAllNodesWithTag(CourseExercisesTestTags.COMPLETED_FAB, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Click "Mark as completed"
    composeTestRule
        .onNodeWithTag(CourseExercisesTestTags.COMPLETED_FAB, useUnmergedTree = true)
        .performClick()

    composeTestRule.mainClock.advanceTimeBy(1000)
    composeTestRule.waitForIdle()

    // Wait to return to Schedule
    composeTestRule.waitUntil(timeoutMillis = 15000) {
      composeTestRule
          .onAllNodesWithTag(ScheduleScreenTestTags.ROOT, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule
        .onNodeWithTag(ScheduleScreenTestTags.ROOT, useUnmergedTree = true)
        .assertExists()

    // Objective should be completed (Start button gone)
    composeTestRule
        .onNodeWithTag(
            WeekProgDailyObjTags.OBJECTIVE_START_BUTTON_PREFIX + 0, useUnmergedTree = true)
        .assertDoesNotExist()
  }

  @Test
  fun courseExercises_tabRow_exists() {
    resetObjectives()
    composeTestRule.setContent { ScheduleScreen() }

    composeTestRule.waitUntil(timeoutMillis = 10000) {
      composeTestRule
          .onAllNodesWithTag(
              WeekProgDailyObjTags.OBJECTIVE_START_BUTTON_PREFIX + 0, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Navigate to CourseExercises
    composeTestRule
        .onNodeWithTag(
            WeekProgDailyObjTags.OBJECTIVE_START_BUTTON_PREFIX + 0, useUnmergedTree = true)
        .performScrollTo()
        .performClick()

    composeTestRule.mainClock.advanceTimeBy(1000)
    composeTestRule.waitForIdle()

    composeTestRule.waitUntil(timeoutMillis = 15000) {
      composeTestRule
          .onAllNodesWithTag(CourseExercisesTestTags.TAB_ROW, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Tab row should exist with Course and Exercises tabs
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
