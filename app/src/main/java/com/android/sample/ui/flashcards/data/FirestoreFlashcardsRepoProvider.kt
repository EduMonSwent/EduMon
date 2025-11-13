package com.android.sample.ui.flashcards.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Minimal provider: guarantees exactly one FirestoreFlashcardsRepository instance.
 * - Call get() anywhere to use it.
 * - (Optional) call install(...) in tests to inject a custom instance.
 * - (Optional) call useEmulator(...) to point to local emulators.
 */
object FirestoreFlashcardsRepoProvider {

  @Volatile private var instance: FlashcardsRepository? = null

  /** Returns the singleton instance, creating it once if needed. */
  fun get(): FlashcardsRepository =
      instance ?: synchronized(this) { instance ?: buildDefault().also { instance = it } }
  // ---- internal ----
  private fun buildDefault(): FlashcardsRepository {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    return FirestoreFlashcardsRepository(db, auth)
  }
}
