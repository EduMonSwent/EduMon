package com.android.sample.feature.weeks.model

import java.time.DayOfWeek

data class DayStatus(
    val dayOfWeek: DayOfWeek,
    val metTarget: Boolean,
    val dayObjectives: List<Objective> = emptyList()
)
