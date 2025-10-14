package com.android.sample

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.android.sample.todo.*
import com.android.sample.todo.ui.EditToDoScreen
import com.android.sample.todo.ui.TestTags
import java.time.LocalDate
import org.junit.*

class EditToDoScreenTest {
  @get:Rule val compose = createComposeRule()

  private val realRepo by lazy { ToDoRepositoryProvider.repository }
  private val fakeRepo = FakeToDoRepository()

  @Before
  fun swapRepo() {
    ToDoRepositoryProvider.repository = fakeRepo
  }

  @After
  fun restoreRepo() {
    ToDoRepositoryProvider.repository = realRepo
  }

  @OptIn(ExperimentalTestApi::class)
  @Test
  fun prefill_and_SaveUpdatesItem() =
      kotlinx.coroutines.test.runTest {
        val original =
            makeToDo(
                    id = "42",
                    title = "Original",
                    due = LocalDate.of(2025, 2, 2),
                    priority = Priority.LOW,
                    status = Status.IN_PROGRESS,
                    note = "old note",
                    notifications = true)
                .copy(links = listOf("https://a", "https://b"), location = "CO 2")
        fakeRepo.add(original)

        var back = false
        compose.setContent { MaterialTheme { EditToDoScreen(id = "42", onBack = { back = true }) } }

        // Wait for prefilled field, focus, then replace text
        compose.waitUntilExactlyOneExists(hasTestTag(TestTags.TitleField))
        compose
            .onNodeWithTag(TestTags.TitleField, useUnmergedTree = true)
            .performClick()
            .performTextReplacement("Updated")

        // Edit links & note (optional section is shown by default in Edit)
        compose.waitUntilExactlyOneExists(hasTestTag(TestTags.LinksField))
        compose
            .onNodeWithTag(TestTags.LinksField, useUnmergedTree = true)
            .performClick()
            .performTextReplacement("https://c, https://d")

        compose.waitUntilExactlyOneExists(hasTestTag(TestTags.NoteField))
        compose
            .onNodeWithTag(TestTags.NoteField, useUnmergedTree = true)
            .performClick()
            .performTextReplacement("new note")

        compose.onNodeWithTag(TestTags.SaveButton).performClick()

        val updated = fakeRepo.getById("42")!!
        Assert.assertEquals("Updated", updated.title)
        Assert.assertEquals(listOf("https://c", "https://d"), updated.links)
        Assert.assertEquals("new note", updated.note)
        Assert.assertTrue(back)
      }
}
