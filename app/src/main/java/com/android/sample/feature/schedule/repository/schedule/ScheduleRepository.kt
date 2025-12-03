package com.android.sample.feature.schedule.repository.schedule

import com.android.sample.feature.schedule.data.schedule.ScheduleEvent
import java.time.LocalDate
import kotlinx.coroutines.flow.*

interface ScheduleRepository {
  val events: StateFlow<List<ScheduleEvent>>

  suspend fun save(event: ScheduleEvent)

  suspend fun update(event: ScheduleEvent)

  suspend fun delete(eventId: String)

  suspend fun getEventsBetween(start: LocalDate, end: LocalDate): List<ScheduleEvent>

  suspend fun getById(id: String): ScheduleEvent?

  suspend fun moveEventDate(id: String, newDate: LocalDate): Boolean

  suspend fun getEventsForDate(date: LocalDate): List<ScheduleEvent>

  suspend fun getEventsForWeek(startDate: LocalDate): List<ScheduleEvent>

  suspend fun importEvents(events: List<ScheduleEvent>)
}
