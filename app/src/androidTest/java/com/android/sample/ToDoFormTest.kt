package com.android.sample

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.android.sample.todo.Priority
import com.android.sample.todo.Status
import com.android.sample.todo.ui.TestTags
import com.android.sample.todo.ui.components.TodoForm
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test

class TodoFormStateTest {

  @get:Rule val compose = createComposeRule()

  @Test
  fun todoForm_whenGivenFullData_displaysAllFieldsCorrectly() {
    // Arrange: Render the form directly with a comprehensive set of static data.
    // We set `showOptionalInitial = true` to cover the `if (showOptional)` block.
    // We set `canSave = false` to cover the disabled button state.
    compose.setContent {
      MaterialTheme {
        TodoForm(
            titleTopBar = "Test Form",
            saveButtonText = "Save",
            onBack = {},
            title = "My Test Title",
            onTitleChange = {},
            dueDate = LocalDate.of(2025, 10, 14),
            onDueDateChange = {},
            priority = Priority.HIGH,
            onPriorityChange = {},
            status = Status.IN_PROGRESS,
            onStatusChange = {},
            showOptionalInitial = true,
            location = "My Location",
            onLocationChange = {},
            linksText = "my-link.com",
            onLinksTextChange = {},
            note = "My test note",
            onNoteChange = {},
            notificationsEnabled = true,
            onNotificationsChange = {},
            canSave = false, // Test the disabled state
            onSave = {})
      }
    }

    // Assert: Verify that all the data is correctly displayed on the screen.
    compose.onNodeWithText("Test Form").assertIsDisplayed() // Top bar title
    compose.onNodeWithTag(TestTags.TitleField).assertTextContains("My Test Title")
    compose.onNodeWithTag(TestTags.LocationField).assertTextContains("My Location")
    compose.onNodeWithTag(TestTags.NoteField).assertTextContains("My test note")
    compose.onNodeWithText("IN PROGRESS").assertIsDisplayed() // Dropdown text
    compose.onNodeWithTag(TestTags.NotificationsSwitch).assertIsOn()
    compose.onNodeWithTag(TestTags.SaveButton).assertIsNotEnabled() // Verify `canSave`
  }

  @Test
  fun todoForm_whenOptionalIsToggled_fieldsBecomeVisible() {
    // Arrange: Start with optional fields hidden.
    compose.setContent {
      MaterialTheme {
        TodoForm(
            titleTopBar = "Test Form",
            saveButtonText = "Save",
            onBack = {},
            title = "Title",
            onTitleChange = {},
            dueDate = LocalDate.now(),
            onDueDateChange = {},
            priority = Priority.LOW,
            onPriorityChange = {},
            status = Status.TODO,
            onStatusChange = {},
            showOptionalInitial = false, // Start hidden
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
    }

    // Assert initial state: Optional fields should not be on screen.
    compose.onNodeWithTag(TestTags.LocationField).assertDoesNotExist()

    // Act: Click the button to show the optional fields.
    compose.onNodeWithTag(TestTags.OptionalToggle).performClick()

    // Assert final state: The optional fields should now be visible.
    compose.onNodeWithTag(TestTags.LocationField).assertIsDisplayed()
    compose.onNodeWithTag(TestTags.NoteField).assertIsDisplayed()
  }
}
