package com.android.sample.feature.weeks.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.android.sample.feature.weeks.model.Objective
import com.android.sample.ui.theme.EduMonTheme
import java.time.DayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CourseExercisesRouteTest {

  @get:Rule val composeTestRule = createComposeRule()

  private val testObjective =
      Objective(
          title = "Finish Quiz 3",
          course = "CS101",
          estimateMinutes = 30,
          completed = false,
          day = DayOfWeek.MONDAY)

  @Test
  fun courseExercisesScreen_rendersWithoutCrashing() {
    composeTestRule.setContent {
      EduMonTheme {
        CourseExercisesRoute(
            objective = testObjective,
            coursePdfLabel = "Course PDF",
            exercisesPdfLabel = "Exercises PDF",
            onBack = {},
            onOpenCoursePdf = {},
            onOpenExercisesPdf = {},
            onCompleted = {})
      }
    }

    composeTestRule.onNodeWithTag(CourseExercisesTestTags.SCREEN).assertExists()
  }

  @Test
  fun topBar_displaysObjectiveTitleAndCourse() {
    composeTestRule.setContent {
      EduMonTheme {
        CourseExercisesRoute(
            objective = testObjective,
            coursePdfLabel = "Course PDF",
            exercisesPdfLabel = "Exercises PDF",
            onBack = {},
            onOpenCoursePdf = {},
            onOpenExercisesPdf = {},
            onCompleted = {})
      }
    }

    composeTestRule
        .onNodeWithTag(CourseExercisesTestTags.OBJECTIVE_TITLE)
        .assertExists()
        .assertTextEquals("Finish Quiz 3")

    composeTestRule
        .onNodeWithTag(CourseExercisesTestTags.OBJECTIVE_COURSE)
        .assertExists()
        .assertTextEquals("CS101")
  }

  @Test
  fun backButton_callsOnBackWhenClicked() {
    var backClicked = false

    composeTestRule.setContent {
      EduMonTheme {
        CourseExercisesRoute(
            objective = testObjective,
            coursePdfLabel = "Course PDF",
            exercisesPdfLabel = "Exercises PDF",
            onBack = { backClicked = true },
            onOpenCoursePdf = {},
            onOpenExercisesPdf = {},
            onCompleted = {})
      }
    }

    composeTestRule.onNodeWithTag(CourseExercisesTestTags.BACK_BUTTON).performClick()

    assertEquals(true, backClicked)
  }

  @Test
  fun completedFab_callsOnCompletedWhenClicked() {
    var completedClicked = false

    composeTestRule.setContent {
      EduMonTheme {
        CourseExercisesRoute(
            objective = testObjective,
            coursePdfLabel = "Course PDF",
            exercisesPdfLabel = "Exercises PDF",
            onBack = {},
            onOpenCoursePdf = {},
            onOpenExercisesPdf = {},
            onCompleted = { completedClicked = true })
      }
    }

    composeTestRule.onNodeWithTag(CourseExercisesTestTags.COMPLETED_FAB).performClick()

    assertEquals(true, completedClicked)
  }

  @Test
  fun headerCard_displaysObjectiveInfo() {
    composeTestRule.setContent {
      EduMonTheme {
        CourseExercisesRoute(
            objective = testObjective,
            coursePdfLabel = "Course PDF",
            exercisesPdfLabel = "Exercises PDF",
            onBack = {},
            onOpenCoursePdf = {},
            onOpenExercisesPdf = {},
            onCompleted = {})
      }
    }

    composeTestRule.onNodeWithTag(CourseExercisesTestTags.HEADER_CARD).assertExists()

    // Verify estimate chip shows
    composeTestRule
        .onNodeWithTag(CourseExercisesTestTags.ESTIMATE_CHIP)
        .assertExists()
        .assertTextContains("30 min focus")
  }

  @Test
  fun headerCard_noEstimateChip_whenEstimateIsZero() {
    val noEstimateObj = testObjective.copy(estimateMinutes = 0)

    composeTestRule.setContent {
      EduMonTheme {
        CourseExercisesRoute(
            objective = noEstimateObj,
            coursePdfLabel = "Course PDF",
            exercisesPdfLabel = "Exercises PDF",
            onBack = {},
            onOpenCoursePdf = {},
            onOpenExercisesPdf = {},
            onCompleted = {})
      }
    }

    composeTestRule.onNodeWithTag(CourseExercisesTestTags.HEADER_CARD).assertExists()
    composeTestRule.onNodeWithTag(CourseExercisesTestTags.ESTIMATE_CHIP).assertDoesNotExist()
  }

  @Test
  fun tabRow_hasCourseAndExercisesTabs() {
    composeTestRule.setContent {
      EduMonTheme {
        CourseExercisesRoute(
            objective = testObjective,
            coursePdfLabel = "Course PDF",
            exercisesPdfLabel = "Exercises PDF",
            onBack = {},
            onOpenCoursePdf = {},
            onOpenExercisesPdf = {},
            onCompleted = {})
      }
    }

    composeTestRule.onNodeWithTag(CourseExercisesTestTags.TAB_ROW).assertExists()
    composeTestRule.onNodeWithTag(CourseExercisesTestTags.COURSE_TAB).assertExists()
    composeTestRule.onNodeWithTag(CourseExercisesTestTags.EXERCISES_TAB).assertExists()
  }

  @Test
  fun switchingBackToCourseTab_showsCoursePdfCard() {
    composeTestRule.setContent {
      EduMonTheme {
        CourseExercisesRoute(
            objective = testObjective,
            coursePdfLabel = "Chapter 5",
            exercisesPdfLabel = "Week 5 Exercises",
            onBack = {},
            onOpenCoursePdf = {},
            onOpenExercisesPdf = {},
            onCompleted = {})
      }
    }

    // Switch to Exercises
    composeTestRule.onNodeWithTag(CourseExercisesTestTags.EXERCISES_TAB).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(CourseExercisesTestTags.EXERCISES_PDF_CARD).assertExists()

    // Switch back to Course
    composeTestRule.onNodeWithTag(CourseExercisesTestTags.COURSE_TAB).performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(CourseExercisesTestTags.COURSE_PDF_CARD).assertExists()
    composeTestRule.onNodeWithTag(CourseExercisesTestTags.EXERCISES_PDF_CARD).assertDoesNotExist()
  }

  @Test
  fun coursePdfCard_callsOnOpenCoursePdfWhenClicked() {
    var coursePdfClicked = false

    composeTestRule.setContent {
      EduMonTheme {
        CourseExercisesRoute(
            objective = testObjective,
            coursePdfLabel = "Course PDF",
            exercisesPdfLabel = "Exercises PDF",
            onBack = {},
            onOpenCoursePdf = { coursePdfClicked = true },
            onOpenExercisesPdf = {},
            onCompleted = {})
      }
    }

    // Click on the course PDF card (or the button inside it)
    composeTestRule.onNodeWithTag(CourseExercisesTestTags.PDF_OPEN_BUTTON).performClick()

    assertEquals(true, coursePdfClicked)
  }

  @Test
  fun exercisesPdfCard_callsOnOpenExercisesPdfWhenClicked() {
    var exercisesPdfClicked = false

    composeTestRule.setContent {
      EduMonTheme {
        CourseExercisesRoute(
            objective = testObjective,
            coursePdfLabel = "Course PDF",
            exercisesPdfLabel = "Exercises PDF",
            onBack = {},
            onOpenCoursePdf = {},
            onOpenExercisesPdf = { exercisesPdfClicked = true },
            onCompleted = {})
      }
    }

    // Switch to Exercises tab
    composeTestRule.onNodeWithTag(CourseExercisesTestTags.EXERCISES_TAB).performClick()
    composeTestRule.waitForIdle()

    // Click on the exercises PDF button
    composeTestRule.onNodeWithTag(CourseExercisesTestTags.PDF_OPEN_BUTTON).performClick()

    assertEquals(true, exercisesPdfClicked)
  }

  @Test
  fun pdfCard_canBeClickedOnCard_triggersCallback() {
    var coursePdfClicked = false

    composeTestRule.setContent {
      EduMonTheme {
        CourseExercisesRoute(
            objective = testObjective,
            coursePdfLabel = "Course PDF",
            exercisesPdfLabel = "Exercises PDF",
            onBack = {},
            onOpenCoursePdf = { coursePdfClicked = true },
            onOpenExercisesPdf = {},
            onCompleted = {})
      }
    }

    // Click directly on the card surface
    composeTestRule.onNodeWithTag(CourseExercisesTestTags.COURSE_PDF_CARD).performClick()

    assertEquals(true, coursePdfClicked)
  }

  @Test
  fun multipleTabs_canBeToggledMultipleTimes() {
    composeTestRule.setContent {
      EduMonTheme {
        CourseExercisesRoute(
            objective = testObjective,
            coursePdfLabel = "Course Material",
            exercisesPdfLabel = "Practice Problems",
            onBack = {},
            onOpenCoursePdf = {},
            onOpenExercisesPdf = {},
            onCompleted = {})
      }
    }

    // Initially Course tab
    composeTestRule.onNodeWithTag(CourseExercisesTestTags.COURSE_PDF_CARD).assertExists()

    // Switch to Exercises
    composeTestRule.onNodeWithTag(CourseExercisesTestTags.EXERCISES_TAB).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(CourseExercisesTestTags.EXERCISES_PDF_CARD).assertExists()

    // Switch back to Course
    composeTestRule.onNodeWithTag(CourseExercisesTestTags.COURSE_TAB).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(CourseExercisesTestTags.COURSE_PDF_CARD).assertExists()

    // Switch to Exercises again
    composeTestRule.onNodeWithTag(CourseExercisesTestTags.EXERCISES_TAB).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(CourseExercisesTestTags.EXERCISES_PDF_CARD).assertExists()
  }

  @Test
  fun allCallbacks_canBeInvokedIndependently() {
    var backClicked = false
    var coursePdfClicked = false
    var exercisesPdfClicked = false
    var completedClicked = false

    composeTestRule.setContent {
      EduMonTheme {
        CourseExercisesRoute(
            objective = testObjective,
            coursePdfLabel = "Course",
            exercisesPdfLabel = "Exercises",
            onBack = { backClicked = true },
            onOpenCoursePdf = { coursePdfClicked = true },
            onOpenExercisesPdf = { exercisesPdfClicked = true },
            onCompleted = { completedClicked = true })
      }
    }

    // Click back
    composeTestRule.onNodeWithTag(CourseExercisesTestTags.BACK_BUTTON).performClick()
    assertEquals(true, backClicked)

    // Click course PDF
    composeTestRule.onNodeWithTag(CourseExercisesTestTags.PDF_OPEN_BUTTON).performClick()
    assertEquals(true, coursePdfClicked)

    // Switch to Exercises and click
    composeTestRule.onNodeWithTag(CourseExercisesTestTags.EXERCISES_TAB).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(CourseExercisesTestTags.PDF_OPEN_BUTTON).performClick()
    assertEquals(true, exercisesPdfClicked)

    // Click completed FAB
    composeTestRule.onNodeWithTag(CourseExercisesTestTags.COMPLETED_FAB).performClick()
    assertEquals(true, completedClicked)
  }

  @Test
  fun pdfOpenButtons_displayCorrectLabels() {
    composeTestRule.setContent {
      EduMonTheme {
        CourseExercisesRoute(
            objective = testObjective,
            coursePdfLabel = "Course",
            exercisesPdfLabel = "Exercises",
            onBack = {},
            onOpenCoursePdf = {},
            onOpenExercisesPdf = {},
            onCompleted = {})
      }
    }

    // Course tab: button should say "Open course"
    composeTestRule
        .onNodeWithTag(CourseExercisesTestTags.PDF_OPEN_BUTTON)
        .assertTextEquals("Open course")

    // Switch to Exercises tab
    composeTestRule.onNodeWithTag(CourseExercisesTestTags.EXERCISES_TAB).performClick()
    composeTestRule.waitForIdle()

    // Button should now say "Open exercises"
    composeTestRule
        .onNodeWithTag(CourseExercisesTestTags.PDF_OPEN_BUTTON)
        .assertTextEquals("Open exercises")
  }

  @Test
  fun courseTab_isSelectedInitially() {
    composeTestRule.setContent {
      EduMonTheme {
        CourseExercisesRoute(
            objective = testObjective,
            coursePdfLabel = "Course",
            exercisesPdfLabel = "Exercises",
            onBack = {},
            onOpenCoursePdf = {},
            onOpenExercisesPdf = {},
            onCompleted = {})
      }
    }

    // Course tab should be selected
    composeTestRule.onNodeWithTag(CourseExercisesTestTags.COURSE_TAB).assertIsSelected()
    composeTestRule.onNodeWithTag(CourseExercisesTestTags.EXERCISES_TAB).assertIsNotSelected()
  }

  @Test
  fun exercisesTab_becomesSelectedWhenClicked() {
    composeTestRule.setContent {
      EduMonTheme {
        CourseExercisesRoute(
            objective = testObjective,
            coursePdfLabel = "Course",
            exercisesPdfLabel = "Exercises",
            onBack = {},
            onOpenCoursePdf = {},
            onOpenExercisesPdf = {},
            onCompleted = {})
      }
    }

    composeTestRule.onNodeWithTag(CourseExercisesTestTags.EXERCISES_TAB).performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(CourseExercisesTestTags.EXERCISES_TAB).assertIsSelected()
    composeTestRule.onNodeWithTag(CourseExercisesTestTags.COURSE_TAB).assertIsNotSelected()
  }
}
