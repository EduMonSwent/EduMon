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
import com.android.sample.ui.todo.TestTags
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Before

// Longer timeout helps on CI
const val UI_WAIT_TIMEOUT = 15_000L

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

  fun sampleToDo(
      id: String = "id-${System.nanoTime()}",
      title: String = "Task",
      due: LocalDate = LocalDate.now(),
      priority: Priority = Priority.MEDIUM,
      status: Status = Status.TODO
  ) = ToDo(id = id, title = title, dueDate = due, priority = priority, status = status)

  fun ComposeTestRule.awaitDisplayed(
      tag: String,
      timeoutMs: Long = UI_WAIT_TIMEOUT
  ): SemanticsNodeInteraction {
    waitUntil(timeoutMs) { onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
    return onNodeWithTag(tag).assertIsDisplayed()
  }

  // ----- form helpers -----
  fun ComposeTestRule.enterTitle(title: String) =
      awaitDisplayed(TestTags.TitleField).performTextInput(title)

  fun ComposeTestRule.clearAndEnterTitle(title: String) {
    awaitDisplayed(TestTags.TitleField).performTextClearance()
    onNodeWithTag(TestTags.TitleField).performTextInput(title)
  }

  /** Only opens the optional section if it's currently hidden. */
  fun ComposeTestRule.openOptionalSection() {
    val links = TestTags.LinksField
    val isVisible = onAllNodesWithTag(links).fetchSemanticsNodes().isNotEmpty()
    if (!isVisible) {
      awaitDisplayed(TestTags.OptionalToggle).performClick()
      awaitDisplayed(links) // wait for fields to appear
    }
  }

  fun ComposeTestRule.enterLocation(text: String) =
      awaitDisplayed(TestTags.LocationField).performTextInput(text)

  /** Idempotent: ensures the optional block is visible first. */
  fun ComposeTestRule.enterLinks(text: String) {
    openOptionalSection()
    awaitDisplayed(TestTags.LinksField).performTextInput(text)
  }

  fun ComposeTestRule.enterNote(text: String) =
      awaitDisplayed(TestTags.NoteField).performTextInput(text)

  fun ComposeTestRule.toggleNotifications() =
      awaitDisplayed(TestTags.NotificationsSwitch).performClick()

  fun ComposeTestRule.clickSave(waitForRedirection: Boolean = false) {
    awaitDisplayed(TestTags.SaveButton).performClick()
    waitUntil(UI_WAIT_TIMEOUT) {
      !waitForRedirection || onAllNodesWithTag(TestTags.SaveButton).fetchSemanticsNodes().isEmpty()
    }
  }

  // ----- overview helpers -----
  fun ComposeTestRule.clickAddFab() = awaitDisplayed(TestTags.FabAdd).performClick()

  fun ComposeTestRule.waitUntilTodoCardShown(id: String): SemanticsNodeInteraction {
    waitUntil(UI_WAIT_TIMEOUT) {
      onAllNodesWithTag(TestTags.card(id)).fetchSemanticsNodes().isNotEmpty()
    }
    return onNodeWithTag(TestTags.card(id)).assertIsDisplayed()
  }

  fun ComposeTestRule.clickStatusChip(id: String) =
      awaitDisplayed(TestTags.status(id)).performClick()

  fun ComposeTestRule.clickDelete(id: String) = awaitDisplayed(TestTags.delete(id)).performClick()

  fun ComposeTestRule.checkTopBarTitleContains(text: String) =
      awaitDisplayed(TestTags.TitleField)
          .assertTextContains(text, substring = true, ignoreCase = true)

  // ----- tiny repo helpers -----
  fun currentTodos(): List<ToDo> =
      (repository.todos as kotlinx.coroutines.flow.MutableStateFlow<List<ToDo>>).value

  fun addAll(vararg items: ToDo) = runBlocking { items.forEach { repository.add(it) } }
}
