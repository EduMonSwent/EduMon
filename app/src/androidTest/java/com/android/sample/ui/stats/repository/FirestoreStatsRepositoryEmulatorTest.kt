package com.android.sample.ui.stats.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.data.FirestoreUserStatsRepository
import com.android.sample.util.FirebaseEmulator
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FirestoreStatsRepositoryEmulatorTest {

  private lateinit var repo: FirestoreUserStatsRepository

  @Before
  fun setUp() = runBlocking {
    assertTrue(
        "Firebase emulators not reachable (expected UI on 4000; firestore:8080, auth:9099). " +
            "Start with: firebase emulators:start --only firestore,auth",
        FirebaseEmulator.isRunning)

    FirebaseEmulator.clearAuthEmulator()
    FirebaseEmulator.clearFirestoreEmulator()
    Tasks.await(FirebaseEmulator.auth.signInAnonymously())

    // Corrected constructor argument order: auth, firestore
    repo = FirestoreUserStatsRepository(FirebaseEmulator.auth, FirebaseEmulator.firestore)
  }

  @After
  fun tearDown() = runBlocking {
    if (FirebaseEmulator.isRunning) {
      FirebaseEmulator.clearFirestoreEmulator()
      FirebaseEmulator.clearAuthEmulator()
    }
  }

  private suspend fun awaitInitialStats() {
    withTimeout(2000) { // Wait for snapshot listener to deliver initial data
      repo.stats.first { it.lastUpdated != 0L }
    }
  }

  @Test
  fun start_seeds_defaults_when_missing() = runBlocking {
    repo.start()
    awaitInitialStats()
    val first = repo.stats.value

    assertNotNull(first)
    assertEquals(0, first.totalStudyMinutes)
    assertEquals(0, first.streak)
    assertEquals(0, first.todayCompletedPomodoros)
  }

  @Test
  fun addStudyMinutes_persists_and_updates_streak() = runBlocking {
    repo.start()
    awaitInitialStats()

    repo.addStudyMinutes(15)
    val afterFirst = repo.stats.value
    assertEquals(15, afterFirst.totalStudyMinutes)
    assertEquals(15, afterFirst.todayStudyMinutes)
    assertEquals(1, afterFirst.streak)

    // Simulate studying on a new day (but not yesterday)
    val futureTime = System.currentTimeMillis() + 2 * 24 * 60 * 60 * 1000
    // To properly test this, we would need to inject a Clock or delay significantly.
    // For emulator test, we just check standard increment for now.
  }

  @Test
  fun incrementCompletedPomodoros_persists() = runBlocking {
    repo.start()
    awaitInitialStats()

    repo.incrementCompletedPomodoros()
    assertEquals(1, repo.stats.value.todayCompletedPomodoros)

    repo.incrementCompletedPomodoros()
    assertEquals(2, repo.stats.value.todayCompletedPomodoros)
  }

  @Test
  fun update_coins_persists() = runBlocking {
    repo.start()
    awaitInitialStats()
    val initialCoins = repo.stats.value.coins
    val addedCoins = 50
    repo.updateCoins(addedCoins)

    assertEquals(initialCoins + addedCoins, repo.stats.value.coins)
  }
}
