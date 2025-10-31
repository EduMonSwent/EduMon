package com.android.sample.model.schedule

import com.android.sample.model.Priority as TaskPriority
import com.android.sample.model.StudyItem
import com.android.sample.model.TaskType
import com.android.sample.model.planner.Class as PlannerClass
import com.android.sample.model.planner.ClassType
import java.time.Duration
import java.time.LocalDate
import java.util.Locale

object StudyItemMapper {
  fun toScheduleEvent(item: StudyItem): ScheduleEvent {
    val (kind, prio) =
        when (item.type) {
          TaskType.STUDY -> EventKind.STUDY to item.priority.toSchedulePriority()
          TaskType.WORK -> guessWorkKind(item) to item.priority.toSchedulePriority()
          TaskType.PERSONAL -> guessActivityKind(item) to null
        }
    return ScheduleEvent(
        id = item.id,
        title = item.title,
        description = item.description,
        date = item.date,
        time = item.time,
        durationMinutes = item.durationMinutes,
        kind = kind,
        isCompleted = item.isCompleted,
        priority = prio,
        location = null,
        courseCode = null,
        sourceTag = SourceTag.Task)
  }

  fun fromScheduleEvent(ev: ScheduleEvent): StudyItem {
    val (taskType, taskPriority) =
        when (ev.kind) {
          EventKind.STUDY,
          EventKind.PROJECT,
          EventKind.EXAM_MIDTERM,
          EventKind.EXAM_FINAL,
          EventKind.SUBMISSION_PROJECT,
          EventKind.SUBMISSION_MILESTONE,
          EventKind.SUBMISSION_WEEKLY ->
              TaskType.WORK to (ev.priority ?: Priority.MEDIUM).toTaskPriority()
          EventKind.ACTIVITY_SPORT,
          EventKind.ACTIVITY_ASSOCIATION -> TaskType.PERSONAL to TaskPriority.MEDIUM
          else -> TaskType.WORK to (ev.priority ?: Priority.MEDIUM).toTaskPriority()
        }
    return StudyItem(
        id = ev.id,
        title = ev.title,
        description = ev.description,
        date = ev.date,
        time = ev.time,
        durationMinutes = ev.durationMinutes,
        isCompleted = ev.isCompleted,
        priority = taskPriority,
        type = taskType)
  }

  private fun TaskPriority.toSchedulePriority() =
      when (this) {
        TaskPriority.LOW -> Priority.LOW
        TaskPriority.MEDIUM -> Priority.MEDIUM
        TaskPriority.HIGH -> Priority.HIGH
      }

  private fun Priority.toTaskPriority() =
      when (this) {
        Priority.LOW -> TaskPriority.LOW
        Priority.MEDIUM -> TaskPriority.MEDIUM
        Priority.HIGH -> TaskPriority.HIGH
      }

  // Lightweight heuristics so you get useful kinds immediately.
  private fun guessWorkKind(item: StudyItem): EventKind {
    val t = "${item.title} ${item.description ?: ""}".lowercase(Locale.getDefault())
    return when {
      "midterm" in t -> EventKind.EXAM_MIDTERM
      "final" in t -> EventKind.EXAM_FINAL
      "milestone" in t -> EventKind.SUBMISSION_MILESTONE
      "weekly" in t || "rendu" in t -> EventKind.SUBMISSION_WEEKLY
      "submit" in t || "deadline" in t || "due" in t -> EventKind.SUBMISSION_PROJECT
      "project" in t -> EventKind.PROJECT
      else -> EventKind.STUDY
    }
  }

  private fun guessActivityKind(item: StudyItem): EventKind {
    val t = "${item.title} ${item.description ?: ""}".lowercase(Locale.getDefault())
    return when {
      "sport" in t || "football" in t || "gym" in t -> EventKind.ACTIVITY_SPORT
      "assoc" in t || "association" in t || "club" in t -> EventKind.ACTIVITY_ASSOCIATION
      else -> EventKind.ACTIVITY_ASSOCIATION
    }
  }
}

object ClassMapper {
  fun toScheduleEvent(c: PlannerClass): ScheduleEvent {
    val kind =
        when (c.type) {
          ClassType.LECTURE -> EventKind.CLASS_LECTURE
          ClassType.EXERCISE -> EventKind.CLASS_EXERCISE
          ClassType.LAB -> EventKind.CLASS_LAB
        }
    val durationMinutes =
        runCatching { Duration.between(c.startTime, c.endTime).toMinutes().toInt() }.getOrNull()
    return ScheduleEvent(
        id = c.id,
        title = c.courseName,
        date = LocalDate.now(),
        time = c.startTime,
        durationMinutes = durationMinutes,
        kind = kind,
        description = null,
        isCompleted = false, // tracked via attendance modal separately
        priority = null,
        courseCode = null,
        location = c.location,
        sourceTag = SourceTag.Class)
  }

  // If/when you add class editing/saving, implement reverse mapping here.
  // fun fromScheduleEvent(ev: ScheduleEvent): Class = ...
}
