package com.android.sample.data

/**
 * Aggregated user study stats backed by Firestore.
 *
 * All minutes are per *user*, not per device.
 *
 * lastStudyDateEpochDay:
 * - null -> never studied
 * - value -> LocalDate.ofEpochDay(value) is the last day where todayStudyMinutes > 0
 */
data class UserStats(
    val totalStudyMinutes: Int = 0,
    val todayStudyMinutes: Int = 0,
    val streak: Int = 0,
    val weeklyGoal: Int = 0,
    val points: Int = 0,
    val coins: Int = 0,
    val lastStudyDateEpochDay: Long? = null,
)
