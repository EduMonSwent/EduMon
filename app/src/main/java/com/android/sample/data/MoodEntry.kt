package com.android.sample.data

import java.time.LocalDate

data class MoodEntry(
    val dateEpochDay: Long,
    val mood: Int, // 1..5, 0 if “empty / no entry”
    val note: String
)

fun LocalDate.toEntry(mood: Int, note: String) =
    MoodEntry(this.toEpochDay(), mood.coerceIn(0, 5), note)
