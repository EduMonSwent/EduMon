package com.android.sample.feature.weeks.model

import java.time.DayOfWeek

// Describes what kind of flow should be started when the user taps "Start".
enum class ObjectiveType {
  QUIZ,
  COURSE_OR_EXERCISES,
  RESUME,
}

// Default PDF URLs for demo/testing purposes
private const val DEFAULT_COURSE_PDF_URL =
    "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf"
private const val DEFAULT_EXERCISE_PDF_URL = "https://www.africau.edu/images/default/sample.pdf"

data class Objective(
    val title: String,
    val course: String,
    val estimateMinutes: Int = 0,
    val completed: Boolean = false,
    val day: DayOfWeek,
    val type: ObjectiveType = ObjectiveType.COURSE_OR_EXERCISES,
    val coursePdfUrl: String = DEFAULT_COURSE_PDF_URL,
    val exercisePdfUrl: String = DEFAULT_EXERCISE_PDF_URL
)
