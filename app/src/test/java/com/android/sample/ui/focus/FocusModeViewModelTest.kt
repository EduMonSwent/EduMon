package com.android.sample.ui.focus

import android.content.ContextWrapper
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

// Parts of this code were written using ChatGPT
@OptIn(ExperimentalCoroutinesApi::class)
class FocusModeViewModelTest {

  private lateinit var viewModel: FocusModeViewModel
  private val testDispatcher = StandardTestDispatcher()
  private lateinit var fakeContext: ContextWrapper

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    fakeContext = ContextWrapper(null)
    FocusTimer.reset()
    viewModel = FocusModeViewModel()
  }

  @After
  fun tearDown() {
    FocusTimer.reset()
    Dispatchers.resetMain()
  }

  @Test
  fun startFocus_startsTimerAndSetsRunningTrue() = runTest {
    // Don't call FocusTimer.start manually before; let viewModel do it
    // If FocusModeViewModel.startFocus calls FocusTimer.start internally:
    viewModel.startFocus(fakeContext, 1)

    // Advance time to let coroutines run
    advanceUntilIdle()

    Assert.assertTrue("isRunning should be true after start", FocusTimer.isRunning.value)
    // Wait, FocusTimer might count down immediately or after 1 second?
    // If it's a countdown, remainingTime starts at duration.
    // 1 minute
    Assert.assertEquals(1.minutes, FocusTimer.remainingTime.value)
  }

  @Test
  fun startFocus_thenStopFocus_resetsProperly() = runTest {
    viewModel.startFocus(fakeContext, 1)
    advanceUntilIdle()

    viewModel.stopFocus(fakeContext)
    advanceUntilIdle()

    Assert.assertFalse("isRunning should be false after stop", FocusTimer.isRunning.value)
    Assert.assertEquals(Duration.ZERO, FocusTimer.remainingTime.value)
  }

  @Test
  fun multipleStartFocus_callsRestartTimer() = runTest {
    viewModel.startFocus(fakeContext, 1)
    advanceUntilIdle()
    val first = FocusTimer.remainingTime.value

    viewModel.startFocus(fakeContext, 1)
    advanceUntilIdle()
    val second = FocusTimer.remainingTime.value

    Assert.assertTrue("Timer should be running", FocusTimer.isRunning.value)
    Assert.assertEquals("Timer should be reset to full duration", 1.minutes, second)
  }

  @Test
  fun resetFocus_resetsTimerAndState() = runTest {
    viewModel.startFocus(fakeContext, 1)
    advanceUntilIdle()
    viewModel.resetFocus()
    advanceUntilIdle()

    Assert.assertFalse(FocusTimer.isRunning.value)
    Assert.assertEquals(Duration.ZERO, FocusTimer.remainingTime.value)
  }
}
