package com.android.sample.ui.location

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Flow-based repository with Firestore listeners:
 * - ONE listener on users/{me}/friendIds
 * - K listeners on profiles via whereIn(documentId(), chunkOfUpTo10)
 * - Emits List<FriendStatus> via friendsFlow whenever Firestore pushes changes
 *
 * We snapshot slow-changing "name" into friendIds when adding a friend. Location stays only in
 * profiles/{uid}.
 */
class ProfilesFriendRepository(private val db: FirebaseFirestore, private val auth: FirebaseAuth) :
    FriendRepository {

  private val currentUid: String =
      auth.currentUser?.uid
          ?: throw IllegalStateException(
              "User must be logged in before creating ProfilesFriendRepositoryListeners.")

  override val friendsFlow: Flow<List<FriendStatus>> = callbackFlow {
    val chunkRegs = mutableListOf<ListenerRegistration>()
    var currentIds: Set<String> = emptySet() // latest friend UID set from friendIds listener
    val cacheMap = mutableMapOf<String, FriendStatus>() // uid -> latest FriendStatus

    // 1) Listen to my friend IDs (graph)
    val idsReg =
        db.collection("users").document(currentUid).collection("friendIds").addSnapshotListener {
            snap,
            err ->
          if (err != null) return@addSnapshotListener

          val ids = snap?.documents?.map { it.id }?.toSet() ?: emptySet()
          currentIds = ids

          // Tear down old chunk listeners and trim cache to current IDs
          chunkRegs.forEach { it.remove() }
          chunkRegs.clear()
          cacheMap.keys.retainAll(currentIds)

          // No friends? Emit empty immediately
          if (ids.isEmpty()) {
            trySend(emptyList())
            return@addSnapshotListener
          }

          // 2) Rebuild profile listeners in chunks of up to 10 UIDs
          ids.chunked(10).forEach { chunk ->
            val reg =
                db.collection("profiles")
                    .whereIn(FieldPath.documentId(), chunk)
                    .addSnapshotListener { qs, _ ->
                      qs ?: return@addSnapshotListener

                      // For each changed/added profile, update our cache
                      for (doc in qs.documents) {
                        val uid = doc.id
                        val status = doc.toFriendStatus(uid)
                        if (uid in currentIds && status != null) {
                          cacheMap[uid] = status
                        } else {
                          cacheMap.remove(uid)
                        }
                      }

                      // Build a sorted list from cache and emit
                      val list =
                          cacheMap.values
                              .filter { it.id in currentIds }
                              .sortedBy { it.name.lowercase() }
                      trySend(list)
                    }
            chunkRegs += reg
          }
        }

    // When the flow collector is cancelled, remove listeners
    awaitClose {
      idsReg.remove()
      chunkRegs.forEach { it.remove() }
      chunkRegs.clear()
    }
  }

  /** Add a friend by UID with friendly errors + a "name" snapshot in friendIds. */
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

    // Create edge with slow-changing name snapshot
    db.collection("users")
        .document(currentUid)
        .collection("friendIds")
        .document(frienduid)
        .set(
            mapOf("addedAt" to FieldValue.serverTimestamp(), "name" to status.name),
            SetOptions.merge())
        .await()

    return status
  }

  override suspend fun addFriendByUsername(username: String): FriendStatus {
    require(username.isNotBlank()) { "username is blank" }
    val q = db.collection("profiles").whereEqualTo("username", username).limit(1).get().await()

    val doc =
        q.documents.firstOrNull()
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
  val updatedAtMillis =
      (getTimestamp("updatedAt") ?: getTimestamp("lastSeen") ?: Timestamp.now()).toDate().time

  return FriendStatus(
      id = uid, name = name, latitude = gp.latitude, longitude = gp.longitude, mode = mode)
}
