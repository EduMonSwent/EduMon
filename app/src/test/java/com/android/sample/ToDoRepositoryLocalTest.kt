package com.android.sample

import com.android.sample.todo.Priority
import com.android.sample.todo.Status
import com.android.sample.todo.ToDo
import com.android.sample.todo.ToDoRepositoryLocal
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test

class ToDoRepositoryLocalTest {

  private val repo = ToDoRepositoryLocal()

  @Test
  fun add_insertsTodo() = runTest {
    val todo =
        ToDo(
            title = "Task 1",
            dueDate = LocalDate.now(),
            priority = Priority.LOW,
            status = Status.TODO)
    repo.add(todo)
    val list = repo.todos.first()
    Assert.assertTrue(list.any { it.id == todo.id })
  }

  @Test
  fun update_changesExistingTodo() = runTest {
    val todo =
        ToDo(
            title = "Task",
            dueDate = LocalDate.now(),
            priority = Priority.MEDIUM,
            status = Status.TODO)
    repo.add(todo)
    val updated = todo.copy(title = "Updated")
    repo.update(updated)
    Assert.assertEquals("Updated", repo.getById(todo.id)?.title)
  }

  @Test
  fun remove_deletesTodo() = runTest {
    val todo =
        ToDo(
            title = "Task",
            dueDate = LocalDate.now(),
            priority = Priority.MEDIUM,
            status = Status.TODO)
    repo.add(todo)
    repo.remove(todo.id)
    Assert.assertNull(repo.getById(todo.id))
  }

  @Test
  fun getById_returnsCorrectTodo() = runTest {
    val todo =
        ToDo(title = "X", dueDate = LocalDate.now(), priority = Priority.HIGH, status = Status.TODO)
    repo.add(todo)
    val result = repo.getById(todo.id)
    Assert.assertEquals(todo.id, result?.id)
  }
}
