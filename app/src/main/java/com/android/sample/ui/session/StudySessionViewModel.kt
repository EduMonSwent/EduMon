// Parts of this file were generated with the help of an AI language model.

package com.android.sample.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.sample.data.Status
import com.android.sample.data.ToDo
import com.android.sample.pet.PetController
import com.android.sample.profile.ProfileRepository
import com.android.sample.profile.ProfileRepositoryProvider
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

/**
 * Parts of this file were generated with the help of an AI assistant.
 * Comments are in English.
 */

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

typealias Task = ToDo

class StudySessionViewModel(
    val pomodoroViewModel: PomodoroViewModelContract = PomodoroViewModel(),
    private val repository: StudySessionRepository,
    private val profileRepository: ProfileRepository = ProfileRepositoryProvider.repository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudySessionUiState())
    val uiState: StateFlow<StudySessionUiState> = _uiState

    private val toDoRepo = ToDoRepositoryProvider.repository

    init {
        observePomodoro()
        loadSuggestedTasks()
        _uiState.update {
            it.copy(
                totalMinutes = profileRepository.profile.value.studyStats.totalTimeMin,
                streakCount = profileRepository.profile.value.streak
            )
        }
    }

    private fun observePomodoro() {
        var lastPhase: PomodoroPhase? = null
        var lastState: PomodoroState? = null

        combine(
            pomodoroViewModel.phase,
            pomodoroViewModel.timeLeft,
            pomodoroViewModel.state
        ) { phase, timeLeft, state ->
            Triple(phase, timeLeft, state)
        }
            .onEach { (phase, timeLeft, state) ->
                _uiState.update {
                    it.copy(
                        pomodoroState = state,
                        timeLeft = timeLeft,
                        isSessionActive = state == PomodoroState.RUNNING
                    )
                }

                if (lastPhase == PomodoroPhase.WORK &&
                    lastState != PomodoroState.FINISHED &&
                    state == PomodoroState.FINISHED
                ) {
                    onPomodoroCompleted()
                }
                lastPhase = phase
                lastState = state
            }
            .launchIn(viewModelScope)
    }

    private suspend fun onPomodoroCompleted() {
        // update profile stats first
        profileRepository.increaseStreakIfCorrect()
        profileRepository.increaseStudyTimeBy(25)

        val newTotal = profileRepository.profile.value.studyStats.totalTimeMin
        val newStreak = profileRepository.profile.value.streak

        _uiState.update {
            it.copy(
                completedPomodoros = pomodoroViewModel.cycleCount.value,
                totalMinutes = newTotal,
                streakCount = newStreak
            )
        }

        repository.saveCompletedSession(_uiState.value)
    }

    private fun loadSuggestedTasks() {
        viewModelScope.launch {
            val tasks = repository.getSuggestedTasks()
            _uiState.update { it.copy(suggestedTasks = tasks) }
            val selectedId = _uiState.value.selectedTask?.id
            _uiState.update {
                it.copy(
                    suggestedTasks = tasks,
                    selectedTask = tasks.find { t -> t.id == selectedId }
                )
            }
        }
    }

    fun setSelectedTaskStatus(newStatus: Status) {
        val selectedId = _uiState.value.selectedTask?.id ?: return
        viewModelScope.launch {
            val todo = toDoRepo.getById(selectedId) ?: return@launch
            toDoRepo.update(todo.copy(status = newStatus))

            loadSuggestedTasks()
            val updated = toDoRepo.todos.first().firstOrNull { it.id == selectedId }
            _uiState.update { it.copy(selectedTask = updated) }
        }
    }

    fun cycleSelectedTaskStatus() {
        val selectedId = _uiState.value.selectedTask?.id ?: return
        viewModelScope.launch {
            val todo = toDoRepo.getById(selectedId) ?: return@launch
            val next = when (todo.status) {
                com.android.sample.data.Status.TODO -> com.android.sample.data.Status.IN_PROGRESS
                com.android.sample.data.Status.IN_PROGRESS -> com.android.sample.data.Status.DONE
                com.android.sample.data.Status.DONE -> com.android.sample.data.Status.TODO
            }
            toDoRepo.update(todo.copy(status = next))

            loadSuggestedTasks()
            val updated = toDoRepo.todos.first().firstOrNull { it.id == selectedId }
            _uiState.update { it.copy(selectedTask = updated) }
        }
    }

    fun selectTask(task: Task) {
        _uiState.update { it.copy(selectedTask = task) }
    }
}
