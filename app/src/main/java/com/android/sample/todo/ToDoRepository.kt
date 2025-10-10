package com.android.sample.todo

import kotlinx.coroutines.flow.Flow

/**
 * Interface for managing To-Do items.
 * - Defines how the app can manage todos.
 */
interface ToDoRepository {

  // Reactive stream of all todos, continuously emitting updates
  val todos: Flow<List<ToDo>>

  // Add a new todo to the list
  suspend fun add(todo: ToDo)

  // Update an existing todo (same id, different fields)
  suspend fun update(todo: ToDo)

  // Remove a todo by its unique id
  suspend fun remove(id: String)

  // Retrieve a specific todo by id (nullable if not found)
  suspend fun getById(id: String): ToDo?
}
