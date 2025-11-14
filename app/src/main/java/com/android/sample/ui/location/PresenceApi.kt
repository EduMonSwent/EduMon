package com.android.sample.ui.location

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

/** Call whenever your device location/mode/name changes. */
suspend fun updateMyPresence(
    name: String,
    mode: FriendMode,
    lat: Double,
    lon: Double,
    db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
  val uid =
      auth.currentUser?.uid
          ?: throw IllegalStateException("User must be logged in to update presence.")

  db.collection("profiles")
      .document(uid)
      .set(
          mapOf(
              "name" to name,
              "mode" to mode.name,
              "location" to GeoPoint(lat, lon),
              "updatedAt" to FieldValue.serverTimestamp()),
          SetOptions.merge())
}

/**
 * Ensure the signed-in user has a profile document with at least name, mode and location. If the
 * document is missing or missing any of these core fields, it will be created/merged. Returns true
 * if a write occurred (new doc or fields added), false if nothing needed.
 */
suspend fun ensureMyProfile(
    name: String,
    mode: FriendMode,
    lat: Double,
    lon: Double,
    db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    auth: FirebaseAuth = FirebaseAuth.getInstance()
): Boolean {
  val uid =
      auth.currentUser?.uid
          ?: throw IllegalStateException("User must be logged in to ensure profile.")

  val docRef = db.collection("profiles").document(uid)
  val snap = docRef.get().await()

  val needsWrite =
      if (!snap.exists()) {
        true
      } else {
        val hasName = !snap.getString("name").isNullOrBlank()
        val hasMode = !snap.getString("mode").isNullOrBlank()
        val hasLocation = snap.getGeoPoint("location") != null
        !(hasName && hasMode && hasLocation)
      }

  if (!needsWrite) return false

  docRef
      .set(
          mapOf(
              "name" to name,
              "mode" to mode.name,
              "location" to GeoPoint(lat, lon),
              "createdOrEnsuredAt" to FieldValue.serverTimestamp(),
              // If existing doc had other fields, merge keeps them.
          ),
          SetOptions.merge())
      .await()
  return true
}
