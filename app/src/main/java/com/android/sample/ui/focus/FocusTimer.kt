package com.android.sample.ui.focus

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Simple focus timer for Pomodoro-like focus sessions. Allows choosing between 15, 25 or 45
 * minutes.
 */
object FocusTimer {

  private var job: Job? = null

  private val _remainingTime = MutableStateFlow(Duration.ZERO)
  val remainingTime = _remainingTime.asStateFlow()

  private val _isRunning = MutableStateFlow(false)
  val isRunning = _isRunning.asStateFlow()

  private var totalDuration: Duration = 25.minutes

  /** Start a focus timer with chosen duration (default 25 min) */
  fun start(durationMinutes: Int = 25) {
    stop()
    totalDuration = durationMinutes.minutes
    _remainingTime.value = totalDuration
    _isRunning.value = true

    job =
        CoroutineScope(Dispatchers.Default).launch {
          while (_remainingTime.value > Duration.ZERO && isActive) {
            delay(1.seconds)
            _remainingTime.value -= 1.seconds
          }
          _isRunning.value = false
        }
  }

  /** Stop the timer (used when leaving focus mode early) */
  fun stop() {
    job?.cancel()
    _isRunning.value = false
    _remainingTime.value = Duration.ZERO
  }

  /** Reset timer back to default state */
  fun reset() {
    stop()
  }
}
