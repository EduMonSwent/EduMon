package com.android.sample.model.planner

import androidx.lifecycle.ViewModel
import com.android.sample.model.planner.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class TestPlannerViewModel : ViewModel() {
  private val _todayClasses = MutableStateFlow<List<Class>>(emptyList())
  val todayClasses: StateFlow<List<Class>> = _todayClasses

  private val _todayAttendanceRecords = MutableStateFlow<List<ClassAttendance>>(emptyList())
  val todayAttendanceRecords: StateFlow<List<ClassAttendance>> = _todayAttendanceRecords

  private val _showAddStudyTaskModal = MutableStateFlow(false)
  val showAddStudyTaskModal: StateFlow<Boolean> = _showAddStudyTaskModal

  private val _showClassAttendanceModal = MutableStateFlow(false)
  val showClassAttendanceModal: StateFlow<Boolean> = _showClassAttendanceModal

  private val _selectedClassForAttendance = MutableStateFlow<Class?>(null)
  val selectedClassForAttendance: StateFlow<Class?> = _selectedClassForAttendance

  fun setTestData(classes: List<Class>, attendance: List<ClassAttendance>) {
    _todayClasses.value = classes
    _todayAttendanceRecords.value = attendance
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
}
