package com.android.sample.feature.weeks

import com.android.sample.feature.weeks.repository.FakeWeeksRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WeeksRepositoryTest {

  @Test
  fun updateByIndex_clampsAndUpdates() = runTest {
    val repo = FakeWeeksRepository()
    val before = repo.getWeeks()
    assertTrue(before.isNotEmpty())

    val updated = repo.updateWeekPercent(index = 1, percent = 250) // clamp to 100
    assertEquals(100, updated[1].percent)

    val updated2 = repo.updateWeekPercent(index = 1, percent = -10) // clamp to 0
    assertEquals(0, updated2[1].percent)
  }

  @Test
  fun updateByIndex_updatesCorrectWeek() = runTest {
    val repo = FakeWeeksRepository()
    val updated = repo.updateWeekPercent(index = 1, percent = 42)
    // Verify Week 2 is 42, others unchanged initial values (100 for Week 1, 10 for Week 3)
    assertEquals(100, updated[0].percent)
    assertEquals(42, updated[1].percent)
    assertEquals(10, updated[2].percent)
  }

  @Test
  fun updateByIndex_nonExistent_returnsUnchanged() = runTest {
    val repo = FakeWeeksRepository()
    val before = repo.getWeeks()
    val after = repo.updateWeekPercent(index = 42, percent = 77) // out-of-range index
    assertEquals(before, after)
  }

  @Test
  fun getDayStatuses_returnsSevenWithAlternatingPattern() = runTest {
    val repo = FakeWeeksRepository()
    val statuses = repo.getDayStatuses()
    assertEquals(7, statuses.size)
    // Pattern in Fake: idx % 2 == 0 -> metTarget true
    statuses.forEachIndexed { i, ds -> assertEquals(i % 2 == 0, ds.metTarget) }
  }
}
