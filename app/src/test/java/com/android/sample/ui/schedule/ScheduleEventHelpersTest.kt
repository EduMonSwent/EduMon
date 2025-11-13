package com.android.sample.ui.schedule

import com.android.sample.feature.schedule.data.schedule.EventKind
import com.android.sample.feature.schedule.data.schedule.ScheduleEvent
import com.android.sample.feature.schedule.data.schedule.SourceTag
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.*
import org.junit.Test

class ScheduleEventHelpersTest {

  @Test
  fun helpers_and_durationDisplay() {
    val e1 =
        ScheduleEvent(
            title = "All-day",
            date = LocalDate.now(),
            kind = EventKind.STUDY,
            sourceTag = SourceTag.Task)
    assertTrue(e1.isTask)
    assertFalse(e1.isClass)
    assertFalse(e1.hasTime)
    assertTrue(e1.isAllDay)
    assertEquals("", e1.durationDisplay)

    val e2 =
        e1.copy(
            time = LocalTime.of(9, 0),
            durationMinutes = 45,
            kind = EventKind.CLASS_LECTURE,
            sourceTag = SourceTag.Class)
    assertTrue(e2.hasTime)
    assertFalse(e2.isAllDay)
    assertEquals("45m", e2.durationDisplay)

    assertEquals("2h", e2.copy(durationMinutes = 120).durationDisplay)
    assertEquals("2h 5m", e2.copy(durationMinutes = 125).durationDisplay)
  }
}
