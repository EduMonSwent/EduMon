package com.android.sample.feature.schedule.viewmodel

import android.content.res.Resources
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.sample.R
import com.android.sample.data.ToDo
import com.android.sample.feature.schedule.data.planner.AttendanceStatus
import com.android.sample.feature.schedule.data.planner.Class
import com.android.sample.feature.schedule.data.planner.ClassAttendance
import com.android.sample.feature.schedule.data.planner.CompletionStatus
import com.android.sample.feature.schedule.data.schedule.ScheduleEvent
import com.android.sample.feature.schedule.repository.planner.PlannerRepository
import com.android.sample.feature.schedule.repository.schedule.ScheduleRepository
import com.android.sample.repos_providors.AppRepositories
import com.android.sample.repositories.ToDoRepository
import com.android.sample.ui.schedule.AdaptivePlanner
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/** This class was implemented with the help of ai (chatgbt) */
data class ScheduleUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val currentDisplayMonth: YearMonth = YearMonth.now(),
    val isMonthView: Boolean = true,
    val allEvents: List<ScheduleEvent> = emptyList(),
    val isAdjustingPlan: Boolean = false,
    val todayClasses: List<Class> = emptyList(),
    val attendanceRecords: List<ClassAttendance> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val showAddTaskModal: Boolean = false,
    val showAttendanceModal: Boolean = false,
    val selectedClass: Class? = null,
    val todos: List<ToDo> = emptyList()
)

class ScheduleViewModel(
    private val scheduleRepository: ScheduleRepository,
    private val plannerRepository: PlannerRepository,
    private val resources: Resources,
    private val toDoRepository: ToDoRepository = AppRepositories.toDoRepository
) : ViewModel() {

  private val _uiState = MutableStateFlow(ScheduleUiState())
  val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

  private val _eventFlow = MutableSharedFlow<UiEvent>()
  val eventFlow = _eventFlow.asSharedFlow()

  sealed class UiEvent {
    data class ShowSnackbar(val message: String) : UiEvent()
  }

  init {
    observeScheduleData()
    observePlannerData()
    observeTodos()
  }

  private fun observeScheduleData() {
    viewModelScope.launch {
      scheduleRepository.events.collect { events ->
        _uiState.update { it.copy(allEvents = events) }
      }
    }
  }

  private fun observePlannerData() {
    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true) }

      // Collect today's classes
      launch {
        plannerRepository
            .getTodayClassesFlow()
            .catch { e ->
              _uiState.update { it.copy(errorMessage = e.localizedMessage, isLoading = false) }
            }
            .collect { classes ->
              _uiState.update { it.copy(todayClasses = classes, isLoading = false) }
            }
      }

      // Collect attendance records
      launch {
        plannerRepository
            .getTodayAttendanceFlow()
            .catch { e ->
              _uiState.update { it.copy(errorMessage = e.localizedMessage, isLoading = false) }
            }
            .collect { attendance ->
              _uiState.update { it.copy(attendanceRecords = attendance, isLoading = false) }
            }
      }
    }
  }

  private fun observeTodos() {
    viewModelScope.launch {
      toDoRepository.todos.collect { todos -> _uiState.update { it.copy(todos = todos) } }
    }
  }

  fun onDateSelected(date: LocalDate) {
    _uiState.update { it.copy(selectedDate = date) }
  }

  fun onPreviousMonthWeekClicked() {
    val currentState = _uiState.value
    if (currentState.isMonthView) {
      val newMonth = currentState.currentDisplayMonth.minusMonths(1)
      _uiState.update {
        it.copy(
            currentDisplayMonth = newMonth,
            selectedDate =
                currentState.selectedDate
                    .withDayOfMonth(
                        currentState.selectedDate.dayOfMonth.coerceAtMost(newMonth.lengthOfMonth()))
                    .withMonth(newMonth.monthValue)
                    .withYear(newMonth.year))
      }
    } else {
      val newDate = currentState.selectedDate.minusWeeks(1)
      _uiState.update {
        it.copy(selectedDate = newDate, currentDisplayMonth = YearMonth.from(newDate))
      }
    }
  }

  fun onNextMonthWeekClicked() {
    val currentState = _uiState.value
    if (currentState.isMonthView) {
      val newMonth = currentState.currentDisplayMonth.plusMonths(1)
      _uiState.update {
        it.copy(
            currentDisplayMonth = newMonth,
            selectedDate =
                currentState.selectedDate
                    .withDayOfMonth(
                        currentState.selectedDate.dayOfMonth.coerceAtMost(newMonth.lengthOfMonth()))
                    .withMonth(newMonth.monthValue)
                    .withYear(newMonth.year))
      }
    } else {
      val newDate = currentState.selectedDate.plusWeeks(1)
      _uiState.update {
        it.copy(selectedDate = newDate, currentDisplayMonth = YearMonth.from(newDate))
      }
    }
  }

  fun setWeekMode() {
    _uiState.update { it.copy(isMonthView = false) }
  }

  fun setMonthMode() {
    _uiState.update { it.copy(isMonthView = true) }
  }

  fun toggleMonthWeekView() {
    _uiState.update { it.copy(isMonthView = !it.isMonthView) }
  }

  // Planner methods
  fun onAddStudyTaskClicked() {
    _uiState.update { it.copy(showAddTaskModal = true) }
  }

  fun onDismissAddStudyTaskModal() {
    _uiState.update { it.copy(showAddTaskModal = false) }
  }

  fun onClassClicked(classItem: Class) {
    _uiState.update { it.copy(selectedClass = classItem, showAttendanceModal = true) }
  }

  fun onDismissClassAttendanceModal() {
    _uiState.update { it.copy(selectedClass = null, showAttendanceModal = false) }
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
        _uiState.update { st ->
          val updated =
              st.attendanceRecords.filterNot { it.classId == classItem.id } + attendanceRecord
          st.copy(attendanceRecords = updated)
        }
        _eventFlow.emit(
            UiEvent.ShowSnackbar(resources.getString(R.string.attendance_saved_success)))
      } else {
        _eventFlow.emit(UiEvent.ShowSnackbar(resources.getString(R.string.attendance_save_error)))
      }
    }
  }

  // Schedule methods
  fun save(event: ScheduleEvent) =
      viewModelScope.launch {
        try {
          scheduleRepository.save(event)
          _eventFlow.emit(UiEvent.ShowSnackbar(resources.getString(R.string.task_saved)))
          _uiState.update { it.copy(showAddTaskModal = false) } // if coming from modal
        } catch (e: Exception) {
          _eventFlow.emit(UiEvent.ShowSnackbar(resources.getString(R.string.task_save_error)))
        }
      }

  fun delete(eventId: String) =
      viewModelScope.launch {
        try {
          scheduleRepository.delete(eventId)
          _eventFlow.emit(UiEvent.ShowSnackbar(resources.getString(R.string.task_deleted)))
        } catch (e: Exception) {
          _eventFlow.emit(UiEvent.ShowSnackbar(resources.getString(R.string.task_delete_error)))
        }
      }

  fun adjustWeeklyPlan(today: LocalDate = LocalDate.now()) {
    _uiState.update { it.copy(isAdjustingPlan = true) }

    viewModelScope.launch {
      try {
        val start = AdaptivePlanner.weekStart(today)
        val end = AdaptivePlanner.weekEnd(today)
        val nextStart = start.plusWeeks(1)
        val nextEnd = end.plusWeeks(1)

        val currentWeek = scheduleRepository.getEventsBetween(start, end)
        val nextWeek = scheduleRepository.getEventsBetween(nextStart, nextEnd)

        val adj = AdaptivePlanner.planAdjustments(today, currentWeek, nextWeek)

        adj.movedMissed.forEach { ev -> scheduleRepository.moveEventDate(ev.id, nextStart) }

        adj.pulledEarlier.forEach { ev ->
          val targetDate = findOptimalDateInCurrentWeek(today, currentWeek)
          scheduleRepository.moveEventDate(ev.id, targetDate)
        }
      } finally {
        _uiState.update { it.copy(isAdjustingPlan = false) }
      }
    }
  }

  private fun findOptimalDateInCurrentWeek(
      today: LocalDate,
      currentWeek: List<ScheduleEvent>
  ): LocalDate {
    val weekEnd = AdaptivePlanner.weekEnd(today)
    val remainingDates =
        generateSequence(today) { it.plusDays(1) }.takeWhile { !it.isAfter(weekEnd) }.toList()

    val tasksByDate = currentWeek.filter { it.date in remainingDates }.groupBy { it.date }
    return remainingDates.minByOrNull { date -> tasksByDate[date]?.size ?: 0 } ?: today
  }

  fun clearError() {
    _uiState.update { it.copy(errorMessage = null) }
  }

  fun startOfWeek(date: LocalDate): LocalDate = date.with(DayOfWeek.MONDAY)

  private fun currentTodos(): List<ToDo> =
      (toDoRepository.todos as? StateFlow<List<ToDo>>)?.value.orEmpty()

  fun todosForDate(date: LocalDate): List<ToDo> = currentTodos().filter { it.dueDate == date }

  fun todosForWeek(weekStart: LocalDate): List<ToDo> {
    val weekEnd = weekStart.plusDays(6)
    return currentTodos().filter { it.dueDate in weekStart..weekEnd }
  }
}
