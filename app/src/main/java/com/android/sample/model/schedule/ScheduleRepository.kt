package com.android.sample.model.schedule

import com.android.sample.model.calendar.PlannerRepository
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

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
}

class ScheduleRepositoryImpl(
    private val taskRepo: PlannerRepository,
    private val classRepo: com.android.sample.model.planner.PlannerRepository,
    coroutineScope: CoroutineScope? = null
) : ScheduleRepository {

  private val scope = coroutineScope ?: CoroutineScope(Dispatchers.Default)

  private val tasksFlow: Flow<List<ScheduleEvent>> =
      taskRepo.tasksFlow.map { it.map(StudyItemMapper::toScheduleEvent) }

  private val classesTodayFlow: Flow<List<ScheduleEvent>> =
      classRepo.getTodayClassesFlow().map { it.map(ClassMapper::toScheduleEvent) }

  private val _events = MutableStateFlow<List<ScheduleEvent>>(emptyList())
  override val events: StateFlow<List<ScheduleEvent>> = _events.asStateFlow()

  init {
    scope.launch {
      combine(tasksFlow, classesTodayFlow) { tasks, classes ->
            (tasks + classes).sortedWith(
                compareBy<ScheduleEvent> { it.date }.thenBy { it.time ?: LocalTime.MIN })
          }
          .collect { _events.value = it }
    }
  }

  override suspend fun save(event: ScheduleEvent) {
    when (event.sourceTag) {
      SourceTag.Task -> {
        val study = StudyItemMapper.fromScheduleEvent(event)
        taskRepo.saveTask(study)
      }
      SourceTag.Class -> {
        // Classes are read-only in FakePlannerRepository (no-op for now)
      }
    }
  }

  override suspend fun update(event: ScheduleEvent) {
    // For Tasks, saveTask(...) acts as upsert
    when (event.sourceTag) {
      SourceTag.Task -> {
        val study = StudyItemMapper.fromScheduleEvent(event)
        taskRepo.saveTask(study)
      }
      SourceTag.Class -> {
        // read-only for now
      }
    }
  }

  override suspend fun delete(eventId: String) {
    // Try tasks first
    taskRepo.getTaskById(eventId)?.let {
      taskRepo.deleteTask(eventId)
      return
    }
    // If you add class saving later, mirror deletions for classes here.
  }

  override suspend fun getEventsBetween(start: LocalDate, end: LocalDate): List<ScheduleEvent> {
    return events.value.filter { it.date in start..end }
  }

  override suspend fun getById(id: String): ScheduleEvent? =
      events.value.firstOrNull { it.id == id }

  override suspend fun moveEventDate(id: String, newDate: LocalDate): Boolean {
    val ev = getById(id) ?: return false
    val updated = ev.copy(date = newDate)
    update(updated)
    return true
  }

  override suspend fun getEventsForDate(date: LocalDate): List<ScheduleEvent> {
    return events.value.filter { it.date == date }
  }

  override suspend fun getEventsForWeek(startDate: LocalDate): List<ScheduleEvent> {
    val endDate = startDate.plusDays(6)
    return getEventsBetween(startDate, endDate)
  }
}
