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
}
