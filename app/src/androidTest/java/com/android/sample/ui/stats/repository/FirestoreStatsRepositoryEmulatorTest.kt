package com.android.sample.ui.stats.repository

// This code has been written partially using A.I (LLM).

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.util.FirebaseEmulator
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FirestoreStatsRepositoryEmulatorTest {

  private lateinit var repo: FirestoreStatsRepository

  @Before
  fun setUp() = runBlocking {
    // Must be called before accessing auth/firestore (side-effect only)
    FirebaseEmulator.connectIfRunning()

    assertTrue(
        "Firebase emulators not reachable (expected UI on 4000; firestore:8080, auth:9099). " +
            "Start with: firebase emulators:start --only firestore,auth",
        FirebaseEmulator.isRunning)

    // Clean isolated state per test
    FirebaseEmulator.clearAuthEmulator()
    FirebaseEmulator.clearFirestoreEmulator()

    // Sign in anonymously
    Tasks.await(FirebaseEmulator.auth.signInAnonymously())

    repo = FirestoreStatsRepository(FirebaseEmulator.firestore, FirebaseEmulator.auth)
  }

  @After
  fun tearDown() = runBlocking {
    // Ensure emulator connection is initialized before checks/clears
    FirebaseEmulator.connectIfRunning()
    if (FirebaseEmulator.isRunning) {
      FirebaseEmulator.clearFirestoreEmulator()
      FirebaseEmulator.clearAuthEmulator()
    }
  }

  @Test
  fun refresh_seeds_defaults_when_missing() = runBlocking {
    // First refresh ensures defaults exist and publishes them
    repo.refresh()
    val first = repo.stats.value

    // Defaults defined in current FirestoreStatsRepository
    assertEquals(0, first.totalTimeMin)
    assertEquals(0, first.weeklyGoalMin)
    assertEquals(0, first.completedGoals)
    assertEquals(7, first.progressByDayMin.size)
    assertTrue(first.courseTimesMin.isEmpty())
  }

  @Test
  fun update_persists_in_firestore_and_is_read_back_on_refresh() = runBlocking {
    repo.refresh()
    val initial = repo.stats.value

    val updated =
        initial.copy(
            totalTimeMin = initial.totalTimeMin + 15,
            completedGoals = initial.completedGoals + 1,
            weeklyGoalMin = 350,
            courseTimesMin = initial.courseTimesMin.toMutableMap().apply { put("New Course", 20) },
            progressByDayMin =
                initial.progressByDayMin.mapIndexed { index, value ->
                  if (index == 0) value + 10 else value
                })

    // Write new stats
    repo.update(updated)

    // Local state is updated immediately
    assertEquals(updated, repo.stats.value)

    // And a subsequent refresh re-reads the same values from Firestore
    repo.refresh()
    val now = repo.stats.value
    assertEquals(updated, now)
  }

  @Test
  fun refresh_applies_external_changes_from_firestore_document() = runBlocking {
    // 1) Seed defaults
    repo.refresh()
    val defaults = repo.stats.value

    // 2) Manually change the stats document in Firestore
    val uid = FirebaseEmulator.auth.currentUser!!.uid
    val statsDoc =
        FirebaseEmulator.firestore
            .collection("users")
            .document(uid)
            .collection("stats")
            .document("stats")

    val external =
        mapOf(
            "totalTimeMin" to (defaults.totalTimeMin + 100),
            "weeklyGoalMin" to 500,
            "completedGoals" to 3,
            "courseTimesMin" to mapOf("External Course" to 45),
            "progressByDayMin" to List(7) { index -> if (index == 2) 30 else 0 },
        )

    Tasks.await(statsDoc.set(external, SetOptions.merge()))

    // 3) Next refresh should read the modified document
    repo.refresh()
    val now = repo.stats.value

    assertEquals(external["totalTimeMin"], now.totalTimeMin)
    assertEquals(external["weeklyGoalMin"], now.weeklyGoalMin)
    assertEquals(external["completedGoals"], now.completedGoals)
    assertEquals(7, now.progressByDayMin.size)
    assertTrue(now.courseTimesMin.containsKey("External Course"))
  }
}
