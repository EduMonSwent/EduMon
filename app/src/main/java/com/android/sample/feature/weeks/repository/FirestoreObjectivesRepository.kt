package com.android.sample.feature.weeks.repository

import com.android.sample.feature.weeks.model.Objective
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import java.time.DayOfWeek
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FirestoreObjectivesRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
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
    return Objective(title, course, estimate, completed, dow)
  }

  private suspend fun fetchOrdered(): MutableList<Pair<DocumentSnapshot, Objective>> =
      withContext(Dispatchers.IO) {
        val snap = Tasks.await(col().orderBy("order", Query.Direction.ASCENDING).get())
        snap.documents.map { it to it.toDomain() }.toMutableList()
      }

  // ---------- unsigned defaults (no Firestore) ----------
  private fun defaultObjectives(): MutableList<Objective> =
      mutableListOf(
          Objective("Setup Android Studio", "CS", 10, true, DayOfWeek.MONDAY),
          Objective("Finish codelab", "CS", 20, true, DayOfWeek.TUESDAY),
          Objective("Read Compose Basics", "CS", 30, false, DayOfWeek.WEDNESDAY),
          Objective("Build layout challenge", "CS", 40, false, DayOfWeek.THURSDAY),
          Objective("Repository implementation", "CS", 50, false, DayOfWeek.FRIDAY),
      )

  // ---------- API ----------
  override suspend fun getObjectives(): List<Objective> =
      withContext(Dispatchers.IO) {
        if (!isSignedIn()) return@withContext defaultObjectives()
        fetchOrdered().map { it.second }
      }

  override suspend fun addObjective(obj: Objective): List<Objective> =
      withContext(Dispatchers.IO) {
        if (!isSignedIn()) {
          val items = defaultObjectives()
          items.add(obj)
          return@withContext items
        }
        val items = fetchOrdered()
        val nextOrder = items.size.toLong()
        val data = obj.toFs(nextOrder).apply { this["createdAt"] = FieldValue.serverTimestamp() }
        Tasks.await(col().document().set(data, SetOptions.merge()))
        getObjectives()
      }

  override suspend fun updateObjective(index: Int, obj: Objective): List<Objective> =
      withContext(Dispatchers.IO) {
        if (!isSignedIn()) {
          val items = defaultObjectives()
          if (index in items.indices) items[index] = obj
          return@withContext items
        }
        val items = fetchOrdered()
        if (index !in items.indices) return@withContext items.map { it.second }
        val (snap, _) = items[index]
        val order = snap.getLong("order") ?: index.toLong()
        Tasks.await(snap.reference.set(obj.toFs(order), SetOptions.merge()))
        getObjectives()
      }

  override suspend fun removeObjective(index: Int): List<Objective> =
      withContext(Dispatchers.IO) {
        if (!isSignedIn()) {
          val items = defaultObjectives()
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
      withContext(Dispatchers.IO) {
        if (!isSignedIn()) {
          val items = defaultObjectives()
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
      withContext(Dispatchers.IO) {
        if (!isSignedIn()) return@withContext objs.toList()
        val existing = Tasks.await(col().get())
        val batch = db.batch()
        existing.documents.forEach { batch.delete(it.reference) }
        objs.forEachIndexed { i, obj ->
          val ref = col().document()
          val data = obj.toFs(i.toLong()).apply { this["createdAt"] = FieldValue.serverTimestamp() }
          batch.set(ref, data)
        }
        Tasks.await(batch.commit())
        getObjectives()
      }
}
