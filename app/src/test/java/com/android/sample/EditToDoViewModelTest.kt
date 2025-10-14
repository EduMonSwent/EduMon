package com.android.sample

import com.android.sample.todo.Priority
import com.android.sample.todo.Status
import com.android.sample.todo.ToDo
import com.android.sample.todo.ui.EditToDoViewModel
import java.time.LocalDate
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class EditToDoViewModelTest {

  @get:Rule val coroutineRule = MainCoroutineRule()

  private val testDispatcher = coroutineRule.testDispatcher

  private val existingTodo =
      ToDo(
          id = "123",
          title = "Initial Title",
          dueDate = LocalDate.of(2025, 1, 1),
          priority = Priority.LOW,
          status = Status.TODO,
          links = listOf("https://initial.com"),
          note = "Initial Note")

  @Test
  fun `init loads existing ToDo data into viewmodel state`() = runTest {
    // Arrange
    val fakeRepo = FakeToDoRepository().apply { setInitialItems(listOf(existingTodo)) }

    // Act
    val viewModel = EditToDoViewModel(fakeRepo, "123")
    testDispatcher.scheduler.advanceUntilIdle() // Let the init coroutine finish

    // Assert
    assertEquals("Initial Title", viewModel.title)
    assertEquals(LocalDate.of(2025, 1, 1), viewModel.dueDate)
    assertEquals(Priority.LOW, viewModel.priority)
    assertEquals(Status.TODO, viewModel.status)
    assertEquals("https://initial.com", viewModel.linksText) // List converted to text
    assertEquals("Initial Note", viewModel.note)
  }

  @Test
  fun `save updates repository with new data and calls onDone`() = runTest {
    // Arrange
    val fakeRepo = FakeToDoRepository().apply { setInitialItems(listOf(existingTodo)) }
    val viewModel = EditToDoViewModel(fakeRepo, "123")
    testDispatcher.scheduler.advanceUntilIdle() // Wait for init
    var onDoneCalled = false

    // Act: Modify the state
    viewModel.apply {
      title = "Updated Title"
      status = Status.DONE
      note = null
    }
    viewModel.save { onDoneCalled = true }

    // Assert
    val updatedItem = fakeRepo.getById("123")
    assertTrue(onDoneCalled)
    assertEquals("Updated Title", updatedItem?.title)
    assertEquals(Status.DONE, updatedItem?.status) // Changed
    assertEquals(null, updatedItem?.note) // Changed
    assertEquals(Priority.LOW, updatedItem?.priority) // Unchanged
  }
}
