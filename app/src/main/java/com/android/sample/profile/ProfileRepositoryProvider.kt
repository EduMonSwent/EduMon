package com.android.sample.profile

/**
 * Provides a single instance of the repository in the app. `repository` is mutable for testing or
 * replacement with another implementation.
 */
object ProfileRepositoryProvider {
  // default repository until firestore usage : fake one
  private val _localRepository: ProfileRepository = FakeProfileRepository() // TODO replace
  val repository: ProfileRepository = _localRepository
}
