package com.android.sample.ui.util

import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.WeekFields
import java.util.Locale

object WeekUtils {
  private val tz = ZoneId.of("Europe/Zurich")

  fun nowLocal(): LocalDate = LocalDate.now(tz)

  fun currentWeekId(date: LocalDate = nowLocal()): String {
    val wf = WeekFields.of(Locale.FRANCE)
    val week = date.get(wf.weekOfWeekBasedYear())
    val year = date.get(wf.weekBasedYear())
    return "week_${year}_${"%02d".format(week)}"
  }

  fun dayIndex(date: LocalDate = nowLocal()): Int = date.dayOfWeek.value - 1 // 0..6
}
