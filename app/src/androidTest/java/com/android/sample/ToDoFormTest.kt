// src/test/java/com/android/sample/todo/ui/TodoFormTest.kt
package com.android.sample

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.android.sample.todo.Priority
import com.android.sample.todo.Status
import com.android.sample.todo.ui.TestTags
import com.android.sample.todo.ui.components.TodoForm
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test

class TodoFormTest {
  @get:Rule val compose = createComposeRule()

  @Test
  fun dropdownsChangeValues() {
    var priority = Priority.MEDIUM
    var status = Status.TODO

    compose.setContent {
      TodoForm(
          "T",
          "S",
          {},
          "X",
          {},
          LocalDate.now(),
          {},
          priority,
          { priority = it },
          status,
          { status = it },
          false,
          null,
          {},
          "",
          {},
          null,
          {},
          false,
          {},
          true,
          {})
    }

    // Priority
    compose.onNodeWithTag(TestTags.PriorityDropdown).performClick()
    compose.onNode(hasText("HIGH"), useUnmergedTree = true).performClick()
    assert(priority == Priority.HIGH)

    // Status
    compose.onNodeWithTag(TestTags.StatusDropdown).performClick()
    compose.onNode(hasText("IN PROGRESS"), useUnmergedTree = true).performClick()
    assert(status == Status.IN_PROGRESS)
  }
}
