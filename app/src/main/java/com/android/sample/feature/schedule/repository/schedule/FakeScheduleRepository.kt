package com.android.sample.feature.schedule.repository.schedule

import com.android.sample.feature.schedule.data.schedule.ScheduleEvent
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Simple in-memory implementation of ScheduleRepository, similar in spirit to
 * FakeObjectivesRepository.
 *
 * Used by FakeRepositoriesProvider (no Firebase).
 */
object FakeScheduleRepository : ScheduleRepository {

  // Backing state – all schedule events in memory
  private val _events = MutableStateFlow<List<ScheduleEvent>>(emptyList())
  override val events: StateFlow<List<ScheduleEvent>> = _events.asStateFlow()

  internal fun clearForTest() {
    _events.value = emptyList()
  }

  override suspend fun save(event: ScheduleEvent) {
    // Upsert behavior (add or replace by id)
    val current = _events.value.toMutableList()
    val idx = current.indexOfFirst { it.id == event.id }
    if (idx >= 0) {
      current[idx] = event
    } else {
      current.add(event)
    }
    _events.value = current
  }

  override suspend fun update(event: ScheduleEvent) {
    // Same as save for this simple in-memory implementation
    save(event)
  }

  override suspend fun delete(eventId: String) {
    if (eventId.isBlank()) return
    _events.value = _events.value.filterNot { it.id == eventId }
  }

  override suspend fun getEventsBetween(start: LocalDate, end: LocalDate): List<ScheduleEvent> =
      _events.value.filter { it.date in start..end }

  override suspend fun getById(id: String): ScheduleEvent? =
      _events.value.firstOrNull { it.id == id }

  override suspend fun moveEventDate(id: String, newDate: LocalDate): Boolean {
    val event = getById(id) ?: return false
    save(event.copy(date = newDate))
    return true
  }

  override suspend fun getEventsForDate(date: LocalDate): List<ScheduleEvent> =
      _events.value.filter { it.date == date }

  override suspend fun getEventsForWeek(startDate: LocalDate): List<ScheduleEvent> {
    val endDate = startDate.plusDays(6)
    return getEventsBetween(startDate, endDate)
  }

  override suspend fun importEvents(events: List<ScheduleEvent>) {
    val current = _events.value.toMutableList()
    current.addAll(events)
    _events.value = current
  }
}
