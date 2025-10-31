package com.android.sample.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.sample.model.schedule.EventKind
import com.android.sample.model.schedule.ScheduleEvent
import com.android.sample.model.schedule.ScheduleRepository
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ScheduleViewModel(private val scheduleRepository: ScheduleRepository) : ViewModel() {

  // --- Calendar state (lifted from CalendarViewModel) ---
  private val _selectedDate = MutableStateFlow(LocalDate.now())
  val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

  private val _currentDisplayMonth = MutableStateFlow(YearMonth.now())
  val currentDisplayMonth: StateFlow<YearMonth> = _currentDisplayMonth.asStateFlow()

  private val _isMonthView = MutableStateFlow(true)
  val isMonthView: StateFlow<Boolean> = _isMonthView.asStateFlow()

  // --- Filters ---
  private val _activeKinds = MutableStateFlow<Set<EventKind>>(emptySet()) // empty = All
  val activeKinds: StateFlow<Set<EventKind>> = _activeKinds.asStateFlow()

  val allEvents: StateFlow<List<ScheduleEvent>> =
      scheduleRepository.events.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
  private val _isAdjustingPlan = MutableStateFlow(false)
  val isAdjustingPlan: StateFlow<Boolean> = _isAdjustingPlan.asStateFlow()

  private val _lastAdjustment = MutableStateFlow<LocalDate?>(null)
  val lastAdjustment: StateFlow<LocalDate?> = _lastAdjustment.asStateFlow()

  val eventsForSelectedDate: StateFlow<List<ScheduleEvent>> =
      combine(selectedDate, allEvents, activeKinds) { date, all, kinds ->
            val base = all.filter { it.date == date }
            if (kinds.isEmpty()) base else base.filter { it.kind in kinds }
          }
          .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

  val eventsForSelectedWeek: StateFlow<List<ScheduleEvent>> =
      combine(selectedDate, allEvents, activeKinds) { date, all, kinds ->
            val start = date.with(java.time.DayOfWeek.MONDAY)
            val end = start.plusDays(6)
            val base = all.filter { it.date >= start && it.date <= end }
            if (kinds.isEmpty()) base else base.filter { it.kind in kinds }
          }
          .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

  // --- Actions ---
  fun onDateSelected(date: LocalDate) {
    _selectedDate.value = date
  }

  fun goToPreviousMonth() {
    _currentDisplayMonth.value = _currentDisplayMonth.value.minusMonths(1)
  }

  fun goToNextMonth() {
    _currentDisplayMonth.value = _currentDisplayMonth.value.plusMonths(1)
  }

  fun onPreviousMonthWeekClicked() {
    if (_isMonthView.value) {
      _currentDisplayMonth.value = _currentDisplayMonth.value.minusMonths(1)
      val newMonth = _currentDisplayMonth.value
      // keep selectedDate within this month
      _selectedDate.value =
          _selectedDate.value
              .withDayOfMonth(_selectedDate.value.dayOfMonth.coerceAtMost(newMonth.lengthOfMonth()))
              .withMonth(newMonth.monthValue)
              .withYear(newMonth.year)
    } else {
      _selectedDate.value = _selectedDate.value.minusWeeks(1)
      _currentDisplayMonth.value = YearMonth.from(_selectedDate.value)
    }
  }

  fun onNextMonthWeekClicked() {
    if (_isMonthView.value) {
      _currentDisplayMonth.value = _currentDisplayMonth.value.plusMonths(1)
      val newMonth = _currentDisplayMonth.value
      // keep selectedDate within this month
      _selectedDate.value =
          _selectedDate.value
              .withDayOfMonth(_selectedDate.value.dayOfMonth.coerceAtMost(newMonth.lengthOfMonth()))
              .withMonth(newMonth.monthValue)
              .withYear(newMonth.year)
    } else {
      _selectedDate.value = _selectedDate.value.plusWeeks(1)
      _currentDisplayMonth.value = YearMonth.from(_selectedDate.value)
    }
  }

  // ScheduleViewModel.kt
  fun setWeekMode() {
    _isMonthView.value = false
  }

  fun setMonthMode() {
    _isMonthView.value = true
  }

  fun toggleMonthWeekView() {
    _isMonthView.value = !_isMonthView.value
  }

  fun setFilters(kinds: Set<EventKind>) {
    _activeKinds.value = kinds
  }

  fun startOfWeek(date: LocalDate): LocalDate = date.with(java.time.DayOfWeek.MONDAY)

  fun save(event: ScheduleEvent) = viewModelScope.launch { scheduleRepository.save(event) }

  fun delete(eventId: String) = viewModelScope.launch { scheduleRepository.delete(eventId) }

  fun adjustWeeklyPlan(today: LocalDate = LocalDate.now()) {
    if (_isAdjustingPlan.value) return
    _isAdjustingPlan.value = true

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
          // Distribute pulled tasks throughout remaining week days
          val targetDate = findOptimalDateInCurrentWeek(today, currentWeek)
          scheduleRepository.moveEventDate(ev.id, targetDate)
        }

        _lastAdjustment.value = today
      } finally {
        _isAdjustingPlan.value = false
      }
    }
  }

  private fun findOptimalDateInCurrentWeek(
      today: LocalDate,
      currentWeek: List<ScheduleEvent>
  ): LocalDate {
    val weekStart = AdaptivePlanner.weekStart(today)
    val weekEnd = AdaptivePlanner.weekEnd(today)

    val remainingDates =
        generateSequence(today) { it.plusDays(1) }.takeWhile { !it.isAfter(weekEnd) }.toList()

    val tasksByDate = currentWeek.filter { it.date in remainingDates }.groupBy { it.date }

    // Find date with minimum tasks
    return remainingDates.minByOrNull { date -> tasksByDate[date]?.size ?: 0 } ?: today
  }

  fun onTaskCompletionChanged(eventId: String, completed: Boolean) {
    viewModelScope.launch {
      val ev = scheduleRepository.getById(eventId) ?: return@launch
      scheduleRepository.update(ev.copy(isCompleted = completed))

      if (completed) {
        adjustWeeklyPlan(LocalDate.now())
      }
    }
  }

  fun refreshWeeklyPlan() {
    adjustWeeklyPlan(LocalDate.now())
  }
}
