package com.android.sample.ui.schedule

import com.android.sample.model.schedule.*
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.*
import org.junit.Test

class AdaptivePlannerTest {

  @Test
  fun weekStart_isMonday() {
    val d = LocalDate.of(2025, 11, 6) // Thursday
    val start = AdaptivePlanner.weekStart(d)
    assertEquals(LocalDate.of(2025, 11, 3), start)
  }

  @Test
  fun weekEnd_isSundayOfSameWeek() {
    val d = LocalDate.of(2025, 11, 6) // Thursday
    val end = AdaptivePlanner.weekEnd(d)
    assertEquals(LocalDate.of(2025, 11, 9), end)
  }

  @Test
  fun planAdjustments_movesMissedFromCurrentWeek_and_pullsOneFromNext_whenCompletedEarlyExists() {
    val today = LocalDate.of(2025, 11, 6) // Thu
    val weekStart = AdaptivePlanner.weekStart(today)
    val weekEnd = AdaptivePlanner.weekEnd(today)
    val nextStart = weekStart.plusWeeks(1)

    // Current week tasks:
    val missed =
        ScheduleEvent(
            id = "missed",
            title = "Missed study",
            date = weekStart.plusDays(0), // Monday < today
            kind = EventKind.STUDY,
            isCompleted = false,
            sourceTag = SourceTag.Task)
    val completedEarly =
        ScheduleEvent(
            id = "completedEarly",
            title = "Completed future task early",
            date = weekEnd, // future wrt today
            time = LocalTime.of(10, 0),
            kind = EventKind.PROJECT,
            isCompleted = true,
            sourceTag = SourceTag.Task)
    val currentWeek = listOf(missed, completedEarly)

    // Next week tasks (choose 1 to pull):
    val lowPriorityNonPreferred =
        ScheduleEvent(
            id = "np",
            title = "Association meeting", // non-preferred
            date = nextStart.plusDays(1),
            kind = EventKind.ACTIVITY_ASSOCIATION,
            isCompleted = false,
            priority = Priority.LOW,
            sourceTag = SourceTag.Task)
    val preferredHigh =
        ScheduleEvent(
            id = "ph",
            title = "Project milestone",
            date = nextStart.plusDays(2),
            kind = EventKind.SUBMISSION_MILESTONE, // preferred
            isCompleted = false,
            priority = Priority.HIGH,
            sourceTag = SourceTag.Task)
    val nextWeek = listOf(lowPriorityNonPreferred, preferredHigh)

    val result = AdaptivePlanner.planAdjustments(today, currentWeek, nextWeek)

    // Missed should include the missed one
    assertEquals(listOf(missed), result.movedMissed)

    // Pulled earlier should include the preferred/high one
    assertEquals(1, result.pulledEarlier.size)
    assertEquals("ph", result.pulledEarlier.first().id)
  }

  @Test
  fun planAdjustments_pullsNone_whenNoCompletedEarly() {
    val today = LocalDate.of(2025, 11, 6)
    val weekStart = AdaptivePlanner.weekStart(today)

    val current =
        listOf(
            ScheduleEvent(
                id = "a",
                title = "Study",
                date = weekStart.plusDays(1),
                kind = EventKind.STUDY,
                isCompleted = false,
                sourceTag = SourceTag.Task))
    val next =
        listOf(
            ScheduleEvent(
                id = "b",
                title = "Future project",
                date = weekStart.plusWeeks(1).plusDays(1),
                kind = EventKind.PROJECT,
                isCompleted = false,
                sourceTag = SourceTag.Task))

    val result = AdaptivePlanner.planAdjustments(today, current, next)
    assertTrue(result.pulledEarlier.isEmpty())
  }

  @Test
  fun planAdjustments_prefers_higher_priority_then_earlier_date_then_time() {
    val today = LocalDate.of(2025, 11, 6)
    val start = AdaptivePlanner.weekStart(today)
    val nextStart = start.plusWeeks(1)

    // Current week: mark one completed in the future to enable "pull"
    val current =
        listOf(
            ScheduleEvent(
                id = "doneFuture",
                title = "Completed future",
                date = today.plusDays(1),
                time = LocalTime.of(12, 0),
                kind = EventKind.STUDY,
                isCompleted = true,
                sourceTag = SourceTag.Task))

    // Next week candidates: all preferred kinds
    val lowLater =
        ScheduleEvent(
            id = "lowLater",
            title = "Low later",
            date = nextStart.plusDays(3),
            time = LocalTime.of(16, 0),
            kind = EventKind.PROJECT,
            isCompleted = false,
            priority = Priority.LOW,
            sourceTag = SourceTag.Task)

    val medEarlier =
        ScheduleEvent(
            id = "medEarlier",
            title = "Medium earlier",
            date = nextStart.plusDays(1),
            time = LocalTime.of(18, 0),
            kind = EventKind.SUBMISSION_PROJECT,
            isCompleted = false,
            priority = Priority.MEDIUM,
            sourceTag = SourceTag.Task)

    val highSameDayEarlierTime =
        ScheduleEvent(
            id = "highPickMe",
            title = "High same day earlier time",
            date = nextStart.plusDays(1),
            time = LocalTime.of(9, 0),
            kind = EventKind.STUDY,
            isCompleted = false,
            priority = Priority.HIGH,
            sourceTag = SourceTag.Task)

    val result =
        AdaptivePlanner.planAdjustments(
            today,
            currentWeekEvents = current,
            nextWeekEvents = listOf(lowLater, medEarlier, highSameDayEarlierTime))

    // Should pick HIGH first; tie on date vs MEDIUM; then earlier time among same date
    assertEquals(1, result.pulledEarlier.size)
    assertEquals("highPickMe", result.pulledEarlier.first().id)
  }
}
