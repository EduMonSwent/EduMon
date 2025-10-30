package com.android.sample.moodLogging

import com.android.sample.data.FakeMoodRepo
import com.android.sample.ui.mood.ChartMode
import com.android.sample.ui.mood.MoodLoggingViewModel
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Pure JVM unit tests for the plain ViewModel version of MoodLoggingViewModel. No Android context,
 * no Robolectric, no DataStore.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MoodLoggingViewModelTest {

  private val dispatcher = StandardTestDispatcher()
  private lateinit var repo: FakeMoodRepo
  private lateinit var vm: MoodLoggingViewModel
  private lateinit var today: LocalDate

  @Before
  fun setUp() {
    Dispatchers.setMain(dispatcher)
    repo = FakeMoodRepo()
    today = LocalDate.of(2025, 10, 30) // fixed "today" for deterministic tests
    vm = MoodLoggingViewModel(repo, clock = { today })
    // VM init launches refreshAll(); give it a turn
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun initial_state_defaults_are_correct() = runTest {
    advanceUntilIdle()
    val s = vm.ui.value
    assertEquals(today, s.today)
    assertEquals(3, s.selectedMood)
    assertTrue(s.note.isEmpty())
    assertEquals(7, s.last7Days.size)
    assertEquals(30, s.monthEntries.size)
    assertEquals(ChartMode.WEEK, s.chartMode)
    assertNull(s.existingToday)
    // lists are gap-filled initially
    assertTrue(s.last7Days.any { it.mood == 0 })
    assertTrue(s.monthEntries.any { it.mood == 0 })
  }

  @Test
  fun selecting_mood_and_note_then_saveToday_persists_and_refreshes() = runTest {
    advanceUntilIdle()

    vm.onMoodSelected(5)
    vm.onNoteChanged("good day")
    vm.saveToday()
    advanceUntilIdle()

    val s = vm.ui.value
    assertEquals(5, s.selectedMood)
    assertEquals("good day", s.note)
    assertNotNull(s.existingToday)
    assertEquals(today.toEpochDay(), s.existingToday!!.dateEpochDay)
    // repo actually contains it
    val fromRepo = repo.getForDate(today)
    assertEquals(5, fromRepo?.mood)
    assertEquals("good day", fromRepo?.note)
  }

  @Test
  fun mood_selection_is_clamped_and_note_is_truncated() = runTest {
    advanceUntilIdle()

    vm.onMoodSelected(99) // -> 5
    vm.onNoteChanged("a".repeat(1000)) // -> 140 chars
    val s = vm.ui.value
    assertEquals(5, s.selectedMood)
    assertEquals(140, s.note.length)
  }

  @Test
  fun chartMode_toggles_properly() = runTest {
    advanceUntilIdle()

    vm.onChartMode(ChartMode.MONTH)
    assertEquals(ChartMode.MONTH, vm.ui.value.chartMode)
    vm.onChartMode(ChartMode.WEEK)
    assertEquals(ChartMode.WEEK, vm.ui.value.chartMode)
  }

  @Test
  fun refreshAll_rebuilds_week_and_month_data() = runTest {
    advanceUntilIdle()

    // Save moods for 5 consecutive "today"s (same day overwrite is fine; we just exercise code)
    repeat(5) {
      vm.onMoodSelected((it % 5) + 1)
      vm.onNoteChanged("note$it")
      vm.saveToday()
      advanceUntilIdle()
    }

    vm.refreshAll()
    advanceUntilIdle()

    val s = vm.ui.value
    assertEquals(7, s.last7Days.size)
    assertTrue(s.last7Days.any { it.mood > 0 })
    assertEquals(30, s.monthEntries.size)
  }

  @Test
  fun week_contains_gaps_for_missing_days() = runTest {
    advanceUntilIdle()

    // If your VM has the helper saveForDate, use it. Otherwise, upsert directly in repo then
    // refresh.
    // Using VM helper (recommended):
    try {
      val method = vm::class.members.firstOrNull { it.name == "saveForDate" }
      if (method != null) {
        for (offset in 0..6 step 2) {
          vm.onMoodSelected(4)
          vm.onNoteChanged("d$offset")
          vm.saveForDate(today.minusDays(offset.toLong()), 4, "d$offset")
        }
      } else {
        // Fallback: write via repo (we can access fake here) then ask VM to refresh
        for (offset in 0..6 step 2) {
          repo.upsertForDate(today.minusDays(offset.toLong()), 4, "d$offset")
        }
      }
    } catch (_: Throwable) {
      // same fallback if reflection fails
      for (offset in 0..6 step 2) {
        repo.upsertForDate(today.minusDays(offset.toLong()), 4, "d$offset")
      }
    }

    vm.refreshAll()
    advanceUntilIdle()

    val week = vm.ui.value.last7Days
    assertEquals(7, week.size)
    assertTrue(week.any { it.mood == 0 }) // at least one gap
  }
}
