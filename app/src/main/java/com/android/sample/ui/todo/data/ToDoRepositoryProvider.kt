package com.android.sample.ui.todo.data

/**
 * Provides a single instance of the repository in the app. `repository` is mutable for testing or
 * replacement with another implementation.
 */
object ToDoRepositoryProvider {
  // Default repository: local in-memory implementation
  private val _localRepository: ToDoRepository = ToDoRepositoryLocal()
  var repository: ToDoRepository = _localRepository
}
