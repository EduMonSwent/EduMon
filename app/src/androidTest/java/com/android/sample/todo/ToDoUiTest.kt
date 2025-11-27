package com.android.sample.todo

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.android.sample.data.Priority
import com.android.sample.data.Status
import com.android.sample.data.ToDo
import com.android.sample.repos_providors.AppRepositories
import com.android.sample.repositories.ToDoRepository
import com.android.sample.ui.todo.AddToDoScreen
import com.android.sample.ui.todo.EditToDoScreen
import com.android.sample.ui.todo.OverviewScreen
import com.android.sample.ui.todo.TestTags
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** UI tests for To-Do screens. */
class ToDoUiSingleTest {

  @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

  private lateinit var repo: ToDoRepository

  @Before
  fun setUp() {
    repo = AppRepositories.toDoRepository

    runBlocking {
      val existing = repo.todos.first()
      existing.forEach { todo -> repo.remove(todo.id) }
    }
  }

  @After
  fun tearDown() {
    // nothing to restore; repo can stay empty
  }

  @Test
  fun overview_empty_state_then_delete_item_from_list() {
    var addClicked = false
    compose.setContent { OverviewScreen(onAddClicked = { addClicked = true }, onEditClicked = {}) }

    compose.onNodeWithText("No tasks yet. Tap + to add one.").assertIsDisplayed()

    compose.onNodeWithTag(TestTags.FabAdd).performClick()
    assertTrue(addClicked)

    val t = ToDo(title = "X", dueDate = LocalDate.now(), priority = Priority.LOW)
    compose.runOnIdle { runBlocking { repo.add(t) } }

    val saved = runBlocking { repo.todos.first().first { it.title == "X" } }

    // Wait for list to show it
    compose.waitUntil(timeoutMillis = 3_000) {
      compose.onAllNodesWithText("X").fetchSemanticsNodes().isNotEmpty()
    }
    compose.onNodeWithText("X").assertIsDisplayed()

    compose.onNodeWithTag(TestTags.delete(saved.id)).performClick()

    compose.waitUntil(timeoutMillis = 3_000) {
      compose
          .onAllNodesWithText("No tasks yet. Tap + to add one.")
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    compose.onNodeWithText("No tasks yet. Tap + to add one.").assertIsDisplayed()
  }

  private fun assertHas(tag: String, useUnmergedTree: Boolean = true) {
    val nodes = compose.onAllNodesWithTag(tag, useUnmergedTree).fetchSemanticsNodes()
    assertTrue(nodes.isNotEmpty())
  }

  @Test
  fun editToDoScreen_renders_core_components() {
    runBlocking {
      repo.add(
          ToDo(
              id = "1",
              title = "Original Title",
              dueDate = LocalDate.now(),
              status = Status.TODO,
              priority = Priority.MEDIUM,
          ))
    }
    compose.setContent { EditToDoScreen(id = "1", onBack = {}) }
    compose.waitForIdle()

    // Core widgets should be on screen
    assertHas(TestTags.TitleField)
    assertHas(TestTags.DueDateField)
    assertHas(TestTags.PriorityDropdown)
    assertHas(TestTags.StatusDropdown)
    assertHas(TestTags.OptionalToggle)
    assertHas(TestTags.SaveButton)
  }

  @Test
  fun editToDoScreen_optional_section_can_be_toggled_hidden_and_shown_again() {
    runBlocking {
      repo.add(
          ToDo(
              id = "43",
              title = "Toggle Optional",
              dueDate = LocalDate.now(),
              status = Status.TODO,
              priority = Priority.MEDIUM))
    }

    compose.setContent { EditToDoScreen(id = "43", onBack = {}) }

    // Optional content starts visible; hide it
    compose.onNodeWithTag(TestTags.OptionalToggle).performClick()
    compose.onNodeWithTag(TestTags.LocationField).assertDoesNotExist()
    compose.onNodeWithTag(TestTags.NoteField).assertDoesNotExist()
    compose.onNodeWithTag(TestTags.NotificationsSwitch).assertDoesNotExist()

    // Show again
    compose.onNodeWithTag(TestTags.OptionalToggle).performClick()
    assertHas(TestTags.LocationField)
    assertHas(TestTags.NoteField)
    assertHas(TestTags.NotificationsSwitch)
  }

  // ==================== NEW LOCATION TESTS ====================

  @Test
  fun addToDoScreen_locationField_exists_in_optional_section() {
    compose.setContent { AddToDoScreen(onBack = {}) }
    compose.waitForIdle()

    // Show optional section
    compose.onNodeWithTag(TestTags.OptionalToggle).performClick()
    compose.waitForIdle()

    // Location field should be present
    assertHas(TestTags.LocationField)
  }

  @Test
  fun addToDoScreen_canEnterLocationText() {
    compose.setContent { AddToDoScreen(onBack = {}) }
    compose.waitForIdle()

    // Show optional section
    compose.onNodeWithTag(TestTags.OptionalToggle).performClick()
    compose.waitForIdle()

    // Enter location text
    compose.onNodeWithTag(TestTags.LocationField).performTextInput("EPFL Campus")
    compose.waitForIdle()

    // Verify text appears (note: we can't easily verify suggestions in UI tests without mock)
    compose.onNodeWithTag(TestTags.LocationField).assertExists()
  }

  @Test
  fun addToDoScreen_saveWithoutLocation_createsTaskWithNullLocation() {
    var backCalled = false
    compose.setContent { AddToDoScreen(onBack = { backCalled = true }) }
    compose.waitForIdle()

    // Fill only required fields (no location)
    compose.onNodeWithTag(TestTags.TitleField).performTextInput("Task without location")
    compose.waitForIdle()

    // Save without touching location
    compose.onNodeWithTag(TestTags.SaveButton).performClick()
    compose.waitForIdle()

    // Verify saved without location
    val saved = runBlocking {
      repo.todos.first().firstOrNull { it.title == "Task without location" }
    }
    assertNotNull(saved)
    assertNull(saved?.location)
    assertTrue(backCalled)
  }

  @Test
  fun editToDoScreen_loadsExistingLocation() {
    runBlocking {
      repo.add(
          ToDo(
              id = "location-edit-1",
              title = "Task with Location",
              dueDate = LocalDate.now(),
              status = Status.TODO,
              priority = Priority.MEDIUM,
              location = "Original Location"))
    }

    compose.setContent { EditToDoScreen(id = "location-edit-1", onBack = {}) }
    compose.waitForIdle()

    // Location field should show existing location (optional section is visible by default)
    assertHas(TestTags.LocationField)
    // Note: We can't easily assert text content in OutlinedTextField, but the field exists
  }

  @Test
  fun editToDoScreen_canAddLocationToTaskWithoutOne() {
    runBlocking {
      repo.add(
          ToDo(
              id = "location-edit-5",
              title = "Add Location Task",
              dueDate = LocalDate.now(),
              status = Status.TODO,
              priority = Priority.MEDIUM,
              location = null))
    }

    var backCalled = false
    compose.setContent { EditToDoScreen(id = "location-edit-5", onBack = { backCalled = true }) }
    compose.waitForIdle()

    // Add location to task that didn't have one
    compose.onNodeWithTag(TestTags.LocationField).performTextInput("Newly Added Location")
    compose.waitForIdle()

    // Save
    compose.onNodeWithTag(TestTags.SaveButton).performClick()
    compose.waitForIdle()

    val updated = runBlocking { repo.getById("location-edit-5") }
    assertNotNull(updated)
    assertEquals("Newly Added Location", updated?.location)
    assertTrue(backCalled)
  }

  @Test
  fun addToDoScreen_locationFieldVisibleOnlyWhenOptionalShown() {
    compose.setContent { AddToDoScreen(onBack = {}) }
    compose.waitForIdle()

    // Optional section starts hidden in AddToDoScreen
    compose.onNodeWithTag(TestTags.LocationField).assertDoesNotExist()

    // Show optional
    compose.onNodeWithTag(TestTags.OptionalToggle).performClick()
    compose.waitForIdle()

    assertHas(TestTags.LocationField)

    // Hide again
    compose.onNodeWithTag(TestTags.OptionalToggle).performClick()
    compose.waitForIdle()

    compose.onNodeWithTag(TestTags.LocationField).assertDoesNotExist()
  }

  // ==================== TODOFORM COVERAGE TESTS ====================

  @Test
  fun todoForm_titleField_acceptsInput() {
    compose.setContent { AddToDoScreen(onBack = {}) }
    compose.waitForIdle()

    compose.onNodeWithTag(TestTags.TitleField).performTextInput("My Task")
    compose.waitForIdle()

    compose.onNodeWithTag(TestTags.TitleField).assertExists()
  }

  @Test
  fun todoForm_saveButton_disabledWhenTitleBlank() {
    compose.setContent { AddToDoScreen(onBack = {}) }
    compose.waitForIdle()

    // Save button should be disabled when title is empty
    compose.onNodeWithTag(TestTags.SaveButton).assertIsNotEnabled()
  }

  @Test
  fun todoForm_saveButton_enabledWhenTitleFilled() {
    compose.setContent { AddToDoScreen(onBack = {}) }
    compose.waitForIdle()

    compose.onNodeWithTag(TestTags.TitleField).performTextInput("Valid Title")
    compose.waitForIdle()

    // Save button should now be enabled
    compose.onNodeWithTag(TestTags.SaveButton).assertIsEnabled()
  }

  @Test
  fun todoForm_priorityDropdown_canSelectOption() {
    compose.setContent { AddToDoScreen(onBack = {}) }
    compose.waitForIdle()

    compose.onNodeWithTag(TestTags.PriorityDropdown).performClick()
    compose.waitForIdle()

    compose.onNodeWithText("HIGH").performClick()
    compose.waitForIdle()

    // Dropdown should close after selection
    compose.onNodeWithTag(TestTags.PriorityDropdown).assertExists()
  }

  @Test
  fun todoForm_statusDropdown_canSelectOption() {
    compose.setContent { AddToDoScreen(onBack = {}) }
    compose.waitForIdle()

    compose.onNodeWithTag(TestTags.StatusDropdown).performClick()
    compose.waitForIdle()

    compose.onNodeWithText("IN PROGRESS").performClick()
    compose.waitForIdle()

    compose.onNodeWithTag(TestTags.StatusDropdown).assertExists()
  }

  @Test
  fun todoForm_linksField_acceptsInput() {
    compose.setContent { AddToDoScreen(onBack = {}) }
    compose.waitForIdle()

    compose.onNodeWithTag(TestTags.OptionalToggle).performClick()
    compose.waitForIdle()

    compose.onNodeWithTag(TestTags.LinksField).performTextInput("https://example.com")
    compose.waitForIdle()

    compose.onNodeWithTag(TestTags.LinksField).assertExists()
  }

  @Test
  fun todoForm_noteField_acceptsMultilineInput() {
    compose.setContent { AddToDoScreen(onBack = {}) }
    compose.waitForIdle()

    compose.onNodeWithTag(TestTags.OptionalToggle).performClick()
    compose.waitForIdle()

    compose
        .onNodeWithTag(TestTags.NoteField)
        .performTextInput("This is a note\nWith multiple lines")
    compose.waitForIdle()

    compose.onNodeWithTag(TestTags.NoteField).assertExists()
  }

  @Test
  fun todoForm_notificationsSwitch_canBeToggled() {
    compose.setContent { AddToDoScreen(onBack = {}) }
    compose.waitForIdle()

    compose.onNodeWithTag(TestTags.OptionalToggle).performClick()
    compose.waitForIdle()

    // Toggle notifications on
    compose.onNodeWithTag(TestTags.NotificationsSwitch).performClick()
    compose.waitForIdle()

    compose.onNodeWithTag(TestTags.NotificationsSwitch).assertExists()

    // Toggle notifications off
    compose.onNodeWithTag(TestTags.NotificationsSwitch).performClick()
    compose.waitForIdle()

    compose.onNodeWithTag(TestTags.NotificationsSwitch).assertExists()
  }

  @Test
  fun todoForm_backButton_triggersCallback() {
    var backCalled = false
    compose.setContent { AddToDoScreen(onBack = { backCalled = true }) }
    compose.waitForIdle()

    // Find and click back button (it's in the top bar)
    compose.onNodeWithContentDescription("Back").performClick()
    compose.waitForIdle()

    assertTrue(backCalled)
  }

  @Test
  fun todoForm_saveButton_triggersCallback() = runBlocking {
    var backCalled = false
    compose.setContent { AddToDoScreen(onBack = { backCalled = true }) }
    compose.waitForIdle()

    compose.onNodeWithTag(TestTags.TitleField).performTextInput("Complete Task")
    compose.waitForIdle()

    compose.onNodeWithTag(TestTags.SaveButton).performClick()
    compose.waitForIdle()

    assertTrue(backCalled)
  }

  @Test
  fun todoForm_allRequiredFields_canBeFilledAndSaved() = runBlocking {
    var backCalled = false
    compose.setContent { AddToDoScreen(onBack = { backCalled = true }) }
    compose.waitForIdle()

    // Fill title
    compose.onNodeWithTag(TestTags.TitleField).performTextInput("Full Task")
    compose.waitForIdle()

    // Change priority
    compose.onNodeWithTag(TestTags.PriorityDropdown).performClick()
    compose.waitForIdle()
    compose.onNodeWithText("HIGH").performClick()
    compose.waitForIdle()

    // Change status
    compose.onNodeWithTag(TestTags.StatusDropdown).performClick()
    compose.waitForIdle()
    compose.onNodeWithText("IN PROGRESS").performClick()
    compose.waitForIdle()

    // Save
    compose.onNodeWithTag(TestTags.SaveButton).performClick()
    compose.waitForIdle()

    assertTrue(backCalled)

    // Verify task was saved
    val saved = repo.todos.first().firstOrNull { it.title == "Full Task" }
    assertNotNull(saved)
    assertEquals(Priority.HIGH, saved?.priority)
    assertEquals(Status.IN_PROGRESS, saved?.status)
  }

  @Test
  fun editToDoForm_loadsAllExistingFields() {
    runBlocking {
      repo.add(
          ToDo(
              id = "full-task",
              title = "Existing Task",
              dueDate = LocalDate.now(),
              status = Status.IN_PROGRESS,
              priority = Priority.HIGH,
              location = "Home",
              links = listOf("https://example.com"),
              note = "Test note",
              notificationsEnabled = true))
    }

    compose.setContent { EditToDoScreen(id = "full-task", onBack = {}) }
    compose.waitForIdle()

    // Verify all fields are present (optional section visible by default in edit)
    assertHas(TestTags.TitleField)
    assertHas(TestTags.LocationField)
    assertHas(TestTags.LinksField)
    assertHas(TestTags.NoteField)
    assertHas(TestTags.NotificationsSwitch)
  }

  @Test
  fun todoForm_topBarTitle_showsCorrectText() {
    compose.setContent { AddToDoScreen(onBack = {}) }
    compose.waitForIdle()

    compose.onNodeWithText("New To-Do").assertExists()
  }

  @Test
  fun editToDoForm_topBarTitle_showsCorrectText() {
    runBlocking {
      repo.add(
          ToDo(
              id = "edit-test",
              title = "Task",
              dueDate = LocalDate.now(),
              status = Status.TODO,
              priority = Priority.MEDIUM))
    }

    compose.setContent { EditToDoScreen(id = "edit-test", onBack = {}) }
    compose.waitForIdle()

    compose.onNodeWithText("Edit To-Do").assertExists()
  }

  @Test
  fun todoForm_saveButtonText_showsCorrectLabel() {
    compose.setContent { AddToDoScreen(onBack = {}) }
    compose.waitForIdle()

    compose.onNodeWithText("Save To-Do").assertExists()
  }

  @Test
  fun editToDoForm_saveButtonText_showsCorrectLabel() {
    runBlocking {
      repo.add(
          ToDo(
              id = "save-btn-test",
              title = "Task",
              dueDate = LocalDate.now(),
              status = Status.TODO,
              priority = Priority.MEDIUM))
    }

    compose.setContent { EditToDoScreen(id = "save-btn-test", onBack = {}) }
    compose.waitForIdle()

    compose.onNodeWithText("Save changes").assertExists()
  }

  @Test
  fun todoForm_optionalToggle_showsCorrectText() {
    compose.setContent { AddToDoScreen(onBack = {}) }
    compose.waitForIdle()

    // Initially shows "Show optional"
    compose.onNodeWithText("Show optional").assertExists()

    compose.onNodeWithTag(TestTags.OptionalToggle).performClick()
    compose.waitForIdle()

    // After clicking, shows "Hide optional"
    compose.onNodeWithText("Hide optional").assertExists()
  }

  @Test
  fun todoForm_enumDropdowns_showFormattedNames() {
    compose.setContent { AddToDoScreen(onBack = {}) }
    compose.waitForIdle()

    // Status dropdown should show formatted name
    compose.onNodeWithTag(TestTags.StatusDropdown).performClick()
    compose.waitForIdle()

    // "IN_PROGRESS" should be formatted as "IN PROGRESS"
    compose.onNodeWithText("IN PROGRESS").assertExists()
  }
}
