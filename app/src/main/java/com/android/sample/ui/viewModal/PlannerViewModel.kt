package com.android.sample.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.sample.model.planner.AttendanceStatus
import com.android.sample.model.planner.Class
import com.android.sample.model.planner.ClassAttendance
import com.android.sample.model.planner.CompletionStatus
import com.android.sample.model.planner.PlannerRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class PlannerUiState(
    val classes: List<Class> = emptyList(),
    val attendanceRecords: List<ClassAttendance> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val showAddTaskModal: Boolean = false,
    val showAttendanceModal: Boolean = false,
    val selectedClass: Class? = null
)

class PlannerViewModel(private val plannerRepository: PlannerRepository = PlannerRepository()) :
    ViewModel() {

  private val _uiState = MutableStateFlow(PlannerUiState())
  val uiState: StateFlow<PlannerUiState> = _uiState.asStateFlow()

  private val _eventFlow = MutableSharedFlow<UiEvent>()
  val eventFlow = _eventFlow.asSharedFlow()

  sealed class UiEvent {
    data class ShowSnackbar(val message: String) : UiEvent()
  }

  init {
    observePlannerData()
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
}
