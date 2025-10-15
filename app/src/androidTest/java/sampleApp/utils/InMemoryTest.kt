package sampleApp.utils

import com.android.sample.model.todo.ToDo
import com.android.sample.model.todo.ToDosRepository

/**
 * Superclass for all local tests, which sets up a local repository before each test and restores
 * the original repository after each test.
 */
object InMemoryTest : BootcampTest {
  override fun createInitializedRepository(): ToDosRepository {
    return InMemoryToDosRepository()
  }

  class InMemoryToDosRepository(val todoList: MutableList<ToDo> = mutableListOf<ToDo>()) :
      ToDosRepository {
    override suspend fun addTodo(toDo: ToDo) {
      todoList.add(toDo)
    }

    override suspend fun editTodo(todoID: String, newValue: ToDo) {
      todoList.replaceAll { if (it.uid == todoID) newValue else it }
    }

    override suspend fun deleteTodo(todoID: String) {
      todoList.removeIf { it.uid == todoID }
    }

    override fun getNewUid(): String {
      return "${todoList.size}"
    }

    override suspend fun getAllTodos(): List<ToDo> {
      return todoList
    }

    override suspend fun getTodo(todoID: String): ToDo {
      return todoList.first<ToDo> { it.uid == todoID }
    }
  }
}
