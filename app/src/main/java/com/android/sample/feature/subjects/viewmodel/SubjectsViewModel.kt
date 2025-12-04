package com.android.sample.feature.subjects.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.sample.feature.subjects.model.StudySubject
import com.android.sample.feature.subjects.repository.SubjectsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * This code has been written partially using A.I (LLM).
 *
 * ViewModel for listing and selecting subjects in the study session screen.
 */
class SubjectsViewModel(
    private val repo: SubjectsRepository,
) : ViewModel() {

  private val _selectedSubjectId = MutableStateFlow<String?>(null)
  val selectedSubjectId: StateFlow<String?> = _selectedSubjectId.asStateFlow()

  val subjects: StateFlow<List<StudySubject>> = repo.subjects

  init {
    viewModelScope.launch { repo.start() }
  }

  fun onSubjectSelected(id: String) {
    _selectedSubjectId.value = id
  }

  fun onCreateSubject(name: String, colorIndex: Int) {
    viewModelScope.launch { repo.createSubject(name = name, colorIndex = colorIndex) }
  }

  fun onRenameSubject(id: String, newName: String) {
    viewModelScope.launch { repo.renameSubject(id = id, newName = newName) }
  }

  fun onDeleteSubject(id: String) {
    viewModelScope.launch {
      repo.deleteSubject(id = id)
      if (_selectedSubjectId.value == id) {
        _selectedSubjectId.value = null
      }
    }
  }
}
