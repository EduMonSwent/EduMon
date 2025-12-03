package com.android.sample.ui.schedule

import android.content.res.Resources
import androidx.test.core.app.ApplicationProvider
import com.android.sample.feature.schedule.data.planner.Class
import com.android.sample.feature.schedule.data.planner.ClassAttendance
import com.android.sample.feature.schedule.data.planner.ClassType
import com.android.sample.feature.schedule.repository.planner.PlannerRepository
import com.android.sample.feature.schedule.repository.schedule.ScheduleRepository
import com.android.sample.feature.schedule.viewmodel.ScheduleViewModel
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ScheduleViewModel_ObservePlannerTest {

  private val dispatcher = StandardTestDispatcher()

  private lateinit var vm: ScheduleViewModel
  private lateinit var scheduleRepo: FakeScheduleRepo
  private lateinit var plannerRepo: FakePlannerRepo
  private lateinit var resources: Resources

  @Before
  fun setUp() {
    Dispatchers.setMain(dispatcher)

    // Freeze time so behavior is deterministic
    mockkStatic(LocalTime::class)
    every { LocalTime.now() } returns LocalTime.of(10, 0)

    scheduleRepo = FakeScheduleRepo()
    plannerRepo = FakePlannerRepo()
    resources = ApplicationProvider.getApplicationContext<android.content.Context>().resources

    vm = ScheduleViewModel(scheduleRepo, plannerRepo, resources)
  }

  @After
  fun tearDown() {
    unmockkStatic(LocalTime::class)
    Dispatchers.resetMain()
  }

  /**
   * ---------------------------------------------------------------
   * Test 1 — Classes are deduped, only upcoming are shown, sorted.
   * ---------------------------------------------------------------
   */
  @Test
  fun `observePlannerData sorts upcoming classes and dedupes correctly`() =
      runTest(dispatcher) {
        val classes =
            listOf(
                Class(
                    "1",
                    "Algo",
                    LocalTime.of(9, 0),
                    LocalTime.of(11, 0),
                    ClassType.LECTURE,
                    "A",
                    "Prof A"),
                Class(
                    "X",
                    "Algo",
                    LocalTime.of(9, 0),
                    LocalTime.of(11, 0),
                    ClassType.LECTURE,
                    "B",
                    "Prof A"), // Duplicate
                Class(
                    "2",
                    "Networks",
                    LocalTime.of(12, 0),
                    LocalTime.of(13, 0),
                    ClassType.LECTURE,
                    "C",
                    "Prof N"),
                Class(
                    "3",
                    "Databases",
                    LocalTime.of(8, 0),
                    LocalTime.of(9, 30),
                    ClassType.LECTURE,
                    "D",
                    "Prof D") // ends BEFORE now → filtered out
                )

        plannerRepo.emitClasses(classes)
        advanceUntilIdle()

        val state = vm.uiState.value

        // Should remove duplicate Algo
        assertEquals(4, classes.size)
        assertEquals(2, state.todayClasses.size)

        // Only classes ending AFTER now (10:00) → Algo and Networks
        assertEquals(listOf("Algo", "Networks"), state.todayClasses.map { it.courseName })

        // Sorted by startTime → Algo(9:00), Networks(12:00)
        assertEquals(LocalTime.of(9, 0), state.todayClasses[0].startTime)
        assertEquals(LocalTime.of(12, 0), state.todayClasses[1].startTime)

        assertFalse(state.allClassesFinished)
      }

  /**
   * ---------------------------------------------------------------
   * Test 2 — All classes finished → allClassesFinished = true
   * ---------------------------------------------------------------
   */
  @Test
  fun `all classes finished sets allClassesFinished true`() =
      runTest(dispatcher) {
        val finishedClasses =
            listOf(
                Class(
                    "1", "Algo", LocalTime.of(7, 0), LocalTime.of(8, 0), ClassType.LECTURE, "", ""),
                Class(
                    "2",
                    "Networks",
                    LocalTime.of(8, 0),
                    LocalTime.of(9, 0),
                    ClassType.LECTURE,
                    "",
                    ""))

        plannerRepo.emitClasses(finishedClasses)

        advanceUntilIdle()

        val state = vm.uiState.value

        // No upcoming classes
        assertTrue(state.todayClasses.isEmpty())

        // All endTime < now(10:00)
        assertTrue(state.allClassesFinished)
      }

  /**
   * ---------------------------------------------------------------
   * Test 3 — Mixed classes → allClassesFinished = false
   * ---------------------------------------------------------------
   */
  @Test
  fun `mixed finished and upcoming classes sets allClassesFinished false`() =
      runTest(dispatcher) {
        val mixed =
            listOf(
                Class(
                    "1",
                    "Algo",
                    LocalTime.of(7, 0),
                    LocalTime.of(8, 0),
                    ClassType.LECTURE,
                    "",
                    ""), // finished
                Class(
                    "2",
                    "Networks",
                    LocalTime.of(11, 0),
                    LocalTime.of(12, 0),
                    ClassType.LECTURE,
                    "",
                    "") // upcoming
                )

        plannerRepo.emitClasses(mixed)
        advanceUntilIdle()

        val state = vm.uiState.value

        assertEquals(1, state.todayClasses.size)
        assertEquals("Networks", state.todayClasses.first().courseName)

        assertFalse(state.allClassesFinished)
      }

  // ----------------------------------------------------------
  // Supporting Fake Repos
  // ----------------------------------------------------------

  private class FakeScheduleRepo : ScheduleRepository {
    override val events =
        MutableStateFlow(
            emptyList<com.android.sample.feature.schedule.data.schedule.ScheduleEvent>())

    override suspend fun save(
        event: com.android.sample.feature.schedule.data.schedule.ScheduleEvent
    ) {
      events.value = events.value + event
    }

    override suspend fun update(
        event: com.android.sample.feature.schedule.data.schedule.ScheduleEvent
    ) = save(event)

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

    override suspend fun importEvents(
        events: List<com.android.sample.feature.schedule.data.schedule.ScheduleEvent>
    ) {}
  }

  private class FakePlannerRepo : PlannerRepository() {
    private val classesFlow = MutableStateFlow<List<Class>>(emptyList())
    private val attendanceFlow = MutableStateFlow<List<ClassAttendance>>(emptyList())

    override fun getTodayClassesFlow(): Flow<List<Class>> = classesFlow

    override fun getTodayAttendanceFlow(): Flow<List<ClassAttendance>> = attendanceFlow

    fun emitClasses(list: List<Class>) {
      classesFlow.value = list
    }

    override suspend fun saveAttendance(att: ClassAttendance) = Result.success(Unit)
  }
}
