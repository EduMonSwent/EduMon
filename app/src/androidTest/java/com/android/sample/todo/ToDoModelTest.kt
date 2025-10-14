package com.android.sample.todo

import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class ToDoModelTest {

  @Test
  fun dueDateFormatted_contains_month_and_year() {
    val t = ToDo(title = "Read", dueDate = LocalDate.of(2025, 1, 2), priority = Priority.HIGH)
    val s = t.dueDateFormatted()
    assertTrue(s.contains("Jan"))
    assertTrue(s.contains("2025"))
  }

  @Test
  fun copy_changes_affect_equality_and_hash() {
    val base = ToDo("id-1", "A", LocalDate.now(), Priority.MEDIUM, Status.TODO, note = "n")
    val changed = base.copy(status = Status.DONE, note = "n2")
    assertEquals("id-1", changed.id)
    assertNotEquals(base, changed)
    assertNotEquals(base.hashCode(), changed.hashCode())
  }
}
