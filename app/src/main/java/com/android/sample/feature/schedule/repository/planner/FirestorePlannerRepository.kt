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
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

/**
 * Firestore-backed implementation of PlannerRepository.
 * - Classes: /users/{uid}/classes/{classId}
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

  private fun userPlannerClassesRef(uid: String) =
      db.collection(usersCollection).document(uid).collection(classesCollection)

  private fun userPlannerAttendanceRef(uid: String) =
      db.collection(usersCollection)
          .document(uid)
          .collection(plannerRoot)
          .document("meta")
          .collection(attendanceCollection)

  // ---------- Classes ----------
  private fun classesSnapshotFlow(uid: String): Flow<List<Class>> = callbackFlow {
    val registration =
        userPlannerClassesRef(uid).addSnapshotListener { snapshot, error ->
          if (error != null || snapshot == null) {
            trySend(emptyList())
            return@addSnapshotListener
          }

          val classes =
              snapshot.documents.mapNotNull { it.toPlannerClass() }.sortedBy { it.startTime }

          trySend(classes)
        }

    awaitClose { registration.remove() }
  }

  private fun attendanceSnapshotFlow(uid: String, dateIso: String): Flow<List<ClassAttendance>> =
      callbackFlow {
        val registration =
            userPlannerAttendanceRef(uid).addSnapshotListener { snapshot, error ->
              if (error != null || snapshot == null) {
                trySend(emptyList())
                return@addSnapshotListener
              }

              val records =
                  snapshot.documents
                      .mapNotNull { it.toClassAttendance() }
                      .filter { it.date.toString() == dateIso }

              trySend(records)
            }

        awaitClose { registration.remove() }
      }

  override fun getTodayClassesFlow(): Flow<List<Class>> {
    val uid = auth.currentUser?.uid ?: return super.getTodayClassesFlow()
    return classesSnapshotFlow(uid)
  }

  // ---------- Attendance ----------

  override fun getTodayAttendanceFlow(): Flow<List<ClassAttendance>> {
    val uid = auth.currentUser?.uid ?: return super.getTodayAttendanceFlow()
    val todayStr = LocalDate.now().toString()

    return attendanceSnapshotFlow(uid, todayStr)
  }

  override fun getAttendanceForClass(classId: String): Flow<ClassAttendance?> {
    val uid = auth.currentUser?.uid ?: return super.getAttendanceForClass(classId)
    val todayStr = LocalDate.now().toString()

    // Re-use shared attendance flow and just pick the matching class for today
    return attendanceSnapshotFlow(uid, todayStr).map { records ->
      records.firstOrNull { it.classId == classId }
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

  override suspend fun saveClass(classItem: Class): Result<Unit> {
    val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))

    return try {
      userPlannerClassesRef(uid)
          .document(classItem.id)
          .set(classItem.toFirestoreMap(), SetOptions.merge())
          .await()

      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  override suspend fun clearClasses(): Result<Unit> {
    val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not logged in"))

    return try {
      // 1. Get all documents in the classes collection
      val snapshot = userPlannerClassesRef(uid).get().await()

      // 2. Create a batch to delete them all at once (more efficient)
      val batch = db.batch()
      for (doc in snapshot.documents) {
        batch.delete(doc.reference)
      }

      // 3. Commit the delete
      batch.commit().await()

      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }
}

/* ---------- Mapping helpers ---------- */

private fun DocumentSnapshot.toPlannerClass(): Class? {
  val courseName = getString("courseName") ?: return null
  val startStr = getString("startTime") ?: return null
  val endStr = getString("endTime") ?: return null
  val typeStr = getString("type") ?: ClassType.LECTURE.name
  val daysList = get("daysOfWeek") as? List<String>
  val daysOfWeek =
      if (daysList != null) {
        daysList.mapNotNull { runCatching { DayOfWeek.valueOf(it) }.getOrNull() }
      } else {
        // Fallback: If "dayOfWeek" (singular) exists, use it. Otherwise, assume every day.
        val singleDay = getString("dayOfWeek")
        if (singleDay != null) {
          listOfNotNull(runCatching { DayOfWeek.valueOf(singleDay) }.getOrNull())
        } else {
          DayOfWeek.values().toList()
        }
      }

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
      instructor = instructor,
      daysOfWeek = daysOfWeek)
}

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

private fun Class.toFirestoreMap(): Map<String, Any?> =
    mapOf(
        "courseName" to courseName,
        "startTime" to startTime.toString(),
        "endTime" to endTime.toString(),
        "type" to type.name,
        "location" to location,
        "instructor" to instructor,
        "daysOfWeek" to daysOfWeek.map { it.name })
