package com.android.sample.ui.flashcards.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.sample.ui.flashcards.data.FirestoreFlashcardsRepoProvider
import com.android.sample.ui.flashcards.data.FlashcardsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ImportDeckViewModel(
    private val repo: FlashcardsRepository = FirestoreFlashcardsRepoProvider.get()
) : ViewModel() {

  private val _state = MutableStateFlow(ImportState())
  val state: StateFlow<ImportState> = _state

  fun importToken(token: String) {
    viewModelScope.launch {
      _state.value = ImportState(loading = true)

      val newId = repo.importSharedDeck(token.trim())

      if (newId.isBlank()) {
        _state.value = ImportState(error = "Share code not valid")
      } else {
        _state.value = ImportState(success = true)
      }
    }
  }
}

data class ImportState(
    val loading: Boolean = false,
    val success: Boolean = false,
    val error: String? = null
)
