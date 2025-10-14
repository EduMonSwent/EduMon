package com.android.sample.todo.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.android.sample.todo.ToDo
import com.android.sample.todo.ToDoRepositoryLocal
import com.android.sample.todo.ToDoRepositoryProvider
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class EditToDoScreenTest {

  @get:Rule val compose = createComposeRule()
  private lateinit var existing: ToDo

  @Before
  fun setup() {
    ToDoRepositoryProvider.repository = ToDoRepositoryLocal()
    existing =
        ToDo(
            id = "E1",
            title = "Orig",
            dueDate = LocalDate.now(),
            priority = com.android.sample.todo.Priority.MEDIUM)
    kotlinx.coroutines.runBlocking { ToDoRepositoryProvider.repository.add(existing) }
  }

  @Test
  fun change_title_and_links_then_save_updates_repo() {
    compose.setContent { EditToDoScreen(id = "E1", onBack = {}) }

    // champs optionnels visibles par défaut sur Edit
    compose.onNodeWithTag(TestTags.TitleField).performTextClearance()
    compose.onNodeWithTag(TestTags.TitleField).performTextInput(" Updated ")
    compose.onNodeWithTag(TestTags.LinksField).performTextInput("x.com, y.com")
    compose.onNodeWithTag(TestTags.SaveButton).performClick()

    compose.runOnIdle {}
    val updated =
        kotlinx.coroutines.runBlocking { ToDoRepositoryProvider.repository.getById("E1")!! }
    assertEquals("Updated", updated.title)
    assertEquals(listOf("x.com", "y.com"), updated.links)
  }
}
