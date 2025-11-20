package com.android.sample.ui.stats.viewmodel

// This code has been written partially using A.I (LLM).

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.sample.data.UserStatsRepository
import com.android.sample.feature.weeks.repository.ObjectivesRepository
import com.android.sample.repos_providors.AppRepositories
import com.android.sample.ui.stats.model.StudyStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for StatsScreen. Reads StudyStats from UserStatsRepository and can sync completed goals
 * from Objectives when requested.
 *
 * Now uses UserStatsRepository as the single source of truth.
 */
class StatsViewModel(
    private val userStatsRepo: UserStatsRepository = AppRepositories.userStatsRepository,
    private val objectivesRepo: ObjectivesRepository = AppRepositories.objectivesRepository,
) : ViewModel() {

  /** Exposes the current StudyStats coming from the unified Firestore stats document. */
  val stats: StateFlow<StudyStats?> =
      userStatsRepo.stats
          .map { userStats ->
            StudyStats(
                totalTimeMin = userStats.totalStudyMinutes,
                courseTimesMin = userStats.courseTimesMin,
                completedGoals = userStats.completedGoals,
                progressByDayMin = userStats.progressByDayMin,
                dailyGoalMin = userStats.dailyGoal,
                weeklyGoalMin = userStats.weeklyGoal)
          }
          .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

  private val _scenarioIndex = MutableStateFlow(0)
  val scenarioIndex: StateFlow<Int> = _scenarioIndex
  val scenarioTitles = listOf("Semaine")

  init {
    // Start listening to Firestore only once when the ViewModel is created.
    userStatsRepo.start()
  }

  fun selectScenario(index: Int) {
    _scenarioIndex.value = index
  }

  /** Computes completed goals from objectives and writes it into the stats document. */
  fun syncCompletedGoalsFromObjectives() {
    viewModelScope.launch {
      val objectives = objectivesRepo.getObjectives()
      val completedCount = objectives.count { it.completed }

      userStatsRepo.updateCompletedGoals(completedCount)
    }
  }
}
