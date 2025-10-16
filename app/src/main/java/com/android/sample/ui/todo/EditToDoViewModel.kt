package com.android.sample.ui.todo

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.sample.data.Priority
import com.android.sample.data.Status
import com.android.sample.repositories.ToDoRepository
import java.time.LocalDate
import kotlin.collections.filter
import kotlin.collections.joinToString
import kotlin.collections.map
import kotlin.let
import kotlin.takeIf
import kotlin.text.isNotBlank
import kotlin.text.isNullOrBlank
import kotlin.text.split
import kotlin.text.trim
import kotlinx.coroutines.launch

/**
 * ViewModel for editing an existing To-Do.
 * - Loads the To-Do data by ID when created
 * - Exposes its fields as observable state to the UI
 * - Handles saving updates back to the repository
 */
class EditToDoViewModel(
    private val repo: ToDoRepository, // reference to shared data source
    private val id: String // ID of the To-Do being edited
) : ViewModel() {

  // ---------- Form state (mirrors fields in the UI) ----------

  // Required
  var title by mutableStateOf("") // title text
  var dueDate by mutableStateOf(LocalDate.now()) // due date (default = today)
  var priority by mutableStateOf(Priority.MEDIUM) // priority level
  var status by mutableStateOf(Status.TODO) // progress status

  // Optional
  var location by mutableStateOf<String?>(null)
  var linksText by mutableStateOf("") // comma-separated links
  var note by mutableStateOf<String?>(null)
  var notificationsEnabled by mutableStateOf(false)

  // Validation rule — only allow saving if there's a title
  val canSave
    get() = title.isNotBlank()

  // ---------- Initialization block ----------
  init {
    // On creation, load the existing To-Do from repository
    viewModelScope.launch {
      repo.getById(id)?.let { t ->
        // Pre-fill all fields with the To-Do's current data
        title = t.title
        dueDate = t.dueDate
        priority = t.priority
        status = t.status
        location = t.location
        linksText = t.links.joinToString(", ") // convert list -> string
        note = t.note
        notificationsEnabled = t.notificationsEnabled
      }
    }
  }

  // ---------- Save logic ----------
  fun save(onDone: () -> Unit) =
      viewModelScope.launch {
        // Get the current version of this To-Do
        val current = repo.getById(id) ?: return@launch

        // Convert the links text into a clean list
        val links = linksText.split(",").map { it.trim() }.filter { it.isNotBlank() }

        // Copy the existing To-Do, replacing updated fields
        repo.update(
            current.copy(
                title = title.trim(),
                dueDate = dueDate,
                priority = priority,
                status = status,
                location = location?.takeIf { it.isNullOrBlank().not() },
                links = links,
                note = note?.takeIf { it.isNullOrBlank().not() },
                notificationsEnabled = notificationsEnabled))

        // Call onDone() — usually navigates back to list screen
        onDone()
      }
}
