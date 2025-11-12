package com.android.sample.ui.schedule

import android.content.res.Resources
import androidx.test.core.app.ApplicationProvider
import com.android.sample.feature.schedule.data.planner.Class
import com.android.sample.feature.schedule.data.planner.ClassAttendance
import com.android.sample.feature.schedule.data.planner.ClassType
import com.android.sample.feature.schedule.data.schedule.ScheduleEvent
import com.android.sample.feature.schedule.repository.planner.PlannerRepository
import com.android.sample.feature.schedule.repository.schedule.ScheduleRepository
import com.android.sample.feature.schedule.viewmodel.ScheduleViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ScheduleViewModelMoreBranchesTest {

  private val dispatcher = StandardTestDispatcher()

  private lateinit var vm: ScheduleViewModel
  private lateinit var scheduleRepo: FakeScheduleRepo
  private lateinit var plannerRepo: FakePlannerRepo
  private lateinit var resources: Resources

  @Before
  fun setUp() {
    Dispatchers.setMain(dispatcher)
    scheduleRepo = FakeScheduleRepo()
    plannerRepo = FakePlannerRepo()
    resources = ApplicationProvider.getApplicationContext<android.content.Context>().resources
    vm = ScheduleViewModel(scheduleRepo, plannerRepo, resources)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun toggles_and_helpers() =
      runTest(dispatcher) {
        // Modal toggles
        vm.onAddStudyTaskClicked()
        Assert.assertTrue(vm.uiState.value.showAddTaskModal)
        vm.onDismissAddStudyTaskModal()
        Assert.assertFalse(vm.uiState.value.showAddTaskModal)

        val klass =
            Class(
                "1",
                "Algo",
                LocalTime.NOON,
                LocalTime.NOON.plusMinutes(60),
                ClassType.LECTURE,
                "R1",
                "Prof")
        vm.onClassClicked(klass)
        Assert.assertTrue(vm.uiState.value.showAttendanceModal)
        vm.onDismissClassAttendanceModal()
        Assert.assertFalse(vm.uiState.value.showAttendanceModal)

        // startOfWeek / clearError
        val th = LocalDate.of(2025, 11, 6)
        Assert.assertEquals(DayOfWeek.MONDAY, vm.startOfWeek(th).dayOfWeek)
        vm.clearError() // just to touch the branch
        Assert.assertNull(vm.uiState.value.errorMessage)

        // toggleMonthWeekView
        val before = vm.uiState.value.isMonthView
        vm.toggleMonthWeekView()
        Assert.assertEquals(!before, vm.uiState.value.isMonthView)
      }

  // ---- minimal fakes ----
  private class FakeScheduleRepo : ScheduleRepository {
    override val events = MutableStateFlow(emptyList<ScheduleEvent>())

    override suspend fun save(event: ScheduleEvent) {
      events.value = events.value + event
    }

    override suspend fun update(event: ScheduleEvent) {
      save(event)
    }

    override suspend fun delete(eventId: String) {
      events.value = events.value.filterNot { it.id == eventId }
    }

    override suspend fun getEventsBetween(s: LocalDate, e: LocalDate) =
        events.value.filter { it.date in s..e }

    override suspend fun getById(id: String) = events.value.firstOrNull { it.id == id }

    override suspend fun moveEventDate(id: String, newDate: LocalDate) = true

    override suspend fun getEventsForDate(date: LocalDate) = events.value.filter { it.date == date }

    override suspend fun getEventsForWeek(startDate: LocalDate) =
        getEventsBetween(startDate, startDate.plusDays(6))
  }

  private class FakePlannerRepo : PlannerRepository() {
    private val classes = MutableStateFlow<List<Class>>(emptyList())
    private val attendance = MutableStateFlow<List<ClassAttendance>>(emptyList())

    override fun getTodayClassesFlow(): Flow<List<Class>> = classes

    override fun getTodayAttendanceFlow(): Flow<List<ClassAttendance>> = attendance

    override suspend fun saveAttendance(attendance: ClassAttendance): Result<Unit> =
        Result.success(Unit)
  }
}
