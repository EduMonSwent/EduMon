package com.android.sample.ui.todo.model

import com.android.sample.ui.todo.ToDoRoutes
import java.time.LocalDate
import org.junit.Assert
import org.junit.Test

class ModelAndRoutesTest {
  @Test
  fun `dueDateFormatted prints readable date`() {
    val t = ToDo(title = "x", dueDate = LocalDate.of(2025, 10, 15), priority = Priority.MEDIUM)
    // Pattern is "EEE, d MMM yyyy" -> e.g., "Wed, 15 Oct 2025"
    val txt = t.dueDateFormatted()
    require(txt.contains("Oct")) { txt }
  }

  @Test
  fun `routes build correctly`() {
    Assert.assertEquals("todos", ToDoRoutes.Todos)
    Assert.assertEquals("todos/add", ToDoRoutes.Add)
    Assert.assertEquals("todos/edit/123", ToDoRoutes.edit("123"))
  }
}
