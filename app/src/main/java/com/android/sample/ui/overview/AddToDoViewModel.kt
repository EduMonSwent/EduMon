package com.android.sample.ui.overview

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.sample.model.map.Location
import com.android.sample.model.todo.ToDo
import com.android.sample.model.todo.ToDoStatus
import com.android.sample.model.todo.ToDosRepository
import com.android.sample.model.todo.ToDosRepositoryProvider
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** UI state for the AddToDo screen. This state holds the data needed to create a new ToDo item. */
data class AddTodoUIState(
    val title: String = "",
    val description: String = "",
    val assigneeName: String = "",
    val dueDate: String = "",
    val selectedLocation: Location? = null,
    val errorMsg: String? = null,
)

/**
 * ViewModel for the AddToDo screen. This ViewModel manages the state of input fields for the
 * AddToDo screen.
 */
class AddTodoViewModel(
    private val repository: ToDosRepository = ToDosRepositoryProvider.repository,
) : ViewModel() {
  // AddToDo UI state
  private val _uiState = MutableStateFlow(AddTodoUIState())
  val uiState: StateFlow<AddTodoUIState> = _uiState.asStateFlow()

  /** Clears the error message in the UI state. */
  fun clearErrorMsg() {
    _uiState.value = _uiState.value.copy(errorMsg = null)
  }

  /** Sets an error message in the UI state. */
  private fun setErrorMsg(errorMsg: String) {
    _uiState.value = _uiState.value.copy(errorMsg = errorMsg)
  }

  /** Adds a ToDo document. */
  fun addTodo() {
    val state = _uiState.value
    val dateStr = state.dueDate
    val dateRegex = Regex("""^\d{2}/\d{2}/\d{4}$""")

    if (!dateRegex.matches(dateStr)) {
      setErrorMsg("Invalid format, date must be DD/MM/YYYY.")
      return
    }

    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    dateFormat.isLenient = false
    val date =
        try {
          dateFormat.parse(dateStr)
        } catch (e: Exception) {
          Log.e("AddToDoViewModel", "Error parsing date: $dateStr", e)
          setErrorMsg("Invalid date value: $dateStr")
          return
        }

    addToDoToRepository(
        ToDo(
            name = state.title,
            description = state.description,
            assigneeName = state.assigneeName,
            dueDate = Timestamp(date),
            location = state.selectedLocation,
            status = ToDoStatus.CREATED,
            uid = repository.getNewUid(),
            ownerId = "Walter Hardwell White"))
    clearErrorMsg()
  }

  private fun addToDoToRepository(todo: ToDo) {
    viewModelScope.launch {
      try {
        repository.addTodo(todo)
      } catch (e: Exception) {
        Log.e("AddToDoViewModel", "Error adding ToDo", e)
        setErrorMsg("Failed to add ToDo: ${e.message}")
      }
    }
  }

  // Functions to update the UI state.

  fun setTitle(title: String) {
    // Update _uiState accordingly
    _uiState.value = _uiState.value.copy(title = title)
  }

  fun setDescription(description: String) {
    _uiState.value = _uiState.value.copy(description = description)
  }

  fun setAssigneeName(assigneeName: String) {
    _uiState.value = _uiState.value.copy(assigneeName = assigneeName)
  }

  fun setDueDate(dueDate: String) {
    _uiState.value = _uiState.value.copy(dueDate = dueDate)
  }

  fun setLocation(location: Location) {
    _uiState.value = _uiState.value.copy(selectedLocation = location)
  }
}
