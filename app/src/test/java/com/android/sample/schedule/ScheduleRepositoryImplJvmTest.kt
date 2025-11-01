package com.android.sample.schedule

import com.android.sample.model.StudyItem
import com.android.sample.model.TaskType
import com.android.sample.model.calendar.PlannerRepository
import com.android.sample.model.planner.Class as PlannerClass
import com.android.sample.model.planner.ClassType
import com.android.sample.model.planner.PlannerRepository as ClassRepo
import com.android.sample.model.schedule.*
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

// ----------------- Fake repositories -----------------

private class FakeTasksRepo : PlannerRepository {
  private val _flow = MutableStateFlow<List<StudyItem>>(emptyList())
  override val tasksFlow: StateFlow<List<StudyItem>>
    get() = _flow

  private val store = linkedMapOf<String, StudyItem>()

  override suspend fun saveTask(item: StudyItem) {
    store[item.id] = item
    _flow.value = store.values.toList()
  }

  override suspend fun deleteTask(id: String) {
    store.remove(id)
    _flow.value = store.values.toList()
  }

  override suspend fun getTaskById(id: String) = store[id]

  override suspend fun getAllTasks() = store.values.toList()
}

private class FakeClassesRepo : ClassRepo() {
  private val _today = MutableStateFlow<List<PlannerClass>>(emptyList())

  fun emit(classes: List<PlannerClass>) {
    _today.value = classes
  }

  override fun getTodayClassesFlow() = _today
}

// ----------------- Tests -----------------

@OptIn(ExperimentalCoroutinesApi::class)
class ScheduleRepositoryImplJvmTest {

  private fun study(id: String, title: String, date: LocalDate) =
      StudyItem(
          id = id,
          title = title,
          description = null,
          date = date,
          time = null,
          durationMinutes = 60,
          isCompleted = false,
          priority = com.android.sample.model.Priority.MEDIUM,
          type = TaskType.WORK)

  private fun klass(id: String, name: String, start: LocalTime, end: LocalTime) =
      PlannerClass(
          id = id,
          courseName = name,
          startTime = start,
          endTime = end,
          location = "X",
          type = ClassType.LECTURE)

  @Test
  fun save_update_delete_and_between_work() = runTest {
    val tasks = FakeTasksRepo()
    val classes = FakeClassesRepo()
    // use a separate scope to avoid uncompleted background jobs
    val scope = CoroutineScope(coroutineContext + SupervisorJob())
    val repo = ScheduleRepositoryImpl(tasks, classes, coroutineScope = scope)

    val d0 = LocalDate.of(2025, 3, 3)
    val e1 = ScheduleEvent(id = "1", title = "A", date = d0, kind = EventKind.STUDY)
    val e2 = ScheduleEvent(id = "2", title = "B", date = d0.plusDays(2), kind = EventKind.STUDY)

    repo.save(e1)
    repo.save(e2)
    advanceUntilIdle() // wait for repository collectors

    val between = repo.getEventsBetween(d0, d0.plusDays(3))
    assertEquals(setOf("1", "2"), between.map { it.id }.toSet())

    // Update and verify persistence
    repo.update(e2.copy(title = "B2"))
    advanceUntilIdle()
    assertEquals("B2", tasks.getTaskById("2")?.title)

    // Move event and verify new date
    assertTrue(repo.moveEventDate("2", d0.plusDays(10)))
    advanceUntilIdle()
    assertEquals(d0.plusDays(10), tasks.getTaskById("2")?.date)

    // Delete and verify removal
    repo.delete("1")
    advanceUntilIdle()
    assertNull(tasks.getTaskById("1"))

    scope.cancel() // ✅ important: clean up child jobs
  }

  @Test
  fun events_flow_includes_tasks_and_today_classes() = runTest {
    val tasks = FakeTasksRepo()
    val classes = FakeClassesRepo()
    val scope = CoroutineScope(coroutineContext + SupervisorJob())
    val repo = ScheduleRepositoryImpl(tasks, classes, coroutineScope = scope)

    // Prepare both sources
    tasks.saveTask(study("t1", "Study", LocalDate.now()))
    classes.emit(listOf(klass("c1", "Math", LocalTime.of(8, 0), LocalTime.of(10, 0))))
    advanceUntilIdle()

    val list = repo.events.value
    assertTrue(
        "Missing Study task in events",
        list.any { it.sourceTag == SourceTag.Task && it.title == "Study" })
    assertTrue(
        "Missing Math class in events",
        list.any { it.sourceTag == SourceTag.Class && it.title == "Math" })

    scope.cancel()
  }
}
