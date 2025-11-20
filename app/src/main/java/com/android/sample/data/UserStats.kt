package com.android.sample.data

// This code has been written partially using A.I (LLM).

/** Global user statistics stored in a single Firestore document: /users/{uid}/stats/stats */
data class UserStats(
    val totalStudyMinutes: Int = 0,
    val todayStudyMinutes: Int = 0,
    val streak: Int = 0,
    val weeklyGoal: Int = 0,
    val coins: Int = 0,
    val points: Int = 0,
    val lastUpdated: Long = 0L
)
