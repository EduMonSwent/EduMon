package com.android.sample.todo.ui

import androidx.compose.ui.test.junit4.createComposeRule
import com.android.sample.todo.Status
import com.android.sample.todo.testutils.ToDoTest
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class OverviewScreenUiTest : ToDoTest {

  @get:Rule val compose = createComposeRule()

  @Before override fun setUpProvider() = super.setUpProvider()

  @Test
  fun list_shows_items_allows_toggle_and_delete() {
    compose.setContent { OverviewScreen(onAddClicked = {}, onEditClicked = {}) }
    // Add two items into repo
    val a = sampleToDo(id = "A", title = "A", status = Status.TODO)
    val b = sampleToDo(id = "B", title = "B", status = Status.IN_PROGRESS)
    addAll(a, b)

    compose.waitUntilTodoCardShown("A")
    compose.waitUntilTodoCardShown("B")

    // Toggle A (TODO -> IN_PROGRESS)
    compose.clickStatusChip("A")
    // Delete B
    compose.clickDelete("B")

    runBlocking {
      val updatedA = repository.getById("A")!!
      val removedB = repository.getById("B")
      assertEquals(Status.IN_PROGRESS, updatedA.status)
      assertEquals(null, removedB)
    }
  }
}
