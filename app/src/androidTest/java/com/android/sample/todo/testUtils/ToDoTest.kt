package com.android.sample.todo.testutils

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.android.sample.todo.Priority
import com.android.sample.todo.Status
import com.android.sample.todo.ToDo
import com.android.sample.todo.ToDoRepository
import com.android.sample.todo.ToDoRepositoryLocal
import com.android.sample.todo.ToDoRepositoryProvider
import com.android.sample.todo.ui.TestTags
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Before

const val UI_WAIT_TIMEOUT = 5_000L

/** Test helpers. */
interface ToDoTest {

  // ----- repo wiring -----
  fun createInitializedRepository(): ToDoRepository = ToDoRepositoryLocal()

  val repository: ToDoRepository
    get() = ToDoRepositoryProvider.repository

  @Before
  fun setUpProvider() {
    ToDoRepositoryProvider.repository = createInitializedRepository()
  }

  // ----- top-level model factory (Option A: no companion) -----
  fun sampleToDo(
      id: String = "id-${System.nanoTime()}",
      title: String = "Task",
      due: LocalDate = LocalDate.now(),
      priority: Priority = Priority.MEDIUM,
      status: Status = Status.TODO
  ) = ToDo(id = id, title = title, dueDate = due, priority = priority, status = status)

  // ----- form helpers -----
  fun ComposeTestRule.enterTitle(title: String) =
      onNodeWithTag(TestTags.TitleField).performTextInput(title)

  fun ComposeTestRule.clearAndEnterTitle(title: String) {
    onNodeWithTag(TestTags.TitleField).performTextClearance()
    onNodeWithTag(TestTags.TitleField).performTextInput(title)
  }

  fun ComposeTestRule.openOptionalSection() =
      onNodeWithTag(TestTags.OptionalToggle).assertIsDisplayed().performClick()

  fun ComposeTestRule.enterLocation(text: String) =
      onNodeWithTag(TestTags.LocationField).performTextInput(text)

  fun ComposeTestRule.enterLinks(text: String) =
      onNodeWithTag(TestTags.LinksField).performTextInput(text)

  fun ComposeTestRule.enterNote(text: String) =
      onNodeWithTag(TestTags.NoteField).performTextInput(text)

  fun ComposeTestRule.toggleNotifications() =
      onNodeWithTag(TestTags.NotificationsSwitch).performClick()

  fun ComposeTestRule.clickSave(waitForRedirection: Boolean = false) {
    onNodeWithTag(TestTags.SaveButton).assertIsDisplayed().performClick()
    waitUntil(UI_WAIT_TIMEOUT) {
      !waitForRedirection || onAllNodesWithTag(TestTags.SaveButton).fetchSemanticsNodes().isEmpty()
    }
  }

  // ----- overview helpers -----
  fun ComposeTestRule.clickAddFab() =
      onNodeWithTag(TestTags.FabAdd).assertIsDisplayed().performClick()

  fun ComposeTestRule.waitUntilTodoCardShown(id: String): SemanticsNodeInteraction {
    waitUntil(UI_WAIT_TIMEOUT) {
      onAllNodesWithTag(TestTags.card(id)).fetchSemanticsNodes().isNotEmpty()
    }
    return onNodeWithTag(TestTags.card(id)).assertIsDisplayed()
  }

  fun ComposeTestRule.clickStatusChip(id: String) =
      onNodeWithTag(TestTags.status(id)).assertIsDisplayed().performClick()

  fun ComposeTestRule.clickDelete(id: String) =
      onNodeWithTag(TestTags.delete(id)).assertIsDisplayed().performClick()

  fun ComposeTestRule.checkTopBarTitleContains(text: String) =
      onNodeWithTag(TestTags.TitleField)
          .assertIsDisplayed()
          .assertTextContains(text, substring = true, ignoreCase = true)

  // ----- tiny repo helpers -----
  fun currentTodos(): List<ToDo> =
      (repository.todos as kotlinx.coroutines.flow.MutableStateFlow<List<ToDo>>).value

  fun addAll(vararg items: ToDo) = runBlocking { items.forEach { repository.add(it) } }
}
