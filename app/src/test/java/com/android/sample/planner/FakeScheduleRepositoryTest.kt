package com.android.sample.planner

import com.android.sample.feature.schedule.data.schedule.EventKind
import com.android.sample.feature.schedule.data.schedule.Priority
import com.android.sample.feature.schedule.data.schedule.ScheduleEvent
import com.android.sample.feature.schedule.data.schedule.SourceTag
import com.android.sample.feature.schedule.repository.schedule.FakeScheduleRepository
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class FakeScheduleRepositoryTest {

  @Before
  fun setup() {
    FakeScheduleRepository.clearForTest()
  }

  private fun event(id: String, date: LocalDate) =
      ScheduleEvent(
          id = id,
          title = "Event $id",
          date = date,
          time = null,
          durationMinutes = 60,
          kind = EventKind.STUDY,
          priority = Priority.MEDIUM,
          sourceTag = SourceTag.Task)

  @Test
  fun save_inserts_and_updates() = runBlocking {
    val d = LocalDate.of(2024, 1, 1)

    val e1 = event("1", d)
    FakeScheduleRepository.save(e1)
    assertEquals(1, FakeScheduleRepository.events.value.size)

    val updated = e1.copy(title = "Updated")
    FakeScheduleRepository.save(updated)
    val stored = FakeScheduleRepository.events.value.single()

    assertEquals("Updated", stored.title)
  }

  @Test
  fun delete_removes_by_id() = runBlocking {
    val d = LocalDate.of(2024, 1, 2)

    FakeScheduleRepository.save(event("1", d))
    FakeScheduleRepository.save(event("2", d))

    FakeScheduleRepository.delete("1")

    val ids = FakeScheduleRepository.events.value.map { it.id }
    assertEquals(listOf("2"), ids)
  }

  @Test
  fun delete_blank_id_is_noop() = runBlocking {
    val d = LocalDate.of(2024, 1, 2)

    FakeScheduleRepository.save(event("1", d))
    FakeScheduleRepository.delete("")

    assertEquals(1, FakeScheduleRepository.events.value.size)
  }

  @Test
  fun getEventsBetween_returns_correct_range() = runBlocking {
    val inside1 = LocalDate.of(2024, 1, 1)
    val inside2 = LocalDate.of(2024, 1, 3)
    val outside = LocalDate.of(2024, 1, 10)

    FakeScheduleRepository.save(event("a", inside1))
    FakeScheduleRepository.save(event("b", inside2))
    FakeScheduleRepository.save(event("c", outside))

    val result =
        FakeScheduleRepository.getEventsBetween(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 5))
            .map { it.id }

    assertEquals(listOf("a", "b"), result)
  }

  @Test
  fun moveEventDate_updates_and_returns_boolean() = runBlocking {
    val d1 = LocalDate.of(2024, 1, 1)
    val d2 = LocalDate.of(2024, 1, 5)

    FakeScheduleRepository.save(event("1", d1))

    val ok = FakeScheduleRepository.moveEventDate("1", d2)
    val updated = FakeScheduleRepository.getById("1")

    assertTrue(ok)
    assertEquals(d2, updated!!.date)

    val fail = FakeScheduleRepository.moveEventDate("missing", d2)
    assertFalse(fail)
  }

  @Test
  fun getEventsForDate_filters_correctly() = runBlocking {
    val d1 = LocalDate.of(2024, 2, 1)
    val d2 = LocalDate.of(2024, 2, 2)

    FakeScheduleRepository.save(event("1", d1))
    FakeScheduleRepository.save(event("2", d2))

    val result = FakeScheduleRepository.getEventsForDate(d2).map { it.id }
    assertEquals(listOf("2"), result)
  }

  @Test
  fun getEventsForWeek_returns_7_day_range() = runBlocking {
    val start = LocalDate.of(2024, 3, 4) // Monday
    val end = start.plusDays(6) // Sunday

    FakeScheduleRepository.save(event("before", start.minusDays(1)))
    FakeScheduleRepository.save(event("monday", start))
    FakeScheduleRepository.save(event("sunday", end))
    FakeScheduleRepository.save(event("after", end.plusDays(1)))

    val result = FakeScheduleRepository.getEventsForWeek(start).map { it.id }
    assertEquals(listOf("monday", "sunday"), result)
  }
}
