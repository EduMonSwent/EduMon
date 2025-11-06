package com.android.sample.ui.stats.model

/** Plain data model for the stats feature. */
data class StudyStats(
    val totalTimeMin: Int = 0,
    val courseTimesMin: Map<String, Int> = emptyMap(),
    val completedGoals: Int = 0,
    val progressByDayMin: List<Int> = List(7) { 0 },
    val dailyGoalMin: Int = 20,
    val weeklyGoalMin: Int = 300
)
