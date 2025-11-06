package com.android.sample.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.sample.model.PlannerRepository
import com.android.sample.model.StudyItem
import com.android.sample.model.calendar.PlannerRepository
import com.android.sample.model.calendar.StudyItem
import com.android.sample.repos_providors.AppRepositories
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CalendarViewModel(
    private val repository: PlannerRepository = AppRepositories.calendarRepository
) : ViewModel() {

  private val _selectedDate = MutableStateFlow(LocalDate.now())
  val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

  private val _currentDisplayMonth = MutableStateFlow(YearMonth.now())
  val currentDisplayMonth: StateFlow<YearMonth> = _currentDisplayMonth.asStateFlow()

  private val _isMonthView = MutableStateFlow(true)
  val isMonthView: StateFlow<Boolean> = _isMonthView.asStateFlow()

  private val _taskToEdit = MutableStateFlow<StudyItem?>(null)
  val taskToEdit: StateFlow<StudyItem?> = _taskToEdit.asStateFlow()

  private val _showAddEditModal = MutableStateFlow(false)
  val showAddEditModal: StateFlow<Boolean> = _showAddEditModal.asStateFlow()

  val allTasks: StateFlow<List<StudyItem>> =
      repository.tasksFlow.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

  val tasksForSelectedDate: StateFlow<List<StudyItem>> =
      combine(selectedDate, allTasks) { date, tasks -> tasks.filter { it.date == date } }
          .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

  // ---- Calendar navigation ----
  fun onNavigateMonthWeek(forward: Boolean) {
    val stepMonths = if (forward) 1 else -1
    val stepWeeks = if (forward) 1 else -1

    if (_isMonthView.value) {
      _currentDisplayMonth.value = _currentDisplayMonth.value.plusMonths(stepMonths.toLong())
    } else {
      _selectedDate.value = _selectedDate.value.plusWeeks(stepWeeks.toLong())
      _currentDisplayMonth.value = YearMonth.from(_selectedDate.value)
    }
  }

  fun onNextMonthWeekClicked() = onNavigateMonthWeek(forward = true)

  fun onPreviousMonthWeekClicked() = onNavigateMonthWeek(forward = false)

  fun onTodayClicked() {
    _selectedDate.value = LocalDate.now()
    _currentDisplayMonth.value = YearMonth.now()
  }

  fun toggleMonthWeekView() {
    _isMonthView.value = !_isMonthView.value
    if (_isMonthView.value) {
      _currentDisplayMonth.value = YearMonth.from(_selectedDate.value)
    }
  }

  // ---- Modal logic ----
  fun onAddTaskClicked(date: LocalDate = _selectedDate.value) {
    _taskToEdit.value = null
    _selectedDate.value = date
    _showAddEditModal.value = true
  }

  fun onEditTaskClicked(task: StudyItem) {
    _taskToEdit.value = task
    _selectedDate.value = task.date
    _currentDisplayMonth.value = YearMonth.from(task.date)
    _showAddEditModal.value = true
  }

  fun onDismissAddEditModal() {
    _showAddEditModal.value = false
    _taskToEdit.value = null
  }

  // ---- Save/Delete logic ----
  fun saveTask(task: StudyItem) {
    viewModelScope.launch {
      repository.saveTask(task)
      onDismissAddEditModal()
    }
  }

  fun deleteTask(task: StudyItem) {
    viewModelScope.launch {
      repository.deleteTask(task.id)
      onDismissAddEditModal()
    }
  }

  // ---- Helper for week rows ----
  fun startOfWeek(date: LocalDate): LocalDate {
    val dow = date.dayOfWeek.value // Monday = 1
    return date.minusDays((dow - 1).toLong())
  }

  fun onDateSelected(date: LocalDate) {
    _selectedDate.value = date
    if (!isMonthView.value) {
      _currentDisplayMonth.value = YearMonth.from(date)
    }
  }
}
