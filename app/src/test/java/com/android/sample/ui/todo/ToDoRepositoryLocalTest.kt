package com.android.sample.ui.todo

import com.android.sample.data.Priority
import com.android.sample.data.Status
import com.android.sample.data.ToDo
import com.android.sample.repositories.ToDoRepositoryLocal
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
  fun `update_replaces_matching_id`() = runTest {
    val repo = ToDoRepositoryLocal()
    val t1 = sample("x")
    repo.add(t1)
    val updated = t1.copy(title = "Updated")
    repo.update(updated)
    assertEquals("Updated", repo.getById("x")!!.title)
  }

  @Test
  fun `remove_deletes_by_id`() = runTest {
    val repo = ToDoRepositoryLocal()
    repo.add(sample("a"))
    repo.add(sample("b"))
    repo.remove("a")
    val ids = (repo.getById("a") == null) to (repo.getById("b") != null)
    assertEquals(Pair(true, true), ids)
  }

  @Test
  fun `getById_returns_null_when_missing`() = runTest {
    val repo = ToDoRepositoryLocal()
    assertNull(repo.getById("nope"))
  }
}
