package com.android.sample.ui.pomodoro

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

// Parts of this code were written using ChatGPT and AndroidStudio Gemini tool.

@OptIn(ExperimentalCoroutinesApi::class)
class PomodoroViewModelTest {

  private lateinit var viewModel: PomodoroViewModel
  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    viewModel = PomodoroViewModel()
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun initialStateIsIdleAndWorkPhase() {
    assertEquals(PomodoroState.IDLE, viewModel.state.value)
    assertEquals(PomodoroPhase.WORK, viewModel.phase.value)
  }

  @Test
  fun startTimerChangesStateToRunning() = runTest {
    viewModel.startTimer()
    assertEquals(PomodoroState.RUNNING, viewModel.state.value)
  }

  @Test
  fun pauseTimerSetsStateToPaused() = runTest {
    viewModel.startTimer()
    viewModel.pauseTimer()
    assertEquals(PomodoroState.PAUSED, viewModel.state.value)
  }

  @Test
  fun resumeTimerCallsStartOnlyWhenPaused() = runTest {
    // Initial state: should not call start when not paused
    viewModel.startTimer() // now RUNNING
    viewModel.resumeTimer()
    assertEquals(PomodoroState.RUNNING, viewModel.state.value)

    // Move to PAUSED state manually
    viewModel.pauseTimer()
    assertEquals(PomodoroState.PAUSED, viewModel.state.value)

    // Now resumeTimer() should behave like startTimer()
    viewModel.resumeTimer()
    assertEquals(PomodoroState.RUNNING, viewModel.state.value)
  }

  @Test
  fun resetTimerResetsTimerAndState() = runTest {
    viewModel.startTimer()
    viewModel.resetTimer()
    assertEquals(PomodoroState.IDLE, viewModel.state.value)
    assertEquals(PomodoroPhase.WORK, viewModel.phase.value)
  }

  @Test
  fun onPhaseCompletedSwitchesFromWorkToShortBreak() = runTest {
    // simulate finishing work phase
    viewModel.startTimer()
    viewModel.onTestPhaseCompleted() // custom helper
    assertEquals(PomodoroPhase.SHORT_BREAK, viewModel.phase.value)
  }

  @Test
  fun every4WorkSessionsTriggersLongBreak() = runTest {
    viewModel.resetTimer()
    repeat(7) {
      viewModel.startTimer()
      viewModel.onTestPhaseCompleted()
    }
    assertEquals(PomodoroPhase.LONG_BREAK, viewModel.phase.value)
  }

  @Test
  fun nextPhaseAdvancesToNextPhase() = runTest {
    // Start from default state (WORK)
    assertEquals(PomodoroPhase.WORK, viewModel.phase.value)

    // When nextPhase() is called, it should act like finishing a work session
    viewModel.nextPhase()

    // Then we should move to SHORT_BREAK phase
    assertEquals(PomodoroPhase.SHORT_BREAK, viewModel.phase.value)

    // Calling nextPhase again should eventually cycle forward
    viewModel.nextPhase()
    assertEquals(PomodoroPhase.WORK, viewModel.phase.value)
  }
}
