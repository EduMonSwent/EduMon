package com.android.sample.todo.testutils

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
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

/** Generous timeout for CI/emulator. */
const val UI_WAIT_TIMEOUT = 15_000L

/** Test helpers shared by all UI tests (connected). */
interface ToDoTest {

  // ----- repo wiring -----
  fun createInitializedRepository(): ToDoRepository = ToDoRepositoryLocal()

  val repository: ToDoRepository
    get() = ToDoRepositoryProvider.repository

  @Before
  fun setUpProvider() {
    // Fresh singleton per test to avoid cross-test bleed on device
    ToDoRepositoryProvider.repository = createInitializedRepository()
  }

  // ----- tiny model factory (no companion needed) -----
  fun sampleToDo(
      id: String = "id-${System.nanoTime()}",
      title: String = "Task",
      due: LocalDate = LocalDate.now(),
      priority: Priority = Priority.MEDIUM,
      status: Status = Status.TODO
  ) = ToDo(id = id, title = title, dueDate = due, priority = priority, status = status)

  // ----- robust node wait -----
  fun ComposeTestRule.awaitDisplayed(tag: String): SemanticsNodeInteraction {
    waitUntil(UI_WAIT_TIMEOUT) { onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
    return onNodeWithTag(tag).assertExists().assertIsDisplayed()
  }

  // ----- repo snapshot & waits -----
  fun currentTodos(): List<ToDo> =
      (repository.todos as kotlinx.coroutines.flow.MutableStateFlow<List<ToDo>>).value

  fun ComposeTestRule.waitUntilRepo(
      timeoutMs: Long = UI_WAIT_TIMEOUT,
      predicate: (List<ToDo>) -> Boolean
  ) {
    waitUntil(timeoutMs) { predicate(currentTodos()) }
  }

  fun ComposeTestRule.waitRepoSize(size: Int, timeoutMs: Long = UI_WAIT_TIMEOUT) =
      waitUntilRepo(timeoutMs) { it.size == size }

  // ----- form helpers (scroll-safe & idempotent where needed) -----
  fun ComposeTestRule.enterTitle(title: String) {
    awaitDisplayed(TestTags.TitleField).performTextInput(title)
  }

  fun ComposeTestRule.clearAndEnterTitle(title: String) {
    awaitDisplayed(TestTags.TitleField).performTextClearance()
    awaitDisplayed(TestTags.TitleField).performTextInput(title)
  }

  /** Ensures optional section is open, but is idempotent if already open. */
  fun ComposeTestRule.openOptionalSection() {
    // If Links field already exists, we assume optional is open
    val alreadyOpen = onAllNodesWithTag(TestTags.LinksField).fetchSemanticsNodes().isNotEmpty()
    if (!alreadyOpen) {
      awaitDisplayed(TestTags.OptionalToggle).performClick()
      // wait until any optional field appears
      waitUntil(UI_WAIT_TIMEOUT) {
        onAllNodesWithTag(TestTags.LinksField).fetchSemanticsNodes().isNotEmpty() ||
            onAllNodesWithTag(TestTags.LocationField).fetchSemanticsNodes().isNotEmpty() ||
            onAllNodesWithTag(TestTags.NoteField).fetchSemanticsNodes().isNotEmpty()
      }
    }
  }

  fun ComposeTestRule.enterLocation(text: String) {
    openOptionalSection()
    awaitDisplayed(TestTags.LocationField).performTextInput(text)
  }

  fun ComposeTestRule.enterLinks(text: String) {
    openOptionalSection()
    awaitDisplayed(TestTags.LinksField).performTextInput(text)
  }

  fun ComposeTestRule.enterNote(text: String) {
    openOptionalSection()
    awaitDisplayed(TestTags.NoteField).performTextInput(text)
  }

  fun ComposeTestRule.toggleNotifications() {
    openOptionalSection()
    awaitDisplayed(TestTags.NotificationsSwitch).performClick()
  }

  /** Clicks Save and waits either for navigation or repo growth. */
  fun ComposeTestRule.clickSaveAndAwait(expectSizeAfter: Int? = null) {
    awaitDisplayed(TestTags.SaveButton).performClick()
    waitUntil(UI_WAIT_TIMEOUT) {
      // Save button might disappear on navigateBack, or repo size might grow
      onAllNodesWithTag(TestTags.SaveButton).fetchSemanticsNodes().isEmpty() ||
          (expectSizeAfter != null && currentTodos().size >= expectSizeAfter)
    }
    runOnIdle {} // yield a frame
  }

  // ----- overview helpers -----
  fun ComposeTestRule.waitUntilTodoCardShown(id: String): SemanticsNodeInteraction {
    waitUntil(UI_WAIT_TIMEOUT) {
      onAllNodesWithTag(TestTags.card(id)).fetchSemanticsNodes().isNotEmpty()
    }
    return onNodeWithTag(TestTags.card(id)).assertExists().assertIsDisplayed()
  }

  fun ComposeTestRule.clickStatusChipAndAwait(id: String) {
    awaitDisplayed(TestTags.status(id)).performClick()
    runOnIdle {} // allow VM to push update
  }

  fun ComposeTestRule.clickDeleteAndAwait(id: String) {
    awaitDisplayed(TestTags.delete(id)).performClick()
    waitUntilRepo { list -> list.none { it.id == id } }
  }

  // ----- tiny repo helper -----
  fun addAll(vararg items: ToDo) = runBlocking { items.forEach { repository.add(it) } }
}
