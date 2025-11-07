package com.android.sample.screen.calendar

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.android.sample.repos_providors.FakeRepositories
import com.android.sample.ui.calendar.CalendarViewModel
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@SmallTest
class CalendarViewModelTest {

  private lateinit var vm: CalendarViewModel

  @Before
  fun setup() {
    vm = CalendarViewModel(FakeRepositories.calendarRepository)
  }

  @Test
  fun initial_state_has_correct_month_and_selected_date() = runTest {
    val now = LocalDate.now()
    assertEquals(now, vm.selectedDate.first())
    assertEquals(YearMonth.from(now), vm.currentDisplayMonth.first())
  }

  @Test
  fun toggleMonthWeekView_switches_view_mode() = runTest {
    val first = vm.isMonthView.first()
    vm.toggleMonthWeekView()
    val toggled = vm.isMonthView.first()
    assertNotEquals(first, toggled)
  }

  @Test
  fun next_and_previous_month_update_currentDisplayMonth() = runTest {
    val original = vm.currentDisplayMonth.first()
    vm.onNextMonthWeekClicked()
    assertEquals(original.plusMonths(1), vm.currentDisplayMonth.first())

    vm.onPreviousMonthWeekClicked()
    assertEquals(original, vm.currentDisplayMonth.first())
  }

  @Test
  fun startOfWeek_returns_Monday_of_same_week() {
    val thursday = LocalDate.of(2025, 10, 9)
    val monday = vm.startOfWeek(thursday)
    assertEquals(LocalDate.of(2025, 10, 6), monday)
  }

  @Test
  fun onDateSelected_updates_selectedDate() = runTest {
    val date = LocalDate.of(2025, 10, 10)
    vm.onDateSelected(date)
    assertEquals(date, vm.selectedDate.first())
  }
}
