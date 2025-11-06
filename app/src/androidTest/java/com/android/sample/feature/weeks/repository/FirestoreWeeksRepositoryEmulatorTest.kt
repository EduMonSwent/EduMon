package com.android.sample.feature.weeks.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.feature.weeks.model.WeekContent
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
class FirestoreWeeksRepositoryEmulatorTest {

  private lateinit var repo: FirestoreWeeksRepository

  @Before
  fun setUp() = runBlocking {
    assertTrue(
        "Firebase emulators not reachable (expected UI on 4000; firestore:8080, auth:9099). " +
            "Start with: firebase emulators:start --only firestore,auth",
        FirebaseEmulator.isRunning)

    // Clean both emulators
    FirebaseEmulator.clearAuthEmulator()
    FirebaseEmulator.clearFirestoreEmulator()

    // Sign in anonymously
    Tasks.await(FirebaseEmulator.auth.signInAnonymously())

    repo = FirestoreWeeksRepository(FirebaseEmulator.firestore, FirebaseEmulator.auth)
  }

  @After
  fun tearDown() = runBlocking {
    if (FirebaseEmulator.isRunning) {
      FirebaseEmulator.clearFirestoreEmulator()
      FirebaseEmulator.clearAuthEmulator()
    }
  }

  @Test
  fun getWeeks_seeds_defaults_when_empty_and_recomputes_percent() = runBlocking {
    val weeks = repo.getWeeks()
    // Defaults define 3 weeks in this implementation
    assertEquals(3, weeks.size)
    // Percent should be recomputed from content; first has all done -> 100
    assertEquals(100, weeks[0].percent)
  }

  @Test
  fun updateWeekPercent_clamps_and_persists() = runBlocking {
    // Seed defaults (and triggers recompute on read)
    repo.getWeeks()

    // Compute expected percent from current content (derived value)
    val content = repo.getWeekContent(1)
    val total = content.courses.size + content.exercises.size
    val done = content.courses.count { it.read } + content.exercises.count { it.done }
    val expectedPercent = if (total == 0) 0 else (done * 100) / total

    // Attempt to set an out-of-range value; repository will clamp then recompute on getWeeks()
    val after = repo.updateWeekPercent(1, 999)
    assertEquals(3, after.size)
    assertEquals(expectedPercent, after[1].percent)

    val snapshot = repo.getWeeks()
    assertEquals(expectedPercent, snapshot[1].percent)
  }

  @Test
  fun getWeekContent_returns_content_for_index() = runBlocking {
    // Ensure seeded
    repo.getWeeks()

    val content: WeekContent = repo.getWeekContent(1)
    // Week 2 has at least one course and one exercise in defaults
    assertTrue(content.courses.isNotEmpty())
    assertTrue(content.exercises.isNotEmpty())
  }

  @Test
  fun markExerciseDone_updates_content_and_percent() = runBlocking {
    // Ensure seeded
    repo.getWeeks()

    val before = repo.getWeekContent(1)
    val exerciseId = before.exercises.first().id

    val afterWeeks = repo.markExerciseDone(1, exerciseId, true)

    assertEquals(3, afterWeeks.size)
    // Recompute check via a fresh snapshot
    val snapshot = repo.getWeeks()
    val updated = snapshot[1]
    assertTrue(updated.percent >= 55) // percent increases compared to default 55
  }

  @Test
  fun markCourseRead_updates_content_and_percent() = runBlocking {
    repo.getWeeks()
    val before = repo.getWeekContent(1)
    val courseId = before.courses.first().id

    val afterWeeks = repo.markCourseRead(1, courseId, true)

    assertEquals(3, afterWeeks.size)
    val snapshot = repo.getWeeks()
    val updated = snapshot[1]
    assertTrue(updated.percent >= 55)
  }

  @Test
  fun out_of_range_indices_are_noops() = runBlocking {
    repo.getWeeks()

    val before = repo.getWeeks()

    val u = repo.updateWeekPercent(99, 50)
    val e = repo.markExerciseDone(99, "x", true)
    val c = repo.markCourseRead(99, "y", true)

    assertEquals(before, u)
    assertEquals(before, e)
    assertEquals(before, c)
  }

  @Test
  fun dayStatuses_seed_defaults_when_empty() = runBlocking {
    val statuses = repo.getDayStatuses()
    assertEquals(DayOfWeek.values().size, statuses.size)
    // Every even index is true in defaults
    assertTrue(statuses.first().metTarget)
  }
}
