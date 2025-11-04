package com.android.sample.feature.weeks.repository

import com.android.sample.feature.weeks.data.DefaultWeeksProvider
import com.android.sample.feature.weeks.model.DayStatus
import com.android.sample.feature.weeks.model.WeekContent
import com.android.sample.feature.weeks.model.WeekProgressItem

interface WeeksRepository {
  suspend fun getWeeks(): List<WeekProgressItem>

  suspend fun getDayStatuses(): List<DayStatus>
  // Update the percent for a specific week by index. Returns the updated list.
  suspend fun updateWeekPercent(index: Int, percent: Int): List<WeekProgressItem>

  // --- Content driven progress ---
  // Returns content (exercises + course materials) for a week index
  suspend fun getWeekContent(index: Int): WeekContent
  // Mark a single exercise done/undone and recompute percent for that week. Returns full weeks
  // list.
  suspend fun markExerciseDone(
      weekIndex: Int,
      exerciseId: String,
      done: Boolean
  ): List<WeekProgressItem>
  // Mark a single course material read/unread and recompute percent for that week. Returns full
  // weeks list.
  suspend fun markCourseRead(
      weekIndex: Int,
      courseId: String,
      read: Boolean
  ): List<WeekProgressItem>
}

class FakeWeeksRepository : WeeksRepository {
  // Use shared defaults to avoid duplication with FirestoreWeeksRepository
  val defaultWeeks: MutableList<WeekProgressItem> =
      DefaultWeeksProvider.provideDefaultWeeks().toMutableList()

  private val sampleStatuses: List<DayStatus> = DefaultWeeksProvider.provideDefaultDayStatuses()

  override suspend fun getWeeks(): List<WeekProgressItem> {
    // Ensure percents reflect current content
    recomputeAllPercents()
    return defaultWeeks.toList()
  }

  override suspend fun getDayStatuses(): List<DayStatus> {
    return sampleStatuses
  }

  override suspend fun updateWeekPercent(index: Int, percent: Int): List<WeekProgressItem> {
    if (index in defaultWeeks.indices) {
      val clamped = percent.coerceIn(0, 100)
      defaultWeeks[index] = defaultWeeks[index].copy(percent = clamped)
    }
    return defaultWeeks.toList()
  }

  override suspend fun getWeekContent(index: Int): WeekContent {
    return defaultWeeks.getOrNull(index)?.content ?: WeekContent(emptyList(), emptyList())
  }

  override suspend fun markExerciseDone(
      weekIndex: Int,
      exerciseId: String,
      done: Boolean
  ): List<WeekProgressItem> {
    if (weekIndex !in defaultWeeks.indices) return defaultWeeks.toList()
    val week = defaultWeeks[weekIndex]
    val current = week.content
    val updated =
        current.copy(
            exercises =
                current.exercises.map { if (it.id == exerciseId) it.copy(done = done) else it })
    defaultWeeks[weekIndex] = week.copy(content = updated)
    recomputePercentFor(weekIndex)
    return defaultWeeks.toList()
  }

  override suspend fun markCourseRead(
      weekIndex: Int,
      courseId: String,
      read: Boolean
  ): List<WeekProgressItem> {
    if (weekIndex !in defaultWeeks.indices) return defaultWeeks.toList()
    val week = defaultWeeks[weekIndex]
    val current = week.content
    val updated =
        current.copy(
            courses = current.courses.map { if (it.id == courseId) it.copy(read = read) else it })
    defaultWeeks[weekIndex] = week.copy(content = updated)
    recomputePercentFor(weekIndex)
    return defaultWeeks.toList()
  }

  // --- Helpers ---
  private fun recomputePercentFor(weekIndex: Int) {
    if (weekIndex !in defaultWeeks.indices) return
    val c = defaultWeeks[weekIndex].content
    val total = c.exercises.size + c.courses.size
    val done = c.exercises.count { it.done } + c.courses.count { it.read }
    val pct = if (total == 0) 0 else ((done * 100.0) / total).toInt().coerceIn(0, 100)
    defaultWeeks[weekIndex] = defaultWeeks[weekIndex].copy(percent = pct)
  }

  private fun recomputeAllPercents() {
    defaultWeeks.indices.forEach { recomputePercentFor(it) }
  }
}
