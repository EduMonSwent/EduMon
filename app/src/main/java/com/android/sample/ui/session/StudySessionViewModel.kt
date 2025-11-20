package com.android.sample.ui.session

// This code has been written partially using A.I (LLM).

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.sample.data.Status
import com.android.sample.data.ToDo
import com.android.sample.data.UserStatsRepository
import com.android.sample.repos_providors.AppRepositories
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

private const val POMODORO_MINUTES = 25
private const val POINTS_PER_COMPLETED_POMODORO = 10

data class StudySessionUiState(
    val selectedTask: Task? = null,
    val suggestedTasks: List<Task> = emptyList(),
    val pomodoroState: PomodoroState = PomodoroState.IDLE,
    val timeLeft: Int = POMODORO_MINUTES * 60,
    val completedPomodoros: Int = 0,
    val totalMinutes: Int = 0,
    val streakCount: Int = 0,
    val isSessionActive: Boolean = false
)

typealias Task = ToDo

class StudySessionViewModel(
    val pomodoroViewModel: PomodoroViewModelContract = PomodoroViewModel(),
    private val repository: StudySessionRepository,
    private val userStatsRepository: UserStatsRepository = AppRepositories.userStatsRepository,
) : ViewModel() {

  private val _uiState = MutableStateFlow(StudySessionUiState())
  val uiState: StateFlow<StudySessionUiState> = _uiState

  private val toDoRepo = ToDoRepositoryProvider.repository

  init {
    observePomodoro()
    loadSuggestedTasks()

    // Keep UI in sync with unified stats (includes streak + today's pomodoros)
    viewModelScope.launch {
      userStatsRepository.start()
      userStatsRepository.stats.collect { stats ->
        val todayCompletedPomodoros = stats.todayStudyMinutes / POMODORO_MINUTES
        _uiState.update {
          it.copy(
              totalMinutes = stats.totalStudyMinutes,
              streakCount = stats.streak,
              completedPomodoros = todayCompletedPomodoros,
          )
        }
      }
    }
  }

  private fun observePomodoro() {
    var lastPhase: PomodoroPhase? = null
    var lastState: PomodoroState? = null
    combine(pomodoroViewModel.phase, pomodoroViewModel.timeLeft, pomodoroViewModel.state) {
            phase,
            timeLeft,
            state,
          ->
          Triple(phase, timeLeft, state)
        }
        .onEach { (phase, timeLeft, state) ->
          _uiState.update {
            it.copy(
                pomodoroState = state,
                timeLeft = timeLeft,
                isSessionActive = state == PomodoroState.RUNNING)
          }

          // End of a work session -> increment stats in Firestore
          if (lastPhase == PomodoroPhase.WORK &&
              lastState != PomodoroState.FINISHED &&
              state == PomodoroState.FINISHED) {
            onPomodoroCompleted()
          }
          lastPhase = phase
          lastState = state
        }
        .launchIn(viewModelScope)
  }

  private fun onPomodoroCompleted() {
    viewModelScope.launch {
      // Unified stats: minutes + points + streak all handled centrally
      userStatsRepository.addStudyMinutes(POMODORO_MINUTES)
      userStatsRepository.addPoints(POINTS_PER_COMPLETED_POMODORO)

      // No need to manually touch completedPomodoros/totalMinutes/streak here:
      // they are updated by the collector above.
      repository.saveCompletedSession(_uiState.value)
    }
  }

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

  /** Selects a task for the study session. */
  fun selectTask(task: Task) {
    _uiState.update { it.copy(selectedTask = task) }
  }
}
