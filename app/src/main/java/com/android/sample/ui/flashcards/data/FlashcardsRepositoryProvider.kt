package com.android.sample.ui.flashcards.data

import android.content.Context

/** Simple provider (no DI). Call init(context) once (e.g. Application.onCreate()). */
object FlashcardsRepositoryProvider {
  @Volatile private var repo: FlashcardsRepository? = null

  fun init(appContext: Context) {
    if (repo != null) return
    val storage = FlashcardsStorage(appContext)
    repo = DataStoreFlashcardsRepository(storage)
  }

  val repository: FlashcardsRepository
    get() =
        checkNotNull(repo) { "FlashcardsRepositoryProvider not initialized. Call init() first." }
}
