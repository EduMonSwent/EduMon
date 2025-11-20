package com.android.sample.schedule

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.feature.schedule.data.schedule.EventKind
import com.android.sample.feature.schedule.data.schedule.ScheduleEvent
import com.android.sample.feature.schedule.data.schedule.SourceTag
import com.android.sample.feature.schedule.repository.schedule.FirestoreScheduleRepository
import com.android.sample.util.FirebaseEmulator
import com.google.android.gms.tasks.Tasks
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FirestoreScheduleRepositoryEmulatorTest {

  private lateinit var repo: FirestoreScheduleRepository

  @Before
  fun setUp() = runBlocking {
    FirebaseEmulator.initIfNeeded(ApplicationProvider.getApplicationContext())
    FirebaseEmulator.connectIfRunning()

    assertTrue(
        "Firebase emulators not reachable. Start with: " +
            "firebase emulators:start --only firestore,auth",
        FirebaseEmulator.isRunning)

    FirebaseEmulator.clearAll()
    Tasks.await(FirebaseEmulator.auth.signInAnonymously())

    repo = FirestoreScheduleRepository(FirebaseEmulator.firestore, FirebaseEmulator.auth)
  }

  @After
  fun tearDown() = runBlocking {
    if (FirebaseEmulator.isRunning) {
      FirebaseEmulator.clearAll()
    }
  }

  private suspend fun awaitEventsCount(expected: Int, timeoutMs: Long = 5000) {
    val start = System.currentTimeMillis()
    while (System.currentTimeMillis() - start < timeoutMs) {
      if (repo.events.value.size == expected) return
      delay(50)
    }
    // If it fails, still assert to show what's actually there
    assertEquals(expected, repo.events.value.size)
  }

  @Test
  fun save_and_eventsFlow_are_sorted_by_date_and_time() = runBlocking {
    val today = LocalDate.now()
    val tomorrow = today.plusDays(1)

    val e1 =
        ScheduleEvent(
            title = "B Task",
            date = today,
            time = LocalTime.of(14, 0),
            kind = EventKind.STUDY,
            sourceTag = SourceTag.Task)
    val e2 =
        ScheduleEvent(
            title = "A Task",
            date = today,
            time = LocalTime.of(9, 0),
            kind = EventKind.STUDY,
            sourceTag = SourceTag.Task)
    val e3 =
        ScheduleEvent(
            title = "Tomorrow Task",
            date = tomorrow,
            time = LocalTime.of(8, 0),
            kind = EventKind.PROJECT,
            sourceTag = SourceTag.Task)

    repo.save(e1)
    repo.save(e2)
    repo.save(e3)

    awaitEventsCount(3)

    val events = repo.events.value

    // Sorted by date, then time
    assertEquals(listOf("A Task", "B Task", "Tomorrow Task"), events.map { it.title })
    assertEquals(listOf(today, today, tomorrow), events.map { it.date })
  }

  @Test
  fun update_and_delete_modify_persisted_events() = runBlocking {
    val today = LocalDate.now()
    val base =
        ScheduleEvent(
            title = "Original",
            date = today,
            time = LocalTime.of(10, 0),
            kind = EventKind.STUDY,
            sourceTag = SourceTag.Task)

    repo.save(base)
    awaitEventsCount(1)

    val saved = repo.events.value.first()
    val edited = saved.copy(title = "Edited Title")

    repo.update(edited)

    // Wait for snapshot listener to pick up update
    val start = System.currentTimeMillis()
    while (System.currentTimeMillis() - start < 5000) {
      if (repo.events.value.first().title == "Edited Title") break
      delay(50)
    }

    assertEquals("Edited Title", repo.events.value.first().title)

    repo.delete(saved.id)
    awaitEventsCount(0)
    assertTrue(repo.events.value.isEmpty())
  }

  @Test
  fun moveEventDate_moves_event_and_reflected_in_queries() = runBlocking {
    val today = LocalDate.now()
    val nextWeek = today.plusDays(7)

    val event =
        ScheduleEvent(
            title = "Move Me",
            date = today,
            time = LocalTime.of(15, 0),
            kind = EventKind.STUDY,
            sourceTag = SourceTag.Task)

    repo.save(event)
    awaitEventsCount(1)

    val saved = repo.events.value.first()
    val moved = repo.moveEventDate(saved.id, nextWeek)
    assertTrue(moved)

    // Allow snapshot to update
    val start = System.currentTimeMillis()
    while (System.currentTimeMillis() - start < 5000) {
      val ev = repo.events.value.firstOrNull()
      if (ev != null && ev.date == nextWeek) break
      delay(50)
    }

    val all = repo.events.value
    assertEquals(1, all.size)
    assertEquals(nextWeek, all[0].date)

    val todayEvents = repo.getEventsForDate(today)
    val nextWeekEvents = repo.getEventsForDate(nextWeek)

    assertTrue(todayEvents.isEmpty())
    assertEquals(1, nextWeekEvents.size)
    assertEquals("Move Me", nextWeekEvents[0].title)
  }
}
