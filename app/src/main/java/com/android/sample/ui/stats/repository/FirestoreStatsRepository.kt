package com.android.sample.ui.stats.repository

import com.android.sample.ui.stats.model.StudyStats
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await

/** Firestore-backed Stats repository stored under users/{uid} without realtime listeners. */
class FirestoreStatsRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
) : StatsRepository {

  private val _stats = MutableStateFlow(defaultStats())
  override val stats: StateFlow<StudyStats> = _stats

  private val _selectedIndex = MutableStateFlow(0)
  override val selectedIndex: StateFlow<Int> = _selectedIndex

  override val titles: List<String> = listOf("Cloud")

  override fun loadScenario(index: Int) {
    _selectedIndex.value = 0 // single scenario
  }

  override suspend fun refresh() {
    val uid = auth.currentUser?.uid ?: return

    // 1) Ensure defaults exist and backfill any missing fields in Firestore
    ensureDefaultStats(uid)

    // 2) Fetch current stats once
    val userDoc = db.collection("users").document(uid).get().await()
    val statsMap = userDoc.data?.get("stats") as? Map<*, *>
    val current = mapToStats(statsMap)

    _stats.value = current
  }

  override suspend fun update(stats: StudyStats) {
    val uid = auth.currentUser?.uid ?: return
    val userDoc = db.collection("users").document(uid)
    val payload = mapOf("stats" to statsToPayload(stats))
    userDoc.set(payload, SetOptions.merge()).await()
    _stats.value = stats
  }

  // --- Helpers ---

  private suspend fun ensureDefaultStats(uid: String) {
    val userDoc = db.collection("users").document(uid)
    val snap = userDoc.get().await()
    val raw = snap.data?.get("stats") as? Map<*, *>

    if (!snap.exists() || raw == null) {
      // No stats present: seed defaults
      val defaults = defaultStats()
      userDoc.set(mapOf("stats" to statsToPayload(defaults)), SetOptions.merge()).await()
      _stats.value = defaults
      return
    }

    // Stats present: if different from current defaults, replace with new defaults
    val current = mapToStats(raw)
    val defaults = defaultStats()
    if (current != defaults) {
      userDoc.set(mapOf("stats" to statsToPayload(defaults)), SetOptions.merge()).await()
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
    val weeklyGoal = (map["weeklyGoalMin"] as? Number)?.toInt() ?: 300
    val completed = (map["completedGoals"] as? Number)?.toInt() ?: 0

    val courseTimes = mutableMapOf<String, Int>()
    val rawCourse = map["courseTimesMin"] as? Map<*, *>
    rawCourse?.forEach { (k, v) ->
      val key = k?.toString() ?: return@forEach
      val value = (v as? Number)?.toInt() ?: 0
      courseTimes[key] = value
    }

    val progress = mutableListOf<Int>()
    val rawDays = map["progressByDayMin"] as? List<*>
    rawDays?.forEach { v -> progress.add((v as? Number)?.toInt() ?: 0) }
    if (progress.size != 7) {
      while (progress.size < 7) progress.add(0)
      if (progress.size > 7) progress.subList(7, progress.size).clear()
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
          totalTimeMin = 5,
          courseTimesMin =
              linkedMapOf(
                  "Analyse I" to 60,
                  "Algèbre linéaire" to 45,
                  "Physique mécanique" to 25,
                  "AICC I" to 15),
          completedGoals = 10,
          progressByDayMin = listOf(0, 25, 30, 15, 50, 20, 5),
          weeklyGoalMin = 300,
      )
}
