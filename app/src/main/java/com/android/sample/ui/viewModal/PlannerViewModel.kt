package com.android.sample.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.sample.data.Priority
import com.android.sample.data.Status
import com.android.sample.data.ToDo
import com.android.sample.model.planner.AttendanceStatus
import com.android.sample.model.planner.Class
import com.android.sample.model.planner.ClassAttendance
import com.android.sample.model.planner.CompletionStatus
import com.android.sample.model.planner.PlannerRepository
import com.android.sample.repositories.ToDoRepository
import com.android.sample.repositories.ToDoRepositoryProvider
import java.time.LocalDate
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class PlannerUiState(
    val classes: List<Class> = emptyList(),
    val attendanceRecords: List<ClassAttendance> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val showAddTaskModal: Boolean = false,
    val showAttendanceModal: Boolean = false,
    val selectedClass: Class? = null,
    // --- Added for adaptive planner ---
    val todos: List<ToDo> = emptyList(),
    val recommendedTask: ToDo? = null
)

open class PlannerViewModel(
    private val plannerRepository: PlannerRepository = PlannerRepository(),
    private val toDoRepository: ToDoRepository = ToDoRepositoryProvider.repository
) : ViewModel() {

  private val _uiState = MutableStateFlow(PlannerUiState())
  val uiState: StateFlow<PlannerUiState> = _uiState.asStateFlow()

  private val _eventFlow = MutableSharedFlow<UiEvent>()
  val eventFlow = _eventFlow.asSharedFlow()

  sealed class UiEvent {
    data class ShowSnackbar(val message: String) : UiEvent()
  }

  init {
    observePlannerData()
    observeToDos() // start observing tasks for recommendations
  }

  /** Collects classes and attendance records from the repository as Flows. */
  private fun observePlannerData() {
    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isLoading = true)

      try {
        // Collect class updates
        launch {
          plannerRepository
              .getTodayClassesFlow()
              .catch { e ->
                _uiState.value =
                    _uiState.value.copy(errorMessage = e.localizedMessage, isLoading = false)
              }
              .collectLatest { classList ->
                _uiState.value = _uiState.value.copy(classes = classList, isLoading = false)
              }
        }

        // Collect attendance updates
        launch {
          plannerRepository
              .getTodayAttendanceFlow()
              .catch { e ->
                _uiState.value =
                    _uiState.value.copy(errorMessage = e.localizedMessage, isLoading = false)
              }
              .collectLatest { records ->
                _uiState.value = _uiState.value.copy(attendanceRecords = records, isLoading = false)
              }
        }
      } catch (e: Exception) {
        _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = e.localizedMessage)
      }
    }
  }

  // --- Adaptive Planner Section ---
  private fun observeToDos() {

    viewModelScope.launch {
      // <-- add todo to repository here for visual testing
      toDoRepository.todos
          .catch { e -> _uiState.value = _uiState.value.copy(errorMessage = e.localizedMessage) }
          .collectLatest { todos ->
            val recommended = recommendNextTask(todos)
            _uiState.value = _uiState.value.copy(todos = todos, recommendedTask = recommended)
          }
    }
  }

  private fun recommendNextTask(tasks: List<ToDo>): ToDo? {
    if (tasks.isEmpty()) return null

    return tasks
        .filter { it.status != Status.DONE }
        .sortedWith(
            compareByDescending<ToDo> { it.priority.weight() }
                .thenBy { it.dueDate }
                .thenBy { it.status.ordinal * (-1) }) // In progress tasks first
        .firstOrNull()
  }

  private fun Priority.weight(): Int =
      when (this) {
        Priority.HIGH -> 3
        Priority.MEDIUM -> 2
        Priority.LOW -> 1
      }

  // --- Class Attendance Management (existing logic) ---
  fun onAddStudyTaskClicked() {
    _uiState.value = _uiState.value.copy(showAddTaskModal = true)
  }

  fun onDismissAddStudyTaskModal() {
    _uiState.value = _uiState.value.copy(showAddTaskModal = false)
  }

  fun onClassClicked(classItem: Class) {
    _uiState.value = _uiState.value.copy(selectedClass = classItem, showAttendanceModal = true)
  }

  fun onDismissClassAttendanceModal() {
    _uiState.value = _uiState.value.copy(selectedClass = null, showAttendanceModal = false)
  }

  fun saveClassAttendance(
      classItem: Class,
      attendance: AttendanceStatus,
      completion: CompletionStatus
  ) {
    viewModelScope.launch {
      val attendanceRecord =
          ClassAttendance(
              classId = classItem.id,
              date = LocalDate.now(),
              attendance = attendance,
              completion = completion)

      val result = plannerRepository.saveAttendance(attendanceRecord)

      if (result.isSuccess) {
        onDismissClassAttendanceModal()
        _eventFlow.emit(UiEvent.ShowSnackbar("Attendance saved successfully!"))
      } else {
        _eventFlow.emit(UiEvent.ShowSnackbar("Error saving attendance"))
      }
    }
  }

  fun clearError() {
    _uiState.value = _uiState.value.copy(errorMessage = null)
  }

  fun updateTestData(classes: List<Class>, attendance: List<ClassAttendance>) {
    _uiState.value = _uiState.value.copy(classes = classes, attendanceRecords = attendance)
  }

  // --- Optional for UI: force refresh recommendation manually ---
  fun refreshRecommendation() {
    _uiState.value = _uiState.value.copy(recommendedTask = recommendNextTask(_uiState.value.todos))
  }
}
