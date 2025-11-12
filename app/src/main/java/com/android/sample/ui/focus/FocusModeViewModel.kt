package com.android.sample.ui.focus

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FocusModeViewModel : ViewModel() {

    val remainingTime = FocusTimer.remainingTime
        .stateIn(viewModelScope, SharingStarted.Lazily, FocusTimer.remainingTime.value)

    val isRunning = FocusTimer.isRunning
        .stateIn(viewModelScope, SharingStarted.Lazily, FocusTimer.isRunning.value)

    fun startFocus(context: Context, durationMinutes: Int = 25) {
        FocusModeManager.activate(context)
        viewModelScope.launch { FocusTimer.start(durationMinutes) }
    }

    fun stopFocus(context: Context) {
        FocusModeManager.deactivate(context)
        viewModelScope.launch { FocusTimer.stop() }
    }

    fun resetFocus() {
        viewModelScope.launch { FocusTimer.reset() }
    }
}
