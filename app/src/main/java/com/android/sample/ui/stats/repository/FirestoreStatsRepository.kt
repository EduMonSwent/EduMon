package com.android.sample.ui.stats.repository

import com.android.sample.core.helpers.setMerged
import com.android.sample.ui.stats.model.StudyStats
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await

/**
 * This code has been written partially using A.I (LLM).
 *
 * Firestore-backed implementation of StatsRepository.
 *
 * Data is stored in the same document as unified user stats: /users/{uid}/stats/stats
 *
 * The document contains flat fields for weekly study statistics:
 * - totalTimeMin: Int
 * - weeklyGoalMin: Int
 * - courseTimesMin: Map<String, Int>
 * - progressByDayMin: List<Int> (size 7)
 * - completedGoals: Int
 */
private const val DAYS_IN_WEEK = 7

class FirestoreStatsRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
) : StatsRepository {

  private val _stats = MutableStateFlow(defaultStats())
  override val stats: StateFlow<StudyStats> = _stats

  private val _selectedIndex = MutableStateFlow(0)
  override val selectedIndex: StateFlow<Int> = _selectedIndex

  // Single “scenario”: the current week
  override val titles: List<String> = listOf("Cloud")

  override fun loadScenario(index: Int) {
    // Only one scenario; always keep index at 0
    _selectedIndex.value = 0
  }

  override suspend fun refresh() {
    val uid = auth.currentUser?.uid ?: return
    val statsDoc = db.collection("users").document(uid).collection("stats").document("stats")

    // Ensure the stats document exists with at least default fields
    ensureDefaultStats(statsDoc)

    val snap = statsDoc.get().await()
    val current = mapToStats(snap.data)

    _stats.value = current
  }

  override suspend fun update(stats: StudyStats) {
    val uid = auth.currentUser?.uid ?: return
    val statsDoc = db.collection("users").document(uid).collection("stats").document("stats")

    statsDoc.setMerged(statsToPayload(stats))
    _stats.value = stats
  }

  // --- Helpers ---

  private suspend fun ensureDefaultStats(
      statsDoc: com.google.firebase.firestore.DocumentReference,
  ) {
    val snap = statsDoc.get().await()

    if (!snap.exists()) {
      val defaults = defaultStats()
      statsDoc.setMerged(statsToPayload(defaults))
      _stats.value = defaults
    }
  }

  private fun statsToPayload(stats: StudyStats): Map<String, Any> =
      hashMapOf(
          "totalTimeMin" to stats.totalTimeMin,
          "weeklyGoalMin" to stats.weeklyGoalMin,
          "courseTimesMin" to stats.courseTimesMin,
          "progressByDayMin" to stats.progressByDayMin,
          "completedGoals" to stats.completedGoals,
      )

  private fun mapToStats(map: Map<*, *>?): StudyStats {
    if (map == null) return defaultStats()
    val total = (map["totalTimeMin"] as? Number)?.toInt() ?: 0
    val weeklyGoal = (map["weeklyGoalMin"] as? Number)?.toInt() ?: 0
    val completed = (map["completedGoals"] as? Number)?.toInt() ?: 0

    val courseTimes = linkedMapOf<String, Int>()
    val rawCourse = map["courseTimesMin"] as? Map<*, *>
    rawCourse?.forEach { (k, v) ->
      val key = k?.toString() ?: return@forEach
      val value = (v as? Number)?.toInt() ?: 0
      courseTimes[key] = value
    }

    val progress = mutableListOf<Int>()
    val rawDays = map["progressByDayMin"] as? List<*>
    rawDays?.forEach { v -> progress.add((v as? Number)?.toInt() ?: 0) }
    while (progress.size < DAYS_IN_WEEK) {
      progress.add(0)
    }
    if (progress.size > DAYS_IN_WEEK) {
      // Keep only the first DAYS_IN_WEEK entries
      while (progress.size > DAYS_IN_WEEK) {
        progress.removeLast()
      }
    }

    return StudyStats(
        totalTimeMin = total,
        courseTimesMin = courseTimes,
        completedGoals = completed,
        progressByDayMin = progress,
        weeklyGoalMin = weeklyGoal,
    )
  }

  private fun defaultStats() =
      StudyStats(
          totalTimeMin = 0,
          courseTimesMin = linkedMapOf(),
          completedGoals = 0,
          progressByDayMin = List(DAYS_IN_WEEK) { 0 },
          weeklyGoalMin = 0,
      )
}
