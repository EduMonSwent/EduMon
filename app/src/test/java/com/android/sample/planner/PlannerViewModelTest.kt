package com.android.sample.planner

import androidx.lifecycle.viewModelScope
import com.android.sample.model.planner.*
import com.android.sample.ui.viewmodel.PlannerViewModel
import java.time.LocalTime
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * ✅ Test stable pour PlannerViewModel
 * - Pas de blocage 10s
 * - Pas besoin de modifier Gradle
 * - Utilise un FakeRepository à flux finis
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class PlannerViewModelTest {

  private val dispatcher = UnconfinedTestDispatcher()
  private lateinit var viewModel: PlannerViewModel

  // Fake repository avec flux finis (pas de collect infini)
  private class FakePlannerRepository : PlannerRepository() {
    override fun getTodayClassesFlow() =
        kotlinx.coroutines.flow.flow {
          emit(
              listOf(
                  Class(
                      id = "1",
                      courseName = "Algorithms",
                      startTime = LocalTime.of(9, 0),
                      endTime = LocalTime.of(10, 0),
                      type = ClassType.LECTURE,
                      location = "INM202",
                      instructor = "Prof. Smith"),
                  Class(
                      id = "2",
                      courseName = "Data Structures",
                      startTime = LocalTime.of(11, 0),
                      endTime = LocalTime.of(12, 30),
                      type = ClassType.EXERCISE,
                      location = "BC101",
                      instructor = "Dr. Johnson"),
                  Class(
                      id = "3",
                      courseName = "Networks",
                      startTime = LocalTime.of(14, 0),
                      endTime = LocalTime.of(16, 0),
                      type = ClassType.LAB,
                      location = "LabA",
                      instructor = "Prof. Davis")))
        }

    override fun getTodayAttendanceFlow() =
        kotlinx.coroutines.flow.flow { emit(emptyList<ClassAttendance>()) }

    override suspend fun saveAttendance(attendance: ClassAttendance): Result<Unit> {
      return Result.success(Unit)
    }
  }

  @Before
  fun setup() {
    Dispatchers.setMain(dispatcher)
    // ⚠️ Important : fake repo qui émet une fois et s’arrête
    viewModel = PlannerViewModel(FakePlannerRepository())
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
    viewModel.viewModelScope.coroutineContext.cancelChildren()
  }

  @Test
  fun `initial state should load classes`() =
      runTest(dispatcher) {
        // Attends la première valeur émise du flux
        val uiState = withTimeout(2000) { viewModel.uiState.take(1).first() }

        assertTrue(uiState.classes.isNotEmpty())
        assertEquals(3, uiState.classes.size)
        assertTrue(uiState.attendanceRecords.isEmpty())
      }

  @Test
  fun `modal visibility should toggle correctly`() =
      runTest(dispatcher) {
        var state = withTimeout(2000) { viewModel.uiState.first() }
        assertFalse(state.showAddTaskModal)

        viewModel.onAddStudyTaskClicked()
        state = withTimeout(2000) { viewModel.uiState.first() }
        assertTrue(state.showAddTaskModal)

        viewModel.onDismissAddStudyTaskModal()
        state = withTimeout(2000) { viewModel.uiState.first() }
        assertFalse(state.showAddTaskModal)
      }

  @Test
  fun `saveClassAttendance should emit snackbar and close modal`() =
      runTest(dispatcher) {
        val classItem = viewModel.uiState.first().classes.first()

        // Open modal
        viewModel.onClassClicked(classItem)

        // Start collecting events in parallel
        val eventDeferred = async { withTimeout(2000) { viewModel.eventFlow.first() } }

        // Trigger save
        viewModel.saveClassAttendance(classItem, AttendanceStatus.YES, CompletionStatus.YES)

        // Await snackbar event
        val event = eventDeferred.await()
        assertTrue(event is PlannerViewModel.UiEvent.ShowSnackbar)
        assertEquals(
            "Attendance saved successfully!",
            (event as PlannerViewModel.UiEvent.ShowSnackbar).message)

        // Check UI state updates (with timeout guard)
        val state = withTimeout(2000) { viewModel.uiState.first() }
        assertFalse(state.showAttendanceModal)
        assertNull(state.selectedClass)
      }
}
