import com.android.sample.model.schedule.ScheduleEvent
import com.android.sample.model.schedule.ScheduleRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

private class FakeScheduleRepository : ScheduleRepository {
  private val _events = MutableStateFlow<List<ScheduleEvent>>(emptyList())
  override val events: StateFlow<List<ScheduleEvent>>
    get() = _events

  fun emit(vararg ev: ScheduleEvent) {
    _events.value = ev.toList()
  }

  // Record calls (for assertions)
  val moved = mutableListOf<Pair<String, LocalDate>>()
  val saved = mutableListOf<ScheduleEvent>()
  val deleted = mutableListOf<String>()
  val updated = mutableListOf<ScheduleEvent>()

  override suspend fun save(event: ScheduleEvent) {
    saved.add(event)
    // reflect into the stream (simulate upsert)
    val exists = _events.value.any { it.id == event.id }
    _events.value =
        if (exists) {
          _events.value.map { if (it.id == event.id) event else it }
        } else {
          _events.value + event
        }
  }

  override suspend fun update(event: ScheduleEvent) {
    updated.add(event)
    // reflect into the stream
    _events.value = _events.value.map { if (it.id == event.id) event else it }
  }

  override suspend fun delete(eventId: String) {
    deleted.add(eventId)
    // reflect into the stream
    _events.value = _events.value.filterNot { it.id == eventId }
  }

  override suspend fun getEventsBetween(start: LocalDate, end: LocalDate) =
      _events.value.filter { it.date in start..end }

  override suspend fun getById(id: String) = _events.value.firstOrNull { it.id == id }

  override suspend fun moveEventDate(id: String, newDate: LocalDate): Boolean {
    moved.add(id to newDate)
    // (optional) reflect into the stream too, keeps internal state consistent
    _events.value = _events.value.map { if (it.id == id) it.copy(date = newDate) else it }
    return true
  }

  override suspend fun getEventsForDate(date: LocalDate) = _events.value.filter { it.date == date }

  override suspend fun getEventsForWeek(startDate: LocalDate) =
      _events.value.filter { it.date in startDate..startDate.plusDays(6) }
}
