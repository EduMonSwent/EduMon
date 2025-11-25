package com.android.sample.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.android.sample.feature.schedule.data.calendar.StudyItem
import com.android.sample.feature.schedule.data.schedule.EventKind
import com.android.sample.feature.schedule.data.schedule.Priority
import com.android.sample.feature.schedule.data.schedule.ScheduleEvent
import com.android.sample.feature.schedule.data.schedule.ScheduleTab
import com.android.sample.feature.schedule.data.schedule.SourceTag
import com.android.sample.feature.schedule.repository.schedule.StudyItemMapper
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
import java.time.YearMonth

/** This class was implemented with the help of ai (chatgbt) */
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
fun ScheduleScreen() {
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
  val allTasks: List<StudyItem> =
      remember(state.allEvents) {
        state.allEvents.map { StudyItemMapper.fromScheduleEvent(it, resources) }
      }

  var currentTab by remember { mutableStateOf(ScheduleTab.DAY) }
  var showAddModal by remember { mutableStateOf(false) }
  var addDate by remember { mutableStateOf<LocalDate?>(null) }
  var activeObjectiveForCourse by remember { mutableStateOf<Objective?>(null) }

  // Snackbar for VM events
  val snackbarHostState = remember { SnackbarHostState() }
  LaunchedEffect(Unit) {
    vm.eventFlow.collect { e ->
      when (e) {
        is ScheduleViewModel.UiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(e.message)
      }
    }
  }

  // Keep VM mode in sync with tab
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

  Scaffold(
      snackbarHost = { SnackbarHost(snackbarHostState) },
      floatingActionButton = {
        FloatingActionButton(
            modifier = Modifier.testTag(ScheduleScreenTestTags.FAB_ADD),
            onClick = {
              addDate =
                  when (currentTab) {
                    ScheduleTab.DAY -> LocalDate.now()
                    ScheduleTab.WEEK -> vm.startOfWeek(state.selectedDate)
                    ScheduleTab.MONTH -> state.selectedDate
                  }
              showAddModal = true
            },
            containerColor = PurplePrimary,
            contentColor = Color.White) {
              Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_event))
            }
      },
      containerColor = Color.Transparent,
      modifier =
          Modifier.background(
              Brush.verticalGradient(listOf(BackgroundDark, BackgroundGradientEnd)))) { padding ->
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag(ScheduleScreenTestTags.ROOT),
            horizontalAlignment = Alignment.CenterHorizontally) {
              // Header now matches Home (use the updated PetHeader you implemented)
              PetHeader(level = 5)

              Spacer(Modifier.height(8.dp))

              // ThemedTabRow will now use the GlassSurface background and adjusted styling
              Box(Modifier.testTag(ScheduleScreenTestTags.TAB_ROW)) {
                ThemedTabRow(
                    selected = currentTab.ordinal,
                    onSelected = { currentTab = ScheduleTab.values()[it] },
                    labels =
                        listOf(
                            stringResource(R.string.tab_day),
                            stringResource(R.string.tab_week),
                            stringResource(R.string.tab_month)))
              }

              Spacer(Modifier.height(8.dp))
              if (state.isAdjustingPlan) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(4.dp))
                Spacer(Modifier.height(8.dp))
              }

              when (currentTab) {
                ScheduleTab.DAY ->
                    Box(
                        modifier =
                            Modifier.fillMaxSize().testTag(ScheduleScreenTestTags.CONTENT_DAY)) {
                          DayTabContent(
                              vm = vm,
                              state = state,
                              objectivesVm = objectivesVm,
                              onObjectiveNavigation = { nav ->
                                when (nav) {
                                  is ObjectiveNavigation.ToCourseExercises -> {
                                    activeObjectiveForCourse = nav.objective
                                  }
                                  else ->
                                      Unit // we ignore ToQuiz/ToResume, since you don't use them
                                // now
                                }
                              })
                        }
                ScheduleTab.WEEK -> {
                  Box(
                      modifier =
                          Modifier.fillMaxSize().testTag(ScheduleScreenTestTags.CONTENT_WEEK)) {
                        WeekTabContent(
                            vm = vm,
                            objectivesVm = objectivesVm,
                            allTasks = allTasks,
                            selectedDate = state.selectedDate)
                      }
                }
                ScheduleTab.MONTH -> {
                  Box(
                      modifier =
                          Modifier.fillMaxSize().testTag(ScheduleScreenTestTags.CONTENT_MONTH)) {
                        MonthTabContent(
                            allTasks = allTasks,
                            selectedDate = state.selectedDate,
                            currentMonth = YearMonth.from(state.selectedDate),
                            onPreviousMonthClick = { vm.onPreviousMonthWeekClicked() },
                            onNextMonthClick = { vm.onNextMonthWeekClicked() },
                            onDateSelected = { vm.onDateSelected(it) },
                        )
                      }
                }
              }
            }
      }

  if (showAddModal && addDate != null && activeObjectiveForCourse != null) {
    AddStudyTaskModal(
        onDismiss = {
          showAddModal = false
          addDate = null
        },
        onAddTask = { subject, title, duration, _, priority ->
          val event =
              ScheduleEvent(
                  title = if (title.isNotBlank()) title else subject,
                  date = addDate!!,
                  time = null,
                  durationMinutes = duration,
                  kind = EventKind.STUDY,
                  priority =
                      when (priority.lowercase()) {
                        "low" -> Priority.LOW
                        "high" -> Priority.HIGH
                        else -> Priority.MEDIUM
                      },
                  sourceTag = SourceTag.Task)
          vm.save(event)
          showAddModal = false
          addDate = null
        })
  }
  val activeObjective = activeObjectiveForCourse
  if (activeObjective != null) {
    CourseExercisesRoute(
        objective = activeObjective,
        coursePdfLabel = "Course material for ${activeObjective.course}",
        exercisesPdfLabel = "Exercises for ${activeObjective.course}",
        onBack = {
          // Just close the screen without changing completion
          activeObjectiveForCourse = null
        },
        onOpenCoursePdf = {
          // TODO: open the course PDF for this objective
        },
        onOpenExercisesPdf = {
          // TODO: open the exercises PDF for this objective
        },
        onCompleted = {
          // 1) Mark objective as completed in the VM
          objectivesVm.markObjectiveCompleted(activeObjective)
          // 2) Close the Course/Exercises screen
          activeObjectiveForCourse = null
        })
  }
}
