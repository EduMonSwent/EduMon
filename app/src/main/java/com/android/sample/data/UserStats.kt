package com.android.sample.data

// This code has been written partially using A.I (LLM).

/**
 * Represents global user statistics stored in a single Firestore document: /users/{uid}/stats/stats
 */
data class UserStats(
    val totalStudyMinutes: Int = 0,
    val todayStudyMinutes: Int = 0,
    val streak: Int = 0,
    val weeklyGoal: Int = 0,
    val dailyGoal: Int = 20, // Added for StatsScreen
    val completedGoals: Int = 0, // Added for StatsScreen
    val todayCompletedPomodoros: Int = 0,
    val coins: Int = 0,
    val points: Int = 0,
    val lastUpdated: Long = 0L, // Used for syncing with remote
    val lastStudyDate: Long = 0L, // Explicitly tracks the last time minutes were added
    val courseTimesMin: Map<String, Int> = emptyMap(), // Added for StatsScreen
    val progressByDayMin: List<Int> = List(7) { 0 } // Added for StatsScreen
)
