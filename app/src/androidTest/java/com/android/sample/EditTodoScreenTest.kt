package com.android.sample

// Add these lines to the top of your test file
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.android.sample.todo.Priority
import com.android.sample.todo.ToDo
import com.android.sample.todo.ToDoRepositoryProvider
import com.android.sample.todo.ui.EditToDoScreen
import com.android.sample.todo.ui.TestTags
import java.time.LocalDate
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class EditScreenStateTest {

  @get:Rule val compose = createComposeRule()

  private lateinit var fakeRepo: FakeToDoRepository
  private val existingTodo =
      ToDo(
          id = "123",
          title = "My Pre-filled Title",
          priority = Priority.HIGH,
          note = "This is a pre-filled note.",
          dueDate = LocalDate.of(2025, 10, 14))

  @Before
  fun setUp() {
    fakeRepo = FakeToDoRepository()
    fakeRepo.setInitialItems(listOf(existingTodo))
    ToDoRepositoryProvider.repository = fakeRepo
  }

  @Test
  fun editScreen_whenLaunched_displaysPrefilledData() {
    // Arrange: Render the EditToDoScreen. The ViewModel will load the data.
    compose.setContent { MaterialTheme { EditToDoScreen(id = "123", onBack = {}) } }

    // Assert: Check that the UI correctly displays the data loaded from the ViewModel.
    // This is stable because we aren't clicking or typing.
    compose.onNodeWithTag(TestTags.TitleField).assertTextContains("My Pre-filled Title")
    compose.onNodeWithTag(TestTags.NoteField).assertTextContains("This is a pre-filled note.")
  }

  // In your existing EditScreenStateTest.kt file...

  @Test
  fun editScreen_whenUserInteracts_updatesViewModelAndSaves() {
    // Arrange: Set up the screen with a flag to check the onBack callback.
    var onBackCalled = false
    compose.setContent {
      MaterialTheme { EditToDoScreen(id = "123", onBack = { onBackCalled = true }) }
    }

    // Wait for the initial data to load and the UI to be idle.
    compose.waitForIdle()

    // Act 1: Simulate the user typing a new title.
    // This will execute the code inside the `onTitleChange` lambda.
    compose.onNodeWithTag(TestTags.TitleField).performTextClearance()
    compose.onNodeWithTag(TestTags.TitleField).performTextInput("Updated by test")

    // Act 2: Simulate the user clicking the save button.
    // This will execute the code inside the `onSave` lambda.
    compose.onNodeWithTag(TestTags.SaveButton).performClick()

    // Assert: Verify that the interactions worked correctly.
    // 1. The onBack callback was called.
    assertTrue("The onBack callback should have been invoked after saving.", onBackCalled)

    // 2. The repository was updated with the new title.
    val updatedItem = fakeRepo.currentList.single()
    assertEquals("Updated by test", updatedItem.title)
  }
}
