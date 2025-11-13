package com.android.sample.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.sample.R
import com.android.sample.feature.schedule.viewmodel.CalendarViewModel
import com.android.sample.ui.theme.BackgroundDark
import com.android.sample.ui.theme.BackgroundGradientEnd
import com.android.sample.ui.theme.DarkBlue
import com.android.sample.ui.theme.PurplePrimary

object CalendarScreenTestTags {
  const val MENU_BUTTON = "menuButton"
  const val VIEW_TOGGLE_BUTTON = "viewToggleButton"
  const val CALENDAR_CARD = "calendarCard"
  const val CALENDAR_HEADER = "calendarHeader"
  const val WEEK_ROW = "weekRow"
  const val WEEK_DAY_BOX_PREFIX = "weekDayBox_"
  const val WEEK_EVENT_PREFIX = "weekEvent_"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(vm: CalendarViewModel = viewModel()) {
  val selectedDate by vm.selectedDate.collectAsState()
  val currentMonth by vm.currentDisplayMonth.collectAsState()
  val isMonthView by vm.isMonthView.collectAsState()
  val allTasks by vm.allTasks.collectAsState()

  Box(
      modifier =
          Modifier.fillMaxSize()
              .background(Brush.verticalGradient(listOf(BackgroundDark, BackgroundGradientEnd)))
              .padding(horizontal = 20.dp, vertical = 24.dp)) {
        Column(
            modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {

              /* --- Floating Menu Button --- */
              Row(
                  modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                  horizontalArrangement = Arrangement.Start) {
                    FloatingActionButton(
                        onClick = { /* TODO: open side menu */},
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.testTag(CalendarScreenTestTags.MENU_BUTTON)) {
                          Icon(
                              imageVector = Icons.Default.KeyboardArrowLeft,
                              contentDescription = "Menu",
                              tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                  }

              /* --- Calendar Title + Toggle Section --- */
              Row(
                  modifier =
                      Modifier.fillMaxWidth()
                          .padding(bottom = 12.dp)
                          .testTag(CalendarScreenTestTags.CALENDAR_HEADER),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.Start) {
                      Text(
                          text = stringResource(R.string.calendar_title),
                          fontSize = 30.sp,
                          fontWeight = FontWeight.Bold,
                          color = MaterialTheme.colorScheme.primary)
                      Text(
                          text = stringResource(R.string.calendar_subtitle),
                          style = MaterialTheme.typography.bodyMedium,
                          color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    FilledTonalButton(
                        onClick = { vm.toggleMonthWeekView() },
                        shape = RoundedCornerShape(10.dp),
                        colors =
                            ButtonDefaults.filledTonalButtonColors(
                                containerColor = PurplePrimary, contentColor = Color.White),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                        modifier = Modifier.testTag(CalendarScreenTestTags.VIEW_TOGGLE_BUTTON)) {
                          Text(if (isMonthView) "Month" else "Week")
                        }
                  }
              /* --- Calendar Card --- */
              Card(
                  modifier =
                      Modifier.fillMaxWidth()
                          .shadow(8.dp, RoundedCornerShape(24.dp))
                          .border(
                              width = 1.dp,
                              color = PurplePrimary,
                              shape = RoundedCornerShape(24.dp))
                          .testTag(CalendarScreenTestTags.CALENDAR_CARD),
                  colors = CardDefaults.cardColors(containerColor = DarkBlue.copy(alpha = 0.85f)),
                  shape = RoundedCornerShape(24.dp)) {
                    if (isMonthView) {
                      MonthGrid(
                          currentMonth = currentMonth,
                          selectedDate = selectedDate,
                          allTasks = allTasks,
                          onDateClick = { vm.onDateSelected(it) },
                          onPrevClick = { vm.onPreviousMonthWeekClicked() },
                          onNextClick = { vm.onNextMonthWeekClicked() })
                    } else {
                      WeekRow(
                          startOfWeek = vm.startOfWeek(selectedDate),
                          selectedDate = selectedDate,
                          allTasks = allTasks,
                          onDayClick = { vm.onDateSelected(it) })
                    }
                  }

              val displayedTasks =
                  if (isMonthView) {
                    allTasks.filter { it.date >= selectedDate }
                  } else {
                    val startOfWeek = vm.startOfWeek(selectedDate)
                    val endOfWeek = startOfWeek.plusDays(6)
                    allTasks.filter { it.date in startOfWeek..endOfWeek }
                  }
              val sectionTitle =
                  if (isMonthView) stringResource(R.string.upcoming_events)
                  else stringResource(R.string.upcoming_events_week)

              UpcomingEventsSection(
                  tasks = displayedTasks.sortedBy { it.date },
                  selectedDate = selectedDate,
                  onAddTaskClick = { vm.onAddTaskClicked(it) },
                  onTaskClick = { vm.onEditTaskClicked(it) },
                  title = sectionTitle)
            }
      }
}
