package com.android.sample.ui.todo

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.sample.todo.ui.components.TodoForm
import com.android.sample.ui.todo.data.ToDoRepositoryProvider

/**
 * Add To-Do screen:
 * - Creates the AddToDoViewModel (wired to the shared repository)
 * - Renders the shared TodoForm with the VM's state and callbacks
 */
@Composable
fun AddToDoScreen(onBack: () -> Unit) {

  // Create a ViewModel instance with a custom factory so we can inject the repository
  val vm: AddToDoViewModel =
      viewModel(
          factory =
              object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                  // Get the single shared repository from the provider
                  return AddToDoViewModel(ToDoRepositoryProvider.repository) as T
                }
              })

  // Reuse the shared form UI, wiring it to the VM's state and events
  TodoForm(
      titleTopBar = "New To-Do",
      saveButtonText = "Save To-Do",
      onBack = onBack,

      // Required fields
      title = vm.title,
      onTitleChange = { vm.title = it },
      dueDate = vm.dueDate,
      onDueDateChange = { vm.dueDate = it },
      priority = vm.priority,
      onPriorityChange = { vm.priority = it },
      status = vm.status,
      onStatusChange = { vm.status = it },

      // Optional fields
      showOptionalInitial = false,
      location = vm.location,
      onLocationChange = { vm.location = it },
      linksText = vm.linksText,
      onLinksTextChange = { vm.linksText = it },
      note = vm.note,
      onNoteChange = { vm.note = it },
      notificationsEnabled = vm.notificationsEnabled,
      onNotificationsChange = { vm.notificationsEnabled = it },

      // Save button wiring
      canSave = vm.canSave,
      onSave = { vm.save(onBack) })
}
