package com.android.sample.data

import java.time.LocalDate

// --- a tiny in-test fake repo (in-memory) ---
class FakeMoodRepo : MoodRepositoryInterface {
  private val byEpoch = mutableMapOf<Long, MoodEntry>()

  override suspend fun upsertForDate(date: LocalDate, mood: Int, note: String) {
    byEpoch[date.toEpochDay()] = MoodEntry(date.toEpochDay(), mood, note)
  }

  override suspend fun getForDate(date: LocalDate): MoodEntry? {
    return byEpoch[date.toEpochDay()]
  }

  override suspend fun getRange(start: LocalDate, end: LocalDate): List<MoodEntry> {
    val from = start.toEpochDay()
    val to = end.toEpochDay()
    return byEpoch.values.filter { it.dateEpochDay in from..to }.sortedBy { it.dateEpochDay }
  }
}
