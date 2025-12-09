package com.android.sample.ui.stats.viewmodel

// This code has been written partially using A.I (LLM).

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.sample.feature.weeks.repository.ObjectivesRepository
import com.android.sample.repos_providors.AppRepositories
import com.android.sample.ui.stats.model.StudyStats
import com.android.sample.ui.stats.repository.StatsRepository
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for StatsScreen. Reads StudyStats from repo and can sync completedGoals from Objectives
 * when requested.
 */
class StatsViewModel(
    private val repo: StatsRepository = AppRepositories.statsRepository,
    private val objectivesRepo: ObjectivesRepository = AppRepositories.objectivesRepository,
) : ViewModel() {

  val stats: StateFlow<StudyStats?> = repo.stats

  val scenarioTitles: List<String>
    get() = repo.titles

  val scenarioIndex: StateFlow<Int>
    get() = repo.selectedIndex

  fun selectScenario(i: Int) = repo.loadScenario(i)

  init {
    viewModelScope.launch { repo.refresh() }
  }

  /** Computes completed goals from objectives and writes it into the stats doc. */
  fun syncCompletedGoalsFromObjectives() {
    viewModelScope.launch {
      if (Firebase.auth.currentUser == null) return@launch

      val base = repo.stats.value
      val objs = objectivesRepo.getObjectives()
      val completed = objs.count { it.completed }

      repo.update(base.copy(completedGoals = completed))
    }
  }
}
