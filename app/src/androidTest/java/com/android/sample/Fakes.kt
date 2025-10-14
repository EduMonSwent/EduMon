package com.android.sample

import com.android.sample.todo.*
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeToDoRepository : ToDoRepository {
  private val _todos = MutableStateFlow<List<ToDo>>(emptyList())
  override val todos = _todos.asStateFlow()

  override suspend fun add(item: ToDo) {
    val withId = if (item.id.isBlank()) item.copy(id = UUID.randomUUID().toString()) else item
    _todos.value = _todos.value + withId
  }

  override suspend fun update(item: ToDo) {
    _todos.value = _todos.value.map { if (it.id == item.id) item else it }
  }

  override suspend fun remove(id: String) {
    _todos.value = _todos.value.filterNot { it.id == id }
  }

  override suspend fun getById(id: String): ToDo? = _todos.value.firstOrNull { it.id == id }
}

// Handy builder
fun makeToDo(
    id: String = UUID.randomUUID().toString(),
    title: String = "Task $id",
    due: LocalDate = LocalDate.now(),
    priority: Priority = Priority.MEDIUM,
    status: Status = Status.TODO,
    note: String? = null,
    notifications: Boolean = false,
) =
    ToDo(
        id = id,
        title = title,
        dueDate = due,
        priority = priority,
        status = status,
        location = null,
        links = emptyList(),
        note = note,
        notificationsEnabled = notifications)
