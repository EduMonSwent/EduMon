package com.android.sample.ui.calendar

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.feature.schedule.viewmodel.CalendarViewModel
import com.android.sample.repos_providors.FakeRepositories
import java.time.LocalDate
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
class CalendarViewModelExtraTest {

  private lateinit var vm: CalendarViewModel

  @Before
  fun setup() {
    vm = CalendarViewModel(FakeRepositories.calendarRepository)
  }

  @Test
  fun onNextMonthWeekClicked_advances_by_one_month() = runTest {
    val start = vm.currentDisplayMonth.first()
    vm.onNextMonthWeekClicked()
    assertEquals(start.plusMonths(1), vm.currentDisplayMonth.first())
  }

  @Test
  fun onPreviousMonthWeekClicked_goes_back_by_one_month() = runTest {
    val start = vm.currentDisplayMonth.first()
    vm.onPreviousMonthWeekClicked()
    assertEquals(start.minusMonths(1), vm.currentDisplayMonth.first())
  }

  @Test
  fun toggleMonthWeekView_toggles_back_and_forth_consistently() = runTest {
    val initial = vm.isMonthView.first()
    vm.toggleMonthWeekView()
    val toggled = vm.isMonthView.first()
    assertNotEquals(initial, toggled)
    vm.toggleMonthWeekView()
    assertEquals(initial, vm.isMonthView.first())
  }

  @Test
  fun startOfWeek_always_returns_a_Monday() {
    val date = LocalDate.of(2025, 10, 15) // Wednesday
    val monday = vm.startOfWeek(date)
    assertEquals(1, monday.dayOfWeek.value)
  }

  @Test
  fun onDateSelected_updates_selectedDate_flow() = runTest {
    val newDate = LocalDate.now().plusDays(5)
    vm.onDateSelected(newDate)
    assertEquals(newDate, vm.selectedDate.first())
  }
}
