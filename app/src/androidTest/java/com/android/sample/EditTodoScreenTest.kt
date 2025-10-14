package com.android.sample

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.android.sample.todo.Priority
import com.android.sample.todo.ToDo
import com.android.sample.todo.ToDoRepositoryProvider
import com.android.sample.todo.ui.EditToDoScreen
import com.android.sample.todo.ui.TestTags
import java.time.LocalDate
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
}
