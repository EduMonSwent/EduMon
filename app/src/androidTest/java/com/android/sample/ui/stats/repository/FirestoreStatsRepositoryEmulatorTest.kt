package com.android.sample.ui.stats.repository

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

    // Defaults defined in repository
    assertEquals(5, first.totalTimeMin)
    assertEquals(300, first.weeklyGoalMin)
    assertEquals(10, first.completedGoals)
    assertEquals(7, first.progressByDayMin.size)
    // Course map contains known keys
    assertTrue(first.courseTimesMin.containsKey("Analyse I"))
  }

  @Test
  fun update_overwrites_stats_in_firestore_and_local_state() = runBlocking {
    repo.refresh()
    val initial = repo.stats.value

    val updated =
        initial.copy(
            totalTimeMin = initial.totalTimeMin + 15,
            completedGoals = initial.completedGoals + 1,
            weeklyGoalMin = 350,
            courseTimesMin = initial.courseTimesMin.toMutableMap().apply { put("New Course", 20) },
            progressByDayMin =
                initial.progressByDayMin.mapIndexed { i, v -> if (i == 0) v + 10 else v })

    repo.update(updated)

    // Local state updated immediately
    assertEquals(updated, repo.stats.value)

    // Refresh enforces current defaults (by design), but Firestore merge may keep extra map keys.
    repo.refresh()
    val now = repo.stats.value

    // Scalars reset to defaults
    assertEquals(initial.totalTimeMin, now.totalTimeMin)
    assertEquals(initial.weeklyGoalMin, now.weeklyGoalMin)
    assertEquals(initial.completedGoals, now.completedGoals)
    assertEquals(initial.progressByDayMin, now.progressByDayMin)

    // Map contains at least defaults and preserves the extra course key from the earlier update
    assertTrue(now.courseTimesMin.keys.containsAll(initial.courseTimesMin.keys))
    assertTrue(now.courseTimesMin.containsKey("New Course"))
  }

  @Test
  fun refresh_replaces_with_new_defaults_when_changed() = runBlocking {
    // 1) Seed defaults
    repo.refresh()
    val defaults = repo.stats.value

    // 2) Manually change Firestore document to simulate older/different defaults
    val uid = FirebaseEmulator.auth.currentUser!!.uid
    val userDoc = FirebaseEmulator.firestore.collection("users").document(uid)
    val payload =
        mapOf(
            "stats" to
                mapOf(
                    "totalTimeMin" to 0,
                    "weeklyGoalMin" to 100,
                    "completedGoals" to 0,
                    "courseTimesMin" to emptyMap<String, Int>(),
                    "progressByDayMin" to List(7) { 0 }))
    Tasks.await(userDoc.set(payload, SetOptions.merge()))

    // 3) Next refresh should detect divergence and replace with current defaults
    repo.refresh()
    val now = repo.stats.value
    assertEquals(defaults, now)
  }
}
