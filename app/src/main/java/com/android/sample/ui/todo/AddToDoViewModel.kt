package com.android.sample.ui.todo

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.sample.ui.todo.data.ToDoRepository
import com.android.sample.ui.todo.model.Priority
import com.android.sample.ui.todo.model.Status
import com.android.sample.ui.todo.model.ToDo
import java.time.LocalDate
import kotlinx.coroutines.launch

/**
 * ViewModel responsible for managing the UI state and logic of the Add To-Do screen.
 * - Holds the current values of the form fields
 * - Validates them
 * - Creates and saves a new ToDo object into the repository
 */
class AddToDoViewModel(private val repo: ToDoRepository) : ViewModel() {

  // ---------- Required fields ----------
  var title by mutableStateOf("") // user-entered title text
  var dueDate by mutableStateOf(LocalDate.now()) // default to today
  var priority by mutableStateOf(Priority.MEDIUM) // default to medium priority
  var status by mutableStateOf(Status.TODO) // default status

  // ---------- Optional fields ----------
  var location by mutableStateOf<String?>(null) // optional location text
  var linksText by mutableStateOf("") // comma-separated links
  var note by mutableStateOf<String?>(null) // optional note/description
  var notificationsEnabled by mutableStateOf(false) // switch for notifications

  // Validation rule: can only save if title is not blank
  val canSave
    get() = title.isNotBlank()

  /**
   * Called when the user presses "Save".
   * - Validates input
   * - Builds a ToDo object
   * - Saves it via the repository
   * - Then triggers onDone() (usually navigate back)
   */
  fun save(onDone: () -> Unit) =
      viewModelScope.launch {
        if (!canSave) return@launch // do nothing if invalid

        // Prepare the list of useful links (split by commas)
        val links = linksText.split(",").map { it.trim() }.filter { it.isNotBlank() }

        // Build a ToDo object with all the user data
        repo.add(
            ToDo(
                title = title.trim(),
                dueDate = dueDate,
                priority = priority,
                status = status,
                location = location?.takeIf { it.isNullOrBlank().not() },
                links = links,
                note = note?.takeIf { it.isNullOrBlank().not() },
                notificationsEnabled = notificationsEnabled))

        // Navigate back or close the screen after saving
        onDone()
      }
}
