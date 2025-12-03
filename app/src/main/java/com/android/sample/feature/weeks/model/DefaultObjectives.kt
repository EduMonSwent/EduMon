// filepath:
// c:\Users\Khalil\EduMon\app\src\main\java\com\android\sample\feature\weeks\model\DefaultObjectives.kt
package com.android.sample.feature.weeks.model

import java.time.DayOfWeek

/**
 * Provides default objectives for new users. These are seeded when a user's objectives list is
 * empty.
 */
object DefaultObjectives {

  // Course constants to avoid duplication
  private const val COURSE_CS_200 = "CS-200"
  private const val COURSE_CS_201 = "CS-201"
  private const val COURSE_COM_100 = "COM-100"
  private const val COURSE_SHS_150 = "SHS-150"
  private const val COURSE_CS_100 = "CS-100"

  fun get(): List<Objective> =
      listOf(
          // Week 1 - Monday
          Objective(
              title = "Complete Quiz 1",
              course = COURSE_CS_200,
              estimateMinutes = 30,
              completed = false,
              day = DayOfWeek.MONDAY,
              type = ObjectiveType.QUIZ,
          ),
          Objective(
              title = "Read Chapter 1",
              course = COURSE_CS_200,
              estimateMinutes = 45,
              completed = false,
              day = DayOfWeek.MONDAY,
              type = ObjectiveType.COURSE_OR_EXERCISES,
          ),

          // Week 1 - Tuesday
          Objective(
              title = "Solve Exercise Set 1",
              course = COURSE_CS_201,
              estimateMinutes = 60,
              completed = false,
              day = DayOfWeek.TUESDAY,
              type = ObjectiveType.COURSE_OR_EXERCISES,
          ),
          Objective(
              title = "Review Lecture Notes",
              course = COURSE_CS_201,
              estimateMinutes = 30,
              completed = false,
              day = DayOfWeek.TUESDAY,
              type = ObjectiveType.RESUME,
          ),

          // Week 1 - Wednesday
          Objective(
              title = "Lab Assignment 1",
              course = COURSE_COM_100,
              estimateMinutes = 90,
              completed = false,
              day = DayOfWeek.WEDNESDAY,
              type = ObjectiveType.COURSE_OR_EXERCISES,
          ),
          Objective(
              title = "Quiz 2",
              course = COURSE_COM_100,
              estimateMinutes = 20,
              completed = false,
              day = DayOfWeek.WEDNESDAY,
              type = ObjectiveType.QUIZ,
          ),

          // Week 1 - Thursday
          Objective(
              title = "Reading Assignment",
              course = COURSE_SHS_150,
              estimateMinutes = 45,
              completed = false,
              day = DayOfWeek.THURSDAY,
              type = ObjectiveType.COURSE_OR_EXERCISES,
          ),
          Objective(
              title = "Essay Outline",
              course = COURSE_SHS_150,
              estimateMinutes = 30,
              completed = false,
              day = DayOfWeek.THURSDAY,
              type = ObjectiveType.RESUME,
          ),
          Objective(
              title = "Discussion Forum Post",
              course = COURSE_SHS_150,
              estimateMinutes = 15,
              completed = false,
              day = DayOfWeek.THURSDAY,
              type = ObjectiveType.COURSE_OR_EXERCISES,
          ),

          // Week 1 - Friday
          Objective(
              title = "Problem Set 2",
              course = COURSE_CS_100,
              estimateMinutes = 75,
              completed = false,
              day = DayOfWeek.FRIDAY,
              type = ObjectiveType.COURSE_OR_EXERCISES,
          ),
          Objective(
              title = "Weekly Quiz",
              course = COURSE_CS_100,
              estimateMinutes = 25,
              completed = false,
              day = DayOfWeek.FRIDAY,
              type = ObjectiveType.QUIZ,
          ),

          // Weekend
          Objective(
              title = "Review Week 1 Material",
              course = COURSE_CS_200,
              estimateMinutes = 60,
              completed = false,
              day = DayOfWeek.SATURDAY,
              type = ObjectiveType.RESUME,
          ),
          Objective(
              title = "Prepare for Midterm",
              course = COURSE_COM_100,
              estimateMinutes = 90,
              completed = false,
              day = DayOfWeek.SUNDAY,
              type = ObjectiveType.RESUME,
          ),
      )
}
