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

    // From defaultObjectives(): 5 items, first two completed
    assertEquals(5, list.size)
    assertEquals(listOf("Setup Android Studio", "Finish codelab"), list.take(2).map { it.title })
  }

  @Test
  fun addObjective_unsigned_appends() = runBlocking {
    val repo = repoWithNoUser()

    val out = repo.addObjective(Objective("Write tests", "CS", 25, false, DayOfWeek.THURSDAY))

    assertEquals(6, out.size)
    assertEquals("Write tests", out.last().title)
  }

  @Test
  fun updateObjective_unsigned_replaces_at_index() = runBlocking {
    val repo = repoWithNoUser()

    val out =
        repo.updateObjective(
            index = 0,
            obj = Objective("Setup Android Studio (done)", "CS", 10, true, DayOfWeek.MONDAY))

    assertEquals("Setup Android Studio (done)", out[0].title)
    assertEquals(true, out[0].completed)
  }

  @Test
  fun removeObjective_unsigned_deletes_and_compacts() = runBlocking {
    val repo = repoWithNoUser()

    val out = repo.removeObjective(index = 1)

    // Removed "Finish codelab"
    assertEquals(4, out.size)
    assertEquals(
        listOf(
            "Setup Android Studio",
            "Read Compose Basics",
            "Build layout challenge",
            "Repository implementation"),
        out.map { it.title })
  }

  @Test
  fun moveObjective_unsigned_reorders() = runBlocking {
    val repo = repoWithNoUser()

    val out = repo.moveObjective(fromIndex = 0, toIndex = 2)

    assertEquals(
        listOf(
            "Finish codelab",
            "Read Compose Basics",
            "Setup Android Studio",
            "Build layout challenge",
            "Repository implementation"),
        out.map { it.title })
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

    val u = repo.updateObjective(index = 10, obj = Objective("X", "CS", 0, false, DayOfWeek.MONDAY))
    val r = repo.removeObjective(index = 99)
    val m = repo.moveObjective(fromIndex = 3, toIndex = 3)

    assertEquals(before.map { it.title }, u.map { it.title })
    assertEquals(before.map { it.title }, r.map { it.title })
    assertEquals(before.map { it.title }, m.map { it.title })
  }
}
