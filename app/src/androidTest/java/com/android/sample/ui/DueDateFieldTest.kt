package com.android.sample.todo.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.android.sample.todo.ui.components.TodoForm
import org.junit.Rule
import org.junit.Test

class DueDateFieldUiTest {

  @get:Rule val compose = createComposeRule()

  @Test
  fun change_button_is_present_and_clickable() {
    // Render in isolation through TodoForm where it's used
    compose.setContent {
      TodoForm(
          titleTopBar = "New To-Do",
          saveButtonText = "Save",
          onBack = {},
          title = "X",
          onTitleChange = {},
          dueDate = java.time.LocalDate.now(),
          onDueDateChange = {},
          priority = com.android.sample.todo.Priority.MEDIUM,
          onPriorityChange = {},
          status = com.android.sample.todo.Status.TODO,
          onStatusChange = {},
          showOptionalInitial = false,
          location = null,
          onLocationChange = {},
          linksText = "",
          onLinksTextChange = {},
          note = null,
          onNoteChange = {},
          notificationsEnabled = false,
          onNotificationsChange = {},
          canSave = true,
          onSave = {})
    }
    // Button lives behind TestTags.ChangeDateBtn
    compose.onNodeWithTag(TestTags.ChangeDateBtn).assertExists().performClick()
    // We don't assert the system DatePicker (off-process), just that our button works.
  }
}
