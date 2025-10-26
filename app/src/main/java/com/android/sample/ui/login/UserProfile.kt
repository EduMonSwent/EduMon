package com.android.sample.ui.login

data class UserProfile(
    val name: String = "Alex",
    val email: String = "alex@university.edu",
    val level: Int = 5,
    val points: Int = 1250,
    val streak: Int = 7,
    val studyTimeToday: Int = 45,
    val dailyGoal: Int = 180,
    val notificationsEnabled: Boolean = true,
    val locationEnabled: Boolean = true,
    val focusModeEnabled: Boolean = false
)
