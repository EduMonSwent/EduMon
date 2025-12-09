package com.android.sample.feature.subjects.repository

import com.android.sample.feature.subjects.model.StudySubject
import kotlinx.coroutines.flow.StateFlow

/**
 * This code has been written partially using A.I (LLM).
 *
 * Repository abstraction for user subjects.
 */
interface SubjectsRepository {

  /** Reactive list of all subjects for the current user. */
  val subjects: StateFlow<List<StudySubject>>

  /** Idempotent: safe to call multiple times. */
  suspend fun start()

  /** Creates a new subject. */
  suspend fun createSubject(name: String, colorIndex: Int)

  /** Renames an existing subject. */
  suspend fun renameSubject(id: String, newName: String)

  /** Deletes a subject. */
  suspend fun deleteSubject(id: String)

  /** Adds study minutes to that subject (for stats / totals). */
  suspend fun addStudyMinutesToSubject(id: String, minutes: Int)
}
