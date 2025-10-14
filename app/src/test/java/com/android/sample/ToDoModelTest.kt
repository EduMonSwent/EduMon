package com.android.sample

import com.android.sample.todo.Priority
import com.android.sample.todo.Status
import com.android.sample.todo.ToDo
import java.time.LocalDate
import org.junit.Assert
import org.junit.Test

class ToDoModelTest {

  @Test
  fun dueDateFormatted_contains_month_and_year() {
    val t = ToDo(title = "Read", dueDate = LocalDate.of(2025, 1, 2), priority = Priority.HIGH)
    val s = t.dueDateFormatted()
    Assert.assertTrue(s.contains("Jan"))
    Assert.assertTrue(s.contains("2025"))
  }

  @Test
  fun copy_changes_affect_equality_and_hash() {
    val base = ToDo("id-1", "A", LocalDate.now(), Priority.MEDIUM, Status.TODO, note = "n")
    val changed = base.copy(status = Status.DONE, note = "n2")
    Assert.assertEquals("id-1", changed.id)
    Assert.assertNotEquals(base, changed)
    Assert.assertNotEquals(base.hashCode(), changed.hashCode())
  }
}
