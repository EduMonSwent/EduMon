package com.android.sample.feature.subjects.repository

import com.android.sample.feature.subjects.model.StudySubject
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

/**
 * This code has been written partially using A.I (LLM).
 *
 * Firestore-backed implementation of SubjectsRepository.
 *
 * Collection: /users/{uid}/subjects/{subjectId} Fields:
 * - name: String
 * - colorIndex: Int
 * - totalStudyMinutes: Int
 */
class FirestoreSubjectsRepository(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : SubjectsRepository {

  private val _subjects = MutableStateFlow<List<StudySubject>>(emptyList())
  override val subjects: StateFlow<List<StudySubject>> = _subjects

  private var listener: ListenerRegistration? = null

  private val uid: String
    get() =
        auth.currentUser?.uid
            ?: throw IllegalStateException(
                "User must be logged in before using SubjectsRepository.")

  private val collection
    get() = db.collection(USERS_COLLECTION).document(uid).collection(SUBJECTS_COLLECTION)

  override suspend fun start() =
      withContext(ioDispatcher) {
        if (listener != null) return@withContext

        listener =
            collection.addSnapshotListener { snapshot, error ->
              if (error != null) return@addSnapshotListener
              if (snapshot == null) return@addSnapshotListener

              val list = snapshot.documents.mapNotNull { doc -> doc.toStudySubject() }

              _subjects.value = list
            }
      }

  override suspend fun createSubject(name: String, colorIndex: Int) =
      withContext(ioDispatcher) {
        val newDoc = collection.document()
        val data =
            mapOf(
                FIELD_NAME to name,
                FIELD_COLOR_INDEX to colorIndex,
                FIELD_TOTAL_MINUTES to DEFAULT_TOTAL_MINUTES,
            )

        newDoc.set(data)
        Unit
      }

  override suspend fun renameSubject(id: String, newName: String) =
      withContext(ioDispatcher) {
        collection.document(id).update(FIELD_NAME, newName)
        Unit
      }

  override suspend fun deleteSubject(id: String) =
      withContext(ioDispatcher) {
        collection.document(id).delete()
        Unit
      }

  override suspend fun addStudyMinutesToSubject(id: String, minutes: Int) =
      withContext(ioDispatcher) {
        if (minutes <= 0) return@withContext

        val docRef = collection.document(id)
        db.runTransaction { tx ->
              val snap = tx.get(docRef)
              val current = snap.toStudySubject() ?: return@runTransaction null
              val updatedTotal =
                  (current.totalStudyMinutes + minutes).coerceAtLeast(DEFAULT_TOTAL_MINUTES)
              tx.update(docRef, FIELD_TOTAL_MINUTES, updatedTotal)
              null
            }
            .addOnFailureListener {
              // Optional: log error if you have a logging mechanism.
            }

        Unit
      }

  private fun DocumentSnapshot.toStudySubject(): StudySubject? {
    val id = id
    val name = getString(FIELD_NAME) ?: return null
    val colorIndex = getLong(FIELD_COLOR_INDEX)?.toInt() ?: DEFAULT_COLOR_INDEX
    val totalMinutes = getLong(FIELD_TOTAL_MINUTES)?.toInt() ?: DEFAULT_TOTAL_MINUTES

    return StudySubject(
        id = id,
        name = name,
        colorIndex = colorIndex,
        totalStudyMinutes = totalMinutes,
    )
  }

  companion object {
    // This code has been written partially using A.I (LLM).

    private const val USERS_COLLECTION = "users"
    private const val SUBJECTS_COLLECTION = "subjects"

    private const val FIELD_NAME = "name"
    private const val FIELD_COLOR_INDEX = "colorIndex"
    private const val FIELD_TOTAL_MINUTES = "totalStudyMinutes"

    private const val DEFAULT_COLOR_INDEX = 0
    private const val DEFAULT_TOTAL_MINUTES = 0
  }
}
