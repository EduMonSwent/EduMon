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
class DailyClassObjectiveGenerator {

  fun generate(
      todayClasses: List<Class>,
      currentWeek: Int,
      day: DayOfWeek = LocalDate.now().dayOfWeek
  ): List<Objective> {
    return todayClasses.mapNotNull { clazz ->
      when (clazz.type) {
        ClassType.LECTURE -> createLectureObjective(clazz, day)
        ClassType.EXERCISE -> createExerciseObjective(clazz, currentWeek, day)
        ClassType.LAB -> createLabObjective(clazz, currentWeek, day)
        ClassType.PROJECT -> createProjectObjective(clazz, day)
      }
    }
  }

  private fun createLectureObjective(clazz: Class, day: DayOfWeek): Objective {
    return Objective(
        title = "Review ${clazz.courseName} lecture",
        course = clazz.courseName,
        estimateMinutes = 45,
        completed = false,
        day = day,
        type = ObjectiveType.COURSE_OR_EXERCISES)
  }

  private fun createExerciseObjective(clazz: Class, week: Int, day: DayOfWeek): Objective {
    return Objective(
        title = "Do ${clazz.courseName} exercise set $week",
        course = clazz.courseName,
        estimateMinutes = 60,
        completed = false,
        day = day,
        type = ObjectiveType.COURSE_OR_EXERCISES)
  }

  private fun createLabObjective(clazz: Class, week: Int, day: DayOfWeek): Objective {
    return Objective(
        title = "Prepare ${clazz.courseName} lab (week $week)",
        course = clazz.courseName,
        estimateMinutes = 90,
        completed = false,
        day = day,
        type = ObjectiveType.COURSE_OR_EXERCISES)
  }

  private fun createProjectObjective(clazz: Class, day: DayOfWeek): Objective {
    return Objective(
        title = "Work on ${clazz.courseName} project",
        course = clazz.courseName,
        estimateMinutes = 60,
        completed = false,
        day = day,
        type = ObjectiveType.COURSE_OR_EXERCISES)
  }
}
