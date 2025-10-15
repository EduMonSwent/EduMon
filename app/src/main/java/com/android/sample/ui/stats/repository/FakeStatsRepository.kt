package com.android.sample.ui.stats.repository

import com.android.sample.ui.stats.model.StudyStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Simple in-memory scenarios provider for Stats. */
class FakeStatsRepository {
  private val scenarios: List<Pair<String, StudyStats>> =
      listOf(
          "Début de semaine" to
              StudyStats(
                  totalTimeMin = 0,
                  courseTimesMin =
                      linkedMapOf(
                          "Analyse I" to 0,
                          "Algèbre linéaire" to 0,
                          "Physique mécanique" to 0,
                          "AICC I" to 0),
                  completedGoals = 0,
                  progressByDayMin = listOf(0, 0, 0, 0, 0, 0, 0),
                  weeklyGoalMin = 300),
          "Semaine active" to
              StudyStats(
                  totalTimeMin = 145,
                  courseTimesMin =
                      linkedMapOf(
                          "Analyse I" to 60,
                          "Algèbre linéaire" to 45,
                          "Physique mécanique" to 25,
                          "AICC I" to 15),
                  completedGoals = 2,
                  progressByDayMin = listOf(0, 25, 30, 15, 50, 20, 5),
                  weeklyGoalMin = 300),
          "Objectif presque atteint" to
              StudyStats(
                  totalTimeMin = 235,
                  courseTimesMin =
                      linkedMapOf(
                          "Analyse I" to 80,
                          "Algèbre linéaire" to 70,
                          "Physique mécanique" to 40,
                          "AICC I" to 45),
                  completedGoals = 5,
                  progressByDayMin = listOf(20, 30, 45, 35, 50, 40, 15),
                  weeklyGoalMin = 300),
          "Objectif atteint" to
              StudyStats(
                  totalTimeMin = 320,
                  courseTimesMin =
                      linkedMapOf(
                          "Analyse I" to 110,
                          "Algèbre linéaire" to 95,
                          "Physique mécanique" to 60,
                          "AICC I" to 55),
                  completedGoals = 7,
                  progressByDayMin = listOf(40, 60, 55, 50, 45, 40, 30),
                  weeklyGoalMin = 300),
          "Full algèbre" to
              StudyStats(
                  totalTimeMin = 180,
                  courseTimesMin =
                      linkedMapOf(
                          "Analyse I" to 20,
                          "Algèbre linéaire" to 130,
                          "Physique mécanique" to 15,
                          "AICC I" to 15),
                  completedGoals = 3,
                  progressByDayMin = listOf(10, 25, 15, 60, 30, 20, 20),
                  weeklyGoalMin = 300))

  private val _stats = MutableStateFlow(scenarios.first().second)
  val stats: StateFlow<StudyStats> = _stats

  private val _selectedIndex = MutableStateFlow(0)
  val selectedIndex: StateFlow<Int> = _selectedIndex

  val titles: List<String>
    get() = scenarios.map { it.first }

  fun loadScenario(index: Int) {
    val i = index.coerceIn(0, scenarios.lastIndex)
    _selectedIndex.value = i
    _stats.value = scenarios[i].second
  }
}
