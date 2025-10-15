package com.android.sample.session

import FakeStudySessionRepository
import com.android.sample.ui.pomodoro.PomodoroPhase
import com.android.sample.ui.pomodoro.PomodoroState
import com.android.sample.ui.pomodoro.PomodoroViewModelContract
import com.android.sample.ui.session.StudySessionViewModel
import com.android.sample.ui.session.Task
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

// Parts of this code were written using ChatGPT and AndroidStudio Gemini tool.

@OptIn(ExperimentalCoroutinesApi::class)
class StudySessionViewModelTest {
  private lateinit var fakePomodoro: FakePomodoroViewModel
  private lateinit var fakeRepo: FakeStudySessionRepository
  private lateinit var viewModel: StudySessionViewModel
  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    fakePomodoro = FakePomodoroViewModel()
    fakeRepo = FakeStudySessionRepository()
    viewModel = StudySessionViewModel(fakePomodoro, fakeRepo)
  }

  @Test
  fun `initial state is idle and no selected task`() = runTest {
    val state = viewModel.uiState.first()
    assertEquals(PomodoroState.IDLE, state.pomodoroState)
    assertNull(state.selectedTask)
    assertEquals(0, state.completedPomodoros)
  }

  @Test
  fun `loadSuggestedTasks populates the task list`() = runTest {
    val state = viewModel.uiState.first()
    assertTrue(state.suggestedTasks.isNotEmpty())
  }

  @Test
  fun `selectTask updates selectedTask`() = runTest {
    val task = Task("Test Task")
    viewModel.selectTask(task)
    assertEquals(task, viewModel.uiState.value.selectedTask)
  }

  @Test
  fun `pomodoro state updates in uiState`() = runTest {
    // When: we emit new state AFTER VM is observing
    fakePomodoro.simulatePhaseAndState(PomodoroPhase.WORK, PomodoroState.RUNNING)
    advanceUntilIdle()

    // Then: VM should reflect that
    val state = viewModel.uiState.value
    assertEquals(PomodoroState.RUNNING, state.pomodoroState)
    assertTrue(state.isSessionActive)
  }

  @Test
  fun `onPomodoroCompleted increments stats and saves session`() = runTest {
    // Simulate phase and completion
    fakePomodoro.simulatePhaseAndState(PomodoroPhase.WORK, PomodoroState.FINISHED)

    // Wait for combine collector to emit
    kotlinx.coroutines.delay(100)

    val state = viewModel.uiState.value
    assertEquals(1, state.completedPomodoros)
    assertEquals(25, state.totalMinutes)
    assertEquals(1, state.streakCount)
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
  }

  fun setTimeLeft(seconds: Int) {
    _timeLeft.value = seconds
  }
}
