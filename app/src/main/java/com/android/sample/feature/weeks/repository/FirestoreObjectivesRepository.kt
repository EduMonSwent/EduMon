package com.android.sample.feature.weeks.repository

import com.android.sample.feature.weeks.model.Objective
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import java.time.DayOfWeek
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Firestore-backed implementation that stores objectives under: users/{uid}/objectives
 *
 * Ordering is maintained by a monotonically increasing `order` field so that index-based operations
 * behave consistently across devices.
 */
class FirestoreObjectivesRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
) : ObjectivesRepository {

  // Collection for the signed-in user; throws if no user.
  private fun col(): CollectionReference {
    val uid =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in to access objectives")
    return db.collection("users").document(uid).collection("objectives")
  }

  // --- Mapping helpers -----------------------------------------------------

  // Firestore DTO. Defaults are required for deserialization.
  private data class ObjectiveFs(
      val title: String = "",
      val course: String = "",
      val estimateMinutes: Int = 0, // using Int to match domain; Firestore will coerce
      val completed: Boolean = false,
      val day: String = DayOfWeek.MONDAY.name,
      val order: Long = 0L
  )

  // Convert domain model to a map suitable for Firestore writes.
  private fun Objective.toFs(order: Long?): MutableMap<String, Any?> =
      hashMapOf(
          "title" to title,
          "course" to course,
          "estimateMinutes" to estimateMinutes,
          "completed" to completed,
          "day" to day.name,
          "order" to (order ?: 0L),
          "updatedAt" to FieldValue.serverTimestamp())

  // Convert a document to domain model (id not exposed by domain in this app).
  private fun DocumentSnapshot.toDomain(): Objective {
    val o = this.toObject(ObjectiveFs::class.java) ?: ObjectiveFs()
    val dow = runCatching { DayOfWeek.valueOf(o.day) }.getOrElse { DayOfWeek.MONDAY }
    return Objective(
        title = o.title,
        course = o.course,
        estimateMinutes = o.estimateMinutes,
        completed = o.completed,
        day = dow,
    )
  }

  // Fetch documents ordered by `order`, returning pairs (snapshot, domain)
  private suspend fun fetchOrdered(): MutableList<Pair<DocumentSnapshot, Objective>> =
      withContext(Dispatchers.IO) {
        val snap = Tasks.await(col().orderBy("order", Query.Direction.ASCENDING).get())
        snap.documents.map { it to it.toDomain() }.toMutableList()
      }

  // --- Interface -----------------------------------------------------------

  override suspend fun getObjectives(): List<Objective> =
      withContext(Dispatchers.IO) { fetchOrdered().map { it.second } }

  override suspend fun addObjective(obj: Objective): List<Objective> =
      withContext(Dispatchers.IO) {
        val items = fetchOrdered()
        val nextOrder = items.size.toLong()
        val data =
            obj.toFs(order = nextOrder).apply { this["createdAt"] = FieldValue.serverTimestamp() }
        Tasks.await(col().document().set(data, SetOptions.merge()))
        getObjectives()
      }

  override suspend fun updateObjective(index: Int, obj: Objective): List<Objective> =
      withContext(Dispatchers.IO) {
        val items = fetchOrdered()
        if (index !in items.indices) return@withContext items.map { it.second }
        val (snap, _) = items[index]
        val order = snap.getLong("order") ?: index.toLong()
        Tasks.await(snap.reference.set(obj.toFs(order), SetOptions.merge()))
        getObjectives()
      }

  override suspend fun removeObjective(index: Int): List<Objective> =
      withContext(Dispatchers.IO) {
        val items = fetchOrdered()
        if (index !in items.indices) return@withContext items.map { it.second }

        val (toDelete, _) = items.removeAt(index)
        Tasks.await(db.runBatch { b -> b.delete(toDelete.reference) })

        Tasks.await(
            db.runBatch { b ->
              items.forEachIndexed { i, (snap, _) ->
                b.update(
                    snap.reference,
                    mapOf("order" to i.toLong(), "updatedAt" to FieldValue.serverTimestamp()))
              }
            })

        getObjectives()
      }

  override suspend fun moveObjective(fromIndex: Int, toIndex: Int): List<Objective> =
      withContext(Dispatchers.IO) {
        val items = fetchOrdered()
        if (items.isEmpty()) return@withContext items.map { it.second }

        val from = fromIndex.coerceIn(0, items.lastIndex)
        val to = toIndex.coerceIn(0, items.lastIndex)
        if (from == to) return@withContext items.map { it.second }

        val item = items.removeAt(from)
        items.add(to, item)

        Tasks.await(
            db.runBatch { b ->
              items.forEachIndexed { i, (snap, _) ->
                b.update(
                    snap.reference,
                    mapOf("order" to i.toLong(), "updatedAt" to FieldValue.serverTimestamp()))
              }
            })

        getObjectives()
      }

  override suspend fun setObjectives(objs: List<Objective>): List<Objective> =
      withContext(Dispatchers.IO) {
        val existing = Tasks.await(col().get())

        Tasks.await(
            db.runBatch { b ->
              existing.documents.forEach { b.delete(it.reference) }
              objs.forEachIndexed { i, obj ->
                val ref = col().document()
                val data =
                    obj.toFs(order = i.toLong()).apply {
                      this["createdAt"] = FieldValue.serverTimestamp()
                    }
                b.set(ref, data)
              }
            })

        getObjectives()
      }
}

// End of FirestoreObjectivesRepository
