package com.android.sample.model.schedule

import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

/** Unified event model that combines both calendar events and planner classes */
data class ScheduleEvent(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val date: LocalDate,
    val time: LocalTime? = null,
    val durationMinutes: Int? = null,
    val kind: EventKind,
    val description: String? = null,
    val isCompleted: Boolean = false,
    val priority: Priority? = null,
    val courseCode: String? = null,
    val location: String? = null,
    val sourceTag: SourceTag = SourceTag.Task
) {

  // Helper properties
  val isClass: Boolean
    get() = sourceTag == SourceTag.Class

  val isTask: Boolean
    get() = sourceTag == SourceTag.Task

  val hasTime: Boolean
    get() = time != null

  val isAllDay: Boolean
    get() = time == null

  // Duration display helper
  val durationDisplay: String
    get() =
        durationMinutes?.let { minutes ->
          when {
            minutes < 60 -> "${minutes}m"
            minutes % 60 == 0 -> "${minutes / 60}h"
            else -> "${minutes / 60}h ${minutes % 60}m"
          }
        } ?: ""
}

enum class EventKind {
  // Class types
  CLASS_LECTURE,
  CLASS_EXERCISE,
  CLASS_LAB,

  // Study types
  STUDY,
  PROJECT,

  // Exam types
  EXAM_MIDTERM,
  EXAM_FINAL,

  // Submission types
  SUBMISSION_PROJECT,
  SUBMISSION_MILESTONE,
  SUBMISSION_WEEKLY,

  // Activity types
  ACTIVITY_SPORT,
  ACTIVITY_ASSOCIATION
}

enum class Priority {
  LOW,
  MEDIUM,
  HIGH
}

enum class SourceTag {
  Task, // From task/study system
  Class // From class/planner system
}

enum class ScheduleTab {
  DAY,
  WEEK,
  MONTH,
  AGENDA
}
