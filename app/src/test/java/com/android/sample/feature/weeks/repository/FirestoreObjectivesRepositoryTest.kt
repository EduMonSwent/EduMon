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
}
