package com.android.sample.feature.weeks.repository

import com.android.sample.feature.weeks.model.Objective
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.time.DayOfWeek
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class FirestoreObjectivesRepositoryTest {

  private fun repoWithNoUser(): FirestoreObjectivesRepository {
    val db: FirebaseFirestore = mock()
    val auth: FirebaseAuth = mock()
    whenever(auth.currentUser).thenReturn(null) // unsigned path
    return FirestoreObjectivesRepository(db, auth)
  }

  @Test
  fun getObjectives_unsigned_returnsDefaultsInOrder() = runBlocking {
    val repo = repoWithNoUser()

    val list = repo.getObjectives()

    // From DefaultObjectives.get(): 13 items
    assertEquals(13, list.size)
    // First two items are Monday objectives
    assertEquals(listOf("Complete Quiz 1", "Read Chapter 1"), list.take(2).map { it.title })
    assertEquals(listOf("CS-200", "CS-200"), list.take(2).map { it.course })
    assertEquals(listOf(DayOfWeek.MONDAY, DayOfWeek.MONDAY), list.take(2).map { it.day })
  }

  @Test
  fun addObjective_unsigned_appends() = runBlocking {
    val repo = repoWithNoUser()

    val out = repo.addObjective(Objective("Write tests", "CS", 25, false, DayOfWeek.THURSDAY))

    assertEquals(14, out.size) // 13 defaults + 1 new
    assertEquals("Write tests", out.last().title)
  }

  @Test
  fun updateObjective_unsigned_replaces_at_index() = runBlocking {
    val repo = repoWithNoUser()

    val out =
        repo.updateObjective(
            index = 0,
            obj = Objective("Complete Quiz 1 (done)", "CS-200", 10, true, DayOfWeek.MONDAY))

    assertEquals("Complete Quiz 1 (done)", out[0].title)
    assertEquals(true, out[0].completed)
  }

  @Test
  fun removeObjective_unsigned_deletes_and_compacts() = runBlocking {
    val repo = repoWithNoUser()

    val out = repo.removeObjective(index = 1) // Remove "Read Chapter 1"

    assertEquals(12, out.size) // 13 - 1
    // First item should still be "Complete Quiz 1"
    assertEquals("Complete Quiz 1", out[0].title)
    // Second item should now be "Solve Exercise Set 1" (was third)
    assertEquals("Solve Exercise Set 1", out[1].title)
  }

  @Test
  fun moveObjective_unsigned_reorders() = runBlocking {
    val repo = repoWithNoUser()

    val out = repo.moveObjective(fromIndex = 0, toIndex = 2)

    assertEquals(13, out.size)
    // "Complete Quiz 1" (was at 0) should now be at index 2
    assertEquals("Complete Quiz 1", out[2].title)
    // "Read Chapter 1" (was at 1) should now be at index 0
    assertEquals("Read Chapter 1", out[0].title)
    // "Solve Exercise Set 1" (was at 2) should now be at index 1
    assertEquals("Solve Exercise Set 1", out[1].title)
  }

  @Test
  fun setObjectives_unsigned_replaces_all() = runBlocking {
    val repo = repoWithNoUser()
    val newObjs =
        listOf(
            Objective("N1", "CS", 5, false, DayOfWeek.THURSDAY),
            Objective("N2", "CS", 15, true, DayOfWeek.FRIDAY),
        )

    val out = repo.setObjectives(newObjs)

    assertEquals(listOf("N1", "N2"), out.map { it.title })
    assertEquals(listOf(false, true), out.map { it.completed })
  }

  @Test
  fun update_remove_move_outOfRange_unsigned_are_noops() = runBlocking {
    val repo = repoWithNoUser()

    val before = repo.getObjectives()

    val u = repo.updateObjective(index = 99, obj = Objective("X", "CS", 0, false, DayOfWeek.MONDAY))
    val r = repo.removeObjective(index = 99)
    val m = repo.moveObjective(fromIndex = 3, toIndex = 3)

    assertEquals(before.map { it.title }, u.map { it.title })
    assertEquals(before.map { it.title }, r.map { it.title })
    assertEquals(before.map { it.title }, m.map { it.title })
  }

  // ========== PDF URL TESTS ==========

  @Test
  fun getObjectives_unsigned_defaultsHavePdfUrls() = runBlocking {
    val repo = repoWithNoUser()

    val list = repo.getObjectives()

    // Default objectives should have PDF URLs
    val firstObjective = list[0] // "Setup Android Studio"
    assertFalse(firstObjective.coursePdfUrl.isEmpty())
    assertFalse(firstObjective.exercisePdfUrl.isEmpty())
    assertTrue(firstObjective.coursePdfUrl.startsWith("https://"))
    assertTrue(firstObjective.exercisePdfUrl.startsWith("https://"))
  }

  @Test
  fun addObjective_withPdfUrls_preservesUrls() = runBlocking {
    val repo = repoWithNoUser()
    val coursePdf = "https://example.com/course.pdf"
    val exercisePdf = "https://example.com/exercise.pdf"

    val newObj =
        Objective(
            title = "Test with PDFs",
            course = "CS-101",
            estimateMinutes = 30,
            completed = false,
            day = DayOfWeek.FRIDAY,
            coursePdfUrl = coursePdf,
            exercisePdfUrl = exercisePdf)

    val result = repo.addObjective(newObj)
    val addedObj = result.last()

    assertEquals(coursePdf, addedObj.coursePdfUrl)
    assertEquals(exercisePdf, addedObj.exercisePdfUrl)
  }

  @Test
  fun updateObjective_canUpdatePdfUrls() = runBlocking {
    val repo = repoWithNoUser()
    val newCoursePdf = "https://example.com/updated-course.pdf"
    val newExercisePdf = "https://example.com/updated-exercise.pdf"

    val objectives = repo.getObjectives()
    val original = objectives[0]

    val updated = original.copy(coursePdfUrl = newCoursePdf, exercisePdfUrl = newExercisePdf)

    val result = repo.updateObjective(index = 0, obj = updated)

    assertEquals(newCoursePdf, result[0].coursePdfUrl)
    assertEquals(newExercisePdf, result[0].exercisePdfUrl)
  }

  @Test
  fun objective_withEmptyPdfUrls_storesEmpty() = runBlocking {
    val repo = repoWithNoUser()

    val objWithoutPdfs =
        Objective(
            title = "Quiz",
            course = "CS-101",
            estimateMinutes = 20,
            completed = false,
            day = DayOfWeek.WEDNESDAY,
            coursePdfUrl = "",
            exercisePdfUrl = "")

    val result = repo.addObjective(objWithoutPdfs)
    val added = result.last()

    assertEquals("", added.coursePdfUrl)
    assertEquals("", added.exercisePdfUrl)
  }

  @Test
  fun objective_canHaveOnlyCoursePdf() = runBlocking {
    val repo = repoWithNoUser()

    val objWithCoursePdfOnly =
        Objective(
            title = "Review Material",
            course = "CS-101",
            estimateMinutes = 45,
            completed = false,
            day = DayOfWeek.SUNDAY,
            coursePdfUrl = "https://example.com/review.pdf",
            exercisePdfUrl = "")

    val result = repo.addObjective(objWithCoursePdfOnly)
    val added = result.last()

    assertFalse(added.coursePdfUrl.isEmpty())
    assertTrue(added.exercisePdfUrl.isEmpty())
  }

  @Test
  fun objective_canHaveOnlyExercisePdf() = runBlocking {
    val repo = repoWithNoUser()

    val objWithExercisePdfOnly =
        Objective(
            title = "Practice Problems",
            course = "CS-101",
            estimateMinutes = 60,
            completed = false,
            day = DayOfWeek.SATURDAY,
            coursePdfUrl = "",
            exercisePdfUrl = "https://example.com/problems.pdf")

    val result = repo.addObjective(objWithExercisePdfOnly)
    val added = result.last()

    assertTrue(added.coursePdfUrl.isEmpty())
    assertFalse(added.exercisePdfUrl.isEmpty())
  }

  @Test
  fun defaultObjectives_allHaveValidPdfUrls() = runBlocking {
    val repo = repoWithNoUser()

    val objectives = repo.getObjectives()

    // All default objectives should have at least one PDF URL
    objectives.forEach { obj ->
      val hasPdf = obj.coursePdfUrl.isNotBlank() || obj.exercisePdfUrl.isNotBlank()
      assertTrue("Objective ${obj.title} should have at least one PDF", hasPdf)
    }
  }

  private fun assertFalse(condition: Boolean) {
    assertEquals(false, condition)
  }

  private fun assertTrue(condition: Boolean) {
    assertEquals(true, condition)
  }

  private fun assertTrue(message: String, condition: Boolean) {
    if (!condition) {
      throw AssertionError(message)
    }
  }
}
