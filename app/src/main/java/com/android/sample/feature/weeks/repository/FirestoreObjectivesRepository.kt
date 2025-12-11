package com.android.sample.feature.weeks.repository

import com.android.sample.core.helpers.DefaultDispatcherProvider
import com.android.sample.core.helpers.DispatcherProvider
import com.android.sample.core.helpers.setMerged
import com.android.sample.feature.weeks.model.DefaultObjectives
import com.android.sample.feature.weeks.model.Objective
import com.android.sample.feature.weeks.model.ObjectiveType
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import java.time.DayOfWeek
import kotlinx.coroutines.withContext

class FirestoreObjectivesRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider,
) : ObjectivesRepository {

  // ---------- helpers ----------
  private fun isSignedIn(): Boolean = auth.currentUser != null

  private fun col(): CollectionReference {
    val uid =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in to access objectives")
    return db.collection("users").document(uid).collection("objectives")
  }

  private fun Objective.toFs(order: Long?): MutableMap<String, Any?> =
      hashMapOf(
          "title" to title,
          "course" to course,
          "estimateMinutes" to estimateMinutes,
          "completed" to completed,
          "day" to day.name,
          "type" to type.name,
          "coursePdfUrl" to coursePdfUrl,
          "exercisePdfUrl" to exercisePdfUrl,
          "order" to (order ?: 0L),
          "updatedAt" to FieldValue.serverTimestamp(),
      )

  private fun DocumentSnapshot.toDomain(): Objective {
    val title = getString("title") ?: ""
    val course = getString("course") ?: ""
    val estimate = (getLong("estimateMinutes") ?: 0L).toInt()
    val completed = getBoolean("completed") ?: false
    val dayStr = getString("day") ?: DayOfWeek.MONDAY.name
    val dow = runCatching { DayOfWeek.valueOf(dayStr) }.getOrElse { DayOfWeek.MONDAY }

    // Safely read the type; default to COURSE_OR_EXERCISES for old documents
    val typeStr = getString("type") ?: ObjectiveType.COURSE_OR_EXERCISES.name
    val type =
        runCatching { ObjectiveType.valueOf(typeStr) }
            .getOrElse { ObjectiveType.COURSE_OR_EXERCISES }

    // Read PDF URLs
    val coursePdfUrl = getString("coursePdfUrl") ?: ""
    val exercisePdfUrl = getString("exercisePdfUrl") ?: ""

    return Objective(
        title = title,
        course = course,
        estimateMinutes = estimate,
        completed = completed,
        day = dow,
        type = type,
        coursePdfUrl = coursePdfUrl,
        exercisePdfUrl = exercisePdfUrl)
  }

  private suspend fun fetchOrdered(): MutableList<Pair<DocumentSnapshot, Objective>> =
      withContext(dispatchers.io) {
        val snap = Tasks.await(col().orderBy("order", Query.Direction.ASCENDING).get())
        snap.documents.map { it to it.toDomain() }.toMutableList()
      }

  // ---------- API ----------
  override suspend fun getObjectives(): List<Objective> =
      withContext(dispatchers.io) {
        if (!isSignedIn()) return@withContext DefaultObjectives.get()
        fetchOrdered().map { it.second }
      }

  override suspend fun addObjective(obj: Objective): List<Objective> =
      withContext(dispatchers.io) {
        if (!isSignedIn()) {
          val items = DefaultObjectives.get().toMutableList()
          items.add(obj)
          return@withContext items
        }
        val items = fetchOrdered()
        val nextOrder = items.size.toLong()
        val data = obj.toFs(nextOrder).apply { this["createdAt"] = FieldValue.serverTimestamp() }
        col().document().setMerged(data)
        getObjectives()
      }

  override suspend fun updateObjective(index: Int, obj: Objective): List<Objective> =
      withContext(dispatchers.io) {
        if (!isSignedIn()) {
          val items = DefaultObjectives.get().toMutableList()
          if (index in items.indices) items[index] = obj
          return@withContext items
        }
        val items = fetchOrdered()
        if (index !in items.indices) return@withContext items.map { it.second }
        val (snap, _) = items[index]
        val order = snap.getLong("order") ?: index.toLong()
        snap.reference.setMerged(obj.toFs(order))
        getObjectives()
      }

  override suspend fun removeObjective(index: Int): List<Objective> =
      withContext(dispatchers.io) {
        if (!isSignedIn()) {
          val items = DefaultObjectives.get().toMutableList()
          if (index in items.indices) items.removeAt(index)
          return@withContext items
        }
        val items = fetchOrdered()
        if (index !in items.indices) return@withContext items.map { it.second }

        val (toDelete, _) = items.removeAt(index)
        val delBatch = db.batch().apply { delete(toDelete.reference) }
        Tasks.await(delBatch.commit())

        val reindex = db.batch()
        items.forEachIndexed { i, (snap, _) ->
          reindex.update(
              snap.reference,
              mapOf("order" to i.toLong(), "updatedAt" to FieldValue.serverTimestamp()))
        }
        Tasks.await(reindex.commit())

        getObjectives()
      }

  override suspend fun moveObjective(fromIndex: Int, toIndex: Int): List<Objective> =
      withContext(dispatchers.io) {
        if (!isSignedIn()) {
          val items = DefaultObjectives.get().toMutableList()
          if (items.isEmpty()) return@withContext items
          val from = fromIndex.coerceIn(0, items.lastIndex)
          val to = toIndex.coerceIn(0, items.lastIndex)
          if (from == to) return@withContext items
          val x = items.removeAt(from)
          items.add(to, x)
          return@withContext items
        }
        val items = fetchOrdered()
        if (items.isEmpty()) return@withContext items.map { it.second }
        val from = fromIndex.coerceIn(0, items.lastIndex)
        val to = toIndex.coerceIn(0, items.lastIndex)
        if (from == to) return@withContext items.map { it.second }

        val x = items.removeAt(from)
        items.add(to, x)

        val batch = db.batch()
        items.forEachIndexed { i, (snap, _) ->
          batch.update(
              snap.reference,
              mapOf("order" to i.toLong(), "updatedAt" to FieldValue.serverTimestamp()))
        }
        Tasks.await(batch.commit())

        getObjectives()
      }

  override suspend fun setObjectives(objs: List<Objective>): List<Objective> =
      withContext(dispatchers.io) {
        if (!isSignedIn()) return@withContext objs.toList()
        val existing = Tasks.await(col().get())
        val batch = db.batch()
        existing.documents.forEach { batch.delete(it.reference) }
        objs.forEachIndexed { i, obj ->
          val ref = col().document()
          val data = obj.toFs(i.toLong()).apply { this["createdAt"] = FieldValue.serverTimestamp() }
          batch.setMerged(ref, data)
        }
        Tasks.await(batch.commit())
        getObjectives()
      }
}
