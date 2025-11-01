package com.android.sample.ui.schedule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.android.sample.feature.weeks.ui.DailyObjectivesSection
import com.android.sample.feature.weeks.ui.WeekDotsRow
import com.android.sample.feature.weeks.ui.WeekProgDailyObjTags
import com.android.sample.feature.weeks.viewmodel.ObjectivesViewModel
import com.android.sample.feature.weeks.viewmodel.WeeksViewModel
import com.android.sample.model.StudyItem
import com.android.sample.model.planner.WellnessEventType
import com.android.sample.ui.calendar.MonthGrid
import com.android.sample.ui.calendar.UpcomingEventsSection
import com.android.sample.ui.calendar.WeekRow
import com.android.sample.ui.planner.ActivityItem
import com.android.sample.ui.planner.WellnessEventItem
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun DayTabContent(
    todayClasses: List<com.android.sample.model.planner.Class>,
    objectivesVm: ObjectivesViewModel
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
fun WeekTabContent(
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
fun MonthTabContent(
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
fun AgendaTabContent(vm: ScheduleViewModel, allTasks: List<StudyItem>, selectedDate: LocalDate) {
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
