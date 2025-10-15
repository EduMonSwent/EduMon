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
import kotlin.String
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** UI state for the AddToDo screen. This state holds the data needed to create a new ToDo item. */
data class EditTodoUIState(
    val title: String = "",
    val description: String = "",
    val assigneeName: String = "",
    val dueDate: String = "",
    val selectedLocation: Location? = null,
    val status: ToDoStatus = ToDoStatus.CREATED,
    val errorMsg: String? = null,
    val createdAt: Timestamp = Timestamp.now(),
)

class EditTodoViewModel(
    private val repository: ToDosRepository = ToDosRepositoryProvider.repository,
) : ViewModel() {
  // AddToDo UI state
  private val _uiState = MutableStateFlow(EditTodoUIState())
  val uiState: StateFlow<EditTodoUIState> = _uiState.asStateFlow()

  /** Clears the error message in the UI state. */
  fun clearErrorMsg() {
    _uiState.value = _uiState.value.copy(errorMsg = null)
  }

  /** Sets an error message in the UI state. */
  private fun setErrorMsg(errorMsg: String) {
    _uiState.value = _uiState.value.copy(errorMsg = errorMsg)
  }

  /**
   * Loads a ToDo by its ID and updates the UI state.
   *
   * @param todoID The ID of the ToDo to be loaded.
   */
  fun loadTodo(todoID: String) {
    viewModelScope.launch {
      try {
        val todo = repository.getTodo(todoID)
        _uiState.value =
            EditTodoUIState(
                title = todo.name,
                description = todo.description,
                assigneeName = todo.assigneeName,
                dueDate =
                    todo.dueDate.let {
                      val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                      return@let dateFormat.format(todo.dueDate.toDate())
                    },
                selectedLocation = todo.location,
                status = todo.status,
                createdAt = todo.createdAt,
            )
      } catch (e: Exception) {
        Log.e("EditTodoViewModel", "Error loading ToDo by ID: $todoID", e)
        setErrorMsg("Failed to load ToDo: ${e.message}")
      }
    }
  }

  /**
   * Adds a ToDo document.
   *
   * @param todo The ToDo document to be added.
   */
  fun editTodo(id: String) {
    val state = _uiState.value
    val dateStr = state.dueDate.trim()
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
          Log.e("EditTodoViewModel", "Error parsing date: $dateStr", e)
          setErrorMsg("Invalid date value: $dateStr")
          return
        }

    editTodoToRepository(
        id = id,
        todo =
            ToDo(
                name = state.title,
                description = state.description,
                assigneeName = state.assigneeName,
                dueDate = Timestamp(date),
                location = state.selectedLocation,
                status = state.status,
                uid = id,
                ownerId = "Walter Hardwell White",
                createdAt = state.createdAt))
    clearErrorMsg()
  }

  /**
   * Edits a ToDo document in the repository.
   *
   * @param id The ID of the ToDo document to be edited.
   * @param todo The ToDo object containing the new values.
   */
  private fun editTodoToRepository(id: String, todo: ToDo) {
    viewModelScope.launch {
      try {
        repository.editTodo(todoID = id, newValue = todo)
      } catch (e: Exception) {
        Log.e("AddToDoViewModel", "Error adding ToDo", e)
        setErrorMsg("Failed to add ToDo: ${e.message}")
      }
    }
  }

  /**
   * Deletes a ToDo document by its ID.
   *
   * @param todoID The ID of the ToDo document to be deleted.
   */
  fun deleteToDo(todoID: String) {
    viewModelScope.launch {
      try {
        repository.deleteTodo(todoID = todoID)
      } catch (e: Exception) {
        Log.e("EditTodoViewModel", "Error deleting ToDo", e)
        setErrorMsg("Failed to delete ToDo: ${e.message}")
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

  fun setLocationName(name: String) {
    val currentLocation = _uiState.value.selectedLocation
    if (currentLocation != null) {

      val updatedLocation = currentLocation.copy(name = name)
      _uiState.value = _uiState.value.copy(selectedLocation = updatedLocation)
      return
    }

    val newLocation = Location(0.0, 0.0, name)
    _uiState.value = _uiState.value.copy(selectedLocation = newLocation)
  }

  fun setStatus(status: ToDoStatus) {
    _uiState.value = _uiState.value.copy(status = status)
  }
}
