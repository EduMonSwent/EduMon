package com.android.sample.planner

import androidx.lifecycle.viewModelScope
import com.android.sample.data.Priority
import com.android.sample.data.Status
import com.android.sample.data.ToDo
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
        // Fake repository emitting 2 tasks
        val fakeRepo =
            object : com.android.sample.repositories.ToDoRepository {
              override val todos =
                  kotlinx.coroutines.flow.flowOf(
                      listOf(
                          ToDo(
                              title = "Finish report",
                              dueDate = java.time.LocalDate.now(),
                              priority = Priority.HIGH),
                          ToDo(
                              title = "Read chapter",
                              dueDate = java.time.LocalDate.now().plusDays(1),
                              priority = Priority.MEDIUM)))

              override suspend fun add(todo: ToDo) {}

              override suspend fun update(todo: ToDo) {}

              override suspend fun remove(id: String) {}

              override suspend fun getById(id: String) = null
            }

        val vm = com.android.sample.ui.viewmodel.PlannerViewModel(FakePlannerRepository())
        // Inject toDoRepository via reflection (since it's private)
        val repoField = vm.javaClass.getDeclaredField("toDoRepository")
        repoField.isAccessible = true
        repoField.set(vm, fakeRepo)

        // Call private observeToDos()
        val method = vm.javaClass.getDeclaredMethod("observeToDos")
        method.isAccessible = true
        method.invoke(vm)

        delay(100) // allow collection to emit

        val state = vm.uiState.first()
        assertEquals(2, state.todos.size)
        assertEquals("Finish report", state.recommendedTask?.title)
      }

  @Test
  fun `observeToDos should catch errors from repository flow`() =
      runTest(dispatcher) {
        val failingRepo =
            object : com.android.sample.repositories.ToDoRepository {
              override val todos =
                  kotlinx.coroutines.flow.flow<List<ToDo>> { throw RuntimeException("Repo error!") }

              override suspend fun add(todo: ToDo) {}

              override suspend fun update(todo: ToDo) {}

              override suspend fun remove(id: String) {}

              override suspend fun getById(id: String) = null
            }

        val vm = com.android.sample.ui.viewmodel.PlannerViewModel(FakePlannerRepository())
        val repoField = vm.javaClass.getDeclaredField("toDoRepository")
        repoField.isAccessible = true
        repoField.set(vm, failingRepo)

        val method = vm.javaClass.getDeclaredMethod("observeToDos")
        method.isAccessible = true
        method.invoke(vm)

        delay(100)

        val state = vm.uiState.first()
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage!!.contains("Repo error"))
      }
}
