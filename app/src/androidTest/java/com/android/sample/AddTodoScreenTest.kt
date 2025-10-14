package com.android.sample

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.android.sample.todo.ToDoRepositoryProvider
import com.android.sample.todo.ui.AddToDoScreen
import com.android.sample.todo.ui.TestTags
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.*

class AddToDoScreenTest {

  @get:Rule val compose = createComposeRule()

  private val fakeRepo = FakeToDoRepository()

  @Before
  fun setUp() {
    // Intercept the repository provider and swap it with our fake for this test
    ToDoRepositoryProvider.repository = fakeRepo
  }

  @Test
  fun saveButton_isDisabled_whenTitleIsEmpty() {
    compose.setContent { MaterialTheme { AddToDoScreen(onBack = {}) } }

    // Assert initial state
    compose.onNodeWithTag(TestTags.SaveButton).assertIsNotEnabled()

    // Type something
    compose.onNodeWithTag(TestTags.TitleField).performTextInput("A")
    compose.onNodeWithTag(TestTags.SaveButton).assertIsEnabled()

    // Clear it
    compose.onNodeWithTag(TestTags.TitleField).performTextClearance()
    compose.onNodeWithTag(TestTags.SaveButton).assertIsNotEnabled()
  }

  @Test
  fun saving_createsItemInRepo_andNavigatesBack() {
    var onBackCalled = false
    compose.setContent { MaterialTheme { AddToDoScreen(onBack = { onBackCalled = true }) } }

    // Fill required field
    compose.onNodeWithTag(TestTags.TitleField).performTextInput("Finish lab 4")

    // Toggle and fill optional fields
    compose.onNodeWithTag(TestTags.OptionalToggle).performClick()
    compose
        .onNodeWithTag(TestTags.NoteField, useUnmergedTree = true)
        .performTextInput("Pair with Lea")
    compose.onNodeWithTag(TestTags.NotificationsSwitch).performClick()

    // Save
    compose.onNodeWithTag(TestTags.SaveButton).performClick()

    // STABLE CI ASSERTION: Wait for the UI to disappear, not the data to change.
    // This correctly waits for the callback to be invoked
    compose.waitUntil(timeoutMillis = 2_000) {
      onBackCalled // The condition is simply the boolean flag itself
    }

    // Now that navigation is confirmed, assert the side-effects
    assertTrue("onBack should have been called", onBackCalled)
    assertEquals(1, fakeRepo.currentList.size)
    val savedItem = fakeRepo.currentList.single()
    assertEquals("Finish lab 4", savedItem.title)
    assertEquals("Pair with Lea", savedItem.note)
    assertTrue(savedItem.notificationsEnabled)
  }
}
