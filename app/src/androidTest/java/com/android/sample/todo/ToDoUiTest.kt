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
import com.android.sample.repositories.ToDoRepositoryLocal
import com.android.sample.repositories.ToDoRepositoryProvider
import com.android.sample.ui.todo.EditToDoScreen
import com.android.sample.ui.todo.OverviewScreen
import com.android.sample.ui.todo.TestTags
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** UI tests for To-Do screens. */
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

  @Test
  fun overview_empty_state_then_delete_item_from_list() {
    var addClicked = false
    compose.setContent { OverviewScreen(onAddClicked = { addClicked = true }, onEditClicked = {}) }

    // Empty state visible
    compose.onNodeWithText("No tasks yet. Tap + to add one.").assertIsDisplayed()

    // Tap FAB (just validate the callback)
    compose.onNodeWithTag(TestTags.FabAdd).performClick()
    assertTrue(addClicked)

    // Simulate another screen saving an item
    val t = ToDo(title = "X", dueDate = LocalDate.now(), priority = Priority.LOW)
    compose.runOnIdle { runBlocking { repo.add(t) } }

    // Wait for list to show it
    compose.waitUntil(timeoutMillis = 3_000) {
      compose.onAllNodesWithText("X").fetchSemanticsNodes().isNotEmpty()
    }
    compose.onNodeWithText("X").assertIsDisplayed()

    // Remove via delete icon
    compose.onNodeWithTag(TestTags.delete(t.id)).performClick()

    // Back to empty
    compose.waitUntil(timeoutMillis = 3_000) {
      compose
          .onAllNodesWithText("No tasks yet. Tap + to add one.")
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    compose.onNodeWithText("No tasks yet. Tap + to add one.").assertIsDisplayed()
  }

  // Tiny helper for tag presence assertions
  private fun assertHas(tag: String, useUnmergedTree: Boolean = true) {
    val nodes = compose.onAllNodesWithTag(tag, useUnmergedTree).fetchSemanticsNodes()
    assertTrue(nodes.isNotEmpty())
  }

  @Test
  fun editToDoScreen_renders_core_components() {
    // Seed repo with a known item
    val repo = ToDoRepositoryProvider.repository
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
  fun editToDoScreen_edits_optional_fields_and_saves_invoking_onBack() {
    // Seed item so EditToDoScreen loads it
    runBlocking {
      repo.add(
          ToDo(
              id = "42",
              title = "Seed",
              dueDate = LocalDate.now(),
              status = Status.TODO,
              priority = Priority.LOW))
    }

    var wentBack = false
    compose.setContent { EditToDoScreen(id = "42", onBack = { wentBack = true }) }

    // Change required + optional fields (exercises onXChange lambdas)
    compose.onNodeWithTag(TestTags.TitleField).performTextInput(" edited")
    compose.onNodeWithTag(TestTags.LocationField).performTextInput("Room 101")
    compose.onNodeWithTag(TestTags.NoteField).performTextInput("Bring snacks")
    compose.onNodeWithTag(TestTags.NotificationsSwitch).performClick()

    // Save -> vm.save(onBack) should call onBack (async)
    compose.onNodeWithTag(TestTags.SaveButton).performClick()
    compose.waitUntil(timeoutMillis = 5_000) { wentBack }
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
}
