// src/main/java/.../firestore/DocumentReferenceExt.kt
package com.android.sample.core.helpers

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

/** Create/update with merge semantics. */
suspend fun DocumentReference.setMerged(data: Any) {
  // Not a Kotlin collection setter → Sonar/IDE false positive.
  @Suppress("ReplaceGetOrSet", "kotlin:S6519") set(data, SetOptions.merge()).await()
}
