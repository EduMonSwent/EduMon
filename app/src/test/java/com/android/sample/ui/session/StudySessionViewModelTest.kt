package com.android.sample.ui.session

import com.android.sample.data.FakeUserStatsRepository
import com.android.sample.data.Priority
import com.android.sample.data.Status
import com.android.sample.data.ToDo
import com.android.sample.data.UserStats
import com.android.sample.profile.FakeProfileRepository
import com.android.sample.repositories.ToDoRepositoryProvider
import com.android.sample.session.FakeStudySessionRepository
import com.android.sample.session.StudySessionRepository
import com.android.sample.ui.pomodoro.PomodoroPhase
import com.android.sample.ui.pomodoro.PomodoroState
import com.android.sample.ui.pomodoro.PomodoroViewModelContract
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StudySessionViewModelTest {

  private lateinit var fakePomodoro: FakePomodoroViewModel
  private lateinit var fakeRepo: FakeStudySessionRepository
  private lateinit var fakeProfileRepo: FakeProfileRepository
  private lateinit var fakeUserStatsRepo: FakeUserStatsRepository
  private lateinit var viewModel: StudySessionViewModel
  private val testDispatcher = StandardTestDispatcher()
  private val toDoRepo = ToDoRepositoryProvider.repository

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    fakePomodoro = FakePomodoroViewModel()
    fakeProfileRepo = FakeProfileRepository()
    fakeRepo = FakeStudySessionRepository()
    fakeUserStatsRepo = FakeUserStatsRepository()

    viewModel = StudySessionViewModel(fakePomodoro, fakeRepo, fakeProfileRepo, fakeUserStatsRepo)
  }

  @Test
  fun `initial state has default values`() = runTest {
    val state = viewModel.uiState.value
    assertEquals(PomodoroState.IDLE, state.pomodoroState)
    assertNull(state.selectedTask)
    assertEquals(0, state.completedPomodoros)
    assertEquals(0, state.totalMinutes)
    assertEquals(0, state.streakCount)
    assertFalse(state.isSessionActive)
  }

  @Test
  fun `selectTask updates selectedTask in uiState`() = runTest {
    val task =
        ToDo(
            title = "Test Task",
            dueDate = LocalDate.of(2025, 1, 1),
            priority = Priority.LOW,
            status = Status.TODO)

    viewModel.selectTask(task)

    assertEquals(task, viewModel.uiState.value.selectedTask)
  }

  @Test
  fun `userStats syncs to uiState`() = runTest {
    fakeUserStatsRepo =
        FakeUserStatsRepository(
            UserStats(todayStudyMinutes = 45, streak = 7, todayCompletedPomodoros = 3))

    viewModel = StudySessionViewModel(fakePomodoro, fakeRepo, fakeProfileRepo, fakeUserStatsRepo)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals(45, state.totalMinutes)
    assertEquals(7, state.streakCount)
    assertEquals(3, state.completedPomodoros)
  }

  @Test
  fun `pomodoro RUNNING updates isSessionActive to true`() = runTest {
    fakePomodoro.startTimer()
    fakePomodoro.simulatePhaseAndState(PomodoroPhase.WORK, PomodoroState.RUNNING)
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertTrue(state.isSessionActive)
    assertEquals(PomodoroState.RUNNING, state.pomodoroState)
  }

  @Test
  fun `pomodoro PAUSED updates isSessionActive to false`() = runTest {
    fakePomodoro.startTimer()
    advanceUntilIdle()

    fakePomodoro.pauseTimer()
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertFalse(state.isSessionActive)
    assertEquals(PomodoroState.PAUSED, state.pomodoroState)
  }

  @Test
  fun `pomodoro IDLE updates isSessionActive to false`() = runTest {
    fakePomodoro.resetTimer()
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertFalse(state.isSessionActive)
    assertEquals(PomodoroState.IDLE, state.pomodoroState)
  }

  @Test
  fun `pomodoro timeLeft syncs to uiState`() = runTest {
    fakePomodoro.setTimeLeft(900)
    advanceUntilIdle()

    assertEquals(900, viewModel.uiState.value.timeLeft)
  }

  @Test
  fun `completing WORK phase triggers onPomodoroCompleted`() = runTest {
    // Start in WORK and RUNNING
    fakePomodoro.simulatePhaseAndState(PomodoroPhase.WORK, PomodoroState.RUNNING)
    advanceUntilIdle()

    val initialMinutes = fakeUserStatsRepo.stats.value.todayStudyMinutes

    // Finish WORK phase
    fakePomodoro.simulatePhaseAndState(PomodoroPhase.WORK, PomodoroState.FINISHED)
    advanceUntilIdle()

    // Should have saved a session
    assertTrue(fakeRepo.getSavedSessions().size > 0)
    // Minutes should have increased
    assertTrue(fakeUserStatsRepo.stats.value.todayStudyMinutes > initialMinutes)
  }

  @Test
  fun `completing SHORT_BREAK does not trigger onPomodoroCompleted`() = runTest {
    fakePomodoro.simulatePhaseAndState(PomodoroPhase.SHORT_BREAK, PomodoroState.RUNNING)
    advanceUntilIdle()

    val initialSessions = fakeRepo.getSavedSessions().size

    fakePomodoro.simulatePhaseAndState(PomodoroPhase.SHORT_BREAK, PomodoroState.FINISHED)
    advanceUntilIdle()

    // Should not save additional session for break completion
    assertEquals(initialSessions, fakeRepo.getSavedSessions().size)
  }

  @Test
  fun `completing LONG_BREAK does not trigger onPomodoroCompleted`() = runTest {
    fakePomodoro.simulatePhaseAndState(PomodoroPhase.LONG_BREAK, PomodoroState.RUNNING)
    advanceUntilIdle()

    val initialSessions = fakeRepo.getSavedSessions().size

    fakePomodoro.simulatePhaseAndState(PomodoroPhase.LONG_BREAK, PomodoroState.FINISHED)
    advanceUntilIdle()

    assertEquals(initialSessions, fakeRepo.getSavedSessions().size)
  }

  @Test
  fun `setSelectedTaskStatus updates task in repository`() = runTest {
    val todo =
        ToDo(
            title = "Test Task",
            dueDate = LocalDate.of(2025, 1, 1),
            priority = Priority.LOW,
            status = Status.TODO)

    toDoRepo.add(todo)
    advanceUntilIdle()

    viewModel =
        StudySessionViewModel(
            fakePomodoro, DelegatingRepoToTodos(), fakeProfileRepo, fakeUserStatsRepo)
    advanceUntilIdle()

    viewModel.selectTask(todo)
    viewModel.setSelectedTaskStatus(Status.IN_PROGRESS)
    advanceUntilIdle()

    val updated = toDoRepo.getById(todo.id)
    assertEquals(Status.IN_PROGRESS, updated?.status)
  }

  @Test
  fun `setSelectedTaskStatus with no selection does nothing`() = runTest {
    advanceUntilIdle()

    assertNull(viewModel.uiState.value.selectedTask)

    viewModel.setSelectedTaskStatus(Status.DONE)
    advanceUntilIdle()

    assertNull(viewModel.uiState.value.selectedTask)
  }

  @Test
  fun `cycleSelectedTaskStatus cycles TODO to IN_PROGRESS`() = runTest {
    val todo =
        ToDo(
            title = "Test Task",
            dueDate = LocalDate.of(2025, 1, 1),
            priority = Priority.LOW,
            status = Status.TODO)

    toDoRepo.add(todo)
    advanceUntilIdle()

    viewModel =
        StudySessionViewModel(
            fakePomodoro, DelegatingRepoToTodos(), fakeProfileRepo, fakeUserStatsRepo)
    advanceUntilIdle()

    viewModel.selectTask(todo)
    viewModel.cycleSelectedTaskStatus()
    advanceUntilIdle()

    assertEquals(Status.IN_PROGRESS, viewModel.uiState.value.selectedTask?.status)
  }

  @Test
  fun `cycleSelectedTaskStatus cycles IN_PROGRESS to DONE`() = runTest {
    val todo =
        ToDo(
            title = "Test Task",
            dueDate = LocalDate.of(2025, 1, 1),
            priority = Priority.LOW,
            status = Status.IN_PROGRESS)

    toDoRepo.add(todo)
    advanceUntilIdle()

    viewModel =
        StudySessionViewModel(
            fakePomodoro, DelegatingRepoToTodos(), fakeProfileRepo, fakeUserStatsRepo)
    advanceUntilIdle()

    viewModel.selectTask(todo)
    viewModel.cycleSelectedTaskStatus()
    advanceUntilIdle()

    assertEquals(Status.DONE, viewModel.uiState.value.selectedTask?.status)
  }

  @Test
  fun `cycleSelectedTaskStatus cycles DONE to TODO`() = runTest {
    val todo =
        ToDo(
            title = "Test Task",
            dueDate = LocalDate.of(2025, 1, 1),
            priority = Priority.LOW,
            status = Status.DONE)

    toDoRepo.add(todo)
    advanceUntilIdle()

    viewModel =
        StudySessionViewModel(
            fakePomodoro, DelegatingRepoToTodos(), fakeProfileRepo, fakeUserStatsRepo)
    advanceUntilIdle()

    viewModel.selectTask(todo)
    viewModel.cycleSelectedTaskStatus()
    advanceUntilIdle()

    assertEquals(Status.TODO, viewModel.uiState.value.selectedTask?.status)
  }

  @Test
  fun `cycleSelectedTaskStatus with no selection does nothing`() = runTest {
    advanceUntilIdle()

    assertNull(viewModel.uiState.value.selectedTask)

    viewModel.cycleSelectedTaskStatus()
    advanceUntilIdle()

    assertNull(viewModel.uiState.value.selectedTask)
  }

  @Test
  fun `suggested no tasks loaded on init`() = runTest {
    fakeRepo = FakeStudySessionRepository()
    viewModel = StudySessionViewModel(fakePomodoro, fakeRepo, fakeProfileRepo, fakeUserStatsRepo)
    advanceUntilIdle()

    assertEquals(0, viewModel.uiState.value.suggestedTasks.size)
  }

  @Test
  fun `pomodoroViewModel cycleCount synced from userStats`() = runTest {
    fakeUserStatsRepo = FakeUserStatsRepository(UserStats(todayCompletedPomodoros = 5))

    viewModel = StudySessionViewModel(fakePomodoro, fakeRepo, fakeProfileRepo, fakeUserStatsRepo)
    advanceUntilIdle()

    // Should have called updateCycleCount(5)
    assertEquals(5, fakePomodoro.cycleCount.value)
  }

  @Test
  fun `onPomodoroCompleted increments all stats correctly`() = runTest {
    fakePomodoro.simulatePhaseAndState(PomodoroPhase.WORK, PomodoroState.RUNNING)
    advanceUntilIdle()

    val initialMinutes = fakeUserStatsRepo.stats.value.todayStudyMinutes
    val initialPoints = fakeUserStatsRepo.stats.value.points
    val initialPomodoros = fakeUserStatsRepo.stats.value.todayCompletedPomodoros

    fakePomodoro.simulatePhaseAndState(PomodoroPhase.WORK, PomodoroState.FINISHED)
    advanceUntilIdle()

    assertEquals(initialMinutes + 25, fakeUserStatsRepo.stats.value.todayStudyMinutes)
    assertEquals(initialPoints + 10, fakeUserStatsRepo.stats.value.points)
    assertEquals(initialPomodoros + 1, fakeUserStatsRepo.stats.value.todayCompletedPomodoros)
  }

  @Test
  fun `phase changes are observed`() = runTest {
    fakePomodoro.simulatePhaseAndState(PomodoroPhase.WORK, PomodoroState.RUNNING)
    advanceUntilIdle()

    fakePomodoro.nextPhase()
    fakePomodoro.simulatePhaseAndState(PomodoroPhase.SHORT_BREAK, PomodoroState.RUNNING)
    advanceUntilIdle()

    assertEquals(PomodoroState.RUNNING, viewModel.uiState.value.pomodoroState)
  }

  @Test
  fun `resuming timer updates state`() = runTest {
    fakePomodoro.startTimer()
    advanceUntilIdle()

    fakePomodoro.pauseTimer()
    advanceUntilIdle()

    fakePomodoro.resumeTimer()
    advanceUntilIdle()

    assertEquals(PomodoroState.RUNNING, viewModel.uiState.value.pomodoroState)
    assertTrue(viewModel.uiState.value.isSessionActive)
  }
}

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

  override fun updateCycleCount(count: Int) {
    _cycleCount.value = count
  }

  fun simulatePhaseAndState(phase: PomodoroPhase, state: PomodoroState) {
    _phase.value = phase
    _state.value = state
    _timeLeft.value = 1000
  }

  fun setTimeLeft(seconds: Int) {
    _timeLeft.value = seconds
  }
}

private class DelegatingRepoToTodos : StudySessionRepository {
  override suspend fun saveCompletedSession(session: StudySessionUiState) {
    // no-op
  }

  override suspend fun getSuggestedTasks(): List<ToDo> =
      ToDoRepositoryProvider.repository.todos.first()
}
