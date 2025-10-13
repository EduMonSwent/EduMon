package com.android.sample.ui.viewmodel

import java.time.DayOfWeek

data class Objective(
    val title: String,
    val course: String,
    val estimateMinutes: Int = 0,
    val reason: String = ""
)

data class DayStatus(val dayOfWeek: DayOfWeek, val metTarget: Boolean)

data class WeekProgressItem(
    val label: String,
    val percent: Int // 0..100
)
