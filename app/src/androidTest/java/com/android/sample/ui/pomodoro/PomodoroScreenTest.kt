package com.android.sample.ui.pomodoro

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test

// Parts of this code were written using ChatGPT and AndroidStudio Gemini tool.

class PomodoroScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var fakeViewModel: FakePomodoroViewModel

  @Before
  fun setup() {
    fakeViewModel = FakePomodoroViewModel()
    composeTestRule.setContent { PomodoroScreen(viewModel = fakeViewModel) }
  }

  @Test
  fun showsInitialIdleState() {
    composeTestRule.onNodeWithTag(PomodoroScreenTestTags.TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(PomodoroScreenTestTags.TIMER).assertIsDisplayed()
    composeTestRule.onNodeWithTag(PomodoroScreenTestTags.START_BUTTON).assertIsDisplayed()
  }

  @Test
  fun showsPhaseNameCorrectly() {
    fakeViewModel.setPhase(PomodoroPhase.SHORT_BREAK)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(PomodoroScreenTestTags.PHASE_TEXT).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(PomodoroScreenTestTags.PHASE_TEXT)
        .assertTextEquals("Phase: Short Break")
  }

  @Test
  fun showsTimerAndPhaseTextAlwaysVisible() {
    composeTestRule.onNodeWithTag(PomodoroScreenTestTags.TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(PomodoroScreenTestTags.TIMER).assertIsDisplayed()
    composeTestRule.onNodeWithTag(PomodoroScreenTestTags.PHASE_TEXT).assertIsDisplayed()
  }

  // --- Button actions ---
  @Test
  fun clickingStartCallsStartTimer() {
    composeTestRule.onNodeWithTag(PomodoroScreenTestTags.START_BUTTON).performClick()
    assert(fakeViewModel.startCalled)
  }

  @Test
  fun clickingPauseCallsPauseTimer() {
    fakeViewModel.setState(PomodoroState.RUNNING)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(PomodoroScreenTestTags.PAUSE_BUTTON).performClick()
    assert(fakeViewModel.pauseCalled)
  }

  @Test
  fun clickingResumeCallsResumeTimer() {
    fakeViewModel.setState(PomodoroState.PAUSED)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(PomodoroScreenTestTags.RESUME_BUTTON).performClick()
    assert(fakeViewModel.resumeCalled)
  }

  @Test
  fun clickingSkipCallsNextPhase() {
    fakeViewModel.setState(PomodoroState.RUNNING)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(PomodoroScreenTestTags.SKIP_BUTTON).performClick()
    assert(fakeViewModel.nextPhaseCalled)
  }

  @Test
  fun clickingResetCallsResetTimer() {
    fakeViewModel.setState(PomodoroState.PAUSED)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(PomodoroScreenTestTags.RESET_BUTTON).performClick()
    assert(fakeViewModel.resetCalled)
  }

  @Test
  fun clickingNextPhaseCallsNextPhaseWhenFinished() {
    fakeViewModel.setState(PomodoroState.FINISHED)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(PomodoroScreenTestTags.NEXT_PHASE_BUTTON).performClick()
    assert(fakeViewModel.nextPhaseCalled)
  }

  // --- State-dependent UI ---
  @Test
  fun showsPauseButtonWhenRunning() {
    fakeViewModel.setState(PomodoroState.RUNNING)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(PomodoroScreenTestTags.PAUSE_BUTTON).assertIsDisplayed()
  }

  @Test
  fun showsResetButtonWhenPaused() {
    fakeViewModel.setState(PomodoroState.PAUSED)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(PomodoroScreenTestTags.RESET_BUTTON).assertIsDisplayed()
  }

  @Test
  fun showsSkipButtonWhenRunning() {
    fakeViewModel.setState(PomodoroState.RUNNING)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(PomodoroScreenTestTags.SKIP_BUTTON).assertIsDisplayed()
  }

  @Test
  fun showsLongBreakPhaseTextCorrectly() {
    fakeViewModel.setPhase(PomodoroPhase.LONG_BREAK)
    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(PomodoroScreenTestTags.PHASE_TEXT)
        .assertTextEquals("Phase: Long Break")
  }

  // --- Timer display ---
  @Test
  fun showsFormattedTimeCorrectly() {
    fakeViewModel.setTime(125)
    composeTestRule.waitForIdle()
    composeTestRule
        .onNodeWithTag(PomodoroScreenTestTags.TIMER)
        .assertTextEquals("02:05")
        .assertIsDisplayed()
  }

  @Test
  fun timerTextUpdatesWhenTimeChanges() {
    fakeViewModel.setTime(300)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(PomodoroScreenTestTags.TIMER).assertTextEquals("05:00")

    fakeViewModel.setTime(59)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(PomodoroScreenTestTags.TIMER).assertTextEquals("00:59")
  }

  // --- Cycle count display ---
  @Test
  fun showsCycleCountText() {
    fakeViewModel.setCycleCount(3)
    composeTestRule.waitForIdle()
    composeTestRule
        .onNodeWithTag(PomodoroScreenTestTags.CYCLE_COUNT)
        .assertTextEquals("Cycles Completed: 3")
        .assertIsDisplayed()
  }

  @Test
  fun cycleCountUpdatesWhenChanged() {
    fakeViewModel.setCycleCount(1)
    composeTestRule.waitForIdle()
    composeTestRule
        .onNodeWithTag(PomodoroScreenTestTags.CYCLE_COUNT)
        .assertTextEquals("Cycles Completed: 1")

    fakeViewModel.setCycleCount(5)
    composeTestRule.waitForIdle()
    composeTestRule
        .onNodeWithTag(PomodoroScreenTestTags.CYCLE_COUNT)
        .assertTextEquals("Cycles Completed: 5")
  }
}

/** A fake PomodoroViewModel for testing purposes. */
class FakePomodoroViewModel : ViewModel(), PomodoroViewModelContract {
  private val _timeLeft = MutableStateFlow(1500)
  override val timeLeft = _timeLeft

  private val _phase = MutableStateFlow(PomodoroPhase.WORK)
  override val phase = _phase

  private val _state = MutableStateFlow(PomodoroState.IDLE)
  override val state = _state

  private val _cycleCount = MutableStateFlow(0)
  override val cycleCount = _cycleCount

  var startCalled = false
  var pauseCalled = false
  var resumeCalled = false
  var resetCalled = false
  var nextPhaseCalled = false

  override fun startTimer() {
    startCalled = true
  }

  override fun pauseTimer() {
    pauseCalled = true
  }

  override fun resumeTimer() {
    resumeCalled = true
  }

  override fun resetTimer() {
    resetCalled = true
  }

  override fun nextPhase() {
    nextPhaseCalled = true
  }

  fun setPhase(p: PomodoroPhase) {
    _phase.value = p
  }

  fun setState(s: PomodoroState) {
    _state.value = s
  }

  fun setTime(seconds: Int) {
    _timeLeft.value = seconds
  }

  fun setCycleCount(count: Int) {
    _cycleCount.value = count
  }
}
