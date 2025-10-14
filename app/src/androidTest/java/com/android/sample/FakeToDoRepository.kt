package com.android.sample

import com.android.sample.todo.ToDo
import com.android.sample.todo.ToDoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * A fake, in-memory repository used for testing. It allows us to control the data state directly
 * without any real disk or network I/O.
 */
class FakeToDoRepository : ToDoRepository {
  private val state = MutableStateFlow<List<ToDo>>(emptyList())
  private var shouldFail = false // Test failure scenarios

  /**
   * Public property for tests to synchronously read the current state. Use this in your tests
   * instead of trying to access a `.value` on the flow.
   */
  val currentList: List<ToDo>
    get() = state.value

  override val todos: Flow<List<ToDo>> = state

  override suspend fun add(todo: ToDo) {
    if (shouldFail) throw Exception("Failed to add")
    state.update { it + todo }
  }

  override suspend fun update(todo: ToDo) {
    if (shouldFail) throw Exception("Failed to update")
    state.update { list -> list.map { if (it.id == todo.id) todo else it } }
  }

  override suspend fun remove(id: String) {
    if (shouldFail) throw Exception("Failed to remove")
    state.update { list -> list.filterNot { it.id == id } }
  }

  override suspend fun getById(id: String): ToDo? = state.value.firstOrNull { it.id == id }

  // --- Test-only helper functions ---
  fun setInitialItems(items: List<ToDo>) {
    state.value = items
  }

  fun setShouldFail(fail: Boolean) {
    shouldFail = fail
  }
}
