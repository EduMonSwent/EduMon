package com.android.sample.ui.focus

import kotlin.test.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest

class FocusTimerTest {

  @BeforeTest
  fun setup() {
    FocusTimer.reset()
  }

  @Test
  fun `start sets isRunning true and remainingTime correct`() = runTest {
    FocusTimer.start(1)
    assertTrue(FocusTimer.isRunning.value)
    assertEquals(1.minutes, FocusTimer.remainingTime.value)
  }

  @Test
  fun `stop cancels job and resets values`() = runTest {
    FocusTimer.start(1)
    FocusTimer.stop()
    assertFalse(FocusTimer.isRunning.value)
    assertEquals(Duration.ZERO, FocusTimer.remainingTime.value)
  }

  @Test
  fun `reset behaves like stop`() = runTest {
    FocusTimer.start(1)
    FocusTimer.reset()
    assertFalse(FocusTimer.isRunning.value)
    assertEquals(Duration.ZERO, FocusTimer.remainingTime.value)
  }
}
