package com.android.sample.schedule

import com.android.sample.model.schedule.EventKind
import com.android.sample.model.schedule.Priority
import com.android.sample.model.schedule.ScheduleEvent
import com.android.sample.model.schedule.SourceTag
import com.android.sample.ui.schedule.AdaptivePlanner
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptivePlannerTest {

  private fun ev(
      title: String,
      date: LocalDate,
      kind: EventKind = EventKind.STUDY,
      completed: Boolean = false,
      source: SourceTag = SourceTag.Task,
      time: LocalTime? = null
  ) =
      ScheduleEvent(
          title = title,
          date = date,
          time = time,
          durationMinutes = null,
          kind = kind,
          description = null,
          isCompleted = completed,
          priority = Priority.MEDIUM,
          courseCode = null,
          location = null,
          sourceTag = source)

  @Test
  fun weekStartEnd_mondayAligned() {
    val d = LocalDate.of(2025, 3, 6) // Thu
    assertEquals(LocalDate.of(2025, 3, 3), AdaptivePlanner.weekStart(d))
    assertEquals(LocalDate.of(2025, 3, 9), AdaptivePlanner.weekEnd(d))
  }

  @Test
  fun planAdjustments_movesOnlyMissedTasks_fromTaskSource() {
    val today = LocalDate.of(2025, 3, 6)
    val start = AdaptivePlanner.weekStart(today) // 2025-03-03

    val missedTask = ev("missed", start.minusDays(1), completed = false, source = SourceTag.Task)
    val completedPast =
        ev("old completed", start.minusDays(2), completed = true, source = SourceTag.Task)
    val futureClass =
        ev("class", start.plusDays(1), kind = EventKind.CLASS_LECTURE, source = SourceTag.Class)

    val res =
        AdaptivePlanner.planAdjustments(
            today,
            currentWeekEvents = listOf(missedTask, completedPast, futureClass),
            nextWeekEvents = emptyList())

    assertEquals(listOf(missedTask), res.movedMissed)
    assertTrue(res.pulledEarlier.isEmpty())
  }

  @Test
  fun planAdjustments_pullsPreferredKindWhenCompletedEarly() {
    val today = LocalDate.of(2025, 3, 6) // Thu
    val start = AdaptivePlanner.weekStart(today)

    // MUST be strictly after 'today' to trigger completedEarly
    val completedEarly = ev("today done but future date", today.plusDays(1), completed = true)

    val nextNonPreferred = ev("misc", start.plusWeeks(1).plusDays(1), kind = EventKind.CLASS_LAB)
    val nextPreferredExam =
        ev("midterm", start.plusWeeks(1).plusDays(2), kind = EventKind.EXAM_MIDTERM)
    val nextPreferredStudyEarlier = ev("study", start.plusWeeks(1), kind = EventKind.STUDY)

    val res =
        AdaptivePlanner.planAdjustments(
            today,
            currentWeekEvents = listOf(completedEarly),
            nextWeekEvents = listOf(nextNonPreferred, nextPreferredExam, nextPreferredStudyEarlier))

    assertEquals(listOf(nextPreferredStudyEarlier), res.pulledEarlier)
  }

  @Test
  fun planAdjustments_pullsEarliestAnyWhenNoPreferred() {
    val today = LocalDate.of(2025, 3, 6) // Thu
    val start = AdaptivePlanner.weekStart(today)

    // MUST be strictly after 'today'
    val completedEarly = ev("done", today.plusDays(1), completed = true)

    val next1 =
        ev(
            "a",
            start.plusWeeks(1).plusDays(3),
            kind = EventKind.CLASS_LAB,
            time = LocalTime.of(10, 0))
    val next2EarlierSameDay =
        ev(
            "b",
            start.plusWeeks(1).plusDays(1),
            kind = EventKind.CLASS_EXERCISE,
            time = LocalTime.of(9, 0))

    val res =
        AdaptivePlanner.planAdjustments(
            today,
            currentWeekEvents = listOf(completedEarly),
            nextWeekEvents = listOf(next1, next2EarlierSameDay))

    assertEquals(listOf(next2EarlierSameDay), res.pulledEarlier)
  }

  @Test
  fun planAdjustments_noPullIfNotCompletedEarly() {
    val today = LocalDate.of(2025, 3, 6)
    val start = AdaptivePlanner.weekStart(today)

    val notCompleted = ev("incomplete future", start.plusDays(3), completed = false)
    val nextCandidate = ev("next", start.plusWeeks(1), kind = EventKind.STUDY)

    val res =
        AdaptivePlanner.planAdjustments(
            today, currentWeekEvents = listOf(notCompleted), nextWeekEvents = listOf(nextCandidate))

    assertTrue(res.pulledEarlier.isEmpty())
  }
}
