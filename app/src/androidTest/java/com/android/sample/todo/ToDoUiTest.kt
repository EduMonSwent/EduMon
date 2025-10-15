package com.android.sample.todo

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.android.sample.todo.ui.components.TodoForm
import com.android.sample.ui.todo.AddToDoScreen
import com.android.sample.ui.todo.EditToDoScreen
import com.android.sample.ui.todo.OverviewScreen
import com.android.sample.ui.todo.TestTags
import com.android.sample.ui.todo.data.ToDoRepositoryLocal
import com.android.sample.ui.todo.data.ToDoRepositoryProvider
import com.android.sample.ui.todo.model.Priority
import com.android.sample.ui.todo.model.Status
import com.android.sample.ui.todo.model.ToDo
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * One file, simple tests, high coverage:
 * - TodoForm interaction (title, optional block, dropdowns, date button, switch, save)
 * - OverviewScreen empty state, list rendering, delete, card tap
 * - AddToDoScreen save flow
 * - EditToDoScreen prefill + save updates
 */
class ToDoUiSingleTest {

  @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

  private val originalRepo by lazy { ToDoRepositoryProvider.repository }
  private lateinit var repo: ToDoRepositoryLocal

  @Before
  fun setUp() {
    repo = ToDoRepositoryLocal()
    ToDoRepositoryProvider.repository = repo
  }

  @After
  fun tearDown() {
    ToDoRepositoryProvider.repository = originalRepo
  }

  // ---- TodoForm (shared) ----
  @Test
  fun todoForm_full_flow_enables_save_and_toggles_optional_and_opens_date_dialog() {
    var saved = false

    compose.setContent {
      var title by remember { mutableStateOf("") }
      var due by remember { mutableStateOf(LocalDate.of(2025, 10, 15)) }
      var priority by remember { mutableStateOf(Priority.MEDIUM) }
      var status by remember { mutableStateOf(Status.TODO) }
      var location by remember { mutableStateOf<String?>(null) }
      var links by remember { mutableStateOf("") }
      var note by remember { mutableStateOf<String?>(null) }
      var notif by remember { mutableStateOf(false) }

      TodoForm(
          titleTopBar = "Top",
          saveButtonText = "Save",
          onBack = {},
          title = title,
          onTitleChange = { title = it },
          dueDate = due,
          onDueDateChange = { due = it },
          priority = priority,
          onPriorityChange = { priority = it },
          status = status,
          onStatusChange = { status = it },
          showOptionalInitial = false,
          location = location,
          onLocationChange = { location = it },
          linksText = links,
          onLinksTextChange = { links = it },
          note = note,
          onNoteChange = { note = it },
          notificationsEnabled = notif,
          onNotificationsChange = { notif = it },
          canSave = title.isNotBlank(),
          onSave = { saved = true })
    }

    // Title typed -> Save enabled
    compose.onNodeWithTag(TestTags.TitleField).performTextInput("Task A")
    compose.onNodeWithTag(TestTags.SaveButton).assertIsEnabled()

    // Priority dropdown
    compose.onNodeWithTag(TestTags.PriorityDropdown).performClick()
    compose.onNodeWithText("HIGH").performClick()

    // Status dropdown
    compose.onNodeWithTag(TestTags.StatusDropdown).performClick()
    compose.onNodeWithText("IN PROGRESS").performClick()

    // Optional section
    compose.onNodeWithTag(TestTags.OptionalToggle).performClick()
    compose.onNodeWithTag(TestTags.LocationField).performTextInput("Office")
    compose.onNodeWithTag(TestTags.LinksField).performTextInput("https://a, https://b")
    compose.onNodeWithTag(TestTags.NoteField).performTextInput("Note")
    compose.onNodeWithTag(TestTags.NotificationsSwitch).performClick()

    // Date field shows formatted date and Change button is clickable
    val fmt = DateTimeFormatter.ofPattern("EEE, d MMM yyyy")
    compose.onNodeWithText(LocalDate.of(2025, 10, 15).format(fmt)).assertIsDisplayed()
    compose.onNodeWithTag(TestTags.ChangeDateBtn).performClick() // opens native dialog (system UI)

    // Save
    compose.onNodeWithTag(TestTags.SaveButton).performClick()
    assertTrue(saved)
  }

  // ---- OverviewScreen ----
  @Test
  fun overview_empty_state_then_delete_item_from_list() {
    // Arrange: compose once
    var addClicked = false
    compose.setContent { OverviewScreen(onAddClicked = { addClicked = true }, onEditClicked = {}) }

    // Empty state visible
    compose.onNodeWithText("No tasks yet. Tap + to add one.").assertIsDisplayed()
    compose.onNodeWithTag(TestTags.FabAdd).performClick()
    assertTrue(addClicked)

    // Add an item to the repo AFTER composition
    val t = ToDo(title = "X", dueDate = LocalDate.now(), priority = Priority.LOW)
    compose.runOnIdle {
      // suspend add inside UI-idle block
      runBlocking { repo.add(t) }
    }

    // Wait until the list shows the new item
    compose.waitUntil(timeoutMillis = 3_000) {
      compose.onAllNodesWithText("X").fetchSemanticsNodes().isNotEmpty()
    }
    compose.onNodeWithText("X").assertIsDisplayed()

    // Delete via icon button
    compose.onNodeWithTag(TestTags.delete(t.id)).performClick()

    // Wait until empty state returns
    compose.waitUntil(timeoutMillis = 3_000) {
      compose
          .onAllNodesWithText("No tasks yet. Tap + to add one.")
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    compose.onNodeWithText("No tasks yet. Tap + to add one.").assertIsDisplayed()
  }

  // ---- AddToDoScreen ----
  @Test
  fun addToDoScreen_saves_and_calls_onBack() {
    var back = false
    compose.setContent { AddToDoScreen(onBack = { back = true }) }

    // Enter only required title and save
    compose.onNodeWithTag(TestTags.TitleField).performTextInput("From Add Screen")
    compose.onNodeWithTag(TestTags.SaveButton).performClick()
    assertTrue(back)

    // Verify repository received item
    val saved = runBlocking {
      repo.todos
          .first { list -> list.any { it.title == "From Add Screen" } }
          .firstOrNull { it.title == "From Add Screen" }
    }
    assertNotNull(saved)
  }

  // ---- EditToDoScreen ----
  @Test
  fun editToDoScreen_prefills_and_updates_and_calls_onBack() {
    // Seed repo
    val id = "edit-1"
    runBlocking {
      repo.add(
          ToDo(
              id = id,
              title = "Old",
              dueDate = LocalDate.of(2025, 1, 1),
              priority = Priority.MEDIUM,
              status = Status.TODO,
              links = listOf("x", "y"),
              note = "n"))
    }

    var back = false
    compose.setContent { EditToDoScreen(id = id, onBack = { back = true }) }

    // Title is shown; change it
    compose.onNodeWithTag(TestTags.TitleField).assertIsDisplayed()
    compose.onNodeWithTag(TestTags.TitleField).performTextClearance()
    compose.onNodeWithTag(TestTags.TitleField).performTextInput("New Title")
    compose.onNodeWithTag(TestTags.SaveButton).performClick()
    assertTrue(back)

    // Verify repo updated
    val updated = runBlocking { repo.getById(id) }
    assertNotNull(updated)
    assertEquals("New Title", updated!!.title)
  }
}
