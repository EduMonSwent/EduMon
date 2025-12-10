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
import com.android.sample.feature.schedule.data.planner.DayScheduleItem
import com.android.sample.feature.schedule.data.planner.ScheduleClassItem
import com.android.sample.feature.schedule.data.planner.ScheduleEventItem
import com.android.sample.feature.schedule.data.planner.ScheduleGapItem
import com.android.sample.feature.schedule.data.schedule.EventKind
import com.android.sample.feature.schedule.data.schedule.ScheduleEvent
import com.android.sample.feature.schedule.repository.planner.PlannerRepository
import com.android.sample.feature.schedule.repository.schedule.ScheduleRepository
import com.android.sample.repos_providors.AppRepositories
import com.android.sample.repositories.ToDoRepository
import com.android.sample.ui.schedule.AdaptivePlanner
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
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
    val todaySchedule: List<DayScheduleItem> = emptyList(),
    val attendanceRecords: List<ClassAttendance> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val showAddTaskModal: Boolean = false,
    val showAttendanceModal: Boolean = false,
    val showGapOptionsModal: Boolean = false,
    val showGapPropositionsModal: Boolean = false,
    val gapPropositions: List<String> = emptyList(),
    val selectedGap: ScheduleGapItem? = null,
    val selectedClass: Class? = null,
    val todos: List<ToDo> = emptyList(),
    val allClassesFinished: Boolean = false
)
// Navigation events triggered by smart gap logic
sealed class ScheduleNavEvent {
  object ToFlashcards : ScheduleNavEvent()

  object ToGames : ScheduleNavEvent()

  object ToStudySession : ScheduleNavEvent()

  object ToStudyTogether : ScheduleNavEvent()

  class ShowWellnessSuggestion(val message: String) : ScheduleNavEvent()
}

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
  private val _navEvents = MutableSharedFlow<ScheduleNavEvent>()
  val navEvents = _navEvents.asSharedFlow()

  sealed class UiEvent {
    data class ShowSnackbar(val message: String) : UiEvent()
  }

  init {
    observeData()
    observeTodos()
  }

  private fun observeData() {
    viewModelScope.launch {
      combine(
              scheduleRepository.events,
              plannerRepository.getTodayClassesFlow(),
              plannerRepository.getTodayAttendanceFlow()) { events, classes, attendance ->
                Triple(events, classes, attendance)
              }
          .collect { (events, classes, attendance) ->
            val today = LocalDate.now()
            val now = LocalTime.now()
            val todayDay = today.dayOfWeek

            // 1. Gather all "Busy Blocks" for today
            val busyBlocks = mutableListOf<DayScheduleItem>()

            // A. Classes
            val todayClasses = classes.filter { it.daysOfWeek.contains(todayDay) }
            todayClasses.forEach { busyBlocks.add(ScheduleClassItem(it)) }

            // B. Scheduled Events (Tasks, Study Sessions, etc.)
            // This includes your "filled" choices, preventing double-booking.
            val todayEvents = events.filter { it.date == today && it.time != null }
            todayEvents.forEach { busyBlocks.add(ScheduleEventItem(it)) }

            // 2. Sort by Start Time
            val sortedBlocks = busyBlocks.sortedBy { it.start }

            // 3. Robust Gap Calculation (Sweep-line)
            // We track `currentBusyEnd` to handle overlapping/nested blocks correctly.
            val fullSchedule = mutableListOf<DayScheduleItem>()
            val minGapMinutes = 15L
            val endOfDay = LocalTime.of(20, 0) // Student limit 20:00

            var currentBusyEnd: LocalTime? = null

            for (block in sortedBlocks) {
              if (currentBusyEnd == null) {
                // First block of the day
                // Optional: Check gap between 'now' and first block?
                // For now, we only stick to the list start.
                currentBusyEnd = block.end
              } else {
                // Check if there is a gap between the previous max end and this block's start
                // logic: if block.start > currentBusyEnd -> GAP
                if (block.start.isAfter(currentBusyEnd)) {
                  val gapSize = Duration.between(currentBusyEnd, block.start).toMinutes()
                  if (gapSize >= minGapMinutes) {
                    fullSchedule.add(ScheduleGapItem(currentBusyEnd!!, block.start))
                  }
                  // Update pointer to this block's end
                  currentBusyEnd = block.end
                } else {
                  // Overlap or touching. Just extend the busy period if this block ends later.
                  if (block.end.isAfter(currentBusyEnd)) {
                    currentBusyEnd = block.end
                  }
                }
              }
              fullSchedule.add(block)
            }

            // 4. Evening Gap (After last busy block until 20:00)
            if (currentBusyEnd != null && currentBusyEnd.isBefore(endOfDay)) {
              val eveningGap = Duration.between(currentBusyEnd, endOfDay).toMinutes()
              if (eveningGap >= minGapMinutes) {
                fullSchedule.add(ScheduleGapItem(currentBusyEnd, endOfDay))
              }
            } else if (sortedBlocks.isEmpty()) {
              // Entire day is free until 20:00? (Optional feature)
            }

            // "All Classes Finished" logic (visual flair)
            val allFinished =
                todayClasses.isNotEmpty() && todayClasses.all { it.endTime.isBefore(now) }

            _uiState.update {
              it.copy(
                  allEvents = events,
                  todayClasses = todayClasses,
                  todaySchedule = fullSchedule,
                  attendanceRecords = attendance,
                  allClassesFinished = allFinished)
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
  // --- SMART GAP LOGIC ---

  // Step 1: Open "Study or Relax?" Dialog
  fun onGapClicked(gap: ScheduleGapItem) {
    _uiState.update {
      it.copy(selectedGap = gap, showGapOptionsModal = true, showGapPropositionsModal = false)
    }
  }

  fun onDismissGapModal() {
    _uiState.update {
      it.copy(selectedGap = null, showGapOptionsModal = false, showGapPropositionsModal = false)
    }
  }

  // Step 2: User Chose Category -> Show Specific Propositions
  fun onGapTypeSelected(isStudy: Boolean) {
    val gap = _uiState.value.selectedGap ?: return
    val mins = gap.durationMinutes

    // Exact logic requested by user
    val propositions =
        if (isStudy) {
          if (mins in 15..30) {
            listOf("Review Flashcards") // Suggestion for 15-30m
          } else {
            listOf("Work on Objectives") // Suggestion for >30m (Study Session)
          }
        } else {
          // Relax
          if (mins in 15..30) {
            listOf("Search Friend", "Play Games", "Walk around campus", "Call Family")
          } else {
            listOf("Go to Gym", "Play Piano", "Read a Book", "Hobby Time")
          }
        }

    _uiState.update {
      it.copy(
          showGapOptionsModal = false,
          showGapPropositionsModal = true,
          gapPropositions = propositions)
    }
  }

  // Step 3: User Clicked a Proposition -> Create Event
  fun onGapPropositionClicked(proposition: String) {
    val gap = _uiState.value.selectedGap ?: return

    // Close modals
    onDismissGapModal()

    // Create the event. This "prints" it into the schedule because we save it to the repo.
    // The observeData() block will then pick it up, see it as a "Busy Block", and replace the Gap
    // with this Event.
    val newEvent =
        ScheduleEvent(
            title = proposition,
            date = LocalDate.now(),
            time = gap.start,
            durationMinutes = gap.durationMinutes.toInt(),
            kind = EventKind.STUDY, // You could differ kinds based on proposition if needed
            description = "Smart suggestion",
            isCompleted = false)

    save(newEvent)
  }

  // --- NAVIGATION LOGIC (When the user clicks the "Printed" event) ---
  fun onScheduleEventClicked(event: ScheduleEvent) {
    viewModelScope.launch {
      when (event.title) {
        "Review Flashcards" -> _navEvents.emit(ScheduleNavEvent.ToFlashcards)
        "Work on Objectives" -> _navEvents.emit(ScheduleNavEvent.ToStudySession)
        "Search Friend" -> _navEvents.emit(ScheduleNavEvent.ToStudyTogether)
        "Play Games" -> _navEvents.emit(ScheduleNavEvent.ToGames)

        // For other non-navigable suggestions (Gym, Piano, etc.), just show feedback
        else -> _eventFlow.emit(UiEvent.ShowSnackbar("Enjoy your ${event.title}!"))
      }
    }
  }

  fun onDeleteScheduleEvent(event: ScheduleEvent) {
    delete(event.id)
  }
}
