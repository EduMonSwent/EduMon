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
import com.android.sample.feature.schedule.repository.calendar.CalendarRepositoryImpl // tasks repo
import com.android.sample.feature.schedule.repository.planner.FakePlannerRepository // classes+attendance repo
import com.android.sample.feature.schedule.repository.schedule.ScheduleRepositoryImpl
import com.android.sample.feature.schedule.repository.schedule.StudyItemMapper
import com.android.sample.feature.schedule.viewmodel.ScheduleViewModel
import com.android.sample.feature.weeks.viewmodel.ObjectivesViewModel
import com.android.sample.feature.weeks.viewmodel.WeeksViewModel
import com.android.sample.ui.planner.AddStudyTaskModal
import com.android.sample.ui.planner.PetHeader
import com.android.sample.ui.theme.BackgroundDark
import com.android.sample.ui.theme.BackgroundGradientEnd
import com.android.sample.ui.theme.PurplePrimary
import java.time.LocalDate

/** This class was implemented with the help of ai (chatgbt) */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen() {
  // Repos
  val taskRepo = remember { CalendarRepositoryImpl() } // tasks
  val plannerRepo = remember { FakePlannerRepository() } // classes & attendance
  val resources = LocalContext.current.resources

  // Schedule repository now needs Resources
  val scheduleRepo = remember { ScheduleRepositoryImpl(taskRepo, plannerRepo, resources) }

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
  val weeksVm: WeeksViewModel = viewModel()

  val state by vm.uiState.collectAsState()
  val allTasks: List<StudyItem> =
      remember(state.allEvents) {
        state.allEvents.map { StudyItemMapper.fromScheduleEvent(it, resources) }
      }

  var currentTab by remember { mutableStateOf(ScheduleTab.DAY) }
  var showAddModal by remember { mutableStateOf(false) }
  var addDate by remember { mutableStateOf<LocalDate?>(null) }

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
            onClick = {
              addDate =
                  when (currentTab) {
                    ScheduleTab.DAY -> LocalDate.now()
                    ScheduleTab.WEEK -> vm.startOfWeek(state.selectedDate)
                    ScheduleTab.MONTH,
                    ScheduleTab.AGENDA -> state.selectedDate
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
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
              // Header now matches Home (use the updated PetHeader you implemented)
              PetHeader(level = 5)

              Spacer(Modifier.height(8.dp))

              // ThemedTabRow will now use the GlassSurface background and adjusted styling
              ThemedTabRow(
                  selected = currentTab.ordinal,
                  onSelected = { currentTab = ScheduleTab.values()[it] },
                  labels =
                      listOf(
                          stringResource(R.string.tab_day),
                          stringResource(R.string.tab_week),
                          stringResource(R.string.tab_month),
                          stringResource(R.string.tab_agenda)))

              Spacer(Modifier.height(8.dp))
              if (state.isAdjustingPlan) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(4.dp))
                Spacer(Modifier.height(8.dp))
              }

              when (currentTab) {
                ScheduleTab.DAY ->
                    DayTabContent(vm = vm, state = state, objectivesVm = objectivesVm)
                ScheduleTab.WEEK -> {
                  WeekTabContent(
                      vm = vm,
                      objectivesVm = objectivesVm,
                      allTasks = allTasks,
                      selectedDate = state.selectedDate)
                }
                ScheduleTab.MONTH -> {
                  // TODO: implement Month
                }
                ScheduleTab.AGENDA -> {
                  // TODO: implement Agenda
                }
              }
            }
      }

  if (showAddModal && addDate != null) {
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
}
