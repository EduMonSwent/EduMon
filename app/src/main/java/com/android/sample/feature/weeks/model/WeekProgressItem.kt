package com.android.sample.feature.weeks.model

// WeekContent is defined in WeekContent.kt (exercises: List<Exercise>, courses:
// List<CourseMaterial>)

data class WeekProgressItem(
    val label: String,
    val percent: Int = 0, // 0..100
    val content: WeekContent = WeekContent(emptyList(), emptyList()),
) {
  fun isEmpty(): Boolean = content.exercises.isEmpty() && content.courses.isEmpty()
}
