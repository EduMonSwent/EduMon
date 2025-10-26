package com.android.sample.feature.weeks.model

import java.time.DayOfWeek

data class Objective(
    val title: String,
    val course: String,
    val estimateMinutes: Int = 0,
    val completed: Boolean = false,
    val day: DayOfWeek,
)
