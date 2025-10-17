package com.android.sample.ui.pomodoro

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Parts of this code were written using ChatGPT and AndroidStudio Gemini tool.

enum class PomodoroPhase {
    WORK,
    SHORT_BREAK,
    LONG_BREAK
}

enum class PomodoroState {
    IDLE,
    RUNNING,
    PAUSED,
    FINISHED
}

interface PomodoroViewModelContract {
    val timeLeft: StateFlow<Int>
    val phase: StateFlow<PomodoroPhase>
    val state: StateFlow<PomodoroState>
    val cycleCount: StateFlow<Int>

    fun startTimer()

    fun pauseTimer()

    fun resumeTimer()

    fun resetTimer()

    fun nextPhase()
}

class PomodoroViewModel : ViewModel(), PomodoroViewModelContract {

    // --- Timer Durations (in seconds) ---
    private val WORK_TIME = 25 * 60
    private val SHORT_BREAK_TIME = 5 * 60
    private val LONG_BREAK_TIME = 15 * 60

    // --- State Variables ---
    private val _phase = MutableStateFlow(PomodoroPhase.WORK)
    override val phase = _phase.asStateFlow()

    private val _timeLeft = MutableStateFlow(WORK_TIME)
    override val timeLeft = _timeLeft.asStateFlow()

    private val _state = MutableStateFlow(PomodoroState.IDLE)
    override val state = _state.asStateFlow()

    private val _cycleCount = MutableStateFlow(0)
    override val cycleCount = _cycleCount.asStateFlow()

    private var timerJob: Job? = null

    // --- Timer Control ---
    /** Start the timer if it's in the IDLE state. */
    override fun startTimer() {
        if (_state.value == PomodoroState.RUNNING) return

        _state.value = PomodoroState.RUNNING
        timerJob =
            viewModelScope.launch {
                while (_timeLeft.value > 0 && _state.value == PomodoroState.RUNNING) {
                    delay(1000L)
                    if (_state.value == PomodoroState.RUNNING)
                        _timeLeft.value -= 1 // condition here to stop timer instantly when pressing pause
                }
                if (_timeLeft.value <= 0) onPhaseCompleted()
            }
    }

    /** Pause the timer if it's in the RUNNING state. */
    override fun pauseTimer() {
        _state.value = PomodoroState.PAUSED
    }

    /** Resume the timer if it's in the PAUSED state. */
    override fun resumeTimer() {
        if (_state.value == PomodoroState.PAUSED) startTimer()
    }
    /** Reset the timer to the initial state. */
    override fun resetTimer() {
        timerJob?.cancel()
        _state.value = PomodoroState.IDLE
        _phase.value = PomodoroPhase.WORK
        _timeLeft.value = WORK_TIME
        _cycleCount.value = 0
    }

    private fun onPhaseCompleted() {
        _state.value = PomodoroState.FINISHED
        timerJob?.cancel()

        when (_phase.value) {
            PomodoroPhase.WORK -> {
                _cycleCount.value += 1
                if (_cycleCount.value % 4 == 0) {
                    switchPhase(PomodoroPhase.LONG_BREAK)
                } else {
                    switchPhase(PomodoroPhase.SHORT_BREAK)
                }
            }
            PomodoroPhase.SHORT_BREAK,
            PomodoroPhase.LONG_BREAK -> {
                switchPhase(PomodoroPhase.WORK)
            }
        }

        // startTimer() // delete if timer must wait when starting new phase
    }

    /** Skip the current phase and move to the next one. */
    override fun nextPhase() {
        // Allow manual skipping
        onPhaseCompleted()
    }

    private fun switchPhase(newPhase: PomodoroPhase) {
        _phase.value = newPhase
        _state.value = PomodoroState.IDLE
        _timeLeft.value =
            when (newPhase) {
                PomodoroPhase.WORK -> WORK_TIME
                PomodoroPhase.SHORT_BREAK -> SHORT_BREAK_TIME
                PomodoroPhase.LONG_BREAK -> LONG_BREAK_TIME
            }
    }

    @VisibleForTesting fun onTestPhaseCompleted() = onPhaseCompleted()
}