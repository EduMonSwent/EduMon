package com.android.sample.pet.model

/**
 * Normalized floats for internal engine, plus a few mirrors for convenience.
 * equippedIds must be a List for Firestore serialization.
 */

data class PetState(
    val energy: Float = 1f,
    val happiness: Float = 0.5f,
    val growth: Float = 0.4f,
    val coins: Int = 0,
    val equippedIds: List<String> = emptyList(),
    val auraId: String? = null,
    val lastProfileMinutes: Int = 0
)

