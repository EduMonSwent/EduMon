// Parts of this file were generated with the help of an AI language model.

package com.android.sample.pet.model

/** Current EduMon state, values normalized in 0f..1f */
data class PetState(
    val energy: Float = 1f,
    val happiness: Float = 0.5f,
    val growth: Float = 0f,
    val coins: Int = 0,
    val equippedIds: List<String> = emptyList(),
    val auraId: String? = null,
    val lastProfileMinutes: Int = 0
)

