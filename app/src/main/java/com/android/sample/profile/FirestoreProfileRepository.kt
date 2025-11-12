package com.android.sample.profile


import com.android.sample.ui.stats.model.StudyStats
import com.android.sample.data.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Profile backed by Firestore, exposes a StateFlow<UserProfile>.
 * Single source of truth for coins, streak, study minutes, accessories and owned.
 */
class FirestoreProfileRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ProfileRepository {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val _profile = MutableStateFlow(defaultProfile())
    override val profile: StateFlow<UserProfile> = _profile

    private var reg: ListenerRegistration? = null
    private var startedUid: String? = null

    init {
        auth.addAuthStateListener { fa ->
            val u = fa.currentUser
            if (u?.uid != startedUid) {
                reg?.remove()
                startedUid = u?.uid
                if (u == null) {
                    _profile.value = defaultProfile()
                } else {
                    attach(u.uid)
                }
            }
        }
        auth.currentUser?.uid?.let { attach(it) }
    }

    private fun attach(uid: String) {
        val ref = db.collection("users").document(uid)
        reg = ref.addSnapshotListener { snap, _ ->
            if (snap == null || !snap.exists()) {
                // initialize a minimal profile if missing
                scope.launch {
                    ref.set(
                        mapOf(
                            "coins" to 0,
                            "streak" to 0,
                            "studyStats" to mapOf("totalTimeMin" to 0, "dailyGoalMin" to 30),
                            "accessories" to emptyList<String>(),
                            "owned" to emptyList<String>()
                        )
                    ).await()
                }
                _profile.value = defaultProfile()
                return@addSnapshotListener
            }

            // defensive read with defaults
            val coins = snap.getLong("coins")?.toInt() ?: 0
            val streak = snap.getLong("streak")?.toInt() ?: 0
            val studyObj = snap.get("studyStats") as? Map<*, *> ?: emptyMap<String, Any>()
            val totalMin = (studyObj["totalTimeMin"] as? Number)?.toInt() ?: 0
            val dailyGoal = (studyObj["dailyGoalMin"] as? Number)?.toInt() ?: 30
            val accessories = (snap.get("accessories") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            val owned = (snap.get("owned") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            val current = _profile.value

            _profile.value = current.copy(
                coins = coins,
                streak = streak,
                studyStats = StudyStats(totalTimeMin = totalMin, dailyGoalMin = dailyGoal),
                accessories = accessories,
                owned = owned
            )
        }
    }

    override suspend fun updateProfile(updated: UserProfile) {
        val uid = auth.currentUser?.uid ?: return
        val ref = db.collection("users").document(uid)
        val payload = mapOf(
            "coins" to updated.coins,
            "streak" to updated.streak,
            "studyStats" to mapOf(
                "totalTimeMin" to updated.studyStats.totalTimeMin,
                "dailyGoalMin" to updated.studyStats.dailyGoalMin
            ),
            "accessories" to updated.accessories,
            "owned" to updated.owned
        )
        ref.set(payload, com.google.firebase.firestore.SetOptions.merge()).await()
    }

    override suspend fun increaseStudyTimeBy(minutes: Int) {
        val uid = auth.currentUser?.uid ?: return
        val ref = db.collection("users").document(uid)
        ref.update("studyStats.totalTimeMin", FieldValue.increment(minutes.toLong())).await()
    }

    override suspend fun increaseStreakIfCorrect() {
        // Simplified streak bump. Replace by calendar based logic if needed.
        val uid = auth.currentUser?.uid ?: return
        val ref = db.collection("users").document(uid)
        ref.update("streak", FieldValue.increment(1)).await()
    }

    private fun defaultProfile(): UserProfile {
        return UserProfile(
            coins = 0,
            streak = 0,
            studyStats = StudyStats(totalTimeMin = 0, dailyGoalMin = 30),
            accessories = emptyList(),
            owned = emptyList()
        )
    }
}
