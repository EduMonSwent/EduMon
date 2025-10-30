package com.android.sample.data

import java.time.LocalDate

/**
 * Repository interface for managing mood data.
 *
 * Responsibilities:
 * - Save or update a mood entry for a specific date.
 * - Retrieve a single mood entry by date.
 * - Retrieve all mood entries within a given date range.
 */
interface MoodRepositoryInterface {

  /**
   * Inserts or updates a mood entry for the given date.
   *
   * @param date The calendar date associated with the mood entry.
   * @param mood The mood rating, typically represented as an integer (e.g., 1–5).
   * @param note Optional text describing the user’s mood for that day.
   */
  suspend fun upsertForDate(date: LocalDate, mood: Int, note: String)

  /**
   * Retrieves the mood entry associated with the specified date.
   *
   * @param date The date to query.
   * @return The MoodEntry if one exists for that date, or null otherwise.
   */
  suspend fun getForDate(date: LocalDate): MoodEntry?

  /**
   * Retrieves all mood entries between start and end, inclusive.
   *
   * @return A list of MoodEntry objects within the specified date range. The list may be empty if
   *   no entries exist (in that range).
   */
  suspend fun getRange(start: LocalDate, end: LocalDate): List<MoodEntry>
}
