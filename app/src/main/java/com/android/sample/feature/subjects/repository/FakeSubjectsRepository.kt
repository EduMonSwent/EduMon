package com.android.sample.feature.subjects.repository

import com.android.sample.feature.subjects.model.StudySubject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * This code has been written partially using A.I (LLM).
 *
 * Simple in-memory fake implementation of SubjectsRepository.
 */
class FakeSubjectsRepository : SubjectsRepository {

  private val _subjects = MutableStateFlow<List<StudySubject>>(emptyList())
  override val subjects: StateFlow<List<StudySubject>> = _subjects

  override suspend fun start() {
    // no-op
  }

  override suspend fun createSubject(name: String, colorIndex: Int) {
    val current = _subjects.value.toMutableList()
    val id = (current.size + 1).toString()
    current.add(
        StudySubject(
            id = id,
            name = name,
            colorIndex = colorIndex,
            totalStudyMinutes = 0,
        ))
    _subjects.value = current
  }

  override suspend fun renameSubject(id: String, newName: String) {
    val updated = _subjects.value.map { if (it.id == id) it.copy(name = newName) else it }
    _subjects.value = updated
  }

  override suspend fun deleteSubject(id: String) {
    _subjects.value = _subjects.value.filterNot { it.id == id }
  }

  override suspend fun addStudyMinutesToSubject(id: String, minutes: Int) {
    if (minutes <= 0) return
    val updated =
        _subjects.value.map {
          if (it.id == id) it.copy(totalStudyMinutes = it.totalStudyMinutes + minutes) else it
        }
    _subjects.value = updated
  }
}
