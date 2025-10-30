package com.android.sample.data

import java.time.LocalDate
import kotlin.ranges.coerceIn

/** Represents a single mood entry for a specific calendar day. */
data class MoodEntry(
    val dateEpochDay: Long, // the date stored as the number of days since January 1, 1970 (UTC).
    val mood: Int, // 1..5, 0 if empty (no entry), 1 is very bad whereas 5 is great mood
    val note: String // optional text describing the user's mood.
)

/** Extension function that converts a LocalDate into a MoodEntry. */
fun LocalDate.toEntry(mood: Int, note: String): MoodEntry =
    MoodEntry(this.toEpochDay(), mood.coerceIn(0, 5), note)
