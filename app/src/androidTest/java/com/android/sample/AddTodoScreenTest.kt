package com.android.sample

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.android.sample.todo.Priority
import com.android.sample.todo.Status
import com.android.sample.todo.ui.TestTags
import com.android.sample.todo.ui.components.TodoForm
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test

class AddToDoScreenTest {

  @get:Rule val compose = createComposeRule()

  @Test
  fun todoForm_whenGivenData_displaysItCorrectly() {
    // This test has no complex rules. It's a simple render check.
    compose.setContent {
      MaterialTheme {
        // We call TodoForm directly with hardcoded, static data.
        // All the callbacks are empty because we are not testing clicks.
        TodoForm(
            titleTopBar = "New To-Do",
            saveButtonText = "Save To-Do",
            onBack = {},
            title = "My Test Title",
            onTitleChange = {},
            dueDate = LocalDate.of(2025, 10, 14),
            onDueDateChange = {},
            priority = Priority.HIGH,
            onPriorityChange = {},
            status = Status.IN_PROGRESS,
            onStatusChange = {},
            showOptionalInitial = true, // Force optional fields to be visible
            location = "My Location",
            onLocationChange = {},
            linksText = "my-link.com",
            onLinksTextChange = {},
            note = "My test note",
            onNoteChange = {},
            notificationsEnabled = true,
            onNotificationsChange = {},
            canSave = true,
            onSave = {})
      }
    }

    compose.onNodeWithTag(TestTags.TitleField).assertTextContains("My Test Title")
    compose.onNodeWithTag(TestTags.NoteField).assertTextContains("My test note")
    compose.onNodeWithTag(TestTags.LocationField).assertTextContains("My Location")
    compose.onNodeWithText("IN PROGRESS").assertIsDisplayed() // Check the dropdown
  }
}
