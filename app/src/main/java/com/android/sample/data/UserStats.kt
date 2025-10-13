package com.android.sample.data

data class UserStats(
    val streakDays: Int = 7,
    val points: Int = 1250,
    val studyTodayMin: Int = 45,
    val dailyGoalMin: Int = 180,
)
