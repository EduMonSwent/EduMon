package com.android.sample.schedule

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
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

  private lateinit var originalRepositories: RepositoriesProvider

  @Before
  fun setup() {
    // Save original repositories and switch to fakes for testing
    originalRepositories = AppRepositories
    AppRepositories = FakeRepositoriesProvider

    // Set up test objectives with PDF URLs for CourseExercises screen
    setupTestObjectives()
  }

  @After
  fun tearDown() {
    // Restore original repositories
    AppRepositories = originalRepositories
  }

  private fun setupTestObjectives() = runBlocking {
    val today = LocalDate.now().dayOfWeek
    FakeObjectivesRepository.setObjectives(
        listOf(
            Objective(
                title = "Finish Quiz 3",
                course = "CS101",
                estimateMinutes = 30,
                completed = false,
                day = today, // Make sure it appears today
                coursePdfUrl = "https://example.com/cs101/course.pdf",
                exercisePdfUrl = "https://example.com/cs101/exercises.pdf"),
            Objective(
                title = "Outline lab report",
                course = "CS101",
                estimateMinutes = 20,
                completed = false,
                day = today,
                coursePdfUrl = "https://example.com/cs101/lab-course.pdf",
                exercisePdfUrl = "https://example.com/cs101/lab-exercises.pdf"),
            Objective(
                title = "Review 15 flashcards",
                course = "ENG200",
                estimateMinutes = 10,
                completed = false,
                day = today,
                coursePdfUrl = "https://example.com/eng200/course.pdf",
                exercisePdfUrl = "https://example.com/eng200/exercises.pdf"),
        ))
  }

  // ========== Basic Rendering Tests ==========

  @Test
  fun scheduleScreen_renders_withoutCrashing() {
    composeTestRule.setContent { ScheduleScreen() }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(ScheduleScreenTestTags.ROOT).assertExists()
  }

  @Test
  fun scheduleScreen_showsTabRow() {
    composeTestRule.setContent { ScheduleScreen() }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(ScheduleScreenTestTags.TAB_ROW).assertExists()
  }

  @Test
  fun scheduleScreen_showsFabButton() {
    composeTestRule.setContent { ScheduleScreen() }
    composeTestRule.waitForIdle()

    composeTestRule.waitUntil(timeoutMillis = 5000) {
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
    composeTestRule.waitForIdle()

    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(ScheduleScreenTestTags.CONTENT_DAY)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule.onNodeWithTag(ScheduleScreenTestTags.CONTENT_DAY).assertExists()
  }

  // ========== Tab Navigation Tests ==========

  @Test
  fun scheduleScreen_canSwitchToWeekTab() {
    composeTestRule.setContent { ScheduleScreen() }
    composeTestRule.waitForIdle()

    // Click Week tab
    composeTestRule.onAllNodesWithText("Week")[0].performClick()
    composeTestRule.waitForIdle()

    composeTestRule.waitUntil(timeoutMillis = 5000) {
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
  fun scheduleScreen_canSwitchBackToDayTab() {
    composeTestRule.setContent { ScheduleScreen() }
    composeTestRule.waitForIdle()

    // Switch to Week
    composeTestRule.onAllNodesWithText("Week")[0].performClick()
    composeTestRule.waitForIdle()

    // Switch back to Day
    composeTestRule.onAllNodesWithText("Day")[0].performClick()
    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(ScheduleScreenTestTags.CONTENT_DAY, useUnmergedTree = true)
        .assertExists()
  }

  // ========== Objectives Section Tests (requires scrolling) ==========

  @Test
  fun objectivesSection_existsAfterScrolling() {
    composeTestRule.setContent { ScheduleScreen() }
    composeTestRule.waitForIdle()

    // Wait for objectives section to load
    composeTestRule.waitUntil(timeoutMillis = 10000) {
      composeTestRule
          .onAllNodesWithTag(WeekProgDailyObjTags.OBJECTIVES_SECTION)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Scroll to make it visible
    composeTestRule.onNodeWithTag(WeekProgDailyObjTags.OBJECTIVES_SECTION).performScrollTo()

    composeTestRule.onNodeWithTag(WeekProgDailyObjTags.OBJECTIVES_SECTION).assertExists()
  }

  @Test
  fun objectivesSection_showsTodaysObjectives() {
    composeTestRule.setContent { ScheduleScreen() }
    composeTestRule.waitForIdle()

    // Wait and scroll to objectives
    composeTestRule.waitUntil(timeoutMillis = 10000) {
      composeTestRule
          .onAllNodesWithTag(WeekProgDailyObjTags.OBJECTIVES_SECTION)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    composeTestRule.onNodeWithTag(WeekProgDailyObjTags.OBJECTIVES_SECTION).performScrollTo()

    // Should have at least one objective
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(WeekProgDailyObjTags.OBJECTIVE_ROW_PREFIX + 0, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule
        .onNodeWithTag(WeekProgDailyObjTags.OBJECTIVE_ROW_PREFIX + 0, useUnmergedTree = true)
        .assertExists()
  }

  @Test
  fun objectiveStartButton_exists() {
    composeTestRule.setContent { ScheduleScreen() }
    composeTestRule.waitForIdle()

    // Wait and scroll to objectives
    composeTestRule.waitUntil(timeoutMillis = 10000) {
      composeTestRule
          .onAllNodesWithTag(WeekProgDailyObjTags.OBJECTIVES_SECTION)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    composeTestRule.onNodeWithTag(WeekProgDailyObjTags.OBJECTIVES_SECTION).performScrollTo()

    // Start button should exist
    composeTestRule.waitUntil(timeoutMillis = 5000) {
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

  // ========== Navigation to CourseExercises Tests ==========

  @Test
  fun clickingStartButton_navigatesToCourseExercisesScreen() {
    composeTestRule.setContent { ScheduleScreen() }
    composeTestRule.waitForIdle()

    // Wait for objectives and scroll
    composeTestRule.waitUntil(timeoutMillis = 10000) {
      composeTestRule
          .onAllNodesWithTag(WeekProgDailyObjTags.OBJECTIVES_SECTION)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    composeTestRule.onNodeWithTag(WeekProgDailyObjTags.OBJECTIVES_SECTION).performScrollTo()

    // Wait for start button
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(
              WeekProgDailyObjTags.OBJECTIVE_START_BUTTON_PREFIX + 0, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Click start button
    composeTestRule
        .onNodeWithTag(
            WeekProgDailyObjTags.OBJECTIVE_START_BUTTON_PREFIX + 0, useUnmergedTree = true)
        .performClick()

    composeTestRule.waitForIdle()

    // CourseExercises screen should appear
    composeTestRule.waitUntil(timeoutMillis = 10000) {
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
  fun courseExercisesScreen_displaysObjectiveTitle() {
    composeTestRule.setContent { ScheduleScreen() }
    composeTestRule.waitForIdle()

    // Navigate to CourseExercises
    navigateToCourseExercises()

    // Verify objective title is displayed
    composeTestRule.waitUntil(timeoutMillis = 5000) {
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
  fun courseExercisesScreen_hasCoursAndExercisesTabs() {
    composeTestRule.setContent { ScheduleScreen() }
    composeTestRule.waitForIdle()

    navigateToCourseExercises()

    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(CourseExercisesTestTags.TAB_ROW, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule
        .onNodeWithTag(CourseExercisesTestTags.COURSE_TAB, useUnmergedTree = true)
        .assertExists()
    composeTestRule
        .onNodeWithTag(CourseExercisesTestTags.EXERCISES_TAB, useUnmergedTree = true)
        .assertExists()
  }

  @Test
  fun courseExercisesScreen_backButton_returnsToSchedule() {
    composeTestRule.setContent { ScheduleScreen() }
    composeTestRule.waitForIdle()

    navigateToCourseExercises()

    // Wait for back button
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(CourseExercisesTestTags.BACK_BUTTON, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Click back
    composeTestRule
        .onNodeWithTag(CourseExercisesTestTags.BACK_BUTTON, useUnmergedTree = true)
        .performClick()
    composeTestRule.waitForIdle()

    // Should return to schedule
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(ScheduleScreenTestTags.ROOT)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule.onNodeWithTag(ScheduleScreenTestTags.ROOT).assertExists()
  }

  @Test
  fun courseExercisesScreen_completedButton_marksObjectiveAsComplete() {
    composeTestRule.setContent { ScheduleScreen() }
    composeTestRule.waitForIdle()

    navigateToCourseExercises()

    // Wait for completed FAB
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(CourseExercisesTestTags.COMPLETED_FAB, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Click completed
    composeTestRule
        .onNodeWithTag(CourseExercisesTestTags.COMPLETED_FAB, useUnmergedTree = true)
        .performClick()
    composeTestRule.waitForIdle()

    // Should return to schedule
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(ScheduleScreenTestTags.ROOT)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // First objective should no longer have start button (it's completed)
    composeTestRule
        .onNodeWithTag(
            WeekProgDailyObjTags.OBJECTIVE_START_BUTTON_PREFIX + 0, useUnmergedTree = true)
        .assertDoesNotExist()
  }

  // ========== Helper Methods ==========

  private fun navigateToCourseExercises() {
    // Wait for objectives section
    composeTestRule.waitUntil(timeoutMillis = 10000) {
      composeTestRule
          .onAllNodesWithTag(WeekProgDailyObjTags.OBJECTIVES_SECTION)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Scroll to objectives
    composeTestRule.onNodeWithTag(WeekProgDailyObjTags.OBJECTIVES_SECTION).performScrollTo()

    // Wait for start button
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(
              WeekProgDailyObjTags.OBJECTIVE_START_BUTTON_PREFIX + 0, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Click start button
    composeTestRule
        .onNodeWithTag(
            WeekProgDailyObjTags.OBJECTIVE_START_BUTTON_PREFIX + 0, useUnmergedTree = true)
        .performClick()

    composeTestRule.waitForIdle()

    // Wait for CourseExercises screen
    composeTestRule.waitUntil(timeoutMillis = 10000) {
      composeTestRule
          .onAllNodesWithTag(CourseExercisesTestTags.SCREEN, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
  }
}
