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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.sample.feature.weeks.viewmodel.ObjectivesViewModel
import com.android.sample.feature.weeks.viewmodel.WeeksViewModel
import com.android.sample.model.StudyItem
import com.android.sample.model.calendar.PlannerRepositoryImpl
import com.android.sample.model.planner.FakePlannerRepository
import com.android.sample.model.schedule.*
import com.android.sample.ui.planner.AddStudyTaskModal
import com.android.sample.ui.planner.PetHeader
import com.android.sample.ui.theme.BackgroundDark
import com.android.sample.ui.theme.BackgroundGradientEnd
import com.android.sample.ui.theme.PurplePrimary
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen() {
  val taskRepo = remember { PlannerRepositoryImpl() }
  val classRepo = remember { FakePlannerRepository() }
  val scheduleRepo = remember { ScheduleRepositoryImpl(taskRepo, classRepo) }

  val vm: ScheduleViewModel =
      viewModel(
          key = "ScheduleVM",
          factory =
              object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: java.lang.Class<T>): T {
                  return ScheduleViewModel(scheduleRepo) as T
                }
              })

  val objectivesVm: ObjectivesViewModel = viewModel()
  val weeksVm: WeeksViewModel = viewModel()

  val selectedDate by vm.selectedDate.collectAsState()
  val currentMonth by vm.currentDisplayMonth.collectAsState()
  val allEvents by vm.allEvents.collectAsState()
  val isAdjustingPlan by vm.isAdjustingPlan.collectAsState()

  val todayClasses by
      remember(classRepo) { classRepo.getTodayClassesFlow() }.collectAsState(initial = emptyList())
  val allTasks: List<StudyItem> =
      remember(allEvents) { allEvents.map(StudyItemMapper::fromScheduleEvent) }

  var currentTab by remember { mutableStateOf(ScheduleTab.DAY) }
  var showAddModal by remember { mutableStateOf(false) }
  var addDate by remember { mutableStateOf<LocalDate?>(null) }

  LaunchedEffect(currentTab) {
    when (currentTab) {
      ScheduleTab.WEEK -> {
        vm.setWeekMode()
        if (!isAdjustingPlan) {
          vm.adjustWeeklyPlan()
        }
      }
      ScheduleTab.MONTH -> vm.setMonthMode()
      else -> {
        /* Day & Agenda don't need special handling */
      }
    }
  }

  Scaffold(
      floatingActionButton = {
        FloatingActionButton(
            onClick = {
              addDate =
                  when (currentTab) {
                    ScheduleTab.DAY -> LocalDate.now()
                    ScheduleTab.WEEK -> vm.startOfWeek(selectedDate)
                    ScheduleTab.MONTH -> selectedDate
                    ScheduleTab.AGENDA -> selectedDate
                  }
              showAddModal = true
            },
            containerColor = PurplePrimary,
            contentColor = Color.White) {
              Icon(Icons.Filled.Add, contentDescription = "Add")
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
              /* HEADER */
              PetHeader(level = 5, onEdumonNameClick = {})
              Spacer(Modifier.height(8.dp))

              /* TABS */
              ThemedTabRow(
                  selected = currentTab.ordinal,
                  onSelected = { currentTab = ScheduleTab.values()[it] },
                  labels = listOf("Day", "Week", "Month", "Agenda"))

              Spacer(Modifier.height(8.dp))
              if (isAdjustingPlan) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(4.dp))
                Spacer(Modifier.height(8.dp))
              }

              when (currentTab) {
                ScheduleTab.DAY -> DayTabContent(todayClasses, objectivesVm)
                ScheduleTab.WEEK ->
                    WeekTabContent(vm, weeksVm, objectivesVm, allTasks, selectedDate)
                ScheduleTab.MONTH -> MonthTabContent(vm, allTasks, selectedDate, currentMonth)
                ScheduleTab.AGENDA -> AgendaTabContent(vm, allTasks, selectedDate)
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
