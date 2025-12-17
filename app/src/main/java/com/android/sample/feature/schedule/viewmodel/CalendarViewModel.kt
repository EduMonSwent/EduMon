package com.android.sample.feature.schedule.viewmodel

import androidx.lifecycle.ViewModel
import com.android.sample.feature.schedule.repository.calendar.CalendarRepository
import com.android.sample.repos_providors.AppRepositories
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** This class was implemented with the help of ai (chatgbt) */
class CalendarViewModel(
    private val repository: CalendarRepository = AppRepositories.calendarRepository
) : ViewModel() {

  private val _selectedDate = MutableStateFlow(LocalDate.now())
  val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

  private val _currentDisplayMonth = MutableStateFlow(YearMonth.now())
  val currentDisplayMonth: StateFlow<YearMonth> = _currentDisplayMonth.asStateFlow()

  private val _isMonthView = MutableStateFlow(true)
  val isMonthView: StateFlow<Boolean> = _isMonthView.asStateFlow()

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

  fun toggleMonthWeekView() {
    _isMonthView.value = !_isMonthView.value
    if (_isMonthView.value) {
      _currentDisplayMonth.value = YearMonth.from(_selectedDate.value)
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
