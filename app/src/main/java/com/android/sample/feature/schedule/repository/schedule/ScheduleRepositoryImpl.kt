package com.android.sample.feature.schedule.repository.schedule

import android.content.res.Resources
import com.android.sample.feature.schedule.data.schedule.EventKind
import com.android.sample.feature.schedule.data.schedule.ScheduleEvent
import com.android.sample.feature.schedule.data.schedule.SourceTag
import com.android.sample.feature.schedule.repository.calendar.CalendarRepository
import com.android.sample.feature.schedule.repository.planner.PlannerRepository
import com.android.sample.repos_providors.FakeRepositoriesProvider.scheduleRepository
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ScheduleRepositoryImpl(
    private val taskRepo: CalendarRepository,
    private val classRepo: PlannerRepository,
    private val resources: Resources,
    coroutineScope: CoroutineScope? = null
) : ScheduleRepository {

  private val scope = coroutineScope ?: CoroutineScope(Dispatchers.Default)

  // Combine tasks and classes into unified ScheduleEvents
  private val tasksFlow: Flow<List<ScheduleEvent>> =
      taskRepo.tasksFlow.map { tasks ->
        tasks.map { StudyItemMapper.toScheduleEvent(it, resources) }
      }

  private val classesFlow: Flow<List<ScheduleEvent>> =
      scheduleRepository.events // from FirestoreScheduleRepository
          .map { events -> events.filter { it.sourceTag == SourceTag.Class } }

  private val _events = MutableStateFlow<List<ScheduleEvent>>(emptyList())
  override val events: StateFlow<List<ScheduleEvent>> = _events.asStateFlow()

  init {
    scope.launch {
      combine(tasksFlow, classesFlow) { tasks, classes ->
            // Unified event stream with proper sorting
            (tasks + classes).sortedWith(
                compareBy<ScheduleEvent> { it.date }.thenBy { it.time ?: LocalTime.MIN })
          }
          .collect { _events.value = it }
    }
  }

  override suspend fun save(event: ScheduleEvent) {
    when (event.sourceTag) {
      SourceTag.Task -> {
        val studyItem = StudyItemMapper.fromScheduleEvent(event, resources)
        taskRepo.saveTask(studyItem)
      }
      SourceTag.Class -> {
        // TODO: extend to support saving via classRepo.saveClass(event)
      }
    }
  }

  override suspend fun update(event: ScheduleEvent) {
    when (event.sourceTag) {
      SourceTag.Task -> {
        val studyItem = StudyItemMapper.fromScheduleEvent(event, resources) // upsert
        taskRepo.saveTask(studyItem)
      }
      SourceTag.Class -> {
        // TODO: extend to support updating
      }
    }
  }

  override suspend fun delete(eventId: String) {
    // Try to delete from tasks first, then classes if needed
    taskRepo.getTaskById(eventId)?.let {
      taskRepo.deleteTask(eventId)
      return
    }
    // Future: Add class deletion when classes become mutable
  }

  override suspend fun getEventsBetween(start: LocalDate, end: LocalDate): List<ScheduleEvent> =
      events.value.filter { it.date in start..end }

  override suspend fun getById(id: String): ScheduleEvent? =
      events.value.firstOrNull { it.id == id }

  override suspend fun moveEventDate(id: String, newDate: LocalDate): Boolean {
    val event = getById(id) ?: return false
    val updatedEvent = event.copy(date = newDate)
    update(updatedEvent)
    return true
  }

  override suspend fun getEventsForDate(date: LocalDate): List<ScheduleEvent> =
      events.value.filter { it.date == date }

  override suspend fun getEventsForWeek(startDate: LocalDate): List<ScheduleEvent> {
    val endDate = startDate.plusDays(6)
    return getEventsBetween(startDate, endDate)
  }

  // Additional unified queries
  suspend fun getEventsByKind(kind: EventKind): List<ScheduleEvent> =
      events.value.filter { it.kind == kind }

  suspend fun getUpcomingEvents(days: Int = 7): List<ScheduleEvent> {
    val start = LocalDate.now()
    val end = start.plusDays(days.toLong())
    return getEventsBetween(start, end)
  }

  override suspend fun importEvents(events: List<ScheduleEvent>) {}
}
