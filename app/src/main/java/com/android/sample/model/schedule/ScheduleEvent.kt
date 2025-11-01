package com.android.sample.model.schedule

import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

enum class EventKind {
  CLASS_LECTURE,
  CLASS_EXERCISE,
  CLASS_LAB,
  STUDY,
  PROJECT,
  EXAM_MIDTERM,
  EXAM_FINAL,
  SUBMISSION_PROJECT,
  SUBMISSION_MILESTONE,
  SUBMISSION_WEEKLY,
  ACTIVITY_SPORT,
  ACTIVITY_ASSOCIATION
}

enum class Priority {
  LOW,
  MEDIUM,
  HIGH
}

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
)

enum class SourceTag {
  Task,
  Class
}

enum class ScheduleTab {
  DAY,
  WEEK,
  MONTH,
  AGENDA
}
