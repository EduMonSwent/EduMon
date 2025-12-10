package com.android.sample.feature.weeks.model

import java.time.DayOfWeek
import org.junit.Assert.*
import org.junit.Test

class ObjectiveTest {

  @Test
  fun objective_withBothPdfUrls_storesBothCorrectly() {
    val coursePdf = "https://example.com/course.pdf"
    val exercisePdf = "https://example.com/exercise.pdf"

    val objective =
        Objective(
            title = "Test",
            course = "CS-101",
            estimateMinutes = 30,
            completed = false,
            day = DayOfWeek.MONDAY,
            coursePdfUrl = coursePdf,
            exercisePdfUrl = exercisePdf)

    assertEquals(coursePdf, objective.coursePdfUrl)
    assertEquals(exercisePdf, objective.exercisePdfUrl)
  }

  @Test
  fun objective_copyWithCoursePdfUrl_updatesOnlyCoursePdf() {
    val original =
        Objective(
            title = "Test",
            course = "CS-101",
            estimateMinutes = 30,
            completed = false,
            day = DayOfWeek.MONDAY,
            coursePdfUrl = "",
            exercisePdfUrl = "https://example.com/exercise.pdf")

    val updated = original.copy(coursePdfUrl = "https://example.com/course.pdf")

    assertEquals("https://example.com/course.pdf", updated.coursePdfUrl)
    assertEquals("https://example.com/exercise.pdf", updated.exercisePdfUrl)
  }

  @Test
  fun objective_courseOrExercisesType_canHavePdfUrls() {
    val objective =
        Objective(
            title = "Build Layout",
            course = "CS-200",
            estimateMinutes = 60,
            completed = false,
            day = DayOfWeek.WEDNESDAY,
            type = ObjectiveType.COURSE_OR_EXERCISES,
            coursePdfUrl = "https://example.com/layouts.pdf",
            exercisePdfUrl = "https://example.com/layout-exercises.pdf")

    assertEquals(ObjectiveType.COURSE_OR_EXERCISES, objective.type)
    assertFalse(objective.coursePdfUrl.isEmpty())
    assertFalse(objective.exercisePdfUrl.isEmpty())
  }

  @Test
  fun objective_quizType_canHaveEmptyPdfUrls() {
    val objective =
        Objective(
            title = "Take Quiz",
            course = "CS-101",
            estimateMinutes = 20,
            completed = false,
            day = DayOfWeek.FRIDAY,
            type = ObjectiveType.QUIZ,
            coursePdfUrl = "",
            exercisePdfUrl = "")

    assertEquals(ObjectiveType.QUIZ, objective.type)
    assertTrue(objective.coursePdfUrl.isEmpty())
    assertTrue(objective.exercisePdfUrl.isEmpty())
  }

  @Test
  fun objective_resumeType_canHaveCoursePdfOnly() {
    val objective =
        Objective(
            title = "Review Week 1",
            course = "CS-101",
            estimateMinutes = 45,
            completed = false,
            day = DayOfWeek.SUNDAY,
            type = ObjectiveType.RESUME,
            coursePdfUrl = "https://example.com/review-notes.pdf",
            exercisePdfUrl = "")

    assertEquals(ObjectiveType.RESUME, objective.type)
    assertFalse(objective.coursePdfUrl.isEmpty())
    assertTrue(objective.exercisePdfUrl.isEmpty())
  }

  @Test
  fun objective_equality_includesPdfUrls() {
    val obj1 =
        Objective(
            title = "Test",
            course = "CS-101",
            estimateMinutes = 30,
            completed = false,
            day = DayOfWeek.MONDAY,
            coursePdfUrl = "https://example.com/course.pdf",
            exercisePdfUrl = "https://example.com/exercise.pdf")

    val obj2 =
        Objective(
            title = "Test",
            course = "CS-101",
            estimateMinutes = 30,
            completed = false,
            day = DayOfWeek.MONDAY,
            coursePdfUrl = "https://example.com/course.pdf",
            exercisePdfUrl = "https://example.com/exercise.pdf")

    assertEquals(obj1, obj2)
  }

  @Test
  fun objective_inequality_whenPdfUrlsDiffer() {
    val obj1 =
        Objective(
            title = "Test",
            course = "CS-101",
            estimateMinutes = 30,
            completed = false,
            day = DayOfWeek.MONDAY,
            coursePdfUrl = "https://example.com/course1.pdf")

    val obj2 =
        Objective(
            title = "Test",
            course = "CS-101",
            estimateMinutes = 30,
            completed = false,
            day = DayOfWeek.MONDAY,
            coursePdfUrl = "https://example.com/course2.pdf")

    assertNotEquals(obj1, obj2)
  }

  @Test
  fun objective_withFirebaseStorageUrl_storesCorrectly() {
    val firebaseUrl =
        "https://firebasestorage.googleapis.com/v0/b/myapp.appspot.com/o/courses%2Fweek1.pdf?alt=media&token=xyz"

    val objective =
        Objective(
            title = "Week 1 Material",
            course = "CS-200",
            estimateMinutes = 60,
            completed = false,
            day = DayOfWeek.MONDAY,
            coursePdfUrl = firebaseUrl)

    assertEquals(firebaseUrl, objective.coursePdfUrl)
  }

  @Test
  fun objective_withMoodleUrl_storesCorrectly() {
    val moodleUrl = "https://moodle.epfl.ch/mod/resource/view.php?id=12345"

    val objective =
        Objective(
            title = "EPFL Lecture",
            course = "COM-101",
            estimateMinutes = 90,
            completed = false,
            day = DayOfWeek.TUESDAY,
            coursePdfUrl = moodleUrl)

    assertEquals(moodleUrl, objective.coursePdfUrl)
  }
}
