package com.android.sample.planner

import androidx.lifecycle.viewModelScope
import com.android.sample.data.Priority
import com.android.sample.data.Status
import com.android.sample.data.ToDo
import com.android.sample.data.UserStats
import com.android.sample.data.UserStatsRepository
import com.android.sample.feature.schedule.data.planner.AttendanceStatus
import com.android.sample.feature.schedule.data.planner.Class
import com.android.sample.feature.schedule.data.planner.ClassAttendance
import com.android.sample.feature.schedule.data.planner.ClassType
import com.android.sample.feature.schedule.data.planner.CompletionStatus
import com.android.sample.feature.schedule.repository.planner.PlannerRepository
import com.android.sample.feature.schedule.viewmodel.PlannerViewModel
import com.android.sample.repositories.ToDoRepositoryLocal
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class PlannerViewModelTest {

  private val dispatcher = UnconfinedTestDispatcher()
  private lateinit var viewModel: PlannerViewModel

  /** Fake UserStatsRepository so tests don't touch Firebase-backed implementation. */
  private class FakeUserStatsRepository : UserStatsRepository {
    private val _stats = MutableStateFlow(UserStats())
    override val stats: StateFlow<UserStats> = _stats

    override suspend fun start() {
      // no-op for tests
    }

    override suspend fun addStudyMinutes(delta: Int) {
      _stats.value = _stats.value.copy(todayStudyMinutes = _stats.value.todayStudyMinutes + delta)
    }

    override suspend fun addPoints(delta: Int) {
      _stats.value = _stats.value.copy(points = _stats.value.points + delta)
    }

    override suspend fun updateCoins(delta: Int) {
      _stats.value = _stats.value.copy(coins = (_stats.value.coins + delta).coerceAtLeast(0))
    }

    override suspend fun setWeeklyGoal(minutes: Int) {
      _stats.value = _stats.value.copy(weeklyGoal = minutes)
    }
    // addReward uses default implementation from the interface, which is fine for tests
  }

  // Fake repository avec flux finis (pas de collect infini)
  private class FakeToDoRepository(
      private val items: List<ToDo> = emptyList(),
      private val throws: Boolean = false
  ) : com.android.sample.repositories.ToDoRepository {
    override val todos =
        if (throws)
            kotlinx.coroutines.flow.flow<List<ToDo>> { throw RuntimeException("Repo error!") }
        else kotlinx.coroutines.flow.flowOf(items)

    override suspend fun add(todo: ToDo) {}

    override suspend fun update(todo: ToDo) {}

    override suspend fun remove(id: String) {}

    override suspend fun getById(id: String) = null
  }

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
    // ⚠️ Important : use fake stats repo so we don't require Firebase
    viewModel =
        PlannerViewModel(
            plannerRepository = FakePlannerRepository(),
            toDoRepository = ToDoRepositoryLocal(),
            userStatsRepository = FakeUserStatsRepository())
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
  fun `saveClassAttendance should emit toast and close modal`() =
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
            "Attendance saved! Earned: +8 XP +3 coins",
            (event as PlannerViewModel.UiEvent.ShowSnackbar).message)

        // Check UI state updates (with timeout guard)
        val state = withTimeout(2000) { viewModel.uiState.first() }
        assertFalse(state.showAttendanceModal)
        assertNull(state.selectedClass)
      }

  @Test
  fun `recommendNextTask should return null when list is empty`() =
      runTest(dispatcher) {
        val method = viewModel.javaClass.getDeclaredMethod("recommendNextTask", List::class.java)
        method.isAccessible = true

        val result = method.invoke(viewModel, emptyList<ToDo>()) as ToDo?
        assertNull(result)
      }

  @Test
  fun `recommendNextTask should prioritize by HIGH priority first`() =
      runTest(dispatcher) {
        val high =
            ToDo(title = "High", dueDate = java.time.LocalDate.now(), priority = Priority.HIGH)
        val low = ToDo(title = "Low", dueDate = java.time.LocalDate.now(), priority = Priority.LOW)
        val medium =
            ToDo(title = "Medium", dueDate = java.time.LocalDate.now(), priority = Priority.MEDIUM)

        val method = viewModel.javaClass.getDeclaredMethod("recommendNextTask", List::class.java)
        method.isAccessible = true
        val result = method.invoke(viewModel, listOf(low, medium, high)) as ToDo?

        assertNotNull(result)
        assertEquals("High", result?.title)
      }

  @Test
  fun `recommendNextTask should prefer earlier due date when priorities are equal`() =
      runTest(dispatcher) {
        val today = java.time.LocalDate.now()
        val tomorrow = today.plusDays(1)

        val task1 = ToDo(title = "Earlier", dueDate = today, priority = Priority.MEDIUM)
        val task2 = ToDo(title = "Later", dueDate = tomorrow, priority = Priority.MEDIUM)

        val method = viewModel.javaClass.getDeclaredMethod("recommendNextTask", List::class.java)
        method.isAccessible = true
        val result = method.invoke(viewModel, listOf(task2, task1)) as ToDo?

        assertEquals("Earlier", result?.title)
      }

  @Test
  fun `recommendNextTask should prefer IN_PROGRESS over TODO for same priority`() =
      runTest(dispatcher) {
        val taskTodo =
            ToDo(
                title = "TodoTask",
                dueDate = java.time.LocalDate.now(),
                priority = Priority.HIGH,
                status = Status.TODO)
        val taskProgress =
            ToDo(
                title = "ProgressTask",
                dueDate = java.time.LocalDate.now(),
                priority = Priority.HIGH,
                status = Status.IN_PROGRESS)

        val method = viewModel.javaClass.getDeclaredMethod("recommendNextTask", List::class.java)
        method.isAccessible = true
        val result = method.invoke(viewModel, listOf(taskTodo, taskProgress)) as ToDo?

        assertEquals("ProgressTask", result?.title)
      }

  @Test
  fun `observeToDos should update uiState with todos and recommended task`() =
      runTest(dispatcher) {
        val vm =
            PlannerViewModel(
                plannerRepository = FakePlannerRepository(),
                toDoRepository =
                    FakeToDoRepository(
                        items =
                            listOf(
                                ToDo(
                                    title = "Finish report",
                                    dueDate = LocalDate.now(),
                                    priority = Priority.HIGH),
                                ToDo(
                                    title = "Read chapter",
                                    dueDate = LocalDate.now().plusDays(1),
                                    priority = Priority.MEDIUM))),
                userStatsRepository = FakeUserStatsRepository())

        // Let collectors run (UnconfinedTestDispatcher -> small delay is fine)
        delay(50)

        val state = vm.uiState.first()
        assertEquals(2, state.todos.size)
        assertEquals("Finish report", state.recommendedTask?.title)
      }

  @Test
  fun `observeToDos should catch errors from repository flow`() =
      runTest(dispatcher) {
        val vm =
            PlannerViewModel(
                plannerRepository = FakePlannerRepository(),
                toDoRepository = FakeToDoRepository(throws = true),
                userStatsRepository = FakeUserStatsRepository())

        delay(50)

        val state = vm.uiState.first()
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage!!.contains("Repo error"))
      }
}
