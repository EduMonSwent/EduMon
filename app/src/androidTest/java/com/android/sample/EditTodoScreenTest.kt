package com.android.sample.todo.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.android.sample.FakeToDoRepository
import com.android.sample.todo.Priority
import com.android.sample.todo.ToDo
import com.android.sample.todo.ToDoRepositoryProvider
import java.time.LocalDate
import junit.framework.TestCase.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class EditToDoScreenTest {

  @get:Rule val compose = createComposeRule()

  private val fakeRepo = FakeToDoRepository()
  private val existingTodo =
      ToDo(
          id = "123",
          title = "Existing Task",
          dueDate = LocalDate.of(2025, 5, 20),
          priority = Priority.MEDIUM,
          note = "Some details")

  @Before
  fun setUp() {
    fakeRepo.setInitialItems(listOf(existingTodo))
    ToDoRepositoryProvider.repository = fakeRepo
  }

  @Test
  fun screen_preFillsData_fromRepository() {
    compose.setContent { MaterialTheme { EditToDoScreen(id = "123", onBack = {}) } }

    // Wait for the ViewModel to load the data
    compose.waitForIdle()

    // Assert that fields are pre-filled
    compose.onNodeWithTag(TestTags.TitleField).assertTextContains("Existing Task")
    compose.onNodeWithTag(TestTags.NoteField).assertTextContains("Some details")
    // Optional fields are visible by default on edit screen
    compose.onNodeWithTag(TestTags.LocationField).assertIsDisplayed()
  }

  @Test
  fun saving_updatesItemInRepo_andNavigatesBack() {
    var onBackCalled = false
    compose.setContent {
      MaterialTheme { EditToDoScreen(id = "123", onBack = { onBackCalled = true }) }
    }
    compose.waitForIdle()

    // Act: Change some data
    compose.onNodeWithTag(TestTags.TitleField).performTextClearance()
    compose.onNodeWithTag(TestTags.TitleField).performTextInput("Updated Task Title")
    compose.onNodeWithTag(TestTags.NotificationsSwitch).performClick()

    // Save
    compose.onNodeWithTag(TestTags.SaveButton).performClick()

    // This correctly waits for the callback to be invoked
    compose.waitUntil(timeoutMillis = 2_000) {
      onBackCalled // The condition is simply the boolean flag itself
    }

    // Assert side-effects after confirming navigation
    assertTrue("onBack should have been called", onBackCalled)
    val updatedItem = fakeRepo.currentList.single()
    assertEquals("123", updatedItem.id) // ID is unchanged
    assertEquals("Updated Task Title", updatedItem.title) // Title is updated
    assertTrue(updatedItem.notificationsEnabled) // Switch was toggled
  }
}
