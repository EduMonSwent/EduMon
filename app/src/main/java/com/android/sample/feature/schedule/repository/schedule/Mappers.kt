package com.android.sample.feature.schedule.repository.schedule

import android.content.res.Resources
import com.android.sample.R
import com.android.sample.feature.schedule.data.calendar.Priority as ModelPriority
import com.android.sample.feature.schedule.data.calendar.StudyItem
import com.android.sample.feature.schedule.data.calendar.TaskType
import com.android.sample.feature.schedule.data.planner.Class as PlannerClass
import com.android.sample.feature.schedule.data.planner.ClassType
import com.android.sample.feature.schedule.data.schedule.EventKind
import com.android.sample.feature.schedule.data.schedule.Priority
import com.android.sample.feature.schedule.data.schedule.ScheduleEvent
import com.android.sample.feature.schedule.data.schedule.SourceTag
import java.time.Duration
import java.time.LocalDate
import java.util.Locale

/** This class was implemented with the help of ai (ChatGPT) */
object StudyItemMapper {

  fun toScheduleEvent(item: StudyItem, res: Resources): ScheduleEvent {
    val (kind, priority) = mapStudyItemToEventKind(item, res)
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

  fun fromScheduleEvent(event: ScheduleEvent, res: Resources): StudyItem {
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

  private fun mapStudyItemToEventKind(item: StudyItem, res: Resources): Pair<EventKind, Priority?> {
    return when (item.type) {
      TaskType.STUDY -> EventKind.STUDY to item.priority.toSchedulePriority()
      TaskType.WORK -> guessWorkKind(item, res) to item.priority.toSchedulePriority()
      TaskType.PERSONAL -> guessActivityKind(item, res) to null
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

  private fun ModelPriority.toSchedulePriority(): Priority =
      when (this) {
        ModelPriority.LOW -> Priority.LOW
        ModelPriority.MEDIUM -> Priority.MEDIUM
        ModelPriority.HIGH -> Priority.HIGH
      }

  private fun Priority.toModelPriority(): ModelPriority =
      when (this) {
        Priority.LOW -> ModelPriority.LOW
        Priority.MEDIUM -> ModelPriority.MEDIUM
        Priority.HIGH -> ModelPriority.HIGH
      }

  private fun guessWorkKind(item: StudyItem, res: Resources): EventKind {
    val locale = Locale.getDefault()
    val text = "${item.title} ${item.description ?: ""}".lowercase(locale)

    // Load localized keywords
    val midterm = res.getString(R.string.keyword_midterm).lowercase(locale)
    val finalExam = res.getString(R.string.keyword_final_exam).lowercase(locale)
    val finalKW = res.getString(R.string.keyword_final).lowercase(locale)
    val examKW = res.getString(R.string.keyword_exam).lowercase(locale)
    val milestone = res.getString(R.string.keyword_milestone).lowercase(locale)
    val weekly = res.getString(R.string.keyword_weekly).lowercase(locale)
    val rendu = res.getString(R.string.keyword_rendu).lowercase(locale)
    val submit = res.getString(R.string.keyword_submit).lowercase(locale)
    val submission = res.getString(R.string.keyword_submission).lowercase(locale)
    val deadline = res.getString(R.string.keyword_deadline).lowercase(locale)
    val due = res.getString(R.string.keyword_due).lowercase(locale)
    val deliverable = res.getString(R.string.keyword_deliverable).lowercase(locale)
    val handIn = res.getString(R.string.keyword_hand_in).lowercase(locale)
    val handin = res.getString(R.string.keyword_handin).lowercase(locale)
    val project = res.getString(R.string.keyword_project).lowercase(locale)

    return when {
      midterm in text -> EventKind.EXAM_MIDTERM
      finalExam in text || (examKW in text && finalKW in text) -> EventKind.EXAM_FINAL
      milestone in text -> EventKind.SUBMISSION_MILESTONE
      weekly in text || rendu in text -> EventKind.SUBMISSION_WEEKLY
      submit in text ||
          submission in text ||
          deadline in text ||
          due in text ||
          deliverable in text ||
          handIn in text ||
          handin in text -> EventKind.SUBMISSION_PROJECT
      project in text -> EventKind.PROJECT
      else -> EventKind.STUDY
    }
  }

  private fun guessActivityKind(item: StudyItem, res: Resources): EventKind {
    val locale = Locale.getDefault()
    val text = "${item.title} ${item.description ?: ""}".lowercase(locale)

    val sport = res.getString(R.string.keyword_sport).lowercase(locale)
    val football = res.getString(R.string.keyword_football).lowercase(locale)
    val gym = res.getString(R.string.keyword_gym).lowercase(locale)
    val assoc = res.getString(R.string.keyword_assoc).lowercase(locale)
    val assoc2 = res.getString(R.string.keyword_association).lowercase(locale)
    val club = res.getString(R.string.keyword_club).lowercase(locale)

    return when {
      sport in text || football in text || gym in text -> EventKind.ACTIVITY_SPORT
      assoc in text || assoc2 in text || club in text -> EventKind.ACTIVITY_ASSOCIATION
      else -> EventKind.ACTIVITY_ASSOCIATION
    }
  }
}

object ClassMapper {

  fun toScheduleEvent(plannerClass: PlannerClass, res: Resources): ScheduleEvent {
    val kind =
        when (plannerClass.type) {
          ClassType.LECTURE -> EventKind.CLASS_LECTURE
          ClassType.EXERCISE -> EventKind.CLASS_EXERCISE
          ClassType.LAB -> EventKind.CLASS_LAB
          ClassType.PROJECT -> EventKind.PROJECT
        }

    val durationMinutes =
        runCatching {
              Duration.between(plannerClass.startTime, plannerClass.endTime).toMinutes().toInt()
            }
            .getOrNull()

    // Localized “at/with” (or use format string)
    val withWord = res.getString(R.string.with) // e.g., "With"
    val atWord = res.getString(R.string.keyword_at) // e.g., "at"

    val description =
        res.getString(
            R.string.class_description_fmt,
            plannerClass.type,
            atWord,
            plannerClass.location,
            withWord,
            plannerClass.instructor)

    return ScheduleEvent(
        id = plannerClass.id,
        title = plannerClass.courseName,
        date = LocalDate.now(), // current impl: classes are today
        time = plannerClass.startTime,
        durationMinutes = durationMinutes,
        kind = kind,
        description = description,
        isCompleted = false,
        priority = null,
        courseCode = null,
        location = plannerClass.location,
        sourceTag = SourceTag.Class)
  }

  fun fromScheduleEvent(event: ScheduleEvent, res: Resources): PlannerClass? {
    if (event.sourceTag != SourceTag.Class) return null

    val classType =
        when (event.kind) {
          EventKind.CLASS_LECTURE -> ClassType.LECTURE
          EventKind.CLASS_EXERCISE -> ClassType.EXERCISE
          EventKind.CLASS_LAB -> ClassType.LAB
          else -> return null
        }

    val time = event.time ?: return null
    val endTime = time.plusMinutes((event.durationMinutes?.toLong() ?: 60L))

    return PlannerClass(
        id = event.id,
        courseName = event.title,
        startTime = time,
        endTime = endTime,
        type = classType,
        location = event.location ?: "",
        instructor = extractInstructorFromDescription(event.description, res))
  }

  private fun extractInstructorFromDescription(description: String?, res: Resources): String {
    // Use localized "with" token (case-insensitive), then trim
    val withToken = res.getString(R.string.with).trim() // e.g., "With"
    val idx =
        description
            ?.lowercase(Locale.getDefault())
            ?.indexOf(withToken.lowercase(Locale.getDefault()) + " ") ?: -1

    return if (idx >= 0) {
      description!!.substring(idx + withToken.length + 1).trim().ifBlank {
        res.getString(R.string.keyword_professor_default)
      }
    } else {
      res.getString(R.string.keyword_professor_default)
    }
  }
}
