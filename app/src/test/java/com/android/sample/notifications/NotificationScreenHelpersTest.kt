package com.android.sample.ui.notifications

import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationsScreenHelpersTest {

  @Test
  fun clampTimeInputs_clamps_and_parses() {
    assertEquals(0 to 0, clampTimeInputs("", ""))
    assertEquals(9 to 5, clampTimeInputs("09", "05"))
    assertEquals(23 to 59, clampTimeInputs("99", "99")) // clamp max
    assertEquals(0 to 0, clampTimeInputs("00", "00"))
    assertEquals(7 to 30, clampTimeInputs("07abc", "30xyz"))
  }

  @Test
  fun formatDayTimeLabel_uses_map_and_defaults() {
    val times = mapOf(Calendar.MONDAY to (7 to 5), Calendar.WEDNESDAY to (18 to 0))
    assertEquals("Mon 07:05", formatDayTimeLabel(Calendar.MONDAY, times))
    assertEquals("Wed 18:00", formatDayTimeLabel(Calendar.WEDNESDAY, times))
    assertEquals("Tue 09:00", formatDayTimeLabel(Calendar.TUESDAY, times))
  }

  @Test
  fun dayShort_has_all_days() {
    val all =
        setOf(
            Calendar.MONDAY,
            Calendar.TUESDAY,
            Calendar.WEDNESDAY,
            Calendar.THURSDAY,
            Calendar.FRIDAY,
            Calendar.SATURDAY,
            Calendar.SUNDAY)
    assertTrue(DAY_SHORT.keys.containsAll(all))
    assertEquals("Mon", DAY_SHORT[Calendar.MONDAY])
    assertEquals("Sun", DAY_SHORT[Calendar.SUNDAY])
  }
}
