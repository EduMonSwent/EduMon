package com.android.sample.feature.weeks.model

import java.time.DayOfWeek

// Describes what kind of flow should be started when the user taps "Start".
enum class ObjectiveType {
  QUIZ,
  COURSE_OR_EXERCISES,
  RESUME,
}

data class Objective(
    val title: String,
    val course: String,
    val estimateMinutes: Int = 0,
    val completed: Boolean = false,
    val day: DayOfWeek,
    // Defaults to COURSE_OR_EXERCISES so existing callers keep working
    val type: ObjectiveType = ObjectiveType.COURSE_OR_EXERCISES,
    // PDFs for COURSE_OR_EXERCISES type
    val coursePdfUrl: String =
        "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf", // URL to the
                                                                                   // course
                                                                                   // material PDF
    val exercisePdfUrl: String =
        "https://www.africau.edu/images/default/sample.pdf" // URL to the exercise PDF
)
