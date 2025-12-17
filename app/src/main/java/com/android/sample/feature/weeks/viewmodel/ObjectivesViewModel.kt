package com.android.sample.feature.weeks.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.sample.data.UserStatsRepository
import com.android.sample.feature.weeks.model.Objective
import com.android.sample.feature.weeks.model.ObjectiveType
import com.android.sample.feature.weeks.repository.ObjectivesRepository
import com.android.sample.repos_providors.AppRepositories
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import java.time.DayOfWeek
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// Reward constants by objective type
private const val POINTS_QUIZ = 5
private const val COINS_QUIZ = 3

private const val POINTS_COURSE_OR_EXERCISES = 3
private const val COINS_COURSE_OR_EXERCISES = 2

private const val POINTS_RESUME = 2
private const val COINS_RESUME = 1

sealed class ObjectiveNavigation {
  data class ToQuiz(val objective: Objective) : ObjectiveNavigation()

  data class ToCourseExercises(val objective: Objective) : ObjectiveNavigation()

  data class ToResume(val objective: Objective) : ObjectiveNavigation()
}

data class ObjectivesUiState(
    val objectives: List<Objective> = emptyList(),
    val showWhy: Boolean = true,
)

class ObjectivesViewModel(
    private val repository: ObjectivesRepository = AppRepositories.objectivesRepository,
    private val userStatsRepository: UserStatsRepository = AppRepositories.userStatsRepository,
    private val requireAuth: Boolean = true,
) : ViewModel() {

  private val _uiState = MutableStateFlow(ObjectivesUiState())
  val uiState = _uiState.asStateFlow()

  /*val todayObjectives =
  _uiState.map { state ->
    val today = LocalDate.now().dayOfWeek
    state.objectives.filter { it.day == today }
  }*/
  private val _autoObjectives = MutableStateFlow<List<Objective>>(emptyList())

  val todayObjectives =
      combine(_uiState, _autoObjectives) { state, auto ->
        val today = LocalDate.now().dayOfWeek
        (state.objectives + auto).filter { it.day == today }
      }

  fun replaceAutoObjectives(objs: List<Objective>) {
    _autoObjectives.update {
      val persistedSourceIds = _uiState.value.objectives.mapNotNull { it.sourceId }.toSet()

      objs.filter { it.sourceId !in persistedSourceIds }
    }
  }

  private val _navigationEvents = MutableSharedFlow<ObjectiveNavigation>()
  val navigationEvents = _navigationEvents.asSharedFlow()

  init {
    viewModelScope.launch {
      if (requireAuth) ensureSignedIn()
      refresh()
    }
  }

  private suspend fun ensureSignedIn() {
    if (Firebase.auth.currentUser == null) {
      Firebase.auth.signInAnonymously().await()
    }
  }

  fun refresh() {
    viewModelScope.launch {
      val objs = repository.getObjectives()
      _uiState.update { it.copy(objectives = objs) }
    }
  }

  fun isObjectivesOfDayCompleted(day: DayOfWeek): Boolean {
    val dayList = _uiState.value.objectives.filter { it.day == day }
    return dayList.isNotEmpty() && dayList.all { it.completed }
  }

  fun setObjectives(objs: List<Objective>) {
    viewModelScope.launch {
      val list = repository.setObjectives(objs)
      _uiState.update { it.copy(objectives = list) }
    }
  }

  fun addObjective(obj: Objective) {
    viewModelScope.launch {
      val list = repository.addObjective(obj)
      _uiState.update { it.copy(objectives = list) }
    }
  }

  fun updateObjective(index: Int, obj: Objective) {
    viewModelScope.launch {
      val list = repository.updateObjective(index, obj)
      _uiState.update { it.copy(objectives = list) }
    }
  }

  fun removeObjective(index: Int) {
    viewModelScope.launch {
      val list = repository.removeObjective(index)
      _uiState.update { it.copy(objectives = list) }
    }
  }

  fun moveObjective(fromIndex: Int, toIndex: Int) {
    viewModelScope.launch {
      val list = repository.moveObjective(fromIndex, toIndex)
      _uiState.update { it.copy(objectives = list) }
    }
  }

  fun setShowWhy(show: Boolean) {
    _uiState.update { it.copy(showWhy = show) }
  }

  /** Calculate rewards based on objective type. Returns Pair<points, coins> */
  private fun calculateRewards(objective: Objective): Pair<Int, Int> {
    return when (objective.type) {
      ObjectiveType.QUIZ -> POINTS_QUIZ to COINS_QUIZ
      ObjectiveType.COURSE_OR_EXERCISES -> POINTS_COURSE_OR_EXERCISES to COINS_COURSE_OR_EXERCISES
      ObjectiveType.RESUME -> POINTS_RESUME to COINS_RESUME
    }
  }

  fun markObjectiveCompleted(objective: Objective) {
    viewModelScope.launch {
      if (objective.isAuto) {
        val stored = objective.copy(completed = true, isAuto = false)
        repository.addObjective(stored)

        _autoObjectives.update { autos -> autos.filterNot { it.sourceId == objective.sourceId } }

        refresh()

        val (points, coins) = calculateRewards(objective)
        userStatsRepository.addReward(points, coins)

        return@launch
      }
      val current = _uiState.value.objectives
      val index = current.indexOf(objective)
      if (index == -1) return@launch

      // Check if it was already completed (to avoid granting rewards twice)
      val wasAlreadyCompleted = current[index].completed

      // Update the objective as completed
      updateObjective(index, current[index].copy(completed = true))

      // Grant rewards only if it wasn't completed before
      if (!wasAlreadyCompleted) {
        val (points, coins) = calculateRewards(objective)
        userStatsRepository.addReward(points = points, coins = coins)
      }
    }
  }

  @Suppress("UNUSED_PARAMETER")
  fun startObjective(index: Int = 0) {
    val obj = _uiState.value.objectives.getOrNull(index) ?: return
    viewModelScope.launch { _navigationEvents.emit(ObjectiveNavigation.ToCourseExercises(obj)) }
  }

  fun startObjective(objective: Objective) {
    viewModelScope.launch {
      when (objective.type) {
        ObjectiveType.QUIZ -> _navigationEvents.emit(ObjectiveNavigation.ToQuiz(objective))
        ObjectiveType.COURSE_OR_EXERCISES ->
            _navigationEvents.emit(ObjectiveNavigation.ToCourseExercises(objective))
        ObjectiveType.RESUME -> _navigationEvents.emit(ObjectiveNavigation.ToResume(objective))
      }
    }
  }
}
