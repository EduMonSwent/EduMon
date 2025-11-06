package com.android.sample.schedule

import com.android.sample.model.schedule.*
import com.android.sample.ui.schdeule.AdaptivePlanner
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
}
