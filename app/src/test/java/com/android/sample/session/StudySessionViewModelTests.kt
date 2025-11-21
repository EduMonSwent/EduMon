package com.android.sample.session

import com.android.sample.data.FakeUserStatsRepository
import com.android.sample.data.Priority
import com.android.sample.data.Status
import com.android.sample.data.ToDo
import com.android.sample.repos_providors.AppRepositories
import com.android.sample.ui.pomodoro.PomodoroPhase
import com.android.sample.ui.pomodoro.PomodoroState
import com.android.sample.ui.pomodoro.PomodoroViewModelContract
import com.android.sample.ui.session.StudySessionUiState
import com.android.sample.ui.session.StudySessionViewModel
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class StudySessionViewModelTest {

  private lateinit var fakePomodoro: FakePomodoroViewModel
  private lateinit var fakeRepo: FakeStudySessionRepository
  private lateinit var fakeUserStatsRepo: FakeUserStatsRepository
  private lateinit var viewModel: StudySessionViewModel

  private val testDispatcher = StandardTestDispatcher()
  private val toDoRepo = AppRepositories.toDoRepository

  @OptIn(ExperimentalCoroutinesApi::class)
  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)

    fakePomodoro = FakePomodoroViewModel()
    fakeRepo = FakeStudySessionRepository()
    fakeUserStatsRepo = FakeUserStatsRepository()

    viewModel =
        StudySessionViewModel(
            pomodoroViewModel = fakePomodoro,
            repository = fakeRepo,
            userStatsRepository = fakeUserStatsRepo)
  }

  @Test
  fun `initial state is idle and no selected task`() = runTest {
    val state = viewModel.uiState.value
    assertEquals(PomodoroState.IDLE, state.pomodoroState)
    assertNull(state.selectedTask)
    assertEquals(0, state.completedPomodoros)
  }

  @Test
  fun `selectTask updates selectedTask`() = runTest {
    val task =
        ToDo(
            title = "Test Task",
            dueDate = LocalDate.of(2025, 1, 1),
            priority = Priority.LOW,
            status = Status.TODO)
    viewModel.selectTask(task)
    assertEquals(task, viewModel.uiState.value.selectedTask)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `pomodoro state updates in uiState`() = runTest {
    fakePomodoro.simulatePhaseAndState(PomodoroPhase.WORK, PomodoroState.RUNNING)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals(PomodoroState.RUNNING, state.pomodoroState)
    assertTrue(state.isSessionActive)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `pomodoro RUNNING updates isSessionActive and timeLeft`() = runTest {
    fakePomodoro.simulatePhaseAndState(PomodoroPhase.WORK, PomodoroState.RUNNING)
    fakePomodoro.setTimeLeft(900)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals(PomodoroState.RUNNING, state.pomodoroState)
    assertTrue(state.isSessionActive)
    assertEquals(900, state.timeLeft)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `pomodoro PAUSED clears isSessionActive`() = runTest {
    fakePomodoro.simulatePhaseAndState(PomodoroPhase.WORK, PomodoroState.RUNNING)
    advanceUntilIdle()
    fakePomodoro.pauseTimer()
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertFalse(state.isSessionActive)
    assertEquals(PomodoroState.PAUSED, state.pomodoroState)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `onPomodoroCompleted triggers only when leaving WORK to FINISHED`() = runTest {
    // WORK → FINISHED should not immediately save
    fakePomodoro.simulatePhaseAndState(PomodoroPhase.WORK, PomodoroState.RUNNING)
    advanceUntilIdle()
    assertEquals(0, fakeRepo.getSavedSessions().size)

    // SHORT_BREAK → FINISHED simulates a completed pomodoro
    fakePomodoro.simulatePhaseAndState(PomodoroPhase.SHORT_BREAK, PomodoroState.FINISHED)
    advanceUntilIdle()

    assertEquals(1, fakeRepo.getSavedSessions().size)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `setSelectedTaskStatus updates repo and ui selection`() = runTest {
    val todo =
        ToDo(
            title = "Test Task",
            dueDate = LocalDate.of(2025, 1, 1),
            priority = Priority.LOW,
            status = Status.TODO)
    runBlocking { toDoRepo.add(todo) }

    viewModel =
        StudySessionViewModel(
            pomodoroViewModel = fakePomodoro,
            repository = DelegatingRepoToTodos(),
            userStatsRepository = fakeUserStatsRepo)

    viewModel.selectTask(todo)

    viewModel.setSelectedTaskStatus(Status.IN_PROGRESS)
    advanceUntilIdle()

    val updated = runBlocking { toDoRepo.getById(todo.id)!! }
    assertEquals(Status.IN_PROGRESS, updated.status)
    assertEquals(Status.IN_PROGRESS, viewModel.uiState.value.selectedTask?.status)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `cycleSelectedTaskStatus cycles TODO - IN_PROGRESS - DONE - TODO`() = runTest {
    val todo =
        ToDo(
            title = "Test Task",
            dueDate = LocalDate.of(2025, 1, 1),
            priority = Priority.LOW,
            status = Status.TODO)
    runBlocking { toDoRepo.add(todo) }

    viewModel =
        StudySessionViewModel(
            pomodoroViewModel = fakePomodoro,
            repository = DelegatingRepoToTodos(),
            userStatsRepository = fakeUserStatsRepo)

    viewModel.selectTask(todo)

    viewModel.cycleSelectedTaskStatus()
    advanceUntilIdle()
    assertEquals(Status.IN_PROGRESS, viewModel.uiState.value.selectedTask?.status)

    viewModel.cycleSelectedTaskStatus()
    advanceUntilIdle()
    assertEquals(Status.DONE, viewModel.uiState.value.selectedTask?.status)

    viewModel.cycleSelectedTaskStatus()
    advanceUntilIdle()
    assertEquals(Status.TODO, viewModel.uiState.value.selectedTask?.status)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `setSelectedTaskStatus no-op when nothing selected`() = runTest {
    advanceUntilIdle()
    val before = viewModel.uiState.value.suggestedTasks

    viewModel.setSelectedTaskStatus(Status.DONE)
    advanceUntilIdle()

    val after = viewModel.uiState.value.suggestedTasks
    assertEquals(before, after)
    assertNull(viewModel.uiState.value.selectedTask)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `onPomodoroCompleted increments stats_and_saves_session`() = runTest {
    // WORK → RUNNING
    fakePomodoro.simulatePhaseAndState(PomodoroPhase.WORK, PomodoroState.RUNNING)
    advanceUntilIdle()

    // then SHORT_BREAK → FINISHED (completed pomodoro)
    fakePomodoro.simulatePhaseAndState(PomodoroPhase.SHORT_BREAK, PomodoroState.FINISHED)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    val stats = fakeUserStatsRepo.stats.value

    assertEquals(fakePomodoro.cycleCount.value, state.completedPomodoros)
    assertEquals(stats.totalStudyMinutes, state.totalMinutes)
    assertEquals(stats.streak, state.streakCount)
    assertEquals(1, fakeRepo.getSavedSessions().size)
  }

  @Test
  fun `resetTimer resets pomodoro to idle`() = runTest {
    fakePomodoro.startTimer()
    fakePomodoro.resetTimer()
    assertEquals(PomodoroState.IDLE, fakePomodoro.state.value)
    assertEquals(PomodoroPhase.WORK, fakePomodoro.phase.value)
  }
}

/** Fake Pomodoro VM used by the tests. */
class FakePomodoroViewModel : PomodoroViewModelContract {
  private val _timeLeft = MutableStateFlow(1500)
  private val _phase = MutableStateFlow(PomodoroPhase.WORK)
  private val _state = MutableStateFlow(PomodoroState.IDLE)
  private val _cycleCount = MutableStateFlow(0)

  override val timeLeft: StateFlow<Int> = _timeLeft
  override val phase: StateFlow<PomodoroPhase> = _phase
  override val state: StateFlow<PomodoroState> = _state
  override val cycleCount: StateFlow<Int> = _cycleCount

  override fun startTimer() {
    _state.value = PomodoroState.RUNNING
  }

  override fun pauseTimer() {
    _state.value = PomodoroState.PAUSED
  }

  override fun resumeTimer() {
    _state.value = PomodoroState.RUNNING
  }

  override fun resetTimer() {
    _state.value = PomodoroState.IDLE
    _phase.value = PomodoroPhase.WORK
  }

  override fun nextPhase() {
    _phase.value =
        when (_phase.value) {
          PomodoroPhase.WORK -> PomodoroPhase.SHORT_BREAK
          PomodoroPhase.SHORT_BREAK -> PomodoroPhase.LONG_BREAK
          PomodoroPhase.LONG_BREAK -> PomodoroPhase.WORK
        }
  }

  fun simulatePhaseAndState(phase: PomodoroPhase, state: PomodoroState) {
    _phase.value = phase
    _state.value = state
    _timeLeft.value = 1000
    if (state == PomodoroState.FINISHED && phase != PomodoroPhase.WORK) {
      _cycleCount.value += 1
    }
  }

  fun setTimeLeft(seconds: Int) {
    _timeLeft.value = seconds
  }
}

/** Repo that just delegates suggestions to the real ToDoRepository in AppRepositories. */
private class DelegatingRepoToTodos : StudySessionRepository {
  override suspend fun saveCompletedSession(session: StudySessionUiState) {
    // no-op
  }

  override suspend fun getSuggestedTasks(): List<ToDo> =
      AppRepositories.toDoRepository.todos.first()
}
