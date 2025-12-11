package com.android.sample.ui.schedule

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.sample.R
import com.android.sample.data.ToDo
import com.android.sample.feature.schedule.data.calendar.StudyItem
import com.android.sample.feature.schedule.data.schedule.ScheduleTab
import com.android.sample.feature.schedule.repository.schedule.StudyItemMapper
import com.android.sample.feature.schedule.viewmodel.ScheduleNavEvent
import com.android.sample.feature.schedule.viewmodel.ScheduleUiState
import com.android.sample.feature.schedule.viewmodel.ScheduleViewModel
import com.android.sample.feature.weeks.model.Objective
import com.android.sample.feature.weeks.ui.CourseExercisesRoute
import com.android.sample.feature.weeks.viewmodel.ObjectiveNavigation
import com.android.sample.feature.weeks.viewmodel.ObjectivesViewModel
import com.android.sample.repos_providors.AppRepositories
import com.android.sample.ui.planner.AddStudyTaskModal
import com.android.sample.ui.planner.PetHeader
import com.android.sample.ui.theme.BackgroundDark
import com.android.sample.ui.theme.BackgroundGradientEnd
import com.android.sample.ui.theme.PurplePrimary
import java.time.LocalDate

/** This class was implemented with the help of ai (ChatGPT) */
object ScheduleScreenTestTags {
  const val ROOT = "schedule_root"
  const val TAB_ROW = "schedule_tab_row"
  const val FAB_ADD = "schedule_fab_add"

  const val CONTENT_DAY = "schedule_content_day"
  const val CONTENT_WEEK = "schedule_content_week"
  const val CONTENT_MONTH = "schedule_content_month"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    onAddTodoClicked: (LocalDate) -> Unit = {},
    onOpenTodo: (String) -> Unit = {},
    @DrawableRes avatarResId: Int = R.drawable.edumon,
    petBackgroundBrush: Brush? = null,
    @DrawableRes environmentResId: Int = R.drawable.home, // 👈 NEW: environment
    level: Int = 5,
    onNavigateTo: (String) -> Unit = {}
) {
  val colorScheme = MaterialTheme.colorScheme

  // Header background: if caller doesn’t override, use theme-based gradient
  val headerBrush =
      petBackgroundBrush
          ?: Brush.verticalGradient(
              listOf(
                  colorScheme.primaryContainer,
                  colorScheme.background,
              ))

  // Repos
  val resources = LocalContext.current.resources
  val repositories = remember { AppRepositories }
  val scheduleRepo = remember { repositories.scheduleRepository }
  val plannerRepo = remember { repositories.plannerRepository }

  val vm: ScheduleViewModel =
      viewModel(
          key = "ScheduleVM",
          factory =
              object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                  return ScheduleViewModel(
                      scheduleRepository = scheduleRepo,
                      plannerRepository = plannerRepo,
                      resources = resources)
                      as T
                }
              })

  val objectivesVm: ObjectivesViewModel = viewModel()
  val state by vm.uiState.collectAsState()

  var currentTab by remember { mutableStateOf(ScheduleTab.DAY) }
  var activeObjective by remember { mutableStateOf<Objective?>(null) }

  val snackbarHostState = remember { SnackbarHostState() }

  val allTasks: List<StudyItem> =
      remember(state.allEvents) {
        state.allEvents.map { StudyItemMapper.fromScheduleEvent(it, resources) }
      }

  val weekStart = vm.startOfWeek(state.selectedDate)
  val weekTodos = state.todos.filter { it.dueDate in weekStart..weekStart.plusDays(6) }
  LaunchedEffect(vm) {
    vm.navEvents.collect { event ->
      when (event) {
        is ScheduleNavEvent.ToFlashcards -> onNavigateTo("flashcards") // Make sure route exists
        is ScheduleNavEvent.ToGames -> onNavigateTo("games")
        is ScheduleNavEvent.ToStudySession -> onNavigateTo("study")
        is ScheduleNavEvent.ToStudyTogether -> onNavigateTo("study_together")
        is ScheduleNavEvent.ShowWellnessSuggestion -> {
          snackbarHostState.showSnackbar(event.message)
        }
      }
    }
  }
  if (state.showAddTaskModal) {
    AddStudyTaskModal(
        onDismiss = { vm.onDismissAddStudyTaskModal() },
        onAddTask = { _, _, _, _, _ -> vm.onDismissAddStudyTaskModal() })
  }

  ScheduleSideEffects(
      vm = vm,
      snackbarHostState = snackbarHostState,
      currentTab = currentTab,
      state = state,
  )

  Scaffold(
      snackbarHost = { SnackbarHost(snackbarHostState) },
      floatingActionButton = {
        ScheduleFab(
            currentTab = currentTab,
            state = state,
            vm = vm,
            onAddTodoClicked = onAddTodoClicked,
        )
      },
      containerColor = Color.Transparent,
      modifier =
          Modifier.background(
              Brush.verticalGradient(
                  listOf(
                      colorScheme.background,
                      colorScheme.surface,
                  )))) { padding ->
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(padding)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag(ScheduleScreenTestTags.ROOT),
            horizontalAlignment = Alignment.CenterHorizontally) {
              PetHeader(
                  level = level,
                  avatarResId = avatarResId, // 👈 sprite from caller
                  backgroundBrush = headerBrush, // 👈 theme colors
                  environmentResId = environmentResId // 👈 environment from caller
                  )

              Spacer(Modifier.height(8.dp))

              Box(Modifier.testTag(ScheduleScreenTestTags.TAB_ROW)) {
                ThemedTabRow(
                    selected = currentTab.ordinal,
                    onSelected = { currentTab = ScheduleTab.values()[it] },
                    labels =
                        listOf(
                            stringResource(R.string.tab_day),
                            stringResource(R.string.tab_week),
                            stringResource(R.string.tab_month),
                        ))
              }

              Spacer(Modifier.height(8.dp))

              if (state.isAdjustingPlan) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                    color = colorScheme.primary,
                    trackColor = colorScheme.surfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
              }

              // Extracted content — major complexity reduction
              ScheduleMainContent(
                  currentTab = currentTab,
                  vm = vm,
                  state = state,
                  objectivesVm = objectivesVm,
                  allTasks = allTasks,
                  weekTodos = weekTodos,
                  onOpenTodo = onOpenTodo,
                  onSelectObjective = { activeObjective = it },
              )
            }
      }

  // Course details modal - uses PDF URLs directly from objective
  activeObjective?.let { obj ->
    CourseExercisesRoute(
        objective = obj,
        coursePdfLabel = "Course material for ${obj.course}",
        exercisesPdfLabel = "Exercises for ${obj.course}",
        coursePdfUrl = obj.coursePdfUrl,
        exercisePdfUrl = obj.exercisePdfUrl,
        onBack = { activeObjective = null },
        onCompleted = {
          objectivesVm.markObjectiveCompleted(obj)
          activeObjective = null
        })
  }
}

@Composable
private fun ScheduleMainContent(
    currentTab: ScheduleTab,
    vm: ScheduleViewModel,
    state: ScheduleUiState,
    objectivesVm: ObjectivesViewModel,
    allTasks: List<StudyItem>,
    weekTodos: List<ToDo>,
    onOpenTodo: (String) -> Unit,
    onSelectObjective: (Objective?) -> Unit
) {
  when (currentTab) {
    ScheduleTab.DAY ->
        Box(Modifier.testTag(ScheduleScreenTestTags.CONTENT_DAY)) {
          DayTabContent(
              vm = vm,
              state = state,
              objectivesVm = objectivesVm,
              onObjectiveNavigation = { nav ->
                if (nav is ObjectiveNavigation.ToCourseExercises) {
                  onSelectObjective(nav.objective)
                }
              },
              onTodoClicked = onOpenTodo,
          )
        }
    ScheduleTab.WEEK ->
        Box(Modifier.testTag(ScheduleScreenTestTags.CONTENT_WEEK)) {
          WeekTabContent(
              vm = vm,
              objectivesVm = objectivesVm,
              allTasks = allTasks,
              selectedDate = state.selectedDate,
              weekTodos = weekTodos,
              onTodoClicked = onOpenTodo,
          )
        }
    ScheduleTab.MONTH ->
        Box(Modifier.testTag(ScheduleScreenTestTags.CONTENT_MONTH)) {
          MonthTabContent(
              allTasks = allTasks,
              selectedDate = state.selectedDate,
              // currentMonth = YearMonth.from(state.selectedDate),
              currentMonth = state.currentDisplayMonth,
              onPreviousMonthClick = { vm.onPreviousMonthWeekClicked() },
              onNextMonthClick = { vm.onNextMonthWeekClicked() },
              onDateSelected = { vm.onDateSelected(it) },
          )
        }
  }
}

@Composable
private fun ScheduleFab(
    currentTab: ScheduleTab,
    state: ScheduleUiState,
    vm: ScheduleViewModel,
    onAddTodoClicked: (LocalDate) -> Unit
) {
  val colorScheme = MaterialTheme.colorScheme

  if (currentTab == ScheduleTab.DAY || currentTab == ScheduleTab.WEEK) {
    FloatingActionButton(
        modifier = Modifier.testTag(ScheduleScreenTestTags.FAB_ADD),
        onClick = {
          val date =
              when (currentTab) {
                ScheduleTab.DAY -> LocalDate.now()
                ScheduleTab.WEEK -> vm.startOfWeek(state.selectedDate)
                ScheduleTab.MONTH -> state.selectedDate
              }
          onAddTodoClicked(date)
        },
        containerColor = colorScheme.primary,
        contentColor = colorScheme.onPrimary,
    ) {
      Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_event))
    }
  }
}

@Composable
private fun ScheduleSideEffects(
    vm: ScheduleViewModel,
    snackbarHostState: SnackbarHostState,
    currentTab: ScheduleTab,
    state: ScheduleUiState
) {
  LaunchedEffect(Unit) {
    vm.eventFlow.collect { e ->
      if (e is ScheduleViewModel.UiEvent.ShowSnackbar) {
        snackbarHostState.showSnackbar(e.message)
      }
    }
  }

  LaunchedEffect(currentTab) {
    when (currentTab) {
      ScheduleTab.WEEK -> {
        vm.setWeekMode()
        if (!state.isAdjustingPlan) vm.adjustWeeklyPlan()
      }
      ScheduleTab.MONTH -> vm.setMonthMode()
      else -> Unit
    }
  }
}
