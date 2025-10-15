package com.android.sample.ui.util

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class WeekUtilsTest {

  @Test
  fun currentWeekId_matchesWeekFieldsForKnownDate() {
    // GIVEN a fixed date (deterministic)
    val date = LocalDate.of(2025, 10, 15) // Wed

    // WHEN
    val id = WeekUtils.currentWeekId(date)

    // THEN (compute expected with same week rules)
    val wf = WeekFields.of(Locale.FRANCE)
    val week = date.get(wf.weekOfWeekBasedYear())
    val year = date.get(wf.weekBasedYear())
    val expected = "week_${year}_${"%02d".format(week)}"

    assertEquals(expected, id)
  }

  @Test
  fun dayIndex_mondayIsZero_sundayIsSix() {
    // Build a Monday/Sunday from an arbitrary anchor date
    val anchor = LocalDate.of(2025, 10, 15) // any date
    val monday = anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val sunday = anchor.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))

    assertEquals(0, WeekUtils.dayIndex(monday)) // Monday -> 0
    assertEquals(6, WeekUtils.dayIndex(sunday)) // Sunday -> 6
  }

  @Test
  fun currentWeekId_handlesYearBoundary() {
    // Dates around new year to ensure week-based year is correct
    val dec31 = LocalDate.of(2020, 12, 31)
    val jan01 = LocalDate.of(2021, 1, 1)

    val wf = WeekFields.of(Locale.FRANCE)

    val dec31Expected =
        "week_${dec31.get(wf.weekBasedYear())}_${"%02d".format(dec31.get(wf.weekOfWeekBasedYear()))}"
    val jan01Expected =
        "week_${jan01.get(wf.weekBasedYear())}_${"%02d".format(jan01.get(wf.weekOfWeekBasedYear()))}"

    assertEquals(dec31Expected, WeekUtils.currentWeekId(dec31))
    assertEquals(jan01Expected, WeekUtils.currentWeekId(jan01))
  }
}
