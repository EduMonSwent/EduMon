package com.android.sample.pet.data

import com.android.sample.pet.domain.PetEngine
import com.android.sample.pet.model.PetState
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

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
            val loaded = snap.toObject(PetState::class.java) ?: PetState()
            // normalize equippedIds to list then set
            _state.value = loaded.copy(equippedIds = loaded.equippedIds)
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
            .copy(lastProfileMinutes = _state.value.lastProfileMinutes + addedMinutes)
        persistAndEmit(next)
    }

    private suspend fun persistAndEmit(next: PetState) {
        val u = uid ?: return
        val payload = next.copy(equippedIds = next.equippedIds.toList())
        db.collection("users").document(u)
            .collection("petState").document("state")
            .set(payload).await()
        _state.value = next
    }
}
