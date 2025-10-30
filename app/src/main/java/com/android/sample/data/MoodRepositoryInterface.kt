package com.android.sample.data

import java.time.LocalDate

/**
 * Abstraction for saving and reading moods. Lets us swap between real DataStore / Room in
 * production and fake in-memory storage in tests.
 */
interface MoodRepositoryInterface {
  suspend fun upsertForDate(date: LocalDate, mood: Int, note: String)

  suspend fun getForDate(date: LocalDate): MoodEntry?

  suspend fun getRange(start: LocalDate, end: LocalDate): List<MoodEntry>
}
