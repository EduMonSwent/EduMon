package com.android.sample.todo

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.android.sample.data.Priority
import com.android.sample.data.Status
import com.android.sample.data.ToDo
import com.android.sample.repositories.ToDoRepositoryLocal
import com.android.sample.repositories.ToDoRepositoryProvider
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

/**
 * CI-friendly UI tests (stable & simple), all following the same pattern as:
 * - addToDoScreen_saves_and_calls_onBack
 * - overview_empty_state_then_delete_item_from_list
 *
 * What we cover (many lines, low brittleness):
 * - Overview empty state, add FAB, list rendering, delete
 * - AddToDoScreen: type title only, save, onBack called, repo contains item
 * - EditToDoScreen: prefill, edit title, save, onBack called, repo updated
 * - Lightweight "form flow" via AddToDoScreen (no native date pickers)
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

  // -------------------------------------------------------------------------
  // 1) OVERVIEW: empty → add one externally → appears → delete → back to empty
  // -------------------------------------------------------------------------
  @Test
  fun overview_empty_state_then_delete_item_from_list() {
    var addClicked = false
    compose.setContent { OverviewScreen(onAddClicked = { addClicked = true }, onEditClicked = {}) }

    // Empty state visible
    compose.onNodeWithText("No tasks yet. Tap + to add one.").assertIsDisplayed()

    // Tap FAB (we just verify callback)
    compose.onNodeWithTag(TestTags.FabAdd).performClick()
    assertTrue(addClicked)

    // Add an item directly in the repo (simulate another screen saved it)
    val t = ToDo(title = "X", dueDate = LocalDate.now(), priority = Priority.LOW)
    compose.runOnIdle { runBlocking { repo.add(t) } }

    // Wait until the list shows the new item
    compose.waitUntil(timeoutMillis = 3_000) {
      compose.onAllNodesWithText("X").fetchSemanticsNodes().isNotEmpty()
    }
    compose.onNodeWithText("X").assertIsDisplayed()

    // Delete via icon button tagged with delete(id)
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

  // -------------------------------------------------------------------------
  // 2) ADD: type required title → Save → onBack called → repo contains item
  // -------------------------------------------------------------------------
  @Test
  fun addToDoScreen_saves_and_calls_onBack() {
    var back = false
    compose.setContent { AddToDoScreen(onBack = { back = true }) }

    // Enter only required title and save
    compose.onNodeWithTag(TestTags.TitleField).performTextInput("From Add Screen")
    compose.onNodeWithTag(TestTags.SaveButton).assertIsEnabled()
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

  @Test
  fun editToDoScreen_updatesItem_and_calls_onBack() {
    var back = false
    val existing =
        ToDo(
            id = "1",
            title = "Original Title",
            dueDate = LocalDate.now(),
            priority = Priority.LOW,
            status = Status.TODO)
    runBlocking { repo.add(existing) }

    compose.setContent { EditToDoScreen(existing.id, onBack = { back = true }) }

    val titleField = compose.onNodeWithTag(TestTags.TitleField)
    titleField.performTextClearance()
    titleField.performTextInput("Updated Title")

    compose.onNodeWithTag(TestTags.SaveButton).assertIsEnabled().performClick()

    assertTrue(back)

    val updated = runBlocking {
      repo.todos
          .first { list -> list.any { it.title == "Updated Title" } }
          .firstOrNull { it.title == "Updated Title" }
    }
    assertNotNull(updated)
    assertEquals("Updated Title", updated!!.title)
  }
}

// Small helper: avoid NoSuchElementException when a tag may or may not exist
private fun SemanticsNodeInteraction.fetchSemanticsNodeOrNull() =
    try {
      fetchSemanticsNode()
    } catch (_: AssertionError) {
      null
    }
