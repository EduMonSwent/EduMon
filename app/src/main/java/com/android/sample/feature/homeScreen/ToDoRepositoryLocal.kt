package com.android.sample.repositories

import com.android.sample.data.ToDo
import kotlin.collections.filterNot
import kotlin.collections.firstOrNull
import kotlin.collections.map
import kotlin.collections.plus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class ToDoRepositoryLocal : ToDoRepository {
  private val state = MutableStateFlow<List<ToDo>>(emptyList())
  override val todos: Flow<List<ToDo>> = state

  override suspend fun add(todo: ToDo) {
    state.update { it + todo }
  }

  override suspend fun update(todo: ToDo) {
    state.update { list -> list.map { if (it.id == todo.id) todo else it } }
  }

  override suspend fun remove(id: String) {
    state.update { list -> list.filterNot { it.id == id } }
  }

  override suspend fun getById(id: String): ToDo? = state.value.firstOrNull { it.id == id }
}
