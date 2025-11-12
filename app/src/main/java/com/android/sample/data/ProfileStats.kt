package com.android.sample.data

import com.android.sample.ui.stats.model.StudyStats

enum class AccessorySlot {
  HEAD,
  TORSO,
  LEGS
}

enum class Rarity {
  COMMON,
  RARE,
  EPIC,
  LEGENDARY
}

data class AccessoryItem(
    val id: String,
    val slot: AccessorySlot,
    val label: String,
    val iconRes: Int? = null, // TODO remplace par tes drawables quand tu les auras
    val rarity: Rarity = Rarity.COMMON
)

data class UserProfile(
    val name: String = DEFAULT_NAME,
    val email: String = DEFAULT_EMAIL,
    val level: Int = DEFAULT_LEVEL,
    val points: Int = DEFAULT_POINTS,
    val coins: Int = DEFAULT_COINS,
    val streak: Int = DEFAULT_STREAK,
    val notificationsEnabled: Boolean = DEFAULT_NOTIFICATIONS,
    val locationEnabled: Boolean = DEFAULT_LOCATION,
    val focusModeEnabled: Boolean = DEFAULT_FOCUS_MODE,
    val avatarAccent: Long = DEFAULT_ACCENT, // ARGB
    val accessories: List<String> = emptyList(),
    val owned: List<String> = emptyList(),
    val studyStats: StudyStats =
        StudyStats(totalTimeMin = DEFAULT_STUDY_TIME, dailyGoalMin = DEFAULT_DAILY_GOAL)
) {
  companion object {
    const val DEFAULT_NAME = "Alex"
    const val DEFAULT_EMAIL = "alex@university.edu"
    const val DEFAULT_LEVEL = 5
    const val DEFAULT_POINTS = 1250
    const val DEFAULT_COINS = 0
    const val DEFAULT_STREAK = 7
    const val DEFAULT_STUDY_TIME = 45
    const val DEFAULT_DAILY_GOAL = 180
    const val DEFAULT_NOTIFICATIONS = true
    const val DEFAULT_LOCATION = true
    const val DEFAULT_FOCUS_MODE = false
    const val DEFAULT_ACCENT = 0xFF9333EAL
  }
}

enum class AccentVariant {
  Base,
  Light,
  Dark,
  Vibrant
}
