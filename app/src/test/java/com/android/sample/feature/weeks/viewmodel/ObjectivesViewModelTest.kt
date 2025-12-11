package com.android.sample.feature.weeks.viewmodel

import app.cash.turbine.test
import com.android.sample.data.UserStats
import com.android.sample.data.UserStatsRepository
import com.android.sample.feature.weeks.model.Objective
import com.android.sample.feature.weeks.model.ObjectiveType
import com.android.sample.feature.weeks.repository.FakeObjectivesRepository
import com.android.sample.login.MainDispatcherRule
import java.time.DayOfWeek
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

// -------------------- Fake UserStatsRepository --------------------

private class FakeUserStatsRepository : UserStatsRepository {
  private val _stats = MutableStateFlow(UserStats())
  override val stats: StateFlow<UserStats> = _stats

  override suspend fun start() {
    // no-op for tests
  }

  override suspend fun addStudyMinutes(delta: Int) {
    _stats.update { it.copy(todayStudyMinutes = it.todayStudyMinutes + delta) }
  }

  override suspend fun addPoints(delta: Int) {
    _stats.update { it.copy(points = it.points + delta) }
  }

  override suspend fun updateCoins(delta: Int) {
    _stats.update { it.copy(coins = (it.coins + delta).coerceAtLeast(0)) }
  }

  override suspend fun setWeeklyGoal(minutes: Int) {
    _stats.update { it.copy(weeklyGoal = minutes) }
  }

  // addReward uses the default implementation from the interface (fine for tests)
}

// -------------------- Tests --------------------

@OptIn(ExperimentalCoroutinesApi::class)
class ObjectivesViewModelTest {

  @get:Rule val mainDispatcherRule = MainDispatcherRule()

  private lateinit var viewModel: ObjectivesViewModel
  private lateinit var fakeUserStatsRepository: FakeUserStatsRepository

  @Before
  fun setup() = runTest {
    // Reset the fake repo to a known state before each test
    FakeObjectivesRepository.setObjectives(defaultObjectives())

    // Fake stats repo so we don't hit real Firebase-backed implementation
    fakeUserStatsRepository = FakeUserStatsRepository()

    // requireAuth = false so we don't trigger Firebase auth during tests
    viewModel =
        ObjectivesViewModel(
            repository = FakeObjectivesRepository,
            userStatsRepository = fakeUserStatsRepository,
            requireAuth = false)
    advanceUntilIdle()
  }

  @Test
  fun initial_state_loads_objectives_from_repository() = runTest {
    val state = viewModel.uiState.value
    assertEquals(3, state.objectives.size)
    assertEquals("Finish Quiz 3", state.objectives[0].title)
    assertTrue(state.showWhy)
  }

  @Test
  fun refresh_reloads_objectives_from_repository() = runTest {
    // Manually update the repo
    FakeObjectivesRepository.addObjective(Objective("New obj", "X", 10, false, DayOfWeek.FRIDAY))

    // Refresh ViewModel
    viewModel.refresh()
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals(4, state.objectives.size)
    assertEquals("New obj", state.objectives.last().title)
  }

  @Test
  fun addObjective_appends_and_updates_state() = runTest {
    val newObj = Objective("Write essay", "ENG200", 45, false, DayOfWeek.SATURDAY)

    viewModel.addObjective(newObj)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals(4, state.objectives.size)
    assertEquals("Write essay", state.objectives.last().title)
  }

  @Test
  fun updateObjective_modifies_at_index() = runTest {
    val updated = viewModel.uiState.value.objectives[0].copy(title = "Updated Quiz 3")

    viewModel.updateObjective(0, updated)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals("Updated Quiz 3", state.objectives[0].title)
  }

  @Test
  fun removeObjective_removes_at_index() = runTest {
    viewModel.removeObjective(1)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals(2, state.objectives.size)
    // Outline lab report should be gone
    assertTrue(state.objectives.none { it.title.contains("Outline lab") })
  }

  @Test
  fun moveObjective_reorders_objectives() = runTest {
    viewModel.moveObjective(0, 2)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals(3, state.objectives.size)
    // First became last
    assertEquals("Outline lab report", state.objectives[0].title)
    assertEquals("Finish Quiz 3", state.objectives[2].title)
  }

  @Test
  fun setObjectives_replaces_entire_list() = runTest {
    val replacement =
        listOf(
            Objective("A", "X", 5, false, DayOfWeek.MONDAY),
            Objective("B", "Y", 10, true, DayOfWeek.TUESDAY))

    viewModel.setObjectives(replacement)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals(2, state.objectives.size)
    assertEquals(listOf("A", "B"), state.objectives.map { it.title })
  }

  @Test
  fun setShowWhy_toggles_flag() = runTest {
    assertTrue(viewModel.uiState.value.showWhy)

    viewModel.setShowWhy(false)
    advanceUntilIdle()

    assertFalse(viewModel.uiState.value.showWhy)

    viewModel.setShowWhy(true)
    advanceUntilIdle()

    assertTrue(viewModel.uiState.value.showWhy)
  }

  @Test
  fun markObjectiveCompleted_updates_completed_flag() = runTest {
    val obj = viewModel.uiState.value.objectives[0]
    assertFalse(obj.completed)

    viewModel.markObjectiveCompleted(obj)
    advanceUntilIdle()

    val updated = viewModel.uiState.value.objectives[0]
    assertTrue(updated.completed)
  }

  @Test
  fun markObjectiveCompleted_noop_when_objective_not_in_list() = runTest {
    val phantom = Objective("Phantom", "XX", 0, false, DayOfWeek.SUNDAY)
    val beforeSize = viewModel.uiState.value.objectives.size

    viewModel.markObjectiveCompleted(phantom)
    advanceUntilIdle()

    // Should not crash and size stays the same
    assertEquals(beforeSize, viewModel.uiState.value.objectives.size)
  }

  @Test
  fun isObjectivesOfDayCompleted_returns_false_when_no_objectives_for_day() = runTest {
    val result = viewModel.isObjectivesOfDayCompleted(DayOfWeek.SUNDAY)
    assertFalse(result)
  }

  @Test
  fun isObjectivesOfDayCompleted_returns_false_when_some_incomplete() = runTest {
    // Monday has one objective (Finish Quiz 3) that is incomplete
    val result = viewModel.isObjectivesOfDayCompleted(DayOfWeek.MONDAY)
    assertFalse(result)
  }

  @Test
  fun isObjectivesOfDayCompleted_returns_true_when_all_complete() = runTest {
    // Mark Monday's objective as complete
    val mondayObj = viewModel.uiState.value.objectives.first { it.day == DayOfWeek.MONDAY }
    viewModel.markObjectiveCompleted(mondayObj)
    advanceUntilIdle()

    val result = viewModel.isObjectivesOfDayCompleted(DayOfWeek.MONDAY)
    assertTrue(result)
  }

  @Test
  fun startObjective_emits_ToCourseExercises_navigation_event() = runTest {
    viewModel.navigationEvents.test {
      viewModel.startObjective(0)
      advanceUntilIdle()

      val event = awaitItem()
      assertTrue(event is ObjectiveNavigation.ToCourseExercises)
      assertEquals(
          "Finish Quiz 3", (event as ObjectiveNavigation.ToCourseExercises).objective.title)
    }
  }

  @Test
  fun startObjective_noop_when_index_out_of_bounds() = runTest {
    viewModel.navigationEvents.test {
      viewModel.startObjective(99)
      advanceUntilIdle()

      // Should not emit anything
      expectNoEvents()
    }
  }

  @Test
  fun startObjective_emits_correct_navigation_for_quiz_type() = runTest {
    // Add a quiz-type objective
    val quizObj =
        Objective(
            title = "Midterm Quiz",
            course = "MATH101",
            estimateMinutes = 60,
            completed = false,
            day = DayOfWeek.WEDNESDAY,
            type = ObjectiveType.QUIZ)
    viewModel.addObjective(quizObj)
    advanceUntilIdle()

    viewModel.navigationEvents.test {
      // Start the quiz objective (index 3)
      viewModel.startObjective(3)
      advanceUntilIdle()

      val event = awaitItem()
      // For now, all objectives emit ToCourseExercises; if you add type-based routing later:
      // assertTrue(event is ObjectiveNavigation.ToQuiz)
      assertTrue(event is ObjectiveNavigation.ToCourseExercises)
    }
  }

  @Test
  fun multiple_objective_operations_maintain_consistent_state() = runTest {
    // Add
    viewModel.addObjective(Objective("Task A", "X", 10, false, DayOfWeek.FRIDAY))
    advanceUntilIdle()
    assertEquals(4, viewModel.uiState.value.objectives.size)

    // Update first
    viewModel.updateObjective(
        0, viewModel.uiState.value.objectives[0].copy(title = "Modified Quiz"))
    advanceUntilIdle()
    assertEquals("Modified Quiz", viewModel.uiState.value.objectives[0].title)

    // Remove second
    viewModel.removeObjective(1)
    advanceUntilIdle()
    assertEquals(3, viewModel.uiState.value.objectives.size)

    // Move
    viewModel.moveObjective(0, 2)
    advanceUntilIdle()
    assertEquals(3, viewModel.uiState.value.objectives.size)
    assertEquals("Modified Quiz", viewModel.uiState.value.objectives.last().title)
  }

  @Test
  fun isObjectivesOfDayCompleted_multi_objectives_same_day() = runTest {
    // Add a second objective for Monday
    viewModel.addObjective(Objective("Extra Monday task", "CS101", 10, false, DayOfWeek.MONDAY))
    advanceUntilIdle()

    // Mark the first Monday objective as complete
    val mondayObj = viewModel.uiState.value.objectives.first { it.day == DayOfWeek.MONDAY }
    viewModel.markObjectiveCompleted(mondayObj)
    advanceUntilIdle()

    // Should return false because we have one incomplete
    assertFalse(viewModel.isObjectivesOfDayCompleted(DayOfWeek.MONDAY))

    // Mark the second Monday objective as complete
    val secondMondayObj =
        viewModel.uiState.value.objectives.first { it.day == DayOfWeek.MONDAY && !it.completed }
    viewModel.markObjectiveCompleted(secondMondayObj)
    advanceUntilIdle()

    // Now should be true
    assertTrue(viewModel.isObjectivesOfDayCompleted(DayOfWeek.MONDAY))
  }

  @Test
  fun setShowWhy_updates_showWhy_flag() = runTest {
    assertEquals(true, viewModel.uiState.value.showWhy)

    viewModel.setShowWhy(false)
    advanceUntilIdle()

    assertEquals(false, viewModel.uiState.value.showWhy)
  }

  @Test
  fun startObjective_with_default_index_uses_zero() = runTest {
    viewModel.navigationEvents.test {
      viewModel.startObjective()
      advanceUntilIdle()

      val event = awaitItem()
      assertTrue(event is ObjectiveNavigation.ToCourseExercises)
      assertEquals(
          "Finish Quiz 3", (event as ObjectiveNavigation.ToCourseExercises).objective.title)
    }
  }

  @Test
  fun navigationEvents_can_emit_multiple_events_sequentially() = runTest {
    viewModel.navigationEvents.test {
      viewModel.startObjective(0)
      advanceUntilIdle()
      val event1 = awaitItem()
      assertEquals(
          "Finish Quiz 3", (event1 as ObjectiveNavigation.ToCourseExercises).objective.title)

      viewModel.startObjective(1)
      advanceUntilIdle()
      val event2 = awaitItem()
      assertEquals(
          "Outline lab report", (event2 as ObjectiveNavigation.ToCourseExercises).objective.title)
    }
  }

  @Test
  fun todayObjectives_filters_by_current_day() = runTest {
    // Set objectives for different days
    val today = java.time.LocalDate.now().dayOfWeek
    val objectives =
        listOf(
            Objective("Today task 1", "CS101", 20, false, today),
            Objective("Today task 2", "CS102", 30, false, today),
            Objective("Tomorrow task", "ENG200", 15, false, today.plus(1)),
            Objective("Yesterday task", "MATH101", 25, false, today.minus(1)))
    viewModel.setObjectives(objectives)
    advanceUntilIdle()

    // Collect todayObjectives
    viewModel.todayObjectives.test {
      val todayList = awaitItem()

      // Should only have 2 objectives for today
      assertEquals(2, todayList.size)
      assertEquals("Today task 1", todayList[0].title)
      assertEquals("Today task 2", todayList[1].title)
      assertTrue(todayList.all { it.day == today })
    }
  }

  @Test
  fun todayObjectives_empty_when_no_objectives_for_current_day() = runTest {
    val today = java.time.LocalDate.now().dayOfWeek
    val notToday = today.plus(1)

    val objectives =
        listOf(
            Objective("Not today 1", "CS101", 20, false, notToday),
            Objective("Not today 2", "CS102", 30, false, notToday))
    viewModel.setObjectives(objectives)
    advanceUntilIdle()

    viewModel.todayObjectives.test {
      val todayList = awaitItem()
      assertTrue(todayList.isEmpty())
    }
  }

  @Test
  fun todayObjectives_updates_when_objectives_change() = runTest {
    val today = java.time.LocalDate.now().dayOfWeek

    viewModel.todayObjectives.test {
      // Skip initial state
      awaitItem()

      // Add an objective for today
      viewModel.addObjective(Objective("New today task", "CS101", 20, false, today))
      advanceUntilIdle()

      val updated = awaitItem()
      assertTrue(updated.any { it.title == "New today task" })
    }
  }

  private fun defaultObjectives(): List<Objective> =
      listOf(
          Objective(
              title = "Finish Quiz 3",
              course = "CS101",
              estimateMinutes = 30,
              completed = false,
              day = DayOfWeek.MONDAY),
          Objective(
              title = "Outline lab report",
              course = "CS101",
              estimateMinutes = 20,
              completed = false,
              day = DayOfWeek.TUESDAY),
          Objective(
              title = "Review 15 flashcards",
              course = "ENG200",
              estimateMinutes = 10,
              completed = false,
              day = DayOfWeek.WEDNESDAY),
      )
}
