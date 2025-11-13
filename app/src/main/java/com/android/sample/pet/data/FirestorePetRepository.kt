package com.android.sample.pet.data

import com.android.sample.pet.domain.PetEngine
import com.android.sample.pet.model.PetState
import com.android.sample.repos_providors.AppRepositories
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Firebase backed pet repository.
 * Persists PetState under users/{uid}/petState/state,
 * keeps an in memory StateFlow for UI consumption.
 */

class FirestorePetRepository(
    private val db: FirebaseFirestore
) : PetRepository {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val _state = MutableStateFlow(PetState())
    override val state: StateFlow<PetState> = _state

    private var uid: String? = null

    override fun start(uid: String) {
        this.uid = uid
        scope.launch {
            val snap = db.collection("users").document(uid)
                .collection("petState").document("state")
                .get().await()
            _state.value = snap.toObject(PetState::class.java) ?: PetState()

            AppRepositories.profileRepository.profile. collectLatest { profile ->
                val cur = _state.value

                val added = profile.studyStats.totalTimeMin - cur.lastProfileMinutes
                var next = cur

                if (added > 0) {
                    next = PetEngine.onProfileMinutesIncrease(cur, added)
                        .copy(
                            lastProfileMinutes = profile.studyStats.totalTimeMin,
                            coins = profile.coins
                        )
                } else if (profile.coins != cur.coins) {
                    next = PetEngine.onCoinsChanged(cur, profile.coins)
                }

                val equipped = profile.accessories.mapNotNull { s ->
                    s.substringAfter(":", "")
                        .takeIf { it.isNotBlank() }
                }

                if (equipped != cur.equippedIds) {
                    next = next.copy(equippedIds = equipped)
                }

                if (next != cur) persistAndEmit(next)
            }
        }
    }

    override suspend fun setEquippedAndRecompute(newEquipped: Set<String>, auraId: String?) {
        val next = PetEngine.onEquip(_state.value, newEquipped, auraId)
        persistAndEmit(next)
    }

    override suspend fun onCoinsChanged(newCoins: Int) {
        val next = PetEngine.onCoinsChanged(_state.value, newCoins)
        persistAndEmit(next)
    }

    override suspend fun onStudyCompleted(addedMinutes: Int, newStreak: Int) {
        val next = PetEngine.onProfileMinutesIncrease(_state.value, addedMinutes)
        persistAndEmit(next)
    }

    private suspend fun persistAndEmit(next: PetState) {
        val user = uid ?: return
        db.collection("users").document(user)
            .collection("petState").document("state")
            .set(next).await()
        _state.value = next
    }
}

