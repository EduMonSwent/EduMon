package com.android.sample.todo

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
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
  fun addToDoScreen_saveWithLocation_createsTaskWithLocation() {
    var backCalled = false
    compose.setContent { AddToDoScreen(onBack = { backCalled = true }) }
    compose.waitForIdle()

    // Fill required fields
    compose.onNodeWithTag(TestTags.TitleField).performTextInput("Task with location")
    compose.waitForIdle()

    // Show optional and add location
    compose.onNodeWithTag(TestTags.OptionalToggle).performClick()
    compose.onNodeWithTag(TestTags.LocationField).performTextInput("Office Building")
    compose.waitForIdle()

    // Save
    compose.onNodeWithTag(TestTags.SaveButton).performClick()
    compose.waitForIdle()

    // Verify saved with location
    val saved = runBlocking { repo.todos.first().firstOrNull { it.title == "Task with location" } }
    assertNotNull(saved)
    assertEquals("Office Building", saved?.location)
    assertTrue(backCalled)
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
  fun addToDoScreen_clearLocation_savesWithNullLocation() {
    var backCalled = false
    compose.setContent { AddToDoScreen(onBack = { backCalled = true }) }
    compose.waitForIdle()

    compose.onNodeWithTag(TestTags.TitleField).performTextInput("Clear location task")
    compose.waitForIdle()

    // Show optional and add location
    compose.onNodeWithTag(TestTags.OptionalToggle).performClick()
    compose.onNodeWithTag(TestTags.LocationField).performTextInput("Initial Location")
    compose.waitForIdle()

    // Clear location
    compose.onNodeWithTag(TestTags.LocationField).performTextClearance()
    compose.waitForIdle()

    // Save
    compose.onNodeWithTag(TestTags.SaveButton).performClick()
    compose.waitForIdle()

    val saved = runBlocking { repo.todos.first().firstOrNull { it.title == "Clear location task" } }
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
  fun editToDoScreen_canUpdateLocation() {
    runBlocking {
      repo.add(
          ToDo(
              id = "location-edit-2",
              title = "Update Location Task",
              dueDate = LocalDate.now(),
              status = Status.TODO,
              priority = Priority.MEDIUM,
              location = "Old Location"))
    }

    var backCalled = false
    compose.setContent { EditToDoScreen(id = "location-edit-2", onBack = { backCalled = true }) }
    compose.waitForIdle()

    // Clear and enter new location
    compose.onNodeWithTag(TestTags.LocationField).performTextClearance()
    compose.onNodeWithTag(TestTags.LocationField).performTextInput("New Location")
    compose.waitForIdle()

    // Save
    compose.onNodeWithTag(TestTags.SaveButton).performClick()
    compose.waitForIdle()

    val updated = runBlocking { repo.getById("location-edit-2") }
    assertNotNull(updated)
    assertEquals("New Location", updated?.location)
    assertTrue(backCalled)
  }

  @Test
  fun editToDoScreen_canClearLocation() {
    runBlocking {
      repo.add(
          ToDo(
              id = "location-edit-3",
              title = "Clear Location Task",
              dueDate = LocalDate.now(),
              status = Status.TODO,
              priority = Priority.MEDIUM,
              location = "Location to Clear"))
    }

    var backCalled = false
    compose.setContent { EditToDoScreen(id = "location-edit-3", onBack = { backCalled = true }) }
    compose.waitForIdle()

    // Clear location field
    compose.onNodeWithTag(TestTags.LocationField).performTextClearance()
    compose.waitForIdle()

    // Save
    compose.onNodeWithTag(TestTags.SaveButton).performClick()
    compose.waitForIdle()

    val updated = runBlocking { repo.getById("location-edit-3") }
    assertNotNull(updated)
    assertNull(updated?.location)
    assertTrue(backCalled)
  }

  @Test
  fun editToDoScreen_preservesLocationWhenNotModified() {
    runBlocking {
      repo.add(
          ToDo(
              id = "location-edit-4",
              title = "Preserve Location Task",
              dueDate = LocalDate.now(),
              status = Status.TODO,
              priority = Priority.MEDIUM,
              location = "Should Stay"))
    }

    var backCalled = false
    compose.setContent { EditToDoScreen(id = "location-edit-4", onBack = { backCalled = true }) }
    compose.waitForIdle()

    // Modify title but not location
    compose.onNodeWithTag(TestTags.TitleField).performTextClearance()
    compose.onNodeWithTag(TestTags.TitleField).performTextInput("Modified Title")
    compose.waitForIdle()

    // Save
    compose.onNodeWithTag(TestTags.SaveButton).performClick()
    compose.waitForIdle()

    val updated = runBlocking { repo.getById("location-edit-4") }
    assertNotNull(updated)
    assertEquals("Should Stay", updated?.location)
    assertEquals("Modified Title", updated?.title)
    assertTrue(backCalled)
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
}
