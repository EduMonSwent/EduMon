package com.android.sample

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.android.sample.todo.Priority
import com.android.sample.todo.Status
import com.android.sample.todo.ui.TestTags
import com.android.sample.todo.ui.components.TodoForm
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test

class DueDateFieldTest {

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
          dueDate = LocalDate.now(),
          onDueDateChange = {},
          priority = Priority.MEDIUM,
          onPriorityChange = {},
          status = Status.TODO,
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
