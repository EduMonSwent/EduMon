package com.android.sample.moodLogging

import com.android.sample.data.FakeMoodRepo
import com.android.sample.data.MoodRepositoryInterface
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Pure JVM test for the in-memory FakeRepo used by MoodLoggingViewModelTest. Mirrors the style of
 * InMemoryFlashcardsRepositoryTest.
 */
class FakeMoodRepositoryTest {

  private lateinit var repo: MoodRepositoryInterface
  private lateinit var today: LocalDate

  @Before
  fun setup() {
    repo = FakeMoodRepo()
    today = LocalDate.of(2025, 10, 30)
  }

  @Test
  fun upsert_then_get_returns_same_entry() = runBlocking {
    repo.upsertForDate(today, 4, "study flow")
    val r = repo.getForDate(today)

    assertNotNull(r)
    assertEquals(4, r!!.mood)
    assertEquals("study flow", r.note)
    assertEquals(today.toEpochDay(), r.dateEpochDay)
  }

  @Test
  fun get_returns_null_when_absent() = runBlocking {
    val r = repo.getForDate(today)
    assertNull(r)
  }

  @Test
  fun getRange_is_inclusive_and_sorted() = runBlocking {
    val start = today.minusDays(6)
    (0..6).forEach { i ->
      val d = start.plusDays(i.toLong())
      repo.upsertForDate(d, (i % 5) + 1, "n$i")
    }

    val range = repo.getRange(start, today)
    assertEquals(7, range.size)
    assertEquals(start.toEpochDay(), range.first().dateEpochDay)
    assertEquals(today.toEpochDay(), range.last().dateEpochDay)
    // Ensure sorted ascending
    assertTrue(range.zipWithNext().all { (a, b) -> a.dateEpochDay <= b.dateEpochDay })
  }

  @Test
  fun multiple_upserts_overwrite_existing_entry() = runBlocking {
    repo.upsertForDate(today, 3, "first")
    repo.upsertForDate(today, 5, "updated")

    val r = repo.getForDate(today)
    assertNotNull(r)
    assertEquals(5, r!!.mood)
    assertEquals("updated", r.note)
  }
}
