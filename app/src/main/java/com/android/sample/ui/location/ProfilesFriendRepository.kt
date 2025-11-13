package com.android.sample.ui.location

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ProfilesFriendRepository(private val db: FirebaseFirestore, private val auth: FirebaseAuth) :
    FriendRepository {

  private val currentUid: String =
      auth.currentUser?.uid
          ?: throw IllegalStateException(
              "User must be logged in before using ProfilesFriendRepository.")

  override val friendsFlow: Flow<List<FriendStatus>> = callbackFlow {
    // NEW: immediately tell collectors “no friends yet”
    trySend(emptyList())

    // Per-friend profile listeners
    val profileRegs = mutableMapOf<String, ListenerRegistration>()
    // Current friend id set + cache of FriendStatus
    var currentIds: Set<String> = emptySet()
    val cache = mutableMapOf<String, FriendStatus>()

    // When adding new friends, we hold emissions until each one delivers its *first* profile
    // snapshot.
    var initialLoadsPending = 0

    fun emitNow() {
      // Emit only friends that are still in the current id set
      val list = cache.values.filter { it.id in currentIds }.sortedBy { it.name.lowercase() }
      trySend(list)
    }

    fun attachProfileListener(uid: String) {
      if (profileRegs.containsKey(uid)) return

      initialLoadsPending++ // first snapshot pending for this uid
      var first = true

      val reg =
          db.collection("profiles").document(uid).addSnapshotListener { doc, _ ->
            if (doc == null || !doc.exists()) {
              cache.remove(uid)
            } else {
              val status = doc.toFriendStatus(uid)
              if (status == null) {
                cache.remove(uid)
              } else {
                cache[uid] = status
              }
            }

            // On the first snapshot for this uid, release one pending slot.
            if (first) {
              first = false
              if (initialLoadsPending > 0) initialLoadsPending--
            }

            // Only emit if we're not in the middle of an "add" burst.
            if (initialLoadsPending == 0) emitNow()
          }

      profileRegs[uid] = reg
    }

    fun detachProfileListener(uid: String) {
      profileRegs.remove(uid)?.remove()
      cache.remove(uid)
    }

    // Listen to my friend IDs; ignore local (pending write) snapshots to avoid add-flicker.
    val idsReg: ListenerRegistration =
        db.collection("users").document(currentUid).collection("friendIds").addSnapshotListener(
            MetadataChanges.INCLUDE) { snap, _ ->
              if (snap == null) return@addSnapshotListener

              // KEY: skip the optimistic local snapshot that causes churn
              if (snap.metadata.hasPendingWrites()) return@addSnapshotListener

              val ids = snap.documents.map { it.id }.toSet()

              val newIds = ids - currentIds
              val removedIds = currentIds - ids
              currentIds = ids

              // Detach removed first (reflect deletions immediately)
              removedIds.forEach { detachProfileListener(it) }

              // Attach new; their first profile snapshots will trigger a coalesced emit
              newIds.forEach { attachProfileListener(it) }

              // Whenever there are no pending initial loads, emit the current list —
              // covers:
              //  - initial state (no friends)  -> emits []
              //  - pure removals               -> emits list without removed friends
              //  - "cleared all friends"       -> emits []
              if (initialLoadsPending == 0) {
                emitNow()
              }
            }

    awaitClose {
      idsReg.remove()
      profileRegs.values.forEach { it.remove() }
      profileRegs.clear()
    }
  }

  override suspend fun addFriendByUid(frienduid: String): FriendStatus {
    require(frienduid.isNotBlank()) { "Enter a UID." }
    require(frienduid != currentUid) { "You can’t add yourself." }

    // Already friends?
    val edgeDoc =
        db.collection("users")
            .document(currentUid)
            .collection("friendIds")
            .document(frienduid)
            .get()
            .await()
    if (edgeDoc.exists()) throw IllegalArgumentException("You're already friends.")

    // Resolve profile
    val doc = db.collection("profiles").document(frienduid).get().await()
    if (!doc.exists()) throw IllegalArgumentException("No user found for that UID.")

    val status =
        doc.toFriendStatus(frienduid)
            ?: throw IllegalStateException("That user hasn’t shared a location yet.")

    // Create edge (we no longer depend on the snapshot name for display)
    db.collection("users")
        .document(currentUid)
        .collection("friendIds")
        .document(frienduid)
        .set(mapOf("addedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()))
        .await()

    return status
  }

  override suspend fun removeFriend(frienduid: String) {
    db.collection("users")
        .document(currentUid)
        .collection("friendIds")
        .document(frienduid)
        .delete()
        .await()
  }
}

/* -------------------- helpers -------------------- */

private fun com.google.firebase.firestore.DocumentSnapshot.toFriendStatus(
    uid: String
): FriendStatus? {
  val name = getString("name") ?: getString("username") ?: uid
  val gp: GeoPoint = getGeoPoint("location") ?: return null
  val mode =
      when (getString("mode")?.lowercase()) {
        "break" -> FriendMode.BREAK
        "idle" -> FriendMode.IDLE
        else -> FriendMode.STUDY
      }

  return FriendStatus(
      id = uid, name = name, latitude = gp.latitude, longitude = gp.longitude, mode = mode)
}
