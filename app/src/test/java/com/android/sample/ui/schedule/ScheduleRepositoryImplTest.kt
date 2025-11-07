package com.android.sample.ui.schedule

import android.content.res.Resources
import androidx.test.core.app.ApplicationProvider
import com.android.sample.model.PlannerRepository // TASKS interface
import com.android.sample.model.StudyItem
import com.android.sample.model.TaskType
import com.android.sample.model.planner.FakePlannerRepository // your CLASSES fake
import com.android.sample.model.schedule.EventKind
import com.android.sample.model.schedule.ScheduleEvent
import com.android.sample.model.schedule.ScheduleRepositoryImpl
import com.android.sample.model.schedule.SourceTag
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ScheduleRepositoryImplTest {

  private val dispatcher = StandardTestDispatcher()
  private lateinit var testScope: TestScope

  private lateinit var tasksRepo: FakeTasksRepo // lightweight TASKS fake
  private lateinit var classesRepo: FakePlannerRepository // your CLASSES fake
  private lateinit var repo: ScheduleRepositoryImpl
  private lateinit var resources: Resources

  @Before
  fun setUp() {
    Dispatchers.setMain(dispatcher)
    testScope = TestScope(dispatcher)

    resources = ApplicationProvider.getApplicationContext<android.content.Context>().resources
    tasksRepo = FakeTasksRepo()
    classesRepo = FakePlannerRepository()

    // IMPORTANT: pass resources + testScope so internal combine() runs on our scheduler
    repo = ScheduleRepositoryImpl(tasksRepo, classesRepo, resources, testScope)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun combine_and_sorting_merges_tasks_and_classes_by_date_then_time() =
      runTest(dispatcher) {
        val today = LocalDate.now()

        // Seed TASKS (8:00 and 10:00)
        tasksRepo.emitTasks(
            listOf(
                StudyItem(
                    title = "T0", date = today, time = LocalTime.of(8, 0), type = TaskType.STUDY),
                StudyItem(
                    title = "T1", date = today, time = LocalTime.of(10, 0), type = TaskType.STUDY)))
        // classesRepo already emits [Algorithms 9:00, Data Structures 11:00, Networks 14:00]
        advanceUntilIdle()

        val titlesInOrder = repo.events.value.map { it.title }
        assertEquals(listOf("T0", "Algorithms", "T1", "Data Structures", "Networks"), titlesInOrder)
      }

  @Test
  fun save_update_delete_and_queries_work_via_tasksRepo() =
      runTest(dispatcher) {
        val d = LocalDate.now()
        val e =
            ScheduleEvent(title = "X", date = d, kind = EventKind.STUDY, sourceTag = SourceTag.Task)

        repo.save(e) // → tasksRepo.saveTask
        advanceUntilIdle()
        assertNotNull(tasksRepo.getTaskById(e.id))

        repo.update(e.copy(title = "X2"))
        advanceUntilIdle()
        assertEquals("X2", tasksRepo.getTaskById(e.id)?.title)

        assertEquals(e.id, repo.getById(e.id)?.id)
        assertTrue(repo.getEventsBetween(d, d).any { it.id == e.id })
        assertTrue(repo.getEventsForDate(d).any { it.id == e.id })
        assertTrue(repo.getEventsForWeek(d).any { it.id == e.id })

        repo.delete(e.id)
        advanceUntilIdle()
        assertTrue(tasksRepo.getTaskById(e.id) == null)
      }

  @Test
  fun moveEventDate_updates_date_using_update() =
      runTest(dispatcher) {
        val d = LocalDate.now()
        val e =
            ScheduleEvent(
                title = "MoveMe", date = d, kind = EventKind.STUDY, sourceTag = SourceTag.Task)
        repo.save(e)
        advanceUntilIdle()

        val newDate = d.plusDays(3)
        val ok = repo.moveEventDate(e.id, newDate)
        advanceUntilIdle()
        assertTrue(ok)
        assertEquals(newDate, repo.getById(e.id)?.date)
      }

  // ---------------- TASKS fake (implements com.android.sample.model.PlannerRepository)
  private class FakeTasksRepo : PlannerRepository {
    private val _tasks = MutableStateFlow<List<StudyItem>>(emptyList())
    override val tasksFlow: StateFlow<List<StudyItem>> = _tasks

    fun emitTasks(list: List<StudyItem>) {
      _tasks.value = list
    }

    override suspend fun getAllTasks(): List<StudyItem> = _tasks.value

    override suspend fun getTaskById(taskId: String): StudyItem? =
        _tasks.value.firstOrNull { it.id == taskId }

    override suspend fun saveTask(task: StudyItem) {
      _tasks.value = _tasks.value.filterNot { it.id == task.id } + task
    }

    override suspend fun deleteTask(taskId: String) {
      _tasks.value = _tasks.value.filterNot { it.id == taskId }
    }
  }
}
