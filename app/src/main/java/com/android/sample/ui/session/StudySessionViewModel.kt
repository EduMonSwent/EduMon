package com.android.sample.ui.session

// This code has been written partially using A.I (LLM).

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.sample.data.Status
import com.android.sample.data.ToDo
import com.android.sample.data.UserStatsRepository
import com.android.sample.feature.subjects.model.StudySubject
import com.android.sample.feature.subjects.repository.SubjectsRepository
import com.android.sample.repos_providors.AppRepositories
import com.android.sample.session.StudySessionRepository
import com.android.sample.ui.pomodoro.PomodoroPhase
import com.android.sample.ui.pomodoro.PomodoroState
import com.android.sample.ui.pomodoro.PomodoroViewModel
import com.android.sample.ui.pomodoro.PomodoroViewModelContract
import com.android.sample.ui.stats.repository.StatsRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val POMODORO_MINUTES = 25
private const val POINTS_PER_COMPLETED_POMODORO = 10
private const val COINS_PER_COMPLETED_POMODORO = 2
private const val DAYS_IN_WEEK = 7
private const val DEFAULT_SUBJECT_COLOR_INDEX = 0

data class StudySessionUiState(
    val selectedTask: Task? = null,
    val suggestedTasks: List<Task> = emptyList(),
    val pomodoroState: PomodoroState = PomodoroState.IDLE,
    val timeLeft: Int = POMODORO_MINUTES * 60,
    val completedPomodoros: Int = 0,
    val totalMinutes: Int = 0,
    val streakCount: Int = 0,
    val isSessionActive: Boolean = false,
    val subjects: List<StudySubject> = emptyList(),
    val selectedSubject: StudySubject? = null,
)

typealias Task = ToDo

class StudySessionViewModel(
    val pomodoroViewModel: PomodoroViewModelContract = PomodoroViewModel(),
    private val repository: StudySessionRepository,
    private val userStatsRepository: UserStatsRepository = AppRepositories.userStatsRepository,
    private val statsRepository: StatsRepository = AppRepositories.statsRepository,
    private val subjectsRepository: SubjectsRepository = AppRepositories.subjectsRepository,
) : ViewModel() {

  private val _uiState = MutableStateFlow(StudySessionUiState())
  val uiState: StateFlow<StudySessionUiState> = _uiState

  // IMPORTANT: use the same repository as tests (AppRepositories.toDoRepository)
  private val toDoRepo = AppRepositories.toDoRepository

  init {
    observePomodoro()
    loadSuggestedTasks()
    observeUserStats()
    observeSubjects()
  }

  private fun observeUserStats() {
    viewModelScope.launch {
      userStatsRepository.start()
      userStatsRepository.stats.collect { stats ->
        val todayCompletedPomodoros = stats.todayStudyMinutes / POMODORO_MINUTES
        _uiState.update {
          it.copy(
              // Use unified user stats here so it updates even offline
              totalMinutes = stats.totalStudyMinutes,
              streakCount = stats.streak,
              completedPomodoros = todayCompletedPomodoros,
          )
        }
      }
    }
  }

  private fun observeSubjects() {
    viewModelScope.launch {
      subjectsRepository.start()
      subjectsRepository.subjects.collect { list ->
        _uiState.update { current ->
          val currentSelectedId = current.selectedSubject?.id
          val newSelected = list.firstOrNull { it.id == currentSelectedId }
          current.copy(subjects = list, selectedSubject = newSelected)
        }
      }
    }
  }

  private fun observePomodoro() {
    var lastPhase: PomodoroPhase? = null
    var lastState: PomodoroState? = null

    kotlinx.coroutines.flow
        .combine(pomodoroViewModel.phase, pomodoroViewModel.timeLeft, pomodoroViewModel.state) {
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
                isSessionActive = state == PomodoroState.RUNNING,
            )
          }

          // End of a work session -> increment stats
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
      // Unified user stats: minutes + points + streak are handled centrally
      userStatsRepository.addStudyMinutes(POMODORO_MINUTES)
      userStatsRepository.addPoints(POINTS_PER_COMPLETED_POMODORO)
      userStatsRepository.updateCoins(COINS_PER_COMPLETED_POMODORO)

      // Persist session snapshot if needed by the study-session feature
      repository.saveCompletedSession(_uiState.value)

      // Per-subject total
      val subject = _uiState.value.selectedSubject
      if (subject != null) {
        subjectsRepository.addStudyMinutesToSubject(subject.id, POMODORO_MINUTES)
      }

      // Update weekly StudyStats used by the Stats screen (pie chart, 7-day bar, etc.)
      updateWeeklyStatsForPomodoro(subjectName = subject?.name)
    }
  }

  /**
   * Updates StudyStats in statsRepository to reflect one completed pomodoro.
   * - Increments totalTimeMin by POMODORO_MINUTES.
   * - If subjectName is provided, increments courseTimesMin for that subject.
   * - Increments today's entry in progressByDayMin.
   */
  private suspend fun updateWeeklyStatsForPomodoro(subjectName: String?) {
    val current = statsRepository.stats.first()
    val minutesDelta = POMODORO_MINUTES

    val updatedTotalTime = current.totalTimeMin + minutesDelta

    val updatedCourseTimes =
        if (subjectName == null) {
          current.courseTimesMin
        } else {
          val mutable = LinkedHashMap(current.courseTimesMin)
          val previous = mutable[subjectName] ?: 0
          mutable[subjectName] = previous + minutesDelta
          mutable
        }

    val today = LocalDate.now()
    val todayIndex = (today.dayOfWeek.value - 1).coerceIn(0, DAYS_IN_WEEK - 1)

    val updatedProgress = current.progressByDayMin.toMutableList()
    while (updatedProgress.size < DAYS_IN_WEEK) {
      updatedProgress.add(0)
    }
    updatedProgress[todayIndex] = updatedProgress[todayIndex] + minutesDelta

    val updatedStats =
        current.copy(
            totalTimeMin = updatedTotalTime,
            courseTimesMin = updatedCourseTimes,
            progressByDayMin = updatedProgress,
        )

    statsRepository.update(updatedStats)
  }

  private fun loadSuggestedTasks() {
    viewModelScope.launch {
      val tasks = repository.getSuggestedTasks()
      val selectedId = _uiState.value.selectedTask?.id
      _uiState.update { current ->
        val refreshedSelected = tasks.firstOrNull { it.id == selectedId } ?: current.selectedTask
        current.copy(suggestedTasks = tasks, selectedTask = refreshedSelected)
      }
    }
  }

  /** Set the status of the currently selected task, then refresh suggestions/selection. */
  fun setSelectedTaskStatus(newStatus: Status) {
    val selectedId = _uiState.value.selectedTask?.id ?: return
    viewModelScope.launch {
      val todo = toDoRepo.getById(selectedId) ?: return@launch
      toDoRepo.update(todo.copy(status = newStatus))

      // Refresh suggestions & selection based on updated repo state
      loadSuggestedTasks()
      val updated = toDoRepo.todos.first().firstOrNull { it.id == selectedId }
      _uiState.update { it.copy(selectedTask = updated) }
    }
  }

  /** One-tap status cycle: TODO -> IN_PROGRESS -> DONE -> TODO. */
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

  /** Selects a subject for the study session. */
  fun selectSubject(subject: StudySubject) {
    _uiState.update { it.copy(selectedSubject = subject) }
  }

  /** Creates a new subject with the given name. */
  fun createSubject(name: String) {
    if (name.isBlank()) return
    viewModelScope.launch {
      subjectsRepository.createSubject(name = name.trim(), colorIndex = DEFAULT_SUBJECT_COLOR_INDEX)
    }
  }
}
