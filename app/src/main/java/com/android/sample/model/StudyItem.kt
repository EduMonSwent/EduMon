package com.android.sample.model

import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.util.UUID

data class StudyItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String? = null,
    val date: LocalDate,
    val time: LocalTime? = null,
    val durationMinutes: Int? = null,
    val isCompleted: Boolean = false,
    val priority: Priority = Priority.MEDIUM,
    val type: TaskType
)

enum class Priority {
  LOW,
  MEDIUM,
  HIGH
}

enum class TaskType {
  STUDY,
  WORK,
  PERSONAL
}

data class CalendarUiState(
    val tasksByDate: Map<LocalDate, List<StudyItem>> = emptyMap(),
    val selectedDate: LocalDate = LocalDate.now(),
    val currentDisplayMonth: YearMonth = YearMonth.now(),
    val isMonthView: Boolean = true,
    val isShowingAddEditModal: Boolean = false,
    val taskToEdit: StudyItem? = null
)
