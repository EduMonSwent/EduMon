package com.android.sample.pomodoro

import com.android.sample.ui.pomodoro.PomodoroPhase
import com.android.sample.ui.pomodoro.PomodoroState
import com.android.sample.ui.pomodoro.PomodoroViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

// Parts of this code were written using ChatGPT and AndroidStudio Gemini tool

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
    Assert.assertEquals(PomodoroState.IDLE, viewModel.state.value)
    Assert.assertEquals(PomodoroPhase.WORK, viewModel.phase.value)
  }

  @Test
  fun startTimerChangesStateToRunning() = runTest {
    viewModel.startTimer()
    Assert.assertEquals(PomodoroState.RUNNING, viewModel.state.value)
  }

  @Test
  fun pauseTimerSetsStateToPaused() = runTest {
    viewModel.startTimer()
    viewModel.pauseTimer()
    Assert.assertEquals(PomodoroState.PAUSED, viewModel.state.value)
  }

  @Test
  fun resumeTimerCallsStartOnlyWhenPaused() = runTest {
    // Initial state: should not call start when not paused
    viewModel.startTimer() // now RUNNING
    viewModel.resumeTimer()
    Assert.assertEquals(PomodoroState.RUNNING, viewModel.state.value)

    // Move to PAUSED state manually
    viewModel.pauseTimer()
    Assert.assertEquals(PomodoroState.PAUSED, viewModel.state.value)

    // Now resumeTimer() should behave like startTimer()
    viewModel.resumeTimer()
    Assert.assertEquals(PomodoroState.RUNNING, viewModel.state.value)
  }

  @Test
  fun resetTimerResetsTimerAndState() = runTest {
    viewModel.startTimer()
    viewModel.resetTimer()
    Assert.assertEquals(PomodoroState.IDLE, viewModel.state.value)
    Assert.assertEquals(PomodoroPhase.WORK, viewModel.phase.value)
  }

  @Test
  fun onPhaseCompletedSwitchesFromWorkToShortBreak() = runTest {
    // simulate finishing work phase
    viewModel.startTimer()
    viewModel.onTestPhaseCompleted() // custom helper
    Assert.assertEquals(PomodoroPhase.SHORT_BREAK, viewModel.phase.value)
  }

  @Test
  fun every4WorkSessionsTriggersLongBreak() = runTest {
    viewModel.resetTimer()
    repeat(7) {
      viewModel.startTimer()
      viewModel.onTestPhaseCompleted()
    }
    Assert.assertEquals(PomodoroPhase.LONG_BREAK, viewModel.phase.value)
  }

  @Test
  fun nextPhaseAdvancesToNextPhase() = runTest {
    // Start from default state (WORK)
    Assert.assertEquals(PomodoroPhase.WORK, viewModel.phase.value)

    // When nextPhase() is called, it should act like finishing a work session
    viewModel.nextPhase()

    // Then we should move to SHORT_BREAK phase
    Assert.assertEquals(PomodoroPhase.SHORT_BREAK, viewModel.phase.value)

    // Calling nextPhase again should eventually cycle forward
    viewModel.nextPhase()
    Assert.assertEquals(PomodoroPhase.WORK, viewModel.phase.value)
  }

  @Test
  fun updateCycleCountUpdatesState() = runTest {
    viewModel.updateCycleCount(10)
    Assert.assertEquals(10, viewModel.cycleCount.value)
  }
}
