package com.android.sample.schedule

import com.android.sample.model.Priority as TaskPriority
import com.android.sample.model.StudyItem
import com.android.sample.model.TaskType
import com.android.sample.model.calendar.PlannerRepository
import com.android.sample.ui.schedule.WeeklyPlanAdjuster
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakePlannerRepository : PlannerRepository {
  private val items = linkedMapOf<String, StudyItem>()
  private val _flow = MutableStateFlow<List<StudyItem>>(emptyList())
  override val tasksFlow: StateFlow<List<StudyItem>>
    get() = _flow

  val saved = mutableListOf<StudyItem>()

  override suspend fun saveTask(item: StudyItem) {
    items[item.id] = item
    _flow.value = items.values.toList()
    saved.add(item)
  }

  override suspend fun deleteTask(id: String) {
    items.remove(id)
    _flow.value = items.values.toList()
  }

  override suspend fun getTaskById(id: String): StudyItem? = items[id]

  override suspend fun getAllTasks(): List<StudyItem> = items.values.toList()

  fun seed(vararg task: StudyItem) {
    task.forEach { items[it.id] = it }
    _flow.value = items.values.toList()
  }
}

class WeeklyPlanAdjusterTest {

  private fun task(
      id: String,
      date: LocalDate,
      completed: Boolean = false,
      priority: TaskPriority = TaskPriority.MEDIUM
  ) =
      StudyItem(
          id = id,
          title = id,
          description = null,
          date = date,
          time = LocalTime.of(9, 0),
          durationMinutes = 60,
          isCompleted = completed,
          priority = priority,
          type = TaskType.WORK)

  @Test
  fun missed_tasks_are_moved_to_same_weekday_next_week() = runTest {
    val repo = FakePlannerRepository()
    val today = LocalDate.of(2025, 3, 6) // Thu
    val missedYesterday = task("m1", today.minusDays(1), completed = false)
    val onTimeToday = task("ok", today, completed = false)

    repo.seed(missedYesterday, onTimeToday)

    WeeklyPlanAdjuster(repo).rebalance(today)

    // The missed task should be saved again with date + 1 week
    val savedMissed = repo.saved.first { it.id == "m1" }
    assertEquals(missedYesterday.date.plusWeeks(1), savedMissed.date)

    // Only the missed one is moved by this rule
    assertTrue(repo.saved.any { it.id == "m1" })
  }

  @Test
  fun completed_early_pulls_best_candidate_from_next_week_into_today() = runTest {
    val repo = FakePlannerRepository()
    val today = LocalDate.of(2025, 3, 6) // Thu
    val nextWeekStart = today.plusWeeks(1).with(java.time.DayOfWeek.MONDAY)

    // Something completed in the future => triggers pull
    val doneTomorrow = task("done", today.plusDays(1), completed = true)

    // Two candidates next week, choose by priority DESC then by earlier date
    val lowPriority =
        task("low", nextWeekStart.plusDays(2), completed = false, priority = TaskPriority.LOW)
    val highPriority =
        task("high", nextWeekStart.plusDays(3), completed = false, priority = TaskPriority.HIGH)

    repo.seed(doneTomorrow, lowPriority, highPriority)

    WeeklyPlanAdjuster(repo).rebalance(today)

    // The pulled candidate should be re-saved dated 'today'
    val pulled = repo.saved.last { it.id == "high" } // should pick HIGH over LOW
    assertEquals(today, pulled.date)
  }

  @Test
  fun no_missed_and_no_completed_early_makes_no_changes() = runTest {
    val repo = FakePlannerRepository()
    val today = LocalDate.of(2025, 3, 6)

    // Only a current-week, not-completed task
    repo.seed(task("a", today, completed = false))

    WeeklyPlanAdjuster(repo).rebalance(today)

    // Nothing should be saved (no rescheduling)
    assertTrue("Expected no changes", repo.saved.isEmpty())
  }
}
