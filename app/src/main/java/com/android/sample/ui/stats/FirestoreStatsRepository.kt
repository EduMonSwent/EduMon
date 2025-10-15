package com.android.sample.ui.stats


import com.android.sample.ui.stats.model.StudyStats
import com.android.sample.ui.util.WeekUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await

class FirestoreStatsRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    private val _stats = MutableStateFlow<StudyStats?>(null)
    val stats: StateFlow<StudyStats?> = _stats

    private fun doc() = db.collection("users")
        .document(requireNotNull(auth.currentUser?.uid) { "No user" })
        .collection("stats")
        .document(WeekUtils.currentWeekId())

    init {
        if (auth.currentUser != null) {
            doc().addSnapshotListener { snap, _ ->
                val s = snap?.toObject<StudyStats>()
                _stats.value = s
            }
        }
    }

    suspend fun ensureDefaults() {
        val ref = doc()
        val cur = ref.get().await()
        if (!cur.exists()) {
            ref.set(
                StudyStats(
                    totalTimeMin = 0,
                    courseTimesMin = mapOf(
                        "Analyse I" to 0, "Algèbre linéaire" to 0, "Physique mécanique" to 0, "AICC I" to 0
                    ),
                    completedGoals = 0,
                    progressByDayMin = List(7) { 0 },
                    weeklyGoalMin = 300
                )
            ).await()
        }
    }
}
