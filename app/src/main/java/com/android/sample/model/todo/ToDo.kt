package com.android.sample.model.todo

import com.android.sample.model.map.Location
import com.google.firebase.Timestamp
import java.util.Locale

data class ToDo(
    val uid: String,
    val name: String,
    val description: String,
    val assigneeName: String,
    val dueDate: Timestamp,
    val location: Location?,
    val status: ToDoStatus,
    val ownerId: String,

    // You could have done this differently, but we need this field to sort the ToDos
    val createdAt: Timestamp = Timestamp.now(),
)

enum class ToDoStatus {
  CREATED,
  STARTED,
  ENDED,
  ARCHIVED
}

/**
 * Converts the ToDoStatus enum to a more readable display string (camel case).
 *
 * @return A string representation of the ToDoStatus, formatted for display.
 */
fun ToDoStatus.displayString(): String =
    name.replace("_", " ").lowercase(Locale.ROOT).replaceFirstChar {
      if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
    }
