package com.android.sample.ui.flashcards.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/** Minimal provider: guarantees exactly one FirestoreFlashcardsRepository instance. */
object FirestoreFlashcardsRepoProvider {

  @Volatile private var instance: FlashcardsRepository? = null

  // Returns the singleton instance, creating it once if needed.
  fun get(): FlashcardsRepository =
      instance ?: synchronized(this) { instance ?: buildDefault().also { instance = it } }

  private fun buildDefault(): FlashcardsRepository {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    return FirestoreFlashcardsRepository(db, auth)
  }
}
