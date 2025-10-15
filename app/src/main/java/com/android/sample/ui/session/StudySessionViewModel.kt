package com.android.sample.ui.session

import FakeStudySessionRepository
import StudySessionRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.sample.ui.pomodoro.PomodoroPhase
import com.android.sample.ui.pomodoro.PomodoroState
import com.android.sample.ui.pomodoro.PomodoroViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Parts of this code were written using ChatGPT and AndroidStudio Gemini tool.

data class StudySessionUiState(
    val selectedTask: Task? = null,
    val suggestedTasks: List<Task> = emptyList(),
    val pomodoroState: PomodoroState = PomodoroState.IDLE,
    val timeLeft: Int = 1500,
    val completedPomodoros: Int = 0,
    val totalMinutes: Int = 0,
    val streakCount: Int = 0,
    val isSessionActive: Boolean = false
)

class Task(val name: String) // temporary

class StudySessionViewModel(
    private val pomodoroViewModel: PomodoroViewModel = PomodoroViewModel(),
    private val repository: StudySessionRepository = FakeStudySessionRepository()
) : ViewModel() {
  private val _uiState = MutableStateFlow(StudySessionUiState())
  val uiState: StateFlow<StudySessionUiState> = _uiState

  init {
    observePomodoro()
    loadSuggestedTasks()
  }

  private fun observePomodoro() {
    combine(pomodoroViewModel.phase, pomodoroViewModel.timeLeft, pomodoroViewModel.state) {
            phase,
            timeLeft,
            state ->
          Triple(phase, timeLeft, state)
        }
        .onEach { (phase, timeLeft, state) ->
          _uiState.update {
            it.copy(
                pomodoroState = state,
                timeLeft = timeLeft,
                isSessionActive = state == PomodoroState.RUNNING)
          }

          // Detect end of a work session to increment stats
          if (phase == PomodoroPhase.WORK && state == PomodoroState.FINISHED) {
            onPomodoroCompleted()
          }
        }
        .launchIn(viewModelScope)
  }

  private fun onPomodoroCompleted() {
    _uiState.update {
      it.copy(
          completedPomodoros = it.completedPomodoros + 1,
          totalMinutes = it.totalMinutes + 25,
          streakCount = it.streakCount + 1)
    }
    viewModelScope.launch { repository.saveCompletedSession(_uiState.value) }
  }

  private fun loadSuggestedTasks() {
    viewModelScope.launch {
      val tasks = repository.getSuggestedTasks()
      _uiState.update { it.copy(suggestedTasks = tasks) }
    }
  }

  /**
   * Selects a task for the study session.
   *
   * @param task The task to select.
   */
  fun selectTask(task: Task) {
    _uiState.update { it.copy(selectedTask = task) }
  }
}
