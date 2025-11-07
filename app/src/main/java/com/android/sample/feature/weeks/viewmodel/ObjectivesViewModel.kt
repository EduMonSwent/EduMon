// app/src/main/java/com/android/sample/feature/weeks/viewmodel/ObjectivesViewModel.kt
package com.android.sample.feature.weeks.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.sample.feature.weeks.model.Objective
import com.android.sample.feature.weeks.repository.ObjectivesRepository
import com.android.sample.repos_providors.AppRepositories
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import java.time.DayOfWeek
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await // <- coroutines-play-services

// Holds only objective-related UI state
data class ObjectivesUiState(
    val objectives: List<Objective> = emptyList(),
    val showWhy: Boolean = true,
)

class ObjectivesViewModel(
    // Default to provider; tests can still pass a Fake repo by overriding this param
    private val repository: ObjectivesRepository = AppRepositories.objectivesRepository,
    // When false, we won't attempt Firebase auth automatically (useful for tests)
    private val requireAuth: Boolean = true,
) : ViewModel() {

  private val _uiState = MutableStateFlow(ObjectivesUiState())
  val uiState = _uiState.asStateFlow()

  init {
    // Ensure we have a user (optionally), then load data
    viewModelScope.launch {
      if (requireAuth) ensureSignedIn()
      refresh()
    }
  }

  private suspend fun ensureSignedIn() {
    if (Firebase.auth.currentUser == null) {
      // For quick testing; swap with your real auth flow in production
      Firebase.auth.signInAnonymously().await()
    }
  }

  // Loads from repository
  fun refresh() {
    viewModelScope.launch {
      val objs = repository.getObjectives()
      _uiState.update { it.copy(objectives = objs) }
    }
  }

  // --- Query API for WeekDots ---
  fun isObjectivesOfDayCompleted(day: DayOfWeek): Boolean {
    val dayList = _uiState.value.objectives.filter { it.day == day }
    return dayList.isNotEmpty() && dayList.all { it.completed }
  }

  // ---- Mutations ----
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

  @Suppress("UNUSED_PARAMETER")
  fun startObjective(index: Int = 0) {
    // hook for analytics / navigation later
  }
}
