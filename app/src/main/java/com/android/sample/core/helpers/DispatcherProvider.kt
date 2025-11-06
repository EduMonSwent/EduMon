package com.android.sample.core.helpers

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Abstraction over coroutine dispatchers to avoid hardcoding Dispatchers.* in production code.
 * Production code uses [DefaultDispatcherProvider]; tests can inject a provider backed by
 * TestDispatcher.
 */
interface DispatcherProvider {
  val io: CoroutineDispatcher
  val main: CoroutineDispatcher
  val default: CoroutineDispatcher
  val unconfined: CoroutineDispatcher
}

object DefaultDispatcherProvider : DispatcherProvider {
  override val io: CoroutineDispatcher = Dispatchers.IO
  override val main: CoroutineDispatcher = Dispatchers.Main
  override val default: CoroutineDispatcher = Dispatchers.Default
  override val unconfined: CoroutineDispatcher = Dispatchers.Unconfined
}
