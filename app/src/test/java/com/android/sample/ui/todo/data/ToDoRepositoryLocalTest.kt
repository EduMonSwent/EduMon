package com.android.sample.ui.todo.data

import com.android.sample.ui.todo.model.Priority
import com.android.sample.ui.todo.model.Status
import com.android.sample.ui.todo.model.ToDo
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class ToDoRepositoryLocalTest {

  private fun sample(id: String = "1") =
      ToDo(
          id = id,
          title = "Write tests",
          dueDate = LocalDate.of(2025, 1, 1),
          priority = Priority.HIGH,
          status = Status.TODO,
          location = "Home",
          links = listOf("https://a", "https://b"),
          note = "cover all branches",
          notificationsEnabled = true)

  @Test
  fun `update replaces matching id`() = runTest {
    val repo = ToDoRepositoryLocal()
    val t1 = sample("x")
    repo.add(t1)
    val updated = t1.copy(title = "Updated")
    repo.update(updated)
    assertEquals("Updated", repo.getById("x")!!.title)
  }

  @Test
  fun `remove deletes by id`() = runTest {
    val repo = ToDoRepositoryLocal()
    repo.add(sample("a"))
    repo.add(sample("b"))
    repo.remove("a")
    val ids = (repo.getById("a") == null) to (repo.getById("b") != null)
    assertEquals(Pair(true, true), ids)
  }

  @Test
  fun `getById returns null when missing`() = runTest {
    val repo = ToDoRepositoryLocal()
    assertNull(repo.getById("nope"))
  }
}
