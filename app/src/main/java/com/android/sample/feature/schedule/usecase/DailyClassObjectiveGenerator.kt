package com.android.sample.feature.schedule.usecase

import com.android.sample.feature.schedule.data.planner.Class
import com.android.sample.feature.schedule.data.planner.ClassType
import com.android.sample.feature.weeks.model.Objective
import com.android.sample.feature.weeks.model.ObjectiveType
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Generates daily learning objectives from today's planner classes. Pure domain logic – no Android,
 * no Firestore, no UI dependencies.
 */
private const val LECTURE_REVIEW_MINUTES = 45
private const val EXERCISE_SET_MINUTES = 60
private const val LAB_PREPARATION_MINUTES = 90
private const val PROJECT_WORK_MINUTES = 60

class DailyClassObjectiveGenerator {

  fun generate(
      todayClasses: List<Class>,
      currentWeek: Int,
      day: DayOfWeek = LocalDate.now().dayOfWeek
  ): List<Objective> {
    return todayClasses.mapNotNull { clazz ->
      when (clazz.type) {
        ClassType.LECTURE -> createLectureObjective(clazz, currentWeek, day)
        ClassType.EXERCISE -> createExerciseObjective(clazz, currentWeek, day)
        ClassType.LAB -> createLabObjective(clazz, currentWeek, day)
        ClassType.PROJECT -> createProjectObjective(clazz, currentWeek, day)
      }
    }
  }

  private fun createLectureObjective(clazz: Class, week: Int, day: DayOfWeek): Objective {
    return Objective(
        title = "Review ${clazz.courseName} lecture",
        course = clazz.courseName,
        estimateMinutes = LECTURE_REVIEW_MINUTES,
        completed = false,
        day = day,
        type = ObjectiveType.COURSE_OR_EXERCISES,
        isAuto = true,
        sourceId = "AUTO:${clazz.courseName}:${clazz.type}:$week")
  }

  private fun createExerciseObjective(clazz: Class, week: Int, day: DayOfWeek): Objective {
    return Objective(
        title = "Do ${clazz.courseName} exercise set $week",
        course = clazz.courseName,
        estimateMinutes = EXERCISE_SET_MINUTES,
        completed = false,
        day = day,
        type = ObjectiveType.COURSE_OR_EXERCISES,
        isAuto = true,
        sourceId = "AUTO:${clazz.courseName}:${clazz.type}:$week")
  }

  private fun createLabObjective(clazz: Class, week: Int, day: DayOfWeek): Objective {
    return Objective(
        title = "Prepare ${clazz.courseName} lab (week $week)",
        course = clazz.courseName,
        estimateMinutes = LAB_PREPARATION_MINUTES,
        completed = false,
        day = day,
        type = ObjectiveType.COURSE_OR_EXERCISES,
        isAuto = true,
        sourceId = "AUTO:${clazz.courseName}:${clazz.type}:$week")
  }

  private fun createProjectObjective(clazz: Class, week: Int, day: DayOfWeek): Objective {
    return Objective(
        title = "Work on ${clazz.courseName} project",
        course = clazz.courseName,
        estimateMinutes = PROJECT_WORK_MINUTES,
        completed = false,
        day = day,
        type = ObjectiveType.COURSE_OR_EXERCISES,
        isAuto = true,
        sourceId = "AUTO:${clazz.courseName}:${clazz.type}:$week")
  }
}
