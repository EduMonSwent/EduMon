package com.android.sample.model.planner

import androidx.compose.ui.graphics.Color
import com.android.sample.R
import com.android.sample.ui.theme.EventColorDefault
import com.android.sample.ui.theme.EventColorLecture
import com.android.sample.ui.theme.EventColorMusic
import com.android.sample.ui.theme.EventColorSocial
import com.android.sample.ui.theme.EventColorSports
import com.android.sample.ui.theme.EventColorYoga
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

data class Class(
    val id: String = UUID.randomUUID().toString(),
    val courseName: String,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val type: ClassType,
    val location: String = "",
    val instructor: String = ""
)

enum class ClassType {
  LECTURE,
  EXERCISE,
  LAB
}

data class ClassAttendance(
    val classId: String,
    val date: LocalDate,
    val attendance: AttendanceStatus,
    val completion: CompletionStatus,
    val timestamp: Instant = Instant.now()
)

enum class AttendanceStatus {
  YES,
  NO,
  ARRIVED_LATE
}

enum class CompletionStatus {
  YES,
  NO,
  PARTIALLY
}

enum class WellnessEventType(val iconRes: Int, val primaryColor: Color) {
  YOGA(R.drawable.ic_yoga, EventColorYoga),
  LECTURE(R.drawable.ic_event, EventColorLecture), // Reusing ic_event, or add specific for lecture
  SPORTS(R.drawable.ic_yoga, EventColorSports), // Assuming you have an ic_sports
  SOCIAL(R.drawable.ic_star, EventColorSocial), // Assuming you have an ic_social
  MUSIC(R.drawable.ic_sparkle, EventColorMusic), // Assuming you have an ic_music
  DEFAULT(R.drawable.ic_event, EventColorDefault); // Default if type isn't matched

  // You might want to provide a way to get the type from a string, if needed
  companion object {
    fun fromTitle(title: String): WellnessEventType {
      return when {
        title.contains("Yoga", ignoreCase = true) -> YOGA
        title.contains("Lecture", ignoreCase = true) || title.contains("Talk", ignoreCase = true) ->
            LECTURE
        title.contains("Sports", ignoreCase = true) ||
            title.contains("Fitness", ignoreCase = true) -> SPORTS
        title.contains("Social", ignoreCase = true) || title.contains("Party", ignoreCase = true) ->
            SOCIAL
        title.contains("Music", ignoreCase = true) ||
            title.contains("Concert", ignoreCase = true) -> MUSIC
        else -> DEFAULT
      }
    }
  }
}
