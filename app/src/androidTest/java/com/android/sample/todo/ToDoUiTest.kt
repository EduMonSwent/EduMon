package com.android.sample.todo

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
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

  private fun assertHas(tag: String, useUnmergedTree: Boolean = true) {
    val nodes = compose.onAllNodesWithTag(tag, useUnmergedTree).fetchSemanticsNodes()
    assertTrue(nodes.isNotEmpty())
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

  // ==================== LOCATION TESTS – UI-ONLY, NO REPO ASSERTS ====================

  @Test
  fun addToDoScreen_locationField_exists_in_optional_section() {
    compose.setContent { AddToDoScreen(onBack = {}) }
    compose.waitForIdle()

    // Optional section hidden initially
    compose.onNodeWithTag(TestTags.LocationField).assertDoesNotExist()

    // Show optional section
    compose.onNodeWithTag(TestTags.OptionalToggle).performClick()
    compose.waitForIdle()

    // Location field should be present
    assertHas(TestTags.LocationField)
  }

  @Test
  fun addToDoScreen_canEnterLocationText_withoutCrashing() {
    compose.setContent { AddToDoScreen(onBack = {}) }
    compose.waitForIdle()

    // Show optional section
    compose.onNodeWithTag(TestTags.OptionalToggle).performClick()
    compose.waitForIdle()

    // Enter location text; if node isn't a text input this would fail
    compose.onNodeWithTag(TestTags.LocationField).performTextInput("EPFL Campus")
    compose.waitForIdle()

    // Field still exists
    compose.onNodeWithTag(TestTags.LocationField).assertExists()
  }

  @Test
  fun editToDoScreen_loadsExistingLocation_fieldPresent() {
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

    // Just ensure the location field exists when task has a location
    assertHas(TestTags.LocationField)
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
