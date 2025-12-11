package com.android.sample.feature.schedule.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.sample.data.Priority
import com.android.sample.data.Status
import com.android.sample.data.ToDo
import com.android.sample.data.UserStatsRepository
import com.android.sample.feature.schedule.data.planner.AttendanceStatus
import com.android.sample.feature.schedule.data.planner.Class
import com.android.sample.feature.schedule.data.planner.ClassAttendance
import com.android.sample.feature.schedule.data.planner.CompletionStatus
import com.android.sample.feature.schedule.repository.planner.PlannerRepository
import com.android.sample.repos_providors.AppRepositories
import com.android.sample.repositories.ToDoRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// Reward constants - adjust these values as needed
private const val POINTS_FOR_ATTENDANCE = 5
private const val POINTS_FOR_COMPLETION = 3
private const val COINS_FOR_ATTENDANCE = 1
private const val COINS_FOR_COMPLETION = 2

data class PlannerUiState(
    val classes: List<Class> = emptyList(),
    val attendanceRecords: List<ClassAttendance> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val showAddTaskModal: Boolean = false,
    val showAttendanceModal: Boolean = false,
    val selectedClass: Class? = null,
    val todos: List<ToDo> = emptyList(),
    val recommendedTask: ToDo? = null
)

open class PlannerViewModel(
    private val plannerRepository: PlannerRepository = AppRepositories.plannerRepository,
    private val toDoRepository: ToDoRepository = AppRepositories.toDoRepository,
    private val userStatsRepository: UserStatsRepository = AppRepositories.userStatsRepository
) : ViewModel() {

  private val _uiState = MutableStateFlow(PlannerUiState())
  val uiState: StateFlow<PlannerUiState> = _uiState.asStateFlow()

  private val _eventFlow = MutableSharedFlow<UiEvent>()
  val eventFlow = _eventFlow.asSharedFlow()

  sealed class UiEvent {
    data class ShowSnackbar(val message: String) : UiEvent()
  }

  init {
    observePlannerData()
    observeToDos()
  }

  private fun observePlannerData() {
    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isLoading = true)

      try {
        launch {
          plannerRepository
              .getTodayClassesFlow()
              .catch { e ->
                _uiState.value =
                    _uiState.value.copy(errorMessage = e.localizedMessage, isLoading = false)
              }
              .collectLatest { classList ->
                _uiState.value = _uiState.value.copy(classes = classList, isLoading = false)
              }
        }

        launch {
          plannerRepository
              .getTodayAttendanceFlow()
              .catch { e ->
                _uiState.value =
                    _uiState.value.copy(errorMessage = e.localizedMessage, isLoading = false)
              }
              .collectLatest { records ->
                _uiState.value = _uiState.value.copy(attendanceRecords = records, isLoading = false)
              }
        }
      } catch (e: Exception) {
        _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = e.localizedMessage)
      }
    }
  }

  private fun observeToDos() {
    viewModelScope.launch {
      toDoRepository.todos
          .catch { e -> _uiState.value = _uiState.value.copy(errorMessage = e.localizedMessage) }
          .collectLatest { todos ->
            val recommended = recommendNextTask(todos)
            _uiState.value = _uiState.value.copy(todos = todos, recommendedTask = recommended)
          }
    }
  }

  private fun recommendNextTask(tasks: List<ToDo>): ToDo? {
    if (tasks.isEmpty()) return null

    return tasks
        .filter { it.status != Status.DONE }
        .sortedWith(
            compareByDescending<ToDo> { it.priority.weight() }
                .thenBy { it.dueDate }
                .thenBy { it.status.ordinal * (-1) })
        .firstOrNull()
  }

  private fun Priority.weight(): Int =
      when (this) {
        Priority.HIGH -> 3
        Priority.MEDIUM -> 2
        Priority.LOW -> 1
      }

  fun onAddStudyTaskClicked() {
    _uiState.value = _uiState.value.copy(showAddTaskModal = true)
  }

  fun onDismissAddStudyTaskModal() {
    _uiState.value = _uiState.value.copy(showAddTaskModal = false)
  }

  fun onClassClicked(classItem: Class) {
    _uiState.value = _uiState.value.copy(selectedClass = classItem, showAttendanceModal = true)
  }

  fun onDismissClassAttendanceModal() {
    _uiState.value = _uiState.value.copy(selectedClass = null, showAttendanceModal = false)
  }

  fun saveClassAttendance(
      classItem: Class,
      attendance: AttendanceStatus,
      completion: CompletionStatus
  ) {
    viewModelScope.launch {
      val attendanceRecord =
          ClassAttendance(
              classId = classItem.id,
              date = LocalDate.now(),
              attendance = attendance,
              completion = completion)

      val result = plannerRepository.saveAttendance(attendanceRecord)

      if (result.isSuccess) {
        // Calculate rewards based on attendance and completion
        val points = calculatePoints(attendance, completion)
        val coins = calculateCoins(attendance, completion)

        // Grant rewards if earned (single Firestore write, one toast)
        if (points > 0 || coins > 0) {
          userStatsRepository.addReward(
              points = points, coins = coins
              // minutes defaults to 0
              )
        }

        onDismissClassAttendanceModal()

        // Show success message with earned rewards
        val message = buildSuccessMessage(attendance, completion, points, coins)
        _eventFlow.emit(UiEvent.ShowSnackbar(message))
      } else {
        _eventFlow.emit(UiEvent.ShowSnackbar("Error saving attendance"))
      }
    }
  }

  private fun calculatePoints(attendance: AttendanceStatus, completion: CompletionStatus): Int {
    var points = 0

    // Points for attending class
    if (attendance == AttendanceStatus.YES) {
      points += POINTS_FOR_ATTENDANCE
    }

    // Bonus points for completing the work
    if (completion == CompletionStatus.YES) {
      points += POINTS_FOR_COMPLETION
    }

    return points
  }

  private fun calculateCoins(attendance: AttendanceStatus, completion: CompletionStatus): Int {
    var coins = 0

    // Coins for attending class
    if (attendance == AttendanceStatus.YES) {
      coins += COINS_FOR_ATTENDANCE
    }

    // Bonus coins for completing the work
    if (completion == CompletionStatus.YES) {
      coins += COINS_FOR_COMPLETION
    }

    return coins
  }

  private fun buildSuccessMessage(
      attendance: AttendanceStatus,
      completion: CompletionStatus,
      points: Int,
      coins: Int
  ): String {
    return buildString {
      append("Attendance saved!")
      if (points > 0 || coins > 0) {
        append(" Earned: ")
        if (points > 0) append("+$points XP")
        if (points > 0 && coins > 0) append(" ")
        if (coins > 0) append("+$coins coins")
      }
    }
  }

  fun clearError() {
    _uiState.value = _uiState.value.copy(errorMessage = null)
  }

  fun updateTestData(classes: List<Class>, attendance: List<ClassAttendance>) {
    _uiState.value = _uiState.value.copy(classes = classes, attendanceRecords = attendance)
  }

  fun refreshRecommendation() {
    _uiState.value = _uiState.value.copy(recommendedTask = recommendNextTask(_uiState.value.todos))
  }
}
