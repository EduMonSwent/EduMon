package com.android.sample.feature.weeks.repository

import com.android.sample.feature.weeks.model.CourseMaterial
import com.android.sample.feature.weeks.model.DayStatus
import com.android.sample.feature.weeks.model.Exercise
import com.android.sample.feature.weeks.model.WeekContent
import com.android.sample.feature.weeks.model.WeekProgressItem
import java.time.DayOfWeek

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
  private val sampleWeeks =
      mutableListOf(
          WeekProgressItem(
              label = "Week 1",
              percent = 100,
              content =
                  WeekContent(
                      courses =
                          listOf(
                              CourseMaterial(id = "c1", title = "Intro to Android", read = true),
                              CourseMaterial(id = "c2", title = "Compose Basics", read = true),
                          ),
                      exercises =
                          listOf(
                              Exercise(id = "e1", title = "Set up environment", done = true),
                              Exercise(id = "e2", title = "Finish codelab", done = true),
                          ))),
          WeekProgressItem(
              label = "Week 2",
              percent = 55,
              content =
                  WeekContent(
                      courses =
                          listOf(
                              CourseMaterial(id = "c3", title = "Compose Layouts", read = false),
                              CourseMaterial(
                                  id = "c4", title = "State and Side-effects", read = true),
                          ),
                      exercises =
                          listOf(
                              Exercise(id = "e3", title = "Build layout challenge", done = false),
                          ))),
          WeekProgressItem(
              label = "Week 3",
              percent = 10,
              content =
                  WeekContent(
                      courses =
                          listOf(
                              CourseMaterial(
                                  id = "c5", title = "Architecture guidance", read = false),
                          ),
                      exercises =
                          listOf(
                              Exercise(
                                  id = "e4", title = "Repository implementation", done = false),
                          ))),
      )

  private val sampleStatuses =
      DayOfWeek.values().mapIndexed { idx, d -> DayStatus(d, metTarget = idx % 2 == 0) }

  override suspend fun getWeeks(): List<WeekProgressItem> {
    // Ensure percents reflect current content
    recomputeAllPercents()
    return sampleWeeks.toList()
  }

  override suspend fun getDayStatuses(): List<DayStatus> {
    return sampleStatuses
  }

  override suspend fun updateWeekPercent(index: Int, percent: Int): List<WeekProgressItem> {
    if (index in sampleWeeks.indices) {
      val clamped = percent.coerceIn(0, 100)
      sampleWeeks[index] = sampleWeeks[index].copy(percent = clamped)
    }
    return sampleWeeks.toList()
  }

  override suspend fun getWeekContent(index: Int): WeekContent {
    return sampleWeeks.getOrNull(index)?.content ?: WeekContent(emptyList(), emptyList())
  }

  override suspend fun markExerciseDone(
      weekIndex: Int,
      exerciseId: String,
      done: Boolean
  ): List<WeekProgressItem> {
    if (weekIndex !in sampleWeeks.indices) return sampleWeeks.toList()
    val week = sampleWeeks[weekIndex]
    val current = week.content
    val updated =
        current.copy(
            exercises =
                current.exercises.map { if (it.id == exerciseId) it.copy(done = done) else it })
    sampleWeeks[weekIndex] = week.copy(content = updated)
    recomputePercentFor(weekIndex)
    return sampleWeeks.toList()
  }

  override suspend fun markCourseRead(
      weekIndex: Int,
      courseId: String,
      read: Boolean
  ): List<WeekProgressItem> {
    if (weekIndex !in sampleWeeks.indices) return sampleWeeks.toList()
    val week = sampleWeeks[weekIndex]
    val current = week.content
    val updated =
        current.copy(
            courses = current.courses.map { if (it.id == courseId) it.copy(read = read) else it })
    sampleWeeks[weekIndex] = week.copy(content = updated)
    recomputePercentFor(weekIndex)
    return sampleWeeks.toList()
  }

  // --- Helpers ---
  private fun recomputePercentFor(weekIndex: Int) {
    if (weekIndex !in sampleWeeks.indices) return
    val c = sampleWeeks[weekIndex].content
    val total = c.exercises.size + c.courses.size
    val done = c.exercises.count { it.done } + c.courses.count { it.read }
    val pct = if (total == 0) 0 else ((done * 100.0) / total).toInt().coerceIn(0, 100)
    sampleWeeks[weekIndex] = sampleWeeks[weekIndex].copy(percent = pct)
  }

  private fun recomputeAllPercents() {
    sampleWeeks.indices.forEach { recomputePercentFor(it) }
  }
}
