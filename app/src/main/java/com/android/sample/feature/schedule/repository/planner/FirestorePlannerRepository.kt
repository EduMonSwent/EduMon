package com.android.sample.feature.schedule.repository.planner

import com.android.sample.feature.schedule.data.planner.AttendanceStatus
import com.android.sample.feature.schedule.data.planner.Class
import com.android.sample.feature.schedule.data.planner.ClassAttendance
import com.android.sample.feature.schedule.data.planner.ClassType
import com.android.sample.feature.schedule.data.planner.CompletionStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Firestore-backed implementation of PlannerRepository.
 * - Classes: /users/{uid}/planner/classes/{classId}
 * - Attendance: /users/{uid}/planner/attendance/{docId}
 */
class FirestorePlannerRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
) : PlannerRepository() {

  private val usersCollection = "users"
  private val plannerRoot = "planner"
  private val classesCollection = "classes"
  private val attendanceCollection = "attendance"

  private fun requireUid(): String =
      auth.currentUser?.uid
          ?: throw IllegalStateException("No authenticated user for PlannerRepository")

  private fun userPlannerClassesRef(uid: String) =
      db.collection(usersCollection).document(uid).collection(classesCollection)

  private fun userPlannerAttendanceRef(uid: String) =
      db.collection(usersCollection)
          .document(uid)
          .collection(plannerRoot)
          .document("meta")
          .collection(attendanceCollection)

  // ---------- Classes ----------

  override fun getTodayClassesFlow(): Flow<List<Class>> {
    val uid = auth.currentUser?.uid

    if (uid == null) {
      return super.getTodayClassesFlow()
    }

    return callbackFlow {
      val registration =
          userPlannerClassesRef(uid).addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) {
              trySend(emptyList())
              return@addSnapshotListener
            }

            val classes =
                snapshot.documents.mapNotNull { it.toPlannerClass() }.sortedBy { it.startTime }

            trySend(classes).isSuccess
          }

      awaitClose { registration.remove() }
    }
  }

  // ---------- Attendance ----------

  override fun getTodayAttendanceFlow(): Flow<List<ClassAttendance>> {
    val uid = auth.currentUser?.uid
    if (uid == null) {
      return super.getTodayAttendanceFlow()
    }

    val todayStr = LocalDate.now().toString()

    return callbackFlow {
      val registration =
          userPlannerAttendanceRef(uid).addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) {
              trySend(emptyList())
              return@addSnapshotListener
            }

            val records =
                snapshot.documents
                    .mapNotNull { it.toClassAttendance() }
                    .filter { it.date.toString() == todayStr }

            trySend(records)
          }

      awaitClose { registration.remove() }
    }
  }

  override fun getAttendanceForClass(classId: String): Flow<ClassAttendance?> {
    val uid = auth.currentUser?.uid
    if (uid == null) {
      return super.getAttendanceForClass(classId)
    }

    val todayStr = LocalDate.now().toString()

    return callbackFlow {
      val registration =
          userPlannerAttendanceRef(uid).addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) {
              trySend(null)
              return@addSnapshotListener
            }

            val match =
                snapshot.documents
                    .mapNotNull { it.toClassAttendance() }
                    .firstOrNull { it.classId == classId && it.date.toString() == todayStr }

            trySend(match)
          }

      awaitClose { registration.remove() }
    }
  }

  override suspend fun saveAttendance(attendance: ClassAttendance): Result<Unit> {
    val uid = auth.currentUser?.uid

    if (uid == null) {
      return super.saveAttendance(attendance)
    }

    return try {
      val col = userPlannerAttendanceRef(uid)

      val docId = "${attendance.classId}_${attendance.date}"
      col.document(docId).set(attendance.toFirestoreMap(), SetOptions.merge()).await()

      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  // You can ignore seedDemoData or implement a Firestore seeding method if you like.
  override suspend fun seedDemoData() {
    super.seedDemoData()
  }
}

/* ---------- Mapping helpers ---------- */

private fun DocumentSnapshot.toPlannerClass(): Class? {
  val courseName = getString("courseName") ?: return null
  val startStr = getString("startTime") ?: return null
  val endStr = getString("endTime") ?: return null
  val typeStr = getString("type") ?: ClassType.LECTURE.name

  val startTime = runCatching { LocalTime.parse(startStr) }.getOrNull() ?: return null
  val endTime = runCatching { LocalTime.parse(endStr) }.getOrNull() ?: startTime

  val type = runCatching { ClassType.valueOf(typeStr) }.getOrNull() ?: ClassType.LECTURE

  val location = getString("location") ?: ""
  val instructor = getString("instructor") ?: ""

  return Class(
      id = id,
      courseName = courseName,
      startTime = startTime,
      endTime = endTime,
      type = type,
      location = location,
      instructor = instructor)
}

private fun Class.toFirestoreMap(): Map<String, Any?> =
    mapOf(
        "courseName" to courseName,
        "startTime" to startTime.toString(), // "09:00"
        "endTime" to endTime.toString(),
        "type" to type.name,
        "location" to location,
        "instructor" to instructor)

private fun DocumentSnapshot.toClassAttendance(): ClassAttendance? {
  val classId = getString("classId") ?: return null
  val dateStr = getString("date") ?: return null

  val date = runCatching { LocalDate.parse(dateStr) }.getOrNull() ?: return null
  val attendanceStr = getString("attendance") ?: AttendanceStatus.YES.name
  val completionStr = getString("completion") ?: CompletionStatus.NO.name

  val attendance =
      runCatching { AttendanceStatus.valueOf(attendanceStr) }.getOrNull() ?: AttendanceStatus.YES
  val completion =
      runCatching { CompletionStatus.valueOf(completionStr) }.getOrNull() ?: CompletionStatus.NO

  val timestampMillis = getLong("timestamp") ?: Instant.now().toEpochMilli()
  val timestamp = Instant.ofEpochMilli(timestampMillis)

  return ClassAttendance(
      classId = classId,
      date = date,
      attendance = attendance,
      completion = completion,
      timestamp = timestamp)
}

private fun ClassAttendance.toFirestoreMap(): Map<String, Any?> =
    mapOf(
        "classId" to classId,
        "date" to date.toString(), // "2025-11-16"
        "attendance" to attendance.name,
        "completion" to completion.name,
        "timestamp" to timestamp.toEpochMilli())
