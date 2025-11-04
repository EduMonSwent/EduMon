package com.android.sample.core.helpers

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.WriteBatch

/** Merge (create or update). */
fun WriteBatch.setMerged(
    ref: DocumentReference,
    data: Map<String, Any?> // <-- explicit type
): WriteBatch = set(ref, data, SetOptions.merge()) // <-- qualify with `this`
