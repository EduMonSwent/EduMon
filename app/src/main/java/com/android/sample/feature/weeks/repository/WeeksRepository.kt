package com.android.sample.feature.weeks.repository

import com.android.sample.feature.weeks.model.DayStatus
import com.android.sample.feature.weeks.model.WeekProgressItem
import java.time.DayOfWeek

interface WeeksRepository {
  suspend fun getWeeks(): List<WeekProgressItem>

  suspend fun getDayStatuses(): List<DayStatus>
  // Update the percent for a specific week by index. Returns the updated list.
  suspend fun updateWeekPercent(index: Int, percent: Int): List<WeekProgressItem>
  // Update the percent for a specific week by label. Returns the updated list.
}

class FakeWeeksRepository : WeeksRepository {
  private val sampleWeeks =
      mutableListOf(
          WeekProgressItem("Week 1", 100),
          WeekProgressItem("Week 2", 55),
          WeekProgressItem("Week 3", 10),
      )

  private val sampleStatuses =
      DayOfWeek.values().mapIndexed { idx, d -> DayStatus(d, metTarget = idx % 2 == 0) }

  override suspend fun getWeeks(): List<WeekProgressItem> {
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
}
