// app/src/main/java/com/android/sample/pet/data/FakePetRepository.kt
// Parts of this file were generated with the help of an AI language model.

package com.android.sample.pet.data

import com.android.sample.pet.model.PetState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakePetRepository : PetRepository {

    private val _state = MutableStateFlow(PetState())
    override val state: StateFlow<PetState> = _state

    override fun start(uid: String) { /* no-op */ }

    override suspend fun setEquippedAndRecompute(newEquipped: Set<String>, auraId: String?) {
        val itemsBoost = (newEquipped.size * 0.03f).coerceAtMost(0.15f)
        val auraBoost = if (auraId.isNullOrBlank()) 0f else 0.05f
        _state.value = _state.value.copy(
            happiness = (_state.value.happiness + itemsBoost + auraBoost).coerceIn(0f, 1f)
        )
    }

    override suspend fun onCoinsChanged(newCoins: Int) {
        _state.value = _state.value.copy(
            happiness = (_state.value.happiness + 0.01f).coerceIn(0f, 1f)
        )
    }

    override suspend fun onStudyCompleted(addedMinutes: Int, newStreak: Int) {
        val workFactor = (addedMinutes / 25f).coerceAtMost(2f)
        val streakBonus = (newStreak * 0.01f).coerceAtMost(0.10f)
        val growthUp = 0.06f * workFactor + streakBonus
        val energyDown = 0.04f * workFactor
        val happyUp = 0.02f + streakBonus * 0.5f
        _state.value = _state.value.copy(
            growth = (_state.value.growth + growthUp).coerceIn(0f, 1f),
            energy = (_state.value.energy - energyDown).coerceIn(0f, 1f),
            happiness = (_state.value.happiness + happyUp).coerceIn(0f, 1f)
        )
    }
}
