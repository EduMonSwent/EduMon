package com.android.sample

import com.android.sample.todo.Priority
import com.android.sample.todo.Status
import com.android.sample.todo.ToDo
import com.android.sample.todo.ui.OverviewViewModel
import java.time.LocalDate
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class OverviewViewModelTest {

  @get:Rule val coroutineRule = MainCoroutineRule()

  @Test
  fun `uiState correctly sorts items`() = runTest {
    // Arrange
    val repo = FakeToDoRepository()
    val vm = OverviewViewModel(repo)
    val todos =
        listOf(
            ToDo(
                "1",
                "Task C (Done)",
                dueDate = LocalDate.of(2025, 1, 1),
                status = Status.DONE,
                priority = Priority.LOW),
            ToDo(
                "2",
                "Task A (Todo)",
                dueDate = LocalDate.of(2025, 1, 10),
                status = Status.TODO,
                priority = Priority.LOW),
            ToDo(
                "3",
                "Task B (Todo)",
                dueDate = LocalDate.of(2025, 1, 5),
                status = Status.TODO,
                priority = Priority.LOW))
    repo.setInitialItems(todos)

    // Act
    val uiState = vm.uiState.first()

    // Assert: Sorted by non-DONE first, then by earliest due date
    assertEquals("Task B (Todo)", uiState.items[0].title) // Jan 5
    assertEquals("Task A (Todo)", uiState.items[1].title) // Jan 10
    assertEquals("Task C (Done)", uiState.items[2].title) // Done items last
  }

  @Test
  fun `cycleStatus moves TODO to IN_PROGRESS`() = runTest {
    val repo = FakeToDoRepository()
    val vm = OverviewViewModel(repo)
    repo.setInitialItems(
        listOf(
            ToDo(
                "1",
                "T",
                dueDate = LocalDate.now(),
                status = Status.TODO,
                priority = Priority.LOW)))

    vm.cycleStatus("1")

    assertEquals(Status.IN_PROGRESS, repo.getById("1")?.status)
  }

  @Test
  fun `delete removes item from repository`() = runTest {
    val repo = FakeToDoRepository()
    val vm = OverviewViewModel(repo)
    repo.setInitialItems(listOf(ToDo("1", "T", dueDate = LocalDate.now(), priority = Priority.LOW)))

    vm.delete("1")

    assertTrue(repo.currentList.isEmpty())
  }
}
