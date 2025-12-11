package com.android.sample.ui.schedule

import android.content.res.Resources
import androidx.test.core.app.ApplicationProvider
import com.android.sample.feature.schedule.data.planner.ScheduleGapItem
import com.android.sample.feature.schedule.data.schedule.EventKind
import com.android.sample.feature.schedule.data.schedule.ScheduleEvent
import com.android.sample.feature.schedule.viewmodel.ScheduleNavEvent
import com.android.sample.feature.schedule.viewmodel.ScheduleViewModel
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ScheduleViewModelGapLogicTest {

  private val dispatcher = StandardTestDispatcher()
  private lateinit var vm: ScheduleViewModel
  private lateinit var scheduleRepo: FakeScheduleRepo
  private lateinit var resources: Resources

  @Before
  fun setup() {
    Dispatchers.setMain(dispatcher)
    scheduleRepo = FakeScheduleRepo()
    resources = ApplicationProvider.getApplicationContext<android.content.Context>().resources
    vm = ScheduleViewModel(scheduleRepo, FakePlannerRepo(), resources)
  }

  @After fun tearDown() = Dispatchers.resetMain()

  // ----------------------------------------------------------
  // Test GAP click → propositions for STUDY during normal time
  // ----------------------------------------------------------
  @Test
  fun `study gap 20 min gives flashcards and quiz`() =
      runTest(dispatcher) {
        val gap = ScheduleGapItem(LocalTime.of(14, 0), LocalTime.of(14, 20))
        vm.onGapClicked(gap)
        vm.onGapTypeSelected(isStudy = true)

        val list = vm.uiState.value.gapPropositions
        assertEquals(listOf("Review Flashcards", "Quick Quiz"), list)
      }

  // ----------------------------------------------------------
  // RELAX 20 min gap
  // ----------------------------------------------------------
  @Test
  fun `relax gap 20 min gives quick relax suggestions`() =
      runTest(dispatcher) {
        val gap = ScheduleGapItem(LocalTime.of(15, 0), LocalTime.of(15, 20))
        vm.onGapClicked(gap)
        vm.onGapTypeSelected(isStudy = false)

        val list = vm.uiState.value.gapPropositions
        assertTrue("Search Friend" in list)
        assertTrue("Play Games" in list)
      }

  // ----------------------------------------------------------
  // Lunch-time logic STUDY (12–13h)
  // ----------------------------------------------------------
  @Test
  fun `lunch time study suggests eating while studying`() =
      runTest(dispatcher) {
        val gap = ScheduleGapItem(LocalTime.of(12, 10), LocalTime.of(12, 40))
        vm.onGapClicked(gap)
        vm.onGapTypeSelected(isStudy = true)

        assertEquals(
            listOf("Review Flashcards (While Eating)", "Light Reading"),
            vm.uiState.value.gapPropositions)
      }

  // ----------------------------------------------------------
  // Lunch-time RELAX suggestions
  // ----------------------------------------------------------
  @Test
  fun `lunch time relax suggests lunch activities`() =
      runTest(dispatcher) {
        val gap = ScheduleGapItem(LocalTime.of(12, 30), LocalTime.of(13, 0))
        vm.onGapClicked(gap)
        vm.onGapTypeSelected(isStudy = false)

        val p = vm.uiState.value.gapPropositions
        assertEquals(listOf("Eat Lunch", "Coffee with Friends", "Nap"), p)
      }

  // ----------------------------------------------------------
  // Clicking a proposition creates and saves a ScheduleEvent
  // ----------------------------------------------------------
  @Test
  fun `onGapPropositionClicked creates event and closes modal`() =
      runTest(dispatcher) {
        val gap = ScheduleGapItem(LocalTime.of(14, 0), LocalTime.of(15, 0))
        vm.onGapClicked(gap)
        vm.onGapPropositionClicked("Review Flashcards")

        advanceUntilIdle()

        val events = scheduleRepo.events.value
        assertEquals(1, events.size)
        assertEquals("Review Flashcards", events[0].title)
        assertEquals(EventKind.STUDY, events[0].kind)

        assertFalse(vm.uiState.value.showGapOptionsModal)
        assertFalse(vm.uiState.value.showGapPropositionsModal)
        assertNull(vm.uiState.value.selectedGap)
      }

  // ----------------------------------------------------------
  // Event navigation (ToFlashcards, ToGames, etc)
  // ----------------------------------------------------------
  @Test
  fun `onScheduleEventClicked emits navigation event`() =
      runTest(dispatcher) {
        val output = async { vm.navEvents.first() }

        val event =
            ScheduleEvent(
                title = "Review Flashcards", date = LocalDate.now(), kind = EventKind.STUDY)

        vm.onScheduleEventClicked(event)

        val nav = output.await()
        assertTrue(nav is ScheduleNavEvent.ToFlashcards)
      }

  @Test
  fun `onDismissGapModal resets modal state`() =
      runTest(dispatcher) {
        val gap = ScheduleGapItem(LocalTime.of(9, 0), LocalTime.of(10, 0))
        vm.onGapClicked(gap)
        vm.onDismissGapModal()

        val s = vm.uiState.value
        assertFalse(s.showGapOptionsModal)
        assertFalse(s.showGapPropositionsModal)
        assertNull(s.selectedGap)
      }

  // minimal fakes
  private class FakePlannerRepo :
      com.android.sample.feature.schedule.repository.planner.PlannerRepository()

  private class FakeScheduleRepo :
      com.android.sample.feature.schedule.repository.schedule.ScheduleRepository {
    private val backing = mutableListOf<ScheduleEvent>()
    private val _events = MutableStateFlow<List<ScheduleEvent>>(emptyList())
    override val events: StateFlow<List<ScheduleEvent>> = _events

    override suspend fun save(event: ScheduleEvent) {
      backing += event
      _events.value = backing
    }

    override suspend fun update(event: ScheduleEvent) {}

    override suspend fun delete(eventId: String) {}

    override suspend fun getEventsBetween(s: LocalDate, e: LocalDate) = emptyList<ScheduleEvent>()

    override suspend fun getById(id: String) = null

    override suspend fun moveEventDate(id: String, newDate: LocalDate) = true

    override suspend fun getEventsForDate(date: LocalDate) = emptyList<ScheduleEvent>()

    override suspend fun getEventsForWeek(startDate: LocalDate) = emptyList<ScheduleEvent>()

    override suspend fun importEvents(events: List<ScheduleEvent>) {}
  }
}
