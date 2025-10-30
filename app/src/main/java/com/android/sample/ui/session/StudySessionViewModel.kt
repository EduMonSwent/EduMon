package com.android.sample.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.sample.data.Status
import com.android.sample.data.ToDo
import com.android.sample.repositories.ToDoRepositoryProvider
import com.android.sample.session.StudySessionRepository
import com.android.sample.ui.pomodoro.PomodoroPhase
import com.android.sample.ui.pomodoro.PomodoroState
import com.android.sample.ui.pomodoro.PomodoroViewModel
import com.android.sample.ui.pomodoro.PomodoroViewModelContract
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Parts of this code were written using ChatGPT and AndroidStudio Gemini tool.

data class StudySessionUiState(
    val selectedTask: Task? = null, // TODO replace with real task class
    val suggestedTasks: List<Task> = emptyList(), // TODO replace with real task class
    val pomodoroState: PomodoroState = PomodoroState.IDLE,
    val timeLeft: Int = 1500,
    val completedPomodoros: Int = 0,
    val totalMinutes: Int = 0,
    val streakCount: Int = 0,
    val isSessionActive: Boolean = false
)

typealias Task = ToDo

class StudySessionViewModel(
    val pomodoroViewModel: PomodoroViewModelContract = PomodoroViewModel(),
    private val repository: StudySessionRepository
) : ViewModel() {
  private val _uiState = MutableStateFlow(StudySessionUiState())
  val uiState: StateFlow<StudySessionUiState> = _uiState
  private val toDoRepo = ToDoRepositoryProvider.repository

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
          if (phase != PomodoroPhase.WORK && state == PomodoroState.FINISHED) {
            onPomodoroCompleted()
          }
        }
        .launchIn(viewModelScope)
  }

  private fun onPomodoroCompleted() {
    _uiState.update {
      it.copy(
          completedPomodoros =
              pomodoroViewModel.cycleCount
                  .value, // TODO: update completed pomodoros with a repository call
          totalMinutes = it.totalMinutes, // TODO: update total minutes with a repository call
          streakCount = it.streakCount) // TODO: update streak count with a repository call
    }
    viewModelScope.launch { repository.saveCompletedSession(_uiState.value) }
  }

  /*private fun loadSuggestedTasks() {
    viewModelScope.launch {
      val tasks = repository.getSuggestedTasks()
      _uiState.update { it.copy(suggestedTasks = tasks) }
    }
  }*/
  private fun loadSuggestedTasks() {
    viewModelScope.launch {
      val tasks = repository.getSuggestedTasks()
      _uiState.update { it.copy(suggestedTasks = tasks) }
      val selectedId = _uiState.value.selectedTask?.id
      _uiState.update {
        it.copy(suggestedTasks = tasks, selectedTask = tasks.find { t -> t.id == selectedId })
      }
    }
  }

  /** Set the status of the currently selected task, then refresh suggestions/selection. */
  fun setSelectedTaskStatus(newStatus: Status) {
    val selectedId = _uiState.value.selectedTask?.id ?: return
    viewModelScope.launch {
      val todo = toDoRepo.getById(selectedId) ?: return@launch
      toDoRepo.update(todo.copy(status = newStatus))

      // Refresh suggestions & selection
      loadSuggestedTasks()
      val updated = toDoRepo.todos.first().firstOrNull { it.id == selectedId }
      _uiState.update { it.copy(selectedTask = updated) }
    }
  }

  /** Optional: one-tap cycle like your Overview screen. */
  fun cycleSelectedTaskStatus() {
    val selectedId = _uiState.value.selectedTask?.id ?: return
    viewModelScope.launch {
      val todo = toDoRepo.getById(selectedId) ?: return@launch
      val next =
          when (todo.status) {
            Status.TODO -> Status.IN_PROGRESS
            Status.IN_PROGRESS -> Status.DONE
            Status.DONE -> Status.TODO
          }
      toDoRepo.update(todo.copy(status = next))

      loadSuggestedTasks()
      val updated = toDoRepo.todos.first().firstOrNull { it.id == selectedId }
      _uiState.update { it.copy(selectedTask = updated) }
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
