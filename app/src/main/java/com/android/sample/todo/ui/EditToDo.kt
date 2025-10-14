package com.android.sample.todo.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.sample.todo.ToDoRepositoryProvider
import com.android.sample.todo.ui.components.TodoForm

/**
 * Screen for editing an existing To-Do. It reuses the same TodoForm composable as AddToDoScreen,
 * but initializes fields with an existing ToDo (via its ID) and updates the repository instead of
 * adding a new entry.
 */
@Composable
fun EditToDoScreen(id: String, onBack: () -> Unit) {
  val context = LocalContext.current

  // Create the ViewModel with a custom factory to pass repository and ToDo ID
  val vm: EditToDoViewModel =
      viewModel(
          factory =
              object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                  return EditToDoViewModel(
                      repo = ToDoRepositoryProvider.repository, // inject shared repo
                      id = id // pass the ToDo ID to load data
                      )
                      as T
                }
              })

  // Reuse the same TodoForm UI used in AddToDoScreen
  TodoForm(
      titleTopBar = "Edit To-Do", // change top bar title
      saveButtonText = "Save changes", // change button text
      onBack = onBack,

      // --- Required fields ---
      title = vm.title,
      onTitleChange = { vm.title = it },
      dueDate = vm.dueDate,
      onDueDateChange = { vm.dueDate = it },
      priority = vm.priority,
      onPriorityChange = { vm.priority = it },
      status = vm.status,
      onStatusChange = { vm.status = it },

      // --- Optional fields ---
      showOptionalInitial = true, // optional section visible by default
      location = vm.location,
      onLocationChange = { vm.location = it },
      linksText = vm.linksText,
      onLinksTextChange = { vm.linksText = it },
      note = vm.note,
      onNoteChange = { vm.note = it },
      notificationsEnabled = vm.notificationsEnabled,
      onNotificationsChange = { vm.notificationsEnabled = it },

      // --- Save ---
      canSave = vm.canSave,
      onSave = { vm.save(onBack) } // update repo and go back
      )
}
