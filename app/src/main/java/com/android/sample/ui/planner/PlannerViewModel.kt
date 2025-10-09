package com.android.sample.ui.planner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.sample.model.planner.AttendanceStatus
import com.android.sample.model.planner.Class
import com.android.sample.model.planner.ClassAttendance
import com.android.sample.model.planner.CompletionStatus
import com.android.sample.model.planner.PlannerRepository // Your repository
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlannerViewModel(private val profileRepository: PlannerRepository = PlannerRepository()) :
    ViewModel() {

  private val _todayClasses = MutableStateFlow<List<Class>>(emptyList())
  val todayClasses: StateFlow<List<Class>> = _todayClasses.asStateFlow()

  private val _todayAttendanceRecords = MutableStateFlow<List<ClassAttendance>>(emptyList())
  val todayAttendanceRecords: StateFlow<List<ClassAttendance>> =
      _todayAttendanceRecords.asStateFlow()

  private val _showAddStudyTaskModal = MutableStateFlow(false)
  val showAddStudyTaskModal: StateFlow<Boolean> = _showAddStudyTaskModal.asStateFlow()

  private val _showClassAttendanceModal = MutableStateFlow(false)
  val showClassAttendanceModal: StateFlow<Boolean> = _showClassAttendanceModal.asStateFlow()

  private val _selectedClassForAttendance = MutableStateFlow<Class?>(null)
  val selectedClassForAttendance: StateFlow<Class?> = _selectedClassForAttendance.asStateFlow()

  init {
    loadTodayClasses()
    loadTodayAttendanceRecords()
  }

  private fun loadTodayClasses() {
    viewModelScope.launch { _todayClasses.value = profileRepository.getTodayClasses() }
  }

  private fun loadTodayAttendanceRecords() {
    viewModelScope.launch {
      _todayAttendanceRecords.value = profileRepository.getTodayAttendanceRecords()
    }
  }

  fun onAddStudyTaskClicked() {
    _showAddStudyTaskModal.value = true
  }

  fun onDismissAddStudyTaskModal() {
    _showAddStudyTaskModal.value = false
  }

  fun onClassClicked(classItem: Class) {
    _selectedClassForAttendance.value = classItem
    _showClassAttendanceModal.value = true
  }

  fun onDismissClassAttendanceModal() {
    _showClassAttendanceModal.value = false
    _selectedClassForAttendance.value = null
  }

  private val _eventFlow = MutableSharedFlow<UiEvent>() // For one-time events
  val eventFlow = _eventFlow.asSharedFlow()

  sealed class UiEvent {
    data class ShowSnackbar(val message: String) : UiEvent()
  }

  fun saveClassAttendance(
      classItem: Class,
      attendance: AttendanceStatus,
      completion: CompletionStatus
  ) {
    viewModelScope.launch {
      try {
        val attendanceRecord =
            ClassAttendance(
                classId = classItem.id,
                date = LocalDate.now(), // Always current date for "today's classes"
                attendance = attendance,
                completion = completion)
        profileRepository.saveAttendance(attendanceRecord)
        loadTodayAttendanceRecords() // Refresh records after saving
        onDismissClassAttendanceModal()
        profileRepository.saveAttendance(attendanceRecord)
        _eventFlow.emit(UiEvent.ShowSnackbar("Attendance saved successfully!"))
        loadTodayAttendanceRecords()
        onDismissClassAttendanceModal()
      } catch (e: Exception) {
        _eventFlow.emit(UiEvent.ShowSnackbar("Error saving attendance: ${e.localizedMessage}"))
      }
    }
  }
}
