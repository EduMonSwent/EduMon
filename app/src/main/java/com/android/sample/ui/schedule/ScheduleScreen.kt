package com.android.sample.ui.schedule

// --- Tes composants/VM de weeks ---
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.sample.feature.weeks.ui.DailyObjectivesSection
import com.android.sample.feature.weeks.ui.WeekDotsRow
import com.android.sample.feature.weeks.ui.WeekProgDailyObjTags
import com.android.sample.feature.weeks.viewmodel.ObjectivesViewModel
import com.android.sample.feature.weeks.viewmodel.WeeksViewModel
import com.android.sample.model.StudyItem
import com.android.sample.model.calendar.PlannerRepositoryImpl
import com.android.sample.model.planner.FakePlannerRepository
import com.android.sample.model.planner.WellnessEventType
import com.android.sample.model.schedule.*
import com.android.sample.ui.calendar.MonthGrid
import com.android.sample.ui.calendar.UpcomingEventsSection
import com.android.sample.ui.calendar.WeekRow
import com.android.sample.ui.planner.ActivityItem
import com.android.sample.ui.planner.AddStudyTaskModal
import com.android.sample.ui.planner.PetHeader
import com.android.sample.ui.planner.WellnessEventItem
import com.android.sample.ui.theme.BackgroundDark
import com.android.sample.ui.theme.BackgroundGradientEnd
import com.android.sample.ui.theme.PurplePrimary
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlin.text.get

enum class ScheduleTab {
  DAY,
  WEEK,
  MONTH,
  AGENDA
}

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

  // Today classes (comme Planner)
  val todayClasses by
      remember(classRepo) { classRepo.getTodayClassesFlow() }.collectAsState(initial = emptyList())

  // Adapter events -> StudyItem pour Calendar UI
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

@Composable
private fun DayTabContent(
    todayClasses: List<com.android.sample.model.planner.Class>,
    objectivesVm: com.android.sample.feature.weeks.viewmodel.ObjectivesViewModel
) {
  LazyColumn(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.spacedBy(16.dp),
      contentPadding = PaddingValues(bottom = 96.dp)) {
        item(key = "day-title") {
          val today = LocalDate.now()
          Text(
              "Today • " + DateTimeFormatter.ofPattern("EEEE, MMM d").format(today),
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        }

        if (todayClasses.isEmpty()) {
          item {
            Text("No classes scheduled today", color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        } else {
          items(todayClasses.size) { idx ->
            ActivityItem(activity = todayClasses[idx], attendanceRecord = null, onClick = {})
            Spacer(Modifier.height(8.dp))
          }
        }

        item(key = "day-objectives") {
          DailyObjectivesSection(viewModel = objectivesVm, modifier = Modifier.fillMaxWidth())
        }

        item(key = "day-wellness") {
          WellnessEventItem(
              title = "Yoga Session",
              time = "18:00",
              description = "Breathing & stretching for 45 min",
              eventType = WellnessEventType.YOGA,
              onClick = {})
          Spacer(Modifier.height(8.dp))
          WellnessEventItem(
              title = "Guest Lecture",
              time = "19:30",
              description = "AI & Society – Auditorium A",
              eventType = WellnessEventType.LECTURE,
              onClick = {})
        }
      }
}

@Composable
private fun WeekTabContent(
    vm: ScheduleViewModel,
    weeksVm: WeeksViewModel,
    objectivesVm: ObjectivesViewModel,
    allTasks: List<StudyItem>,
    selectedDate: LocalDate
) {
  LazyColumn(
      modifier = Modifier.fillMaxSize().testTag("WeekContent"),
      verticalArrangement = Arrangement.spacedBy(16.dp),
      contentPadding = PaddingValues(bottom = 96.dp)) {
        item(key = "week-row") {
          FramedSection {
            val weekStart = vm.startOfWeek(selectedDate)
            WeekRow(
                startOfWeek = weekStart,
                selectedDate = selectedDate,
                allTasks = allTasks,
                onDayClick = { vm.onDateSelected(it) },
                onPrevClick = { vm.onPreviousMonthWeekClicked() },
                onNextClick = { vm.onNextMonthWeekClicked() })
          }
        }

        item(key = "week-dots") {
          FramedSection {
            WeekDotsRow(
                objectivesVm,
                modifier =
                    Modifier.fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                        .testTag(WeekProgDailyObjTags.WEEK_DOTS_ROW))
          }
        }

        item(key = "week-upcoming") {
          FramedSection(modifier = Modifier.testTag("WeekUpcomingSection")) {
            val start = vm.startOfWeek(selectedDate)
            val end = start.plusDays(6)
            val weekTasks = allTasks.filter { it.date in start..end }
            UpcomingEventsSection(
                tasks =
                    weekTasks.sortedWith(
                        compareBy({ it.date }, { it.time ?: java.time.LocalTime.MIN })),
                selectedDate = selectedDate,
                onAddTaskClick = { /* handled by FAB */ _ -> },
                onTaskClick = {},
                title = "This week")
          }
        }
      }
}

@Composable
private fun MonthTabContent(
    vm: ScheduleViewModel,
    allTasks: List<StudyItem>,
    selectedDate: LocalDate,
    currentMonth: YearMonth
) {
  Column(
      modifier =
          Modifier.fillMaxSize()
              .testTag("MonthContent")
              .verticalScroll(rememberScrollState()) // Add scroll if needed
              .padding(bottom = 96.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Month Grid
        FramedSection(modifier = Modifier.testTag("MonthImportantSection")) {
          MonthGrid(
              currentMonth = currentMonth,
              selectedDate = selectedDate,
              allTasks = allTasks,
              onDateClick = { vm.onDateSelected(it) },
              onPrevClick = { vm.onPreviousMonthWeekClicked() },
              onNextClick = { vm.onNextMonthWeekClicked() })
        }

        // Important events section
        val monthStart = selectedDate.withDayOfMonth(1)
        val monthEnd = monthStart.plusMonths(1).minusDays(1)

        val monthTasks =
            allTasks
                .filter { it.date >= monthStart && it.date <= monthEnd }
                .distinctBy { it.id }
                .sortedWith(compareBy({ it.date }, { it.time ?: java.time.LocalTime.MIN }))

        SectionHeader("Most important this month")

        FramedSection {
          if (monthTasks.isEmpty()) {
            Text(
                "No important events this month",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
          } else {
            monthTasks.forEach {
              Text("• ${it.title} — ${it.date}")
              Spacer(modifier = Modifier.height(8.dp))
            }
          }
        }
      }
}

@Composable
private fun AgendaTabContent(
    vm: ScheduleViewModel,
    allTasks: List<com.android.sample.model.StudyItem>,
    selectedDate: LocalDate
) {
  LazyColumn(
      modifier = Modifier.fillMaxSize().testTag("AgendaContent"),
      verticalArrangement = Arrangement.spacedBy(16.dp),
      contentPadding = PaddingValues(bottom = 96.dp)) {
        item(key = "agenda-list") {
          FramedSection(modifier = Modifier.testTag("AgendaSection")) {
            val upcoming = allTasks.filter { it.date >= selectedDate }
            UpcomingEventsSection(
                tasks =
                    upcoming.sortedWith(
                        compareBy({ it.date }, { it.time ?: java.time.LocalTime.MIN })),
                selectedDate = selectedDate,
                onAddTaskClick = { /* handled by FAB */ _ -> },
                onTaskClick = {},
                title = "Agenda")
          }
        }
      }
}

/* --- simple helper for Month --- */
@Composable
private fun MostImportantEventsSection(tasks: List<StudyItem>, title: String) {
  Text(title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
  Spacer(Modifier.height(6.dp))
  val ranked =
      tasks
          .sortedWith(compareBy<StudyItem>({ it.date }, { it.time ?: java.time.LocalTime.MIN }))
          .take(8)
  if (ranked.isEmpty()) {
    Text("No important events this month", color = MaterialTheme.colorScheme.onSurfaceVariant)
  } else {
    ranked.forEach {
      Text("• ${it.title} — ${it.date}")
      Spacer(Modifier.height(4.dp))
    }
  }
}
// ---- Shared UI tokens/helpers ----
private val SchedulePadding = 16.dp

@Composable
private fun SectionHeader(text: String) {
  Text(
      text,
      style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
      color = MaterialTheme.colorScheme.onSurface,
      modifier = Modifier.padding(horizontal = SchedulePadding))
}

@Composable
private fun FramedSection(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
  val shape = RoundedCornerShape(24.dp)
  Box(
      modifier =
          modifier
              .fillMaxWidth()
              .shadow(8.dp, shape)
              .border(1.dp, com.android.sample.ui.theme.PurplePrimary, shape)
              .background(com.android.sample.ui.theme.DarkBlue.copy(alpha = 0.85f), shape)
              .padding(contentPadding)) {
        Column(content = content)
      }
}

@Composable
private fun ThemedTabRow(selected: Int, onSelected: (Int) -> Unit, labels: List<String>) {
  val cs = MaterialTheme.colorScheme
  TabRow(
      selectedTabIndex = selected,
      containerColor = Color.Transparent,
      indicator = { positions ->
        TabRowDefaults.Indicator(
            modifier = Modifier.tabIndicatorOffset(positions[selected]).height(3.dp),
            color = cs.primary)
      },
      divider = {}) {
        labels.forEachIndexed { i, label ->
          Tab(
              selected = selected == i,
              onClick = { onSelected(i) },
              selectedContentColor = cs.primary,
              unselectedContentColor = cs.onSurface.copy(alpha = 0.75f),
              text = {
                Text(
                    label,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 10.dp))
              })
        }
      }
}
