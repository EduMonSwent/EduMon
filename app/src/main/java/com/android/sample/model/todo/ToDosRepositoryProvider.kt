package com.android.sample.model.todo

/**
 * Provides a single instance of the repository in the app. `repository` is mutable for testing
 * purposes.
 */
object ToDosRepositoryProvider {
  private val _repositoryB1: ToDosRepository = ToDosRepositoryLocal()

  // You will need to replace with `_repository` when you implement B2.
  var repository: ToDosRepository = _repositoryB1
}
