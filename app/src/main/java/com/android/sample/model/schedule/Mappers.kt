package com.android.sample.model.schedule

import com.android.sample.model.Priority as ModelPriority
import com.android.sample.model.StudyItem
import com.android.sample.model.TaskType
import com.android.sample.model.planner.Class as PlannerClass
import com.android.sample.model.planner.ClassType
import java.time.Duration
import java.time.LocalDate
import java.util.Locale

object StudyItemMapper {
  fun toScheduleEvent(item: StudyItem): ScheduleEvent {
    val (kind, priority) = mapStudyItemToEventKind(item)
    return ScheduleEvent(
        id = item.id,
        title = item.title,
        description = item.description,
        date = item.date,
        time = item.time,
        durationMinutes = item.durationMinutes,
        kind = kind,
        isCompleted = item.isCompleted,
        priority = priority,
        location = null,
        courseCode = null,
        sourceTag = SourceTag.Task)
  }

  fun fromScheduleEvent(event: ScheduleEvent): StudyItem {
    val (taskType, priority) = mapEventKindToStudyItem(event.kind, event.priority)
    return StudyItem(
        id = event.id,
        title = event.title,
        description = event.description,
        date = event.date,
        time = event.time,
        durationMinutes = event.durationMinutes,
        isCompleted = event.isCompleted,
        priority = priority,
        type = taskType)
  }

  private fun mapStudyItemToEventKind(item: StudyItem): Pair<EventKind, Priority?> {
    return when (item.type) {
      TaskType.STUDY -> EventKind.STUDY to item.priority.toSchedulePriority()
      TaskType.WORK -> guessWorkKind(item) to item.priority.toSchedulePriority()
      TaskType.PERSONAL -> guessActivityKind(item) to null
    }
  }

  private fun mapEventKindToStudyItem(
      kind: EventKind,
      priority: Priority?
  ): Pair<TaskType, ModelPriority> {
    return when (kind) {
      EventKind.STUDY,
      EventKind.PROJECT,
      EventKind.EXAM_MIDTERM,
      EventKind.EXAM_FINAL,
      EventKind.SUBMISSION_PROJECT,
      EventKind.SUBMISSION_MILESTONE,
      EventKind.SUBMISSION_WEEKLY ->
          TaskType.WORK to (priority ?: Priority.MEDIUM).toModelPriority()
      EventKind.CLASS_LECTURE,
      EventKind.CLASS_EXERCISE,
      EventKind.CLASS_LAB -> TaskType.WORK to (priority ?: Priority.MEDIUM).toModelPriority()
      EventKind.ACTIVITY_SPORT,
      EventKind.ACTIVITY_ASSOCIATION -> TaskType.PERSONAL to ModelPriority.MEDIUM
    }
  }

  private fun ModelPriority.toSchedulePriority(): Priority {
    return when (this) {
      ModelPriority.LOW -> Priority.LOW
      ModelPriority.MEDIUM -> Priority.MEDIUM
      ModelPriority.HIGH -> Priority.HIGH
    }
  }

  private fun Priority.toModelPriority(): ModelPriority {
    return when (this) {
      Priority.LOW -> ModelPriority.LOW
      Priority.MEDIUM -> ModelPriority.MEDIUM
      Priority.HIGH -> ModelPriority.HIGH
    }
  }

  private fun guessWorkKind(item: StudyItem): EventKind {
    val text = "${item.title} ${item.description ?: ""}".lowercase(Locale.getDefault())
    return when {
      "midterm" in text -> EventKind.EXAM_MIDTERM
      "final exam" in text || ("exam" in text && "final" in text) -> EventKind.EXAM_FINAL
      "milestone" in text -> EventKind.SUBMISSION_MILESTONE
      "weekly" in text || "rendu" in text -> EventKind.SUBMISSION_WEEKLY
      "submit" in text ||
          "submission" in text ||
          "deadline" in text ||
          "due" in text ||
          "deliverable" in text ||
          "hand-in" in text ||
          "handin" in text -> EventKind.SUBMISSION_PROJECT
      "project" in text -> EventKind.PROJECT
      else -> EventKind.STUDY
    }
  }

  private fun guessActivityKind(item: StudyItem): EventKind {
    val text = "${item.title} ${item.description ?: ""}".lowercase(Locale.getDefault())
    return when {
      "sport" in text || "football" in text || "gym" in text -> EventKind.ACTIVITY_SPORT
      "assoc" in text || "association" in text || "club" in text -> EventKind.ACTIVITY_ASSOCIATION
      else -> EventKind.ACTIVITY_ASSOCIATION
    }
  }
}

object ClassMapper {
  fun toScheduleEvent(plannerClass: PlannerClass): ScheduleEvent {
    val kind =
        when (plannerClass.type) {
          ClassType.LECTURE -> EventKind.CLASS_LECTURE
          ClassType.EXERCISE -> EventKind.CLASS_EXERCISE
          ClassType.LAB -> EventKind.CLASS_LAB
        }

    val durationMinutes =
        runCatching {
              Duration.between(plannerClass.startTime, plannerClass.endTime).toMinutes().toInt()
            }
            .getOrNull()

    return ScheduleEvent(
        id = plannerClass.id,
        title = plannerClass.courseName,
        date = LocalDate.now(), // Classes are for today in current implementation
        time = plannerClass.startTime,
        durationMinutes = durationMinutes,
        kind = kind,
        description =
            "${plannerClass.type} at ${plannerClass.location} with ${plannerClass.instructor}",
        isCompleted = false,
        priority = null,
        courseCode = null,
        location = plannerClass.location,
        sourceTag = SourceTag.Class)
  }

  fun fromScheduleEvent(event: ScheduleEvent): PlannerClass? {
    if (event.sourceTag != SourceTag.Class) return null

    val classType =
        when (event.kind) {
          EventKind.CLASS_LECTURE -> ClassType.LECTURE
          EventKind.CLASS_EXERCISE -> ClassType.EXERCISE
          EventKind.CLASS_LAB -> ClassType.LAB
          else -> return null
        }
    val time = event.time ?: return null
    val endTime = time.plusMinutes((event.durationMinutes?.toLong() ?: 60))

    return PlannerClass(
        id = event.id,
        courseName = event.title,
        startTime = event.time,
        endTime = endTime,
        type = classType,
        location = event.location ?: "",
        instructor = extractInstructorFromDescription(event.description))
  }

  private fun extractInstructorFromDescription(description: String?): String {
    return description?.substringAfter("with ")?.takeIf { it.isNotBlank() } ?: "Professor"
  }
}
