package com.android.sample.feature.weeks.repository

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.feature.weeks.model.Objective
import com.android.sample.util.FirebaseEmulator
import com.google.android.gms.tasks.Tasks
import java.time.DayOfWeek
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FirestoreObjectivesRepositoryEmulatorTest {

  private lateinit var repo: FirestoreObjectivesRepository

  @Before
  fun setUp() = runBlocking {
    // Ensure FirebaseApp is initialized even in unusual test setups.
    FirebaseEmulator.initIfNeeded(ApplicationProvider.getApplicationContext())

    // Connect to emulators if they’re up.
    FirebaseEmulator.connectIfRunning()

    // Fail fast (now checks 4000 and 4400)
    assertTrue(
        "Firebase emulators not reachable (expected UI on 4000/4400; firestore:8080, auth:9099). " +
            "Start with: firebase emulators:start --only firestore,auth",
        FirebaseEmulator.isRunning)

    // Start clean
    FirebaseEmulator.clearAll()

    // Sign in anonymously so rules with request.auth.uid work
    Tasks.await(FirebaseEmulator.auth.signInAnonymously())

    // Repo bound to emulator instances
    repo = FirestoreObjectivesRepository(FirebaseEmulator.firestore, FirebaseEmulator.auth)
  }

  @After
  fun tearDown() = runBlocking {
    if (FirebaseEmulator.isRunning) {
      FirebaseEmulator.clearAll()
    }
  }

  @Test
  fun add_and_get_objectives_persists_in_order() = runBlocking {
    val initial = repo.getObjectives()
    assertTrue(initial.isEmpty())
    val a = Objective("A", "CS", 5, false, DayOfWeek.MONDAY)
    val b = Objective("B", "CS", 10, true, DayOfWeek.TUESDAY)
    val c = Objective("C", "CS", 15, false, DayOfWeek.WEDNESDAY)
    repo.addObjective(a)
    repo.addObjective(b)
    val afterTwo = repo.addObjective(c)
    assertEquals(listOf("A", "B", "C"), afterTwo.map { it.title })
    val snapshot = repo.getObjectives()
    assertEquals(listOf("A", "B", "C"), snapshot.map { it.title })
  }

  // … your tests unchanged below …
  @Test
  fun updateObjective_updates_document_by_index() = runBlocking {
    repo.setObjectives(listOf(Objective("First", "CS", 10, false, DayOfWeek.MONDAY)))
    val modified = Objective("First (edited)", "CS", 20, true, DayOfWeek.MONDAY)
    kotlinx.coroutines.delay(10000)
    val out = repo.updateObjective(0, modified)
    assertEquals(1, out.size)
    assertEquals("First (edited)", out[0].title)
    assertTrue(out[0].completed)
    val snapshot = repo.getObjectives()
    assertEquals("First (edited)", snapshot[0].title)
  }

  @Test
  fun removeObjective_deletes_and_reindexes_orders() = runBlocking {
    repo.setObjectives(
        listOf(
            Objective("A", "CS", 5, false, DayOfWeek.MONDAY),
            Objective("B", "CS", 10, false, DayOfWeek.TUESDAY),
            Objective("C", "CS", 15, false, DayOfWeek.WEDNESDAY),
        ))
    val after = repo.removeObjective(1)
    assertEquals(2, after.size)
    assertEquals(listOf("A", "C"), after.map { it.title })
    val snapshot = repo.getObjectives()
    assertEquals(listOf("A", "C"), snapshot.map { it.title })
  }

  @Test
  fun moveObjective_reorders_and_persists() = runBlocking {
    repo.setObjectives(
        listOf(
            Objective("A", "CS", 5, false, DayOfWeek.MONDAY),
            Objective("B", "CS", 10, false, DayOfWeek.TUESDAY),
            Objective("C", "CS", 15, false, DayOfWeek.WEDNESDAY),
        ))
    val moved = repo.moveObjective(0, 2)
    assertEquals(listOf("B", "C", "A"), moved.map { it.title })
    val snapshot = repo.getObjectives()
    assertEquals(listOf("B", "C", "A"), snapshot.map { it.title })
  }

  @Test
  fun setObjectives_replaces_entire_collection() = runBlocking {
    val after =
        repo.setObjectives(
            listOf(
                Objective("N1", "CS", 5, false, DayOfWeek.THURSDAY),
                Objective("N2", "CS", 15, true, DayOfWeek.FRIDAY),
            ))
    assertEquals(listOf("N1", "N2"), after.map { it.title })
    val snapshot = repo.getObjectives()
    assertEquals(listOf("N1", "N2"), snapshot.map { it.title })
  }

  @Test
  fun out_of_range_operations_are_noops() = runBlocking {
    repo.setObjectives(
        listOf(
            Objective("A", "CS", 5, false, DayOfWeek.MONDAY),
            Objective("B", "CS", 10, false, DayOfWeek.TUESDAY),
        ))
    val before = repo.getObjectives()
    val u = repo.updateObjective(index = 10, obj = Objective("X", "CS", 0, false, DayOfWeek.MONDAY))
    val r = repo.removeObjective(index = 99)
    val m = repo.moveObjective(fromIndex = 1, toIndex = 1)
    assertEquals(before, u)
    assertEquals(before, r)
    assertEquals(before, m)
  }
}
