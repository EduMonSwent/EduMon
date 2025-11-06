package com.android.sample.ui.schedule

import android.content.res.Resources
import androidx.test.core.app.ApplicationProvider
import com.android.sample.model.planner.AttendanceStatus
import com.android.sample.model.planner.Class
import com.android.sample.model.planner.CompletionStatus
import com.android.sample.model.planner.FakePlannerRepository
import com.android.sample.model.schedule.EventKind
import com.android.sample.model.schedule.Priority
import com.android.sample.model.schedule.ScheduleEvent
import com.android.sample.model.schedule.ScheduleRepository
import com.android.sample.model.schedule.SourceTag
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ScheduleViewModelTest {

  private val testDispatcher = UnconfinedTestDispatcher()
  private lateinit var fakeScheduleRepo: FakeScheduleRepository
  private lateinit var fakePlannerRepo: FakePlannerRepository
  private lateinit var vm: ScheduleViewModel
  private lateinit var resources: Resources

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    fakeScheduleRepo = FakeScheduleRepository()
    fakePlannerRepo = FakePlannerRepository() // your provided fake
    resources = ApplicationProvider.getApplicationContext<android.content.Context>().resources
    vm = ScheduleViewModel(fakeScheduleRepo, fakePlannerRepo, resources)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun init_collectsPlannerFlows_usesProvidedFakeEmissions() = runTest {
    advanceUntilIdle()
    val s = vm.uiState.value
    assertEquals(3, s.todayClasses.size)
    assertEquals(
        listOf("Algorithms", "Data Structures", "Networks"), s.todayClasses.map(Class::courseName))
    assertTrue(s.attendanceRecords.isEmpty())
    assertFalse(s.isLoading)
    assertNull(s.errorMessage)
  }

  @Test
  fun dateNavigation_monthMode_updatesMonthAndSelectedDateSafely() {
    val start = vm.uiState.value
    assertTrue(start.isMonthView)

    vm.onPreviousMonthWeekClicked()
    val afterPrev = vm.uiState.value
    assertEquals(YearMonth.now().minusMonths(1), afterPrev.currentDisplayMonth)

    vm.onNextMonthWeekClicked()
    val afterNext = vm.uiState.value
    assertEquals(YearMonth.now(), afterNext.currentDisplayMonth)
  }

  @Test
  fun dateNavigation_weekMode_shiftsByWeek() {
    vm.setWeekMode()
    val before = vm.uiState.value.selectedDate
    vm.onNextMonthWeekClicked()
    assertEquals(before.plusWeeks(1), vm.uiState.value.selectedDate)
    vm.onPreviousMonthWeekClicked()
    assertEquals(before, vm.uiState.value.selectedDate)
  }

  @Test
  fun save_delete_update_flowReflectsInUi() = runTest {
    val e =
        ScheduleEvent(
            title = "Study OS",
            date = LocalDate.now(),
            kind = EventKind.STUDY,
            sourceTag = SourceTag.Task)
    vm.save(e)
    advanceUntilIdle()
    assertTrue(fakeScheduleRepo.events.value.any { it.id == e.id })

    vm.delete(e.id)
    advanceUntilIdle()
    assertFalse(fakeScheduleRepo.events.value.any { it.id == e.id })

    vm.save(e.copy(title = "Study OS v2"))
    advanceUntilIdle()
    val updated = fakeScheduleRepo.events.value.first { it.id == e.id }
    assertEquals("Study OS v2", updated.title)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun saveClassAttendance_emitsSuccessSnackbar() = runTest {
    var captured: ScheduleViewModel.UiEvent? = null
    val collectJob =
        launch(UnconfinedTestDispatcher(testScheduler)) {
          vm.eventFlow.collect { e ->
            captured = e
            this.cancel()
          }
        }

    val klass = vm.uiState.value.todayClasses.first()
    vm.saveClassAttendance(klass, AttendanceStatus.YES, CompletionStatus.PARTIALLY)
    advanceUntilIdle()

    assertTrue(captured is ScheduleViewModel.UiEvent.ShowSnackbar)
    collectJob.cancel()
  }

  @Test
  fun adjustWeeklyPlan_movesMissedAndPullsEarlier() = runTest {
    val today = LocalDate.of(2025, 11, 6)
    val start = AdaptivePlanner.weekStart(today)
    val end = AdaptivePlanner.weekEnd(today)
    val nextStart = start.plusWeeks(1)

    val missed =
        ScheduleEvent(
            id = "m1",
            title = "Missed task",
            date = start,
            kind = EventKind.STUDY,
            isCompleted = false,
            sourceTag = SourceTag.Task)
    val completedEarly =
        ScheduleEvent(
            id = "doneFuture",
            title = "Completed early",
            date = end,
            time = LocalTime.of(12, 0),
            kind = EventKind.PROJECT,
            isCompleted = true,
            sourceTag = SourceTag.Task)
    val toPull =
        ScheduleEvent(
            id = "pullMe",
            title = "Project milestone",
            date = nextStart.plusDays(2),
            kind = EventKind.SUBMISSION_MILESTONE,
            isCompleted = false,
            priority = Priority.HIGH,
            sourceTag = SourceTag.Task)

    fakeScheduleRepo.seedBetween(start, end, listOf(missed, completedEarly))
    fakeScheduleRepo.seedBetween(nextStart, end.plusWeeks(1), listOf(toPull))

    vm.adjustWeeklyPlan(today)
    advanceUntilIdle()

    assertTrue(fakeScheduleRepo.moved.any { it.first == "m1" && it.second == nextStart })

    val pulledMove = fakeScheduleRepo.moved.firstOrNull { it.first == "pullMe" }
    assertNotNull(pulledMove)
    val movedDate = pulledMove!!.second
    assertTrue(movedDate >= today && movedDate <= end)
  }

  // -------- Simple in-memory fake for ScheduleRepository --------
  private class FakeScheduleRepository : ScheduleRepository {
    private val backing = mutableListOf<ScheduleEvent>()
    private val _events = MutableStateFlow<List<ScheduleEvent>>(emptyList())
    override val events: StateFlow<List<ScheduleEvent>> = _events

    val moved = mutableListOf<Pair<String, LocalDate>>()

    fun seedBetween(start: LocalDate, end: LocalDate, list: List<ScheduleEvent>) {
      backing += list
      _events.value =
          backing.sortedWith(
              compareBy<ScheduleEvent> { it.date }.thenBy { it.time ?: LocalTime.MIN })
    }

    override suspend fun save(event: ScheduleEvent) {
      backing.removeAll { it.id == event.id }
      backing += event
      _events.value = backing.sortedBy { it.date }
    }

    override suspend fun update(event: ScheduleEvent) = save(event)

    override suspend fun delete(eventId: String) {
      backing.removeAll { it.id == eventId }
      _events.value = backing.sortedBy { it.date }
    }

    override suspend fun getEventsBetween(start: LocalDate, end: LocalDate): List<ScheduleEvent> =
        backing.filter { it.date in start..end }

    override suspend fun getById(id: String): ScheduleEvent? = backing.firstOrNull { it.id == id }

    override suspend fun moveEventDate(id: String, newDate: LocalDate): Boolean {
      val idx = backing.indexOfFirst { it.id == id }
      if (idx < 0) return false
      backing[idx] = backing[idx].copy(date = newDate)
      moved += id to newDate
      _events.value = backing.sortedBy { it.date }
      return true
    }

    override suspend fun getEventsForDate(date: LocalDate): List<ScheduleEvent> =
        backing.filter { it.date == date }

    override suspend fun getEventsForWeek(startDate: LocalDate): List<ScheduleEvent> =
        getEventsBetween(startDate, startDate.plusDays(6))
  }
}
