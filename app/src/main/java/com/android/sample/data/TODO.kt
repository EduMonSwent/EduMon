package com.android.sample.data

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

// Levels of importance for a task
enum class Priority {
  LOW,
  MEDIUM,
  HIGH
}

// Current progress of a task
enum class Status {
  TODO,
  IN_PROGRESS,
  DONE
}

/**
 * Represents a single To-Do item.
 * - Required fields: title, due date, priority, status
 * - Optional fields: location, links, note, notificationsEnabled
 */
data class ToDo(
    // Unique ID generated for each task
    val id: String = UUID.randomUUID().toString(),
    val title: String, // short task name
    val dueDate: LocalDate, // deadline
    val priority: Priority, // importance level
    val status: Status = Status.TODO, // defaults to "TODO"
    val location: String? = null, // place related to task (optional)
    val links: List<String> = emptyList(), // related resources (optional)
    val note: String? = null, // extra description (optional)
    val notificationsEnabled: Boolean = false // reminder toggle
) {
  /**
   * Utility to display due date in a human-friendly format
   *
   * @return formatted due date
   */
  fun dueDateFormatted(): String = dueDate.format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy"))
}
