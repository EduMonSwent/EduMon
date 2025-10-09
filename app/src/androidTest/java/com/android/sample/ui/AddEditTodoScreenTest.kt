package com.android.sample.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.android.sample.todo.Priority
import com.android.sample.todo.Status
import com.android.sample.todo.ui.components.TodoForm
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test

class AddEditTodoScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun canEnterTitleAndSave() {
    var saved = false
    composeTestRule.setContent {
      TodoForm(
          titleTopBar = "New To-Do",
          saveButtonText = "Save",
          onBack = {},
          title = "",
          onTitleChange = {},
          dueDate = LocalDate.now(),
          onDueDateChange = {},
          priority = Priority.MEDIUM,
          onPriorityChange = {},
          status = Status.TODO,
          onStatusChange = {},
          location = null,
          onLocationChange = {},
          linksText = "",
          onLinksTextChange = {},
          note = null,
          onNoteChange = {},
          notificationsEnabled = false,
          onNotificationsChange = {},
          canSave = true,
          onSave = { saved = true })
    }

    composeTestRule.onNodeWithText("Save").performClick()
    assert(saved)
  }

  /**
   * @Test fun changeDateButtonExists() { composeTestRule.setContent { DueDateField( date =
   *   LocalDate.now(), onDateChange = {}, modifier = Modifier.testTag(TestTags.ChangeDateBtn) ) }
   *   composeTestRule.onNodeWithTag(TestTags.ChangeDateBtn).assertExists() }
   */
}
