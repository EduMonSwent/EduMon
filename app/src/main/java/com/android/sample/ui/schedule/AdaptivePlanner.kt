package com.android.sample.ui.schedule

import com.android.sample.model.schedule.EventKind
import com.android.sample.model.schedule.Priority
import com.android.sample.model.schedule.ScheduleEvent
import com.android.sample.model.schedule.SourceTag
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

object AdaptivePlanner {

  data class AdjustmentResult(
      val movedMissed: List<ScheduleEvent> = emptyList(),
      val pulledEarlier: List<ScheduleEvent> = emptyList()
  )

  fun weekStart(d: LocalDate): LocalDate = d.with(DayOfWeek.MONDAY)

  fun weekEnd(d: LocalDate): LocalDate = weekStart(d).plusDays(6)

  fun planAdjustments(
      today: LocalDate,
      currentWeekEvents: List<ScheduleEvent>,
      nextWeekEvents: List<ScheduleEvent>
  ): AdjustmentResult {

    val start = weekStart(today)
    val nextStart = start.plusWeeks(1)

    // Consider only Task-origin events for adjustments
    val currentTasks = currentWeekEvents.filter { it.sourceTag == SourceTag.Task }
    val nextTasks = nextWeekEvents.filter { it.sourceTag == SourceTag.Task }

    // 1) Missed tasks => move to next week start
    val missed = currentTasks.filter { !it.isCompleted && it.date.isBefore(today) }
    val completedEarly = currentTasks.any { it.isCompleted && it.date.isAfter(today) }

    val pulled =
        if (completedEarly && nextTasks.isNotEmpty()) {
          val preferredKinds =
              setOf(
                  EventKind.STUDY,
                  EventKind.PROJECT,
                  EventKind.SUBMISSION_PROJECT,
                  EventKind.SUBMISSION_MILESTONE,
                  EventKind.SUBMISSION_WEEKLY,
                  EventKind.EXAM_MIDTERM,
                  EventKind.EXAM_FINAL)

          nextTasks
              .filter { !it.isCompleted }
              .sortedWith(
                  compareBy(
                      { it.kind !in preferredKinds }, // Preferred kinds first
                      { it.priority ?: Priority.MEDIUM }, // Higher priority first
                      { it.date }, // Sooner dates first
                      { it.time ?: LocalTime.MIN }))
              .firstOrNull()
              ?.let { listOf(it) } ?: emptyList()
        } else emptyList()

    return AdjustmentResult(movedMissed = missed, pulledEarlier = pulled)
  }
}
