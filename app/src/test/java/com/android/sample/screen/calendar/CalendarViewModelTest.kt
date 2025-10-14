package com.android.sample.screen.calendar

import com.android.sample.model.calendar.*
import com.android.sample.ui.calendar.CalendarViewModel
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

class CalendarViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun prev_next_behaviour_month_vs_week() = runTest {
        val vm = CalendarViewModel(repository = PlannerRepositoryImpl())
        val startMonth = vm.currentDisplayMonth.value
        vm.onNextMonthWeekClicked()
        assertEquals(startMonth.plusMonths(1), vm.currentDisplayMonth.value)
        vm.onPreviousMonthWeekClicked()
        assertEquals(startMonth, vm.currentDisplayMonth.value)

        val startDate = vm.selectedDate.value
        vm.toggleMonthWeekView()
        vm.onNextMonthWeekClicked()
        assertEquals(startDate.plusWeeks(1), vm.selectedDate.value)
        vm.onPreviousMonthWeekClicked()
        assertEquals(startDate, vm.selectedDate.value)
    }
}
