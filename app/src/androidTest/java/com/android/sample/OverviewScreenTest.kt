package com.android.sample

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.android.sample.todo.Priority
import com.android.sample.todo.ToDo
import com.android.sample.todo.ToDoRepositoryProvider
import com.android.sample.todo.ui.OverviewScreen
import com.android.sample.todo.ui.TestTags
import java.time.LocalDate
import junit.framework.TestCase.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class OverviewScreenTest {

  @get:Rule val compose = createComposeRule()

  @get:Rule val coroutineRule = MainCoroutineRule()

  private lateinit var fakeRepo: FakeToDoRepository

  @Before
  fun setUp() {
    compose.mainClock.autoAdvance = false // Disable animations for stability
    fakeRepo = FakeToDoRepository()
    ToDoRepositoryProvider.repository = fakeRepo
  }

  @Test
  fun whenScreenHasItems_displaysListOfToDos() {
    // Arrange
    val testItems =
        listOf(
            ToDo(
                "1",
                "Task 1",
                dueDate = LocalDate.now(),
                note = "With note",
                priority = Priority.HIGH),
            ToDo("2", "Task 2", dueDate = LocalDate.now(), priority = Priority.LOW) // No note
            )
    fakeRepo.setInitialItems(testItems)

    // Act
    compose.setContent { MaterialTheme { OverviewScreen(onAddClicked = {}, onEditClicked = {}) } }
    compose.mainClock.advanceTimeByFrame()
    compose.waitForIdle()

    // Assert
    compose.onNodeWithTag(TestTags.card("1")).assertIsDisplayed()
    compose.onNodeWithText("Task 1").assertIsDisplayed()
    compose.onNodeWithText("With note").assertIsDisplayed() // Covers the `if (!item.note...` branch

    compose.onNodeWithTag(TestTags.card("2")).assertIsDisplayed()
    compose.onNodeWithText("Task 2").assertIsDisplayed()
  }

  @Test
  fun whenScreenHasNoItems_displaysEmptyStateMessage() {
    // Arrange: Repository is empty by default

    // Act
    compose.setContent { MaterialTheme { OverviewScreen(onAddClicked = {}, onEditClicked = {}) } }
    compose.mainClock.advanceTimeByFrame()
    compose.waitForIdle()

    // Assert: Covers the `if (state.items.isEmpty())` branch
    compose.onNodeWithText("No tasks yet. Tap + to add one.").assertIsDisplayed()
    compose.onNodeWithTag(TestTags.List).assertDoesNotExist()
  }

  @Test
  fun clickingFab_callsOnAddClicked() {
    var addClicked = false

    compose.setContent {
      MaterialTheme { OverviewScreen(onAddClicked = { addClicked = true }, onEditClicked = {}) }
    }
    compose.mainClock.advanceTimeByFrame()
    compose.waitForIdle()

    compose.onNodeWithTag(TestTags.FabAdd).performClick()

    assertTrue(addClicked)
  }
}
