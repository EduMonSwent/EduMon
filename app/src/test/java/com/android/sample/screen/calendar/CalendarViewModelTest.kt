package com.android.sample.screen.calendar

import com.android.sample.ui.calendar.CalendarViewModel
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModelTest {

  private lateinit var vm: CalendarViewModel

  @Before
  fun setup() {
    vm = CalendarViewModel()
  }

  @Test
  fun `initial state has correct month and selected date`() = runTest {
    val now = LocalDate.now()
    assertEquals(now, vm.selectedDate.first())
    assertEquals(YearMonth.from(now), vm.currentDisplayMonth.first())
  }

  @Test
  fun `toggleMonthWeekView switches view mode`() = runTest {
    val first = vm.isMonthView.first()
    vm.toggleMonthWeekView()
    val toggled = vm.isMonthView.first()
    assertNotEquals(first, toggled)
  }

  @Test
  fun `next and previous month update currentDisplayMonth`() = runTest {
    val original = vm.currentDisplayMonth.first()
    vm.onNextMonthWeekClicked()
    assertEquals(original.plusMonths(1), vm.currentDisplayMonth.first())

    vm.onPreviousMonthWeekClicked()
    assertEquals(original, vm.currentDisplayMonth.first())
  }

  @Test
  fun `startOfWeek returns Monday of same week`() {
    val thursday = LocalDate.of(2025, 10, 9)
    val monday = vm.startOfWeek(thursday)
    assertEquals(LocalDate.of(2025, 10, 6), monday)
  }

  @Test
  fun `onDateSelected updates selectedDate`() = runTest {
    val date = LocalDate.of(2025, 10, 10)
    vm.onDateSelected(date)
    assertEquals(date, vm.selectedDate.first())
  }
}
