package com.android.sample.feature.schedule.repository.schedule

import com.android.sample.feature.schedule.data.schedule.EventKind
import com.android.sample.feature.schedule.data.schedule.Priority
import com.android.sample.feature.schedule.data.schedule.ScheduleEvent
import com.android.sample.feature.schedule.data.schedule.SourceTag
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

/**
 * Firestore-backed implementation of the unified ScheduleRepository.
 *
 * Events are stored under: /users/{uid}/schedule/{eventId}
 *
 * - Real-time sync via snapshot listeners
 * - Offline support via Firestore's local cache
 */
class FirestoreScheduleRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
) : ScheduleRepository {

    private val _events = MutableStateFlow<List<ScheduleEvent>>(emptyList())
    override val events: StateFlow<List<ScheduleEvent>> = _events.asStateFlow()

    private val collectionName = "schedule"
    private val usersCollection = "users"

    init {
        startListening()
    }

    private fun startListening() {
        val uid = auth.currentUser?.uid ?: return

        db.collection(usersCollection)
            .document(uid)
            .collection(collectionName)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    // You might log the error here
                    return@addSnapshotListener
                }

                val events =
                    snapshot.documents
                        .mapNotNull { it.toScheduleEvent() }
                        .sortedWith(
                            compareBy<ScheduleEvent> { it.date }.thenBy { it.time ?: LocalTime.MIN })

                _events.value = events
            }
    }

    private fun userScheduleCollection(uid: String) =
        db.collection(usersCollection).document(uid).collection(collectionName)

    private fun requireUid(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("No authenticated user for ScheduleRepository")
    }

    override suspend fun save(event: ScheduleEvent) {
        // Treat save as an upsert
        val uid = requireUid()
        val collection = userScheduleCollection(uid)

        val id = if (event.id.isBlank()) collection.document().id else event.id
        val eventWithId = event.copy(id = id)

        collection.document(id).set(eventWithId.toFirestoreMap(), SetOptions.merge()).await()
        // No need to update _events manually; snapshot listener will fire.
    }

    override suspend fun update(event: ScheduleEvent) {
        val uid = requireUid()
        val id =
            event.id.ifBlank {
                throw IllegalArgumentException("update() requires an event with a non-blank id")
            }

        userScheduleCollection(uid).document(id).set(event.toFirestoreMap(), SetOptions.merge()).await()
    }

    override suspend fun delete(eventId: String) {
        val uid = requireUid()
        if (eventId.isBlank()) return

        userScheduleCollection(uid).document(eventId).delete().await()
    }

    override suspend fun getEventsBetween(start: LocalDate, end: LocalDate): List<ScheduleEvent> =
        events.value.filter { it.date in start..end }

    override suspend fun getById(id: String): ScheduleEvent? =
        events.value.firstOrNull { it.id == id }

    override suspend fun moveEventDate(id: String, newDate: LocalDate): Boolean {
        val event = getById(id) ?: return false
        val updated = event.copy(date = newDate)
        update(updated)
        return true
    }

    override suspend fun getEventsForDate(date: LocalDate): List<ScheduleEvent> =
        events.value.filter { it.date == date }

    override suspend fun getEventsForWeek(startDate: LocalDate): List<ScheduleEvent> {
        val endDate = startDate.plusDays(6)
        return getEventsBetween(startDate, endDate)
    }
}

/* ---------- Firestore mapping helpers ---------- */

private fun ScheduleEvent.toFirestoreMap(): Map<String, Any?> =
    mapOf(
        "title" to title,
        "date" to date.toString(), // ISO-8601, e.g. "2025-11-16"
        "time" to time?.toString(), // ISO-8601 time, e.g. "14:30"
        "durationMinutes" to durationMinutes,
        "kind" to kind.name,
        "description" to description,
        "isCompleted" to isCompleted,
        "priority" to priority?.name,
        "courseCode" to courseCode,
        "location" to location,
        "sourceTag" to sourceTag.name,
    )

private fun DocumentSnapshot.toScheduleEvent(): ScheduleEvent? {
    val title = getString("title") ?: return null
    val dateStr = getString("date") ?: return null

    val date = kotlin.runCatching { LocalDate.parse(dateStr) }.getOrNull() ?: return null
    val timeStr = getString("time")
    val time = timeStr?.let { kotlin.runCatching { LocalTime.parse(it) }.getOrNull() }

    val durationMinutes = getLong("durationMinutes")?.toInt()

    val kindName = getString("kind")
    val kind =
        kindName
            ?.let { kotlin.runCatching { EventKind.valueOf(it) }.getOrNull() }
            ?: EventKind.STUDY

    val isCompleted = getBoolean("isCompleted") ?: false
    val description = getString("description")

    val priorityName = getString("priority")
    val priority =
        priorityName?.let { kotlin.runCatching { Priority.valueOf(it) }.getOrNull() }

    val courseCode = getString("courseCode")
    val location = getString("location")

    val sourceTagName = getString("sourceTag") ?: SourceTag.Task.name
    val sourceTag =
        kotlin.runCatching { SourceTag.valueOf(sourceTagName) }.getOrNull() ?: SourceTag.Task

    return ScheduleEvent(
        id = id, // Firestore doc id
        title = title,
        date = date,
        time = time,
        durationMinutes = durationMinutes,
        kind = kind,
        description = description,
        isCompleted = isCompleted,
        priority = priority,
        courseCode = courseCode,
        location = location,
        sourceTag = sourceTag)
}
