// ProfilesFriendRepository.kt
package com.android.sample.ui.location

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ProfilesFriendRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) : FriendRepository {

    private val currentUid: String =
        auth.currentUser?.uid ?: throw IllegalStateException(
            "User must be logged in before creating ProfilesFriendRepository."
        )

    override val friendsFlow: Flow<List<FriendStatus>> = callbackFlow {
        // Active per-friend profile listeners
        val profileRegs = mutableMapOf<String, ListenerRegistration>()
        // Latest friend set + profile cache
        var currentIds: Set<String> = emptySet()
        val cache = mutableMapOf<String, FriendStatus>()

        fun emitNow() {
            // Only emit friends that are still in the current id set, sorted by name
            val list = cache.values
                .filter { it.id in currentIds }
                .sortedBy { it.name.lowercase() }
            trySend(list)
        }

        // Helper to attach a single profile listener
        fun attachProfileListener(uid: String) {
            if (profileRegs.containsKey(uid)) return
            val reg = db.collection("profiles").document(uid)
                .addSnapshotListener { doc, _ ->
                    if (doc == null || !doc.exists()) {
                        cache.remove(uid)
                        emitNow()
                        return@addSnapshotListener
                    }
                    val status = doc.toFriendStatus(uid)
                    if (status == null) {
                        cache.remove(uid) // no location yet
                    } else {
                        cache[uid] = status
                    }
                    emitNow()
                }
            profileRegs[uid] = reg
        }

        // Helper to detach a single profile listener
        fun detachProfileListener(uid: String) {
            profileRegs.remove(uid)?.remove()
            cache.remove(uid)
        }

        // Listen to my friend IDs (subcollection of documents named by friend uid)
        val idsReg = db.collection("users")
            .document(currentUid)
            .collection("friendIds")
            .addSnapshotListener { snap, _ ->
                val ids = snap?.documents?.map { it.id }?.toSet() ?: emptySet()
                // Compute delta
                val newIds = ids - currentIds
                val removedIds = currentIds - ids
                currentIds = ids

                // Detach removed
                removedIds.forEach { detachProfileListener(it) }

                // Attach new
                newIds.forEach { attachProfileListener(it) }

                // Emit immediately to reflect removals before profile listeners fire
                emitNow()
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
        val edgeDoc = db.collection("users")
            .document(currentUid)
            .collection("friendIds")
            .document(frienduid)
            .get()
            .await()
        if (edgeDoc.exists()) throw IllegalArgumentException("You're already friends.")

        // Resolve profile → must exist and have a location
        val doc = db.collection("profiles").document(frienduid).get().await()
        if (!doc.exists()) throw IllegalArgumentException("No user found for that UID.")

        val status = doc.toFriendStatus(frienduid)
            ?: throw IllegalStateException("That user hasn’t shared a location yet.")

        // Create the edge (we no longer need to snapshot name here for display logic)
        db.collection("users")
            .document(currentUid)
            .collection("friendIds")
            .document(frienduid)
            .set(mapOf("addedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()))
            .await()

        return status
    }

    override suspend fun addFriendByUsername(username: String): FriendStatus {
        require(username.isNotBlank()) { "username is blank" }
        val q = db.collection("profiles").whereEqualTo("username", username).limit(1).get().await()
        val doc = q.documents.firstOrNull()
            ?: throw IllegalArgumentException("No profile found for username \"$username\"")
        return addFriendByUid(doc.id)
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

/* -------------------- helpers (same shape as before) -------------------- */

private fun com.google.firebase.firestore.DocumentSnapshot.toFriendStatus(
    uid: String
): FriendStatus? {
    val name = getString("name") ?: getString("username") ?: uid
    val gp: GeoPoint = getGeoPoint("location") ?: return null
    val mode = when (getString("mode")?.lowercase()) {
        "break" -> FriendMode.BREAK
        "idle" -> FriendMode.IDLE
        else -> FriendMode.STUDY
    }
    // val updatedAt = (getTimestamp("updatedAt") ?: getTimestamp("lastSeen") ?: Timestamp.now())
    return FriendStatus(
        id = uid,
        name = name,
        latitude = gp.latitude,
        longitude = gp.longitude,
        mode = mode
    )
}
