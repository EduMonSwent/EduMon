package com.android.sample.ui.schedule

import com.android.sample.data.ToDo
import com.android.sample.repositories.ToDoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// ---- Simple fake ToDoRepository for tests ----
class FakeToDoRepository(initial: List<ToDo> = emptyList()) : ToDoRepository {
  private val _todos = MutableStateFlow(initial)
  override val todos: StateFlow<List<ToDo>> = _todos.asStateFlow()

  override suspend fun add(todo: ToDo) {
    _todos.value = _todos.value + todo
  }

  override suspend fun update(todo: ToDo) {
    _todos.value = _todos.value.map { if (it.id == todo.id) todo else it }
  }

  override suspend fun remove(id: String) {
    _todos.value = _todos.value.filterNot { it.id == id }
  }

  override suspend fun getById(id: String): ToDo? = _todos.value.firstOrNull { it.id == id }
}
