// app/src/main/java/com/android/sample/pet/domain/PetEngine.kt
package com.android.sample.pet.domain

import com.android.sample.pet.model.PetState

object PetEngine {
    private const val ENERGY_PER_MIN = 0.0025f
    private const val HAPPINESS_PER_MIN = 0.0018f
    private const val GROWTH_PER_MIN = 0.0030f

    fun onProfileMinutesIncrease(state: PetState, addedMinutes: Int): PetState {
        if (addedMinutes <= 0) return state
        val dEnergy = -addedMinutes * ENERGY_PER_MIN
        val dHappy = addedMinutes * HAPPINESS_PER_MIN
        val dGrowth = addedMinutes * GROWTH_PER_MIN
        return state.copy(
            energy = (state.energy + dEnergy).coerceIn(0f, 1f),
            happiness = (state.happiness + dHappy).coerceIn(0f, 1f),
            growth = (state.growth + dGrowth).coerceIn(0f, 1f)
        )
    }

    fun onEquip(state: PetState, newEquipped: Set<String>, newAuraId: String?): PetState {
        return state.copy(equippedIds = newEquipped.toList(), auraId = newAuraId)
    }

    fun onCoinsChanged(state: PetState, newCoins: Int): PetState {
        return state.copy(coins = newCoins)
    }
}
