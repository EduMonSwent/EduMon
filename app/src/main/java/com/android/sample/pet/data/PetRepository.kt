package com.android.sample.pet.data

import com.android.sample.pet.model.PetState
import kotlinx.coroutines.flow.StateFlow

interface PetRepository {
    val state: StateFlow<PetState>

    fun start(uid: String)

    suspend fun setEquippedAndRecompute(newEquipped: Set<String>, auraId: String?)

    suspend fun onCoinsChanged(newCoins: Int)

    suspend fun onStudyCompleted(addedMinutes: Int, newStreak: Int)
}
