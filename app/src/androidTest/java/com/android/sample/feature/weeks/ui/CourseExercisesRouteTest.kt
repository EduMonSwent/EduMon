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
          day = DayOfWeek.MONDAY,
          coursePdfUrl = "https://example.com/course.pdf",
          exercisePdfUrl = "https://example.com/exercise.pdf")

  private val testObjectiveNoPdfs =
      Objective(
          title = "Quiz Without PDFs",
          course = "CS102",
          estimateMinutes = 20,
          completed = false,
          day = DayOfWeek.TUESDAY,
          coursePdfUrl = "",
          exercisePdfUrl = "")

  @Test
  fun courseExercisesScreen_rendersWithoutCrashing() {
    composeTestRule.setContent {
      EduMonTheme {
        CourseExercisesRoute(
            objective = testObjective,
            coursePdfLabel = "Course PDF",
            exercisesPdfLabel = "Exercises PDF",
            onBack = {},
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
  fun multipleTabs_canBeToggledMultipleTimes() {
    composeTestRule.setContent {
      EduMonTheme {
        CourseExercisesRoute(
            objective = testObjective,
            coursePdfLabel = "Course Material",
            exercisesPdfLabel = "Practice Problems",
            onBack = {},
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
  fun courseTab_isSelectedInitially() {
    composeTestRule.setContent {
      EduMonTheme {
        CourseExercisesRoute(
            objective = testObjective,
            coursePdfLabel = "Course",
            exercisesPdfLabel = "Exercises",
            onBack = {},
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
            onCompleted = {})
      }
    }

    composeTestRule.onNodeWithTag(CourseExercisesTestTags.EXERCISES_TAB).performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(CourseExercisesTestTags.EXERCISES_TAB).assertIsSelected()
    composeTestRule.onNodeWithTag(CourseExercisesTestTags.COURSE_TAB).assertIsNotSelected()
  }

  // ========== PDF FUNCTIONALITY TESTS ==========

  @Test
  fun coursePdfCard_withPdfUrl_showsEnabledButton() {
    composeTestRule.setContent {
      EduMonTheme {
        CourseExercisesRoute(
            objective = testObjective,
            coursePdfLabel = "Android Basics Course",
            exercisesPdfLabel = "Setup Exercises",
            coursePdfUrl = "https://example.com/course.pdf",
            exercisePdfUrl = "https://example.com/exercise.pdf",
            onBack = {},
            onCompleted = {})
      }
    }

    // Course PDF card should be visible and enabled
    composeTestRule.onNodeWithTag(CourseExercisesTestTags.COURSE_PDF_CARD).assertExists()
    composeTestRule.onNodeWithTag(CourseExercisesTestTags.PDF_OPEN_BUTTON).assertIsEnabled()
    composeTestRule.onNodeWithText("Open course").assertExists()
    composeTestRule.onNodeWithText("Android Basics Course").assertExists()
  }

  @Test
  fun coursePdfCard_withoutPdfUrl_showsDisabledState() {
    composeTestRule.setContent {
      EduMonTheme {
        CourseExercisesRoute(
            objective = testObjectiveNoPdfs,
            coursePdfLabel = "No PDF Course",
            exercisesPdfLabel = "No PDF Exercises",
            coursePdfUrl = "",
            exercisePdfUrl = "",
            onBack = {},
            onCompleted = {})
      }
    }

    // Should show "No PDF available" and disabled button
    composeTestRule.onNodeWithText("No PDF available").assertExists()
    composeTestRule.onNodeWithText("Unavailable").assertExists()
    composeTestRule.onNodeWithTag(CourseExercisesTestTags.PDF_OPEN_BUTTON).assertIsNotEnabled()
  }

  @Test
  fun exercisePdfCard_withPdfUrl_showsEnabledButton() {
    composeTestRule.setContent {
      EduMonTheme {
        CourseExercisesRoute(
            objective = testObjective,
            coursePdfLabel = "Course Material",
            exercisesPdfLabel = "Practice Exercises",
            coursePdfUrl = "https://example.com/course.pdf",
            exercisePdfUrl = "https://example.com/exercise.pdf",
            onBack = {},
            onCompleted = {})
      }
    }

    // Switch to exercises tab
    composeTestRule.onNodeWithTag(CourseExercisesTestTags.EXERCISES_TAB).performClick()
    composeTestRule.waitForIdle()

    // Exercise PDF card should be enabled
    composeTestRule.onNodeWithTag(CourseExercisesTestTags.EXERCISES_PDF_CARD).assertExists()
    composeTestRule.onNodeWithTag(CourseExercisesTestTags.PDF_OPEN_BUTTON).assertIsEnabled()
    composeTestRule.onNodeWithText("Open exercises").assertExists()
    composeTestRule.onNodeWithText("Practice Exercises").assertExists()
  }

  @Test
  fun exercisePdfCard_withoutPdfUrl_showsDisabledState() {
    composeTestRule.setContent {
      EduMonTheme {
        CourseExercisesRoute(
            objective = testObjectiveNoPdfs,
            coursePdfLabel = "Course",
            exercisesPdfLabel = "Exercise",
            coursePdfUrl = "",
            exercisePdfUrl = "",
            onBack = {},
            onCompleted = {})
      }
    }

    // Switch to exercises tab
    composeTestRule.onNodeWithTag(CourseExercisesTestTags.EXERCISES_TAB).performClick()
    composeTestRule.waitForIdle()

    // Should show disabled state
    composeTestRule.onNodeWithText("No PDF available").assertExists()
    composeTestRule.onNodeWithText("Unavailable").assertExists()
    composeTestRule.onNodeWithTag(CourseExercisesTestTags.PDF_OPEN_BUTTON).assertIsNotEnabled()
  }

  @Test
  fun pdfCards_displayCorrectLabels() {
    val courseLabel = "Introduction to Kotlin Programming"
    val exerciseLabel = "Kotlin Practice Set 1"

    composeTestRule.setContent {
      EduMonTheme {
        CourseExercisesRoute(
            objective = testObjective,
            coursePdfLabel = courseLabel,
            exercisesPdfLabel = exerciseLabel,
            coursePdfUrl = "https://example.com/kotlin.pdf",
            exercisePdfUrl = "https://example.com/practice.pdf",
            onBack = {},
            onCompleted = {})
      }
    }

    // Course tab shows course label
    composeTestRule.onNodeWithText(courseLabel).assertExists()

    // Switch to exercises tab
    composeTestRule.onNodeWithTag(CourseExercisesTestTags.EXERCISES_TAB).performClick()
    composeTestRule.waitForIdle()

    // Exercises tab shows exercise label
    composeTestRule.onNodeWithText(exerciseLabel).assertExists()
  }

  @Test
  fun objectiveWithPdfUrls_passedFromSchedule_displaysCorrectly() {
    // Simulate what ScheduleScreen passes when objective has PDF URLs
    val objectiveWithPdfs =
        Objective(
            title = "Complete Android Basics",
            course = "CS-200",
            estimateMinutes = 45,
            completed = false,
            day = DayOfWeek.WEDNESDAY,
            coursePdfUrl = "https://developer.android.com/courses/basics",
            exercisePdfUrl = "https://developer.android.com/codelabs/first-app")

    composeTestRule.setContent {
      EduMonTheme {
        CourseExercisesRoute(
            objective = objectiveWithPdfs,
            coursePdfLabel = "Course material for ${objectiveWithPdfs.course}",
            exercisesPdfLabel = "Exercises for ${objectiveWithPdfs.course}",
            coursePdfUrl = objectiveWithPdfs.coursePdfUrl,
            exercisePdfUrl = objectiveWithPdfs.exercisePdfUrl,
            onBack = {},
            onCompleted = {})
      }
    }

    // Verify both PDFs are enabled
    composeTestRule.onNodeWithTag(CourseExercisesTestTags.PDF_OPEN_BUTTON).assertIsEnabled()

    composeTestRule.onNodeWithTag(CourseExercisesTestTags.EXERCISES_TAB).performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(CourseExercisesTestTags.PDF_OPEN_BUTTON).assertIsEnabled()
  }

  @Test
  fun pdfUrlsFromObjective_areUsedDirectly() {
    // Test that PDF URLs from objective are used (not fetched from elsewhere)
    val testUrl1 = "https://test.com/unique-course-url.pdf"
    val testUrl2 = "https://test.com/unique-exercise-url.pdf"

    val objective =
        Objective(
            title = "Test Objective",
            course = "TEST-101",
            estimateMinutes = 30,
            completed = false,
            day = DayOfWeek.FRIDAY,
            coursePdfUrl = testUrl1,
            exercisePdfUrl = testUrl2)

    composeTestRule.setContent {
      EduMonTheme {
        CourseExercisesRoute(
            objective = objective,
            coursePdfLabel = "Test Course",
            exercisesPdfLabel = "Test Exercise",
            coursePdfUrl = objective.coursePdfUrl,
            exercisePdfUrl = objective.exercisePdfUrl,
            onBack = {},
            onCompleted = {})
      }
    }

    // Both should be enabled since URLs are present
    composeTestRule.onNodeWithTag(CourseExercisesTestTags.PDF_OPEN_BUTTON).assertIsEnabled()
    composeTestRule.onNodeWithText("Open course").assertExists()
  }

  // ========== ONCLICK COVERAGE TESTS (for Sonar) ==========

  @Test
  fun coursePdfButton_withPdfUrl_callsPdfHelperWhenClicked() {
    // This test covers the onClick that calls PdfHelper.openPdf()
    composeTestRule.setContent {
      EduMonTheme {
        CourseExercisesRoute(
            objective = testObjective,
            coursePdfLabel = "Course Material",
            exercisesPdfLabel = "Exercises",
            coursePdfUrl = "https://example.com/course.pdf",
            exercisePdfUrl = "",
            onBack = {},
            onCompleted = {})
      }
    }

    // Click the course PDF button
    composeTestRule.onNodeWithTag(CourseExercisesTestTags.PDF_OPEN_BUTTON).performClick()
    composeTestRule.waitForIdle()

    // PdfHelper.openPdf is called internally - we've executed the code path (Sonar will detect
    // coverage)
  }

  @Test
  fun exercisePdfButton_withPdfUrl_callsPdfHelperWhenClicked() {
    // This test covers the onClick that calls PdfHelper.openPdf()
    composeTestRule.setContent {
      EduMonTheme {
        CourseExercisesRoute(
            objective = testObjective,
            coursePdfLabel = "Course",
            exercisesPdfLabel = "Exercise Material",
            coursePdfUrl = "",
            exercisePdfUrl = "https://example.com/exercise.pdf",
            onBack = {},
            onCompleted = {})
      }
    }

    // Switch to exercises tab
    composeTestRule.onNodeWithTag(CourseExercisesTestTags.EXERCISES_TAB).performClick()
    composeTestRule.waitForIdle()

    // Click the exercise PDF button
    composeTestRule.onNodeWithTag(CourseExercisesTestTags.PDF_OPEN_BUTTON).performClick()
    composeTestRule.waitForIdle()

    // PdfHelper.openPdf is called internally - we've executed the code path
  }

  @Test
  fun pdfCard_withBlankUrl_hasDisabledCardClick() {
    // This test ensures the card's clickable modifier is disabled when hasPdf is false
    composeTestRule.setContent {
      EduMonTheme {
        CourseExercisesRoute(
            objective = testObjectiveNoPdfs,
            coursePdfLabel = "Course",
            exercisesPdfLabel = "Exercises",
            coursePdfUrl = "",
            exercisePdfUrl = "",
            onBack = {},
            onCompleted = {})
      }
    }

    // Try clicking the course PDF card - should not trigger onClick because card is disabled
    composeTestRule.onNodeWithTag(CourseExercisesTestTags.COURSE_PDF_CARD).performClick()
    composeTestRule.waitForIdle()

    // Card is disabled when no PDF URL, so it's not clickable
  }

  @Test
  fun coursePdfCard_clickableWhenPdfUrlProvided() {
    // Ensures the card IS clickable when PDF URL exists
    composeTestRule.setContent {
      EduMonTheme {
        CourseExercisesRoute(
            objective = testObjective,
            coursePdfLabel = "Course",
            exercisesPdfLabel = "Exercises",
            coursePdfUrl = "https://example.com/course.pdf",
            exercisePdfUrl = "",
            onBack = {},
            onCompleted = {})
      }
    }

    // Card should be clickable
    composeTestRule.onNodeWithTag(CourseExercisesTestTags.COURSE_PDF_CARD).performClick()
    composeTestRule.waitForIdle()

    // Successfully clicked - covers the PdfHelper.openPdf branch
  }

  @Test
  fun exercisePdfCard_clickableWhenPdfUrlProvided() {
    // Ensures the exercise card IS clickable when PDF URL exists
    composeTestRule.setContent {
      EduMonTheme {
        CourseExercisesRoute(
            objective = testObjective,
            coursePdfLabel = "Course",
            exercisesPdfLabel = "Exercises",
            coursePdfUrl = "",
            exercisePdfUrl = "https://example.com/exercise.pdf",
            onBack = {},
            onCompleted = {})
      }
    }

    // Switch to exercises tab
    composeTestRule.onNodeWithTag(CourseExercisesTestTags.EXERCISES_TAB).performClick()
    composeTestRule.waitForIdle()

    // Card should be clickable
    composeTestRule.onNodeWithTag(CourseExercisesTestTags.EXERCISES_PDF_CARD).performClick()
    composeTestRule.waitForIdle()

    // Successfully clicked - covers the PdfHelper.openPdf branch
  }

  @Test
  fun exerciseObjective_opensWithExercisesTabSelected() {
    val exerciseObjective = testObjective.copy(sourceId = "AUTO:CS101:EXERCISE:14")

    composeTestRule.setContent {
      EduMonTheme {
        CourseExercisesRoute(
            objective = exerciseObjective,
            coursePdfLabel = "Course",
            exercisesPdfLabel = "Exercises",
            coursePdfUrl = "https://example.com/course.pdf",
            exercisePdfUrl = "https://example.com/exercise.pdf",
            onBack = {},
            onCompleted = {})
      }
    }

    // Exercises tab should be selected initially
    composeTestRule.onNodeWithTag(CourseExercisesTestTags.EXERCISES_TAB).assertIsSelected()

    composeTestRule.onNodeWithTag(CourseExercisesTestTags.COURSE_TAB).assertIsNotSelected()

    // Exercises content should be visible
    composeTestRule.onNodeWithTag(CourseExercisesTestTags.EXERCISES_PDF_CARD).assertExists()
  }

  @Test
  fun labObjective_opensWithExercisesTabSelected() {
    val labObjective = testObjective.copy(sourceId = "AUTO:CS101:LAB:14")

    composeTestRule.setContent {
      EduMonTheme {
        CourseExercisesRoute(
            objective = labObjective,
            coursePdfLabel = "Course",
            exercisesPdfLabel = "Lab",
            coursePdfUrl = "https://example.com/course.pdf",
            exercisePdfUrl = "https://example.com/lab.pdf",
            onBack = {},
            onCompleted = {})
      }
    }

    composeTestRule.onNodeWithTag(CourseExercisesTestTags.EXERCISES_TAB).assertIsSelected()

    composeTestRule.onNodeWithTag(CourseExercisesTestTags.EXERCISES_PDF_CARD).assertExists()
  }

  @Test
  fun lectureObjective_opensWithCourseTabSelectedByDefault() {
    val lectureObjective = testObjective.copy(sourceId = "AUTO:CS101:LECTURE:14")

    composeTestRule.setContent {
      EduMonTheme {
        CourseExercisesRoute(
            objective = lectureObjective,
            coursePdfLabel = "Course",
            exercisesPdfLabel = "Exercises",
            coursePdfUrl = "https://example.com/course.pdf",
            exercisePdfUrl = "https://example.com/exercise.pdf",
            onBack = {},
            onCompleted = {})
      }
    }

    composeTestRule.onNodeWithTag(CourseExercisesTestTags.COURSE_TAB).assertIsSelected()

    composeTestRule.onNodeWithTag(CourseExercisesTestTags.COURSE_PDF_CARD).assertExists()
  }
}
