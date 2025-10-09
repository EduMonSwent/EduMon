package com.android.sample.ui.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.DayOfWeek

// Data models --------------------------------------------------------------

data class Objective(
    val title: String,
    val course: String,
    val estimateMinutes: Int = 0,
    val reason: String = ""
)

data class DayStatus(
    val dayOfWeek: DayOfWeek,
    val metTarget: Boolean
)

data class WeekProgressItem(
    val label: String,
    val percent: Int // 0..100
)

// UI State -----------------------------------------------------------------

data class WeekProgressUiState(
    val weekProgressPercent: Int = 0,
    val dayStatuses: List<DayStatus> = emptyList(),
    val objectives: List<Objective> = emptyList(),
    val showWhy: Boolean = true,
    val weeks: List<WeekProgressItem> = emptyList(),
    val selectedWeekIndex: Int = 0
)

// ViewModel ----------------------------------------------------------------

class WeekProgressViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(
        WeekProgressUiState(
            weekProgressPercent = 55,
            dayStatuses = listOf(
                DayStatus(DayOfWeek.MONDAY, true),
                DayStatus(DayOfWeek.TUESDAY, true),
                DayStatus(DayOfWeek.WEDNESDAY, false),
                DayStatus(DayOfWeek.THURSDAY, true),
                DayStatus(DayOfWeek.FRIDAY, false),
                DayStatus(DayOfWeek.SATURDAY, false),
                DayStatus(DayOfWeek.SUNDAY, false),
            ),
            objectives = listOf(
                Objective(
                    title = "Finish Quiz 3",
                    course = "CS101",
                    estimateMinutes = 30,
                    reason = "Due tomorrow • high impact"
                ),
                Objective(
                    title = "Read Chapter 5",
                    course = "Math201",
                    estimateMinutes = 25,
                    reason = "Prereq for next lecture"
                ),
                Objective(
                    title = "Flashcards Review",
                    course = "Bio110",
                    estimateMinutes = 15,
                    reason = "Spaced repetition"
                )
            ),
            weeks = listOf(
                WeekProgressItem("Week 1", 100),
                WeekProgressItem("Week 2", 55),
                WeekProgressItem("Week 3", 10),
                WeekProgressItem("Week 4", 0)
            ),
            selectedWeekIndex = 1
        )
    )
    val uiState = _uiState.asStateFlow()

    // --- Actions the UI can call ---
    @Suppress("UNUSED_PARAMETER")
    fun startObjective(index: Int = 0) { /* hook for analytics / repo later */ }

    fun selectWeek(index: Int) {
        _uiState.update { current ->
            val safe = index.coerceIn(0, current.weeks.lastIndex.coerceAtLeast(0))
            val newPct = current.weeks.getOrNull(safe)?.percent ?: current.weekProgressPercent
            current.copy(selectedWeekIndex = safe, weekProgressPercent = newPct)
        }
    }

    fun setProgress(pct: Int) {
        _uiState.update { it.copy(weekProgressPercent = pct.coerceIn(0, 100)) }
    }
}
