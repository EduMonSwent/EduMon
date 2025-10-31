package com.android.sample.feature.weeks.repository

import com.android.sample.feature.weeks.model.CourseMaterial
import com.android.sample.feature.weeks.model.DayStatus
import com.android.sample.feature.weeks.model.Exercise
import com.android.sample.feature.weeks.model.WeekContent
import com.android.sample.feature.weeks.model.WeekProgressItem
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
 * Firestore implementation of WeeksRepository storing data under:
 * - users/{uid}/weeks (ordered by 'order')
 * - users/{uid}/dayStatuses (documents per day)
 */
class FirestoreWeeksRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
) : WeeksRepository {

  // --- Firestore helpers ---
  private fun isSignedIn(): Boolean = auth.currentUser != null

  private fun colWeeks(): CollectionReference {
    val uid =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in to access weeks")
    return db.collection("users").document(uid).collection("weeks")
  }

  private fun colDayStatuses(): CollectionReference {
    val uid =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in to access day statuses")
    return db.collection("users").document(uid).collection("dayStatuses")
  }

  // Map Firestore doc to domain using direct field access (avoids duplicate DTOs)
  private fun DocumentSnapshot.toDomainWeek(): WeekProgressItem {
    val label = getString("label") ?: ""
    val percent = (getLong("percent") ?: 0L).toInt().coerceIn(0, 100)

    val exercises: List<Exercise> =
        (get("exercises") as? List<*>)
            ?.mapNotNull { it as? Map<*, *> }
            ?.map {
              val id = it["id"] as? String ?: ""
              val title = it["title"] as? String ?: ""
              val done = it["done"] as? Boolean ?: false
              Exercise(id = id, title = title, done = done)
            } ?: emptyList()

    val courses: List<CourseMaterial> =
        (get("courses") as? List<*>)
            ?.mapNotNull { it as? Map<*, *> }
            ?.map {
              val id = it["id"] as? String ?: ""
              val title = it["title"] as? String ?: ""
              val read = it["read"] as? Boolean ?: false
              CourseMaterial(id = id, title = title, read = read)
            } ?: emptyList()

    return WeekProgressItem(
        label = label, percent = percent, content = WeekContent(exercises, courses))
  }

  private fun WeekProgressItem.toWriteMap(order: Long): Map<String, Any?> =
      hashMapOf(
          "label" to label,
          "percent" to percent,
          "order" to order,
          "exercises" to
              content.exercises.map {
                mapOf("id" to it.id, "title" to it.title, "done" to it.done)
              },
          "courses" to
              content.courses.map { mapOf("id" to it.id, "title" to it.title, "read" to it.read) },
          "updatedAt" to FieldValue.serverTimestamp())

  private suspend fun fetchWeeksOrdered(): MutableList<Pair<DocumentSnapshot, WeekProgressItem>> =
      withContext(Dispatchers.IO) {
        val snap = Tasks.await(colWeeks().orderBy("order", Query.Direction.ASCENDING).get())
        snap.documents.map { it to it.toDomainWeek() }.toMutableList()
      }

  private fun recomputePercent(content: WeekContent): Int {
    val total = content.exercises.size + content.courses.size
    if (total == 0) return 0
    val done = content.exercises.count { it.done } + content.courses.count { it.read }
    return ((done * 100.0) / total).toInt().coerceIn(0, 100)
  }

  // Default weeks (mirrors FakeWeeksRepository content). Percents will be recomputed on read.
  private fun buildDefaultWeeks(): List<WeekProgressItem> =
      listOf(
          WeekProgressItem(
              label = "Week 1",
              percent = 100,
              content =
                  WeekContent(
                      courses =
                          listOf(
                              CourseMaterial(id = "c1", title = "Intro to Android", read = true),
                              CourseMaterial(id = "c2", title = "Compose Basics", read = true),
                          ),
                      exercises =
                          listOf(
                              Exercise(id = "e1", title = "Set up environment", done = true),
                              Exercise(id = "e2", title = "Finish codelab", done = true),
                          ))),
          WeekProgressItem(
              label = "Week 2",
              percent = 55,
              content =
                  WeekContent(
                      courses =
                          listOf(
                              CourseMaterial(id = "c3", title = "Compose Layouts", read = false),
                              CourseMaterial(
                                  id = "c4", title = "State and Side-effects", read = true),
                          ),
                      exercises =
                          listOf(
                              Exercise(id = "e3", title = "Build layout challenge", done = false),
                          ))),
          WeekProgressItem(
              label = "Week 3",
              percent = 10,
              content =
                  WeekContent(
                      courses =
                          listOf(
                              CourseMaterial(
                                  id = "c5", title = "Architecture guidance", read = false),
                          ),
                      exercises =
                          listOf(
                              Exercise(
                                  id = "e4", title = "Repository implementation", done = false),
                          ))),
      )

  private fun defaultDayStatuses(): List<DayStatus> =
      DayOfWeek.values().mapIndexed { idx, d -> DayStatus(d, metTarget = idx % 2 == 0) }

  override suspend fun getWeeks(): List<WeekProgressItem> =
      withContext(Dispatchers.IO) {
        // If no user is signed in, return an in-memory default set so tests and previews work.
        if (!isSignedIn()) {
          val defaults = buildDefaultWeeks()
          return@withContext defaults.map { it.copy(percent = recomputePercent(it.content)) }
        }

        var items = fetchWeeksOrdered()

        if (items.isEmpty()) {
          // Seed defaults if no weeks exist
          val defaults = buildDefaultWeeks()
          Tasks.await(
              db.runBatch { b ->
                defaults.forEachIndexed { idx, week ->
                  val ref = colWeeks().document()
                  val data = week.toWriteMap(idx.toLong()).toMutableMap()
                  data["createdAt"] = FieldValue.serverTimestamp()
                  b.set(ref, data)
                }
              })
          // Refetch after seeding
          items = fetchWeeksOrdered()
        }

        // Ensure percent is in sync with content; write back if needed
        Tasks.await(
            db.runBatch { b ->
              items.forEachIndexed { idx, (snap, week) ->
                val pct = recomputePercent(week.content)
                if (pct != week.percent) {
                  b.update(
                      snap.reference,
                      mapOf("percent" to pct, "updatedAt" to FieldValue.serverTimestamp()))
                  items[idx] = snap to week.copy(percent = pct)
                }
              }
            })
        items.map { it.second }
      }

  override suspend fun getDayStatuses(): List<DayStatus> =
      withContext(Dispatchers.IO) {
        // Fallback for unsigned sessions
        if (!isSignedIn()) return@withContext defaultDayStatuses()

        val snap = Tasks.await(colDayStatuses().get())
        val docs = snap.documents
        if (docs.isEmpty()) {
          // Seed defaults similar to FakeWeeksRepository
          Tasks.await(
              db.runBatch { b ->
                DayOfWeek.values().forEachIndexed { idx, d ->
                  val ref = colDayStatuses().document(d.name)
                  b.set(ref, mapOf("day" to d.name, "metTarget" to (idx % 2 == 0)))
                }
              })
          return@withContext DayOfWeek.values().mapIndexed { idx, d ->
            DayStatus(d, metTarget = idx % 2 == 0)
          }
        }
        docs.map { d ->
          val dayStr = d.getString("day") ?: d.id
          val met = d.getBoolean("metTarget") ?: false
          val day = runCatching { DayOfWeek.valueOf(dayStr) }.getOrElse { DayOfWeek.MONDAY }
          DayStatus(dayOfWeek = day, metTarget = met)
        }
      }

  override suspend fun updateWeekPercent(index: Int, percent: Int): List<WeekProgressItem> =
      withContext(Dispatchers.IO) {
        // For unsigned sessions, just update in-memory value by returning adjusted defaults.
        if (!isSignedIn()) {
          val weeks = buildDefaultWeeks().toMutableList()
          if (index in weeks.indices) {
            val clamped = percent.coerceIn(0, 100)
            weeks[index] = weeks[index].copy(percent = clamped)
          }
          return@withContext weeks
        }

        val items = fetchWeeksOrdered()
        if (index !in items.indices) return@withContext items.map { it.second }
        val (snap, week) = items[index]
        val clamped = percent.coerceIn(0, 100)
        Tasks.await(
            snap.reference.set(
                week.copy(percent = clamped).toWriteMap(index.toLong()), SetOptions.merge()))
        getWeeks()
      }

  override suspend fun getWeekContent(index: Int): WeekContent =
      withContext(Dispatchers.IO) {
        // Fallback when unsigned
        if (!isSignedIn()) {
          return@withContext buildDefaultWeeks().getOrNull(index)?.content
              ?: WeekContent(emptyList(), emptyList())
        }
        val items = fetchWeeksOrdered()
        items.getOrNull(index)?.second?.content ?: WeekContent(emptyList(), emptyList())
      }

  override suspend fun markExerciseDone(
      weekIndex: Int,
      exerciseId: String,
      done: Boolean
  ): List<WeekProgressItem> =
      withContext(Dispatchers.IO) {
        // Unsigned fallback: return updated defaults only in-memory
        if (!isSignedIn()) {
          val weeks = buildDefaultWeeks().toMutableList()
          if (weekIndex in weeks.indices) {
            val w = weeks[weekIndex]
            val updated =
                w.content.copy(
                    exercises =
                        w.content.exercises.map {
                          if (it.id == exerciseId) it.copy(done = done) else it
                        })
            val pct = recomputePercent(updated)
            weeks[weekIndex] = w.copy(content = updated, percent = pct)
          }
          return@withContext weeks
        }

        val items = fetchWeeksOrdered()
        if (weekIndex !in items.indices) return@withContext items.map { it.second }

        val (snap, week) = items[weekIndex]
        val updatedContent =
            week.content.copy(
                exercises =
                    week.content.exercises.map {
                      if (it.id == exerciseId) it.copy(done = done) else it
                    })
        val newPercent = recomputePercent(updatedContent)

        Tasks.await(
            snap.reference.set(
                week
                    .copy(percent = newPercent, content = updatedContent)
                    .toWriteMap(weekIndex.toLong()),
                SetOptions.merge()))

        getWeeks()
      }

  override suspend fun markCourseRead(
      weekIndex: Int,
      courseId: String,
      read: Boolean
  ): List<WeekProgressItem> =
      withContext(Dispatchers.IO) {
        // Unsigned fallback: return updated defaults only in-memory
        if (!isSignedIn()) {
          val weeks = buildDefaultWeeks().toMutableList()
          if (weekIndex in weeks.indices) {
            val w = weeks[weekIndex]
            val updated =
                w.content.copy(
                    courses =
                        w.content.courses.map {
                          if (it.id == courseId) it.copy(read = read) else it
                        })
            val pct = recomputePercent(updated)
            weeks[weekIndex] = w.copy(content = updated, percent = pct)
          }
          return@withContext weeks
        }

        val items = fetchWeeksOrdered()
        if (weekIndex !in items.indices) return@withContext items.map { it.second }

        val (snap, week) = items[weekIndex]
        val updatedContent =
            week.content.copy(
                courses =
                    week.content.courses.map {
                      if (it.id == courseId) it.copy(read = read) else it
                    })
        val newPercent = recomputePercent(updatedContent)

        Tasks.await(
            snap.reference.set(
                week
                    .copy(percent = newPercent, content = updatedContent)
                    .toWriteMap(weekIndex.toLong()),
                SetOptions.merge()))

        getWeeks()
      }
}
