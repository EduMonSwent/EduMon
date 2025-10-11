package com.android.sample.screen

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.MainActivity
import com.android.sample.R
import com.android.sample.ui.planner.PlannerScreenTestTags
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlannerScreenAndroidTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<MainActivity>()

  // Helper function to wait for conditions with better error handling
  private fun waitForCondition(timeoutMs: Long = 10000, condition: () -> Boolean) {
    composeTestRule.waitUntil(timeoutMs) {
      try {
        condition()
      } catch (e: Exception) {
        false
      }
    }
  }

  @Test
  fun testPlannerScreen_mainComponentsDisplayed() {
    // Use MainActivity's existing content instead of setting new content
    // Just wait for the main screen to load

    waitForCondition {
      composeTestRule
          .onAllNodesWithTag(PlannerScreenTestTags.PLANNER_SCREEN)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Verify main screen structure with more specific matchers
    composeTestRule
        .onNodeWithTag(PlannerScreenTestTags.PLANNER_SCREEN)
        .assertExists()
        .assertIsDisplayed()

    // Check for pet header
    composeTestRule
        .onNodeWithTag(PlannerScreenTestTags.PET_HEADER)
        .assertExists("Pet header should exist")
  }

  @Test
  fun testPlannerScreen_todayClassesListDisplayed() {
    // Wait for any class to appear instead of specific text
    waitForCondition {
      composeTestRule
          .onAllNodesWithText("Lecture", substring = true)
          .fetchSemanticsNodes()
          .isNotEmpty() ||
          composeTestRule
              .onAllNodesWithText("Exercise", substring = true)
              .fetchSemanticsNodes()
              .isNotEmpty() ||
          composeTestRule
              .onAllNodesWithText("Lab", substring = true)
              .fetchSemanticsNodes()
              .isNotEmpty()
    }

    // Use more flexible text matching
    composeTestRule
        .onNodeWithText("Lecture", substring = true)
        .assertExists("Lecture class should exist")
        .assertIsDisplayed()
  }

  @Test
  fun testPlannerScreen_fabOpensAddTaskModal() {
    // Find FAB by content description or other property
    waitForCondition {
      composeTestRule
          .onAllNodesWithContentDescription(
              composeTestRule.activity.getString(R.string.add_study_task))
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Click on FAB
    composeTestRule
        .onNodeWithContentDescription(composeTestRule.activity.getString(R.string.add_study_task))
        .performClick()

    // Wait for modal and verify
    waitForCondition {
      composeTestRule
          .onAllNodesWithTag(PlannerScreenTestTags.ADD_TASK_MODAL)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule
        .onNodeWithTag(PlannerScreenTestTags.ADD_TASK_MODAL)
        .assertExists("Add task modal should open")
  }

  @Test
  fun testPlannerScreen_classItemOpensAttendanceModal() {
    // Wait for classes to load
    waitForCondition {
      composeTestRule
          .onAllNodesWithText("Lecture", substring = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Find and click the first class item using a more specific approach
    composeTestRule.onAllNodesWithText("Lecture", substring = true).onFirst().performClick()

    // Wait for attendance modal
    waitForCondition {
      composeTestRule
          .onAllNodesWithTag(PlannerScreenTestTags.CLASS_ATTENDANCE_MODAL)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule
        .onNodeWithTag(PlannerScreenTestTags.CLASS_ATTENDANCE_MODAL)
        .assertExists("Attendance modal should open")
  }

  /*@Test
  fun testPlannerScreen_wellnessEventsDisplayed() {
      // Wait for wellness content with the actual text from your strings
      waitForCondition {
          composeTestRule.onAllNodesWithText("Yoga Session", substring = true)
              .fetchSemanticsNodes().isNotEmpty() ||
                  composeTestRule.onAllNodesWithText("Guest Lecture", substring = true)
                      .fetchSemanticsNodes().isNotEmpty()
      }

      // Use the actual text from your string resources
      composeTestRule.onNodeWithText("Yoga Session", substring = true)
          .assertExists("Yoga event should exist")

      composeTestRule.onNodeWithText("Guest Lecture", substring = true)
          .assertExists("Wellness lecture should exist")
  }*/

  @Test
  fun testPlannerScreen_attendanceModalSaveFunctionality() {
    // Wait for classes and open attendance modal
    waitForCondition {
      composeTestRule
          .onAllNodesWithText("Lecture", substring = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule.onAllNodesWithText("Lecture", substring = true).onFirst().performClick()

    // Wait for modal
    waitForCondition {
      composeTestRule
          .onAllNodesWithTag(PlannerScreenTestTags.CLASS_ATTENDANCE_MODAL)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Select options - use more specific selectors
    composeTestRule.onAllNodesWithText("Yes", ignoreCase = true).onFirst().performClick()

    composeTestRule.onAllNodesWithText("Partially", ignoreCase = true).onFirst().performClick()

    // Save
    composeTestRule.onNodeWithText("Save", ignoreCase = true).performClick()

    // Verify modal closes - wait for it to disappear
    waitForCondition(5000) {
      composeTestRule
          .onAllNodesWithTag(PlannerScreenTestTags.CLASS_ATTENDANCE_MODAL)
          .fetchSemanticsNodes()
          .isEmpty()
    }
  }

  @Test
  fun testPlannerScreen_petHeaderComponents() {
    // Wait for pet header
    waitForCondition {
      composeTestRule
          .onAllNodesWithTag(PlannerScreenTestTags.PET_HEADER)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule
        .onNodeWithTag(PlannerScreenTestTags.PET_HEADER)
        .assertExists("Pet header should exist")

    // Check for level display with substring matching
    composeTestRule.onNodeWithText("Lv", substring = true).assertExists("Level should be displayed")
  }

  @Test
  fun testPlannerScreen_aiRecommendationCard() {
    // Check for AI card using substring matching
    composeTestRule
        .onNodeWithText("recommendation", substring = true, ignoreCase = true)
        .assertExists("AI recommendation should exist")

    composeTestRule
        .onNodeWithText("studying", substring = true, ignoreCase = true)
        .assertExists("Study button should exist")
  }

  @Test
  fun testPlannerScreen_addTaskModalValidation() {
    // Open modal
    composeTestRule
        .onNodeWithContentDescription(composeTestRule.activity.getString(R.string.add_study_task))
        .performClick()

    // Wait for modal
    waitForCondition {
      composeTestRule
          .onAllNodesWithTag(PlannerScreenTestTags.ADD_TASK_MODAL)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Use test tags to input text
    composeTestRule.onNodeWithTag("subject_field").performTextInput("Mathematics")

    composeTestRule.onNodeWithTag("task_title_field").performTextInput("Complete exercises")

    composeTestRule.onNodeWithTag("duration_field").performTextInput("60")

    // Verify button becomes enabled
    composeTestRule.onNodeWithText("Add Task").assertIsEnabled()
  }
}
