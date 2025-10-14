package com.android.sample.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.sample.R
import com.android.sample.model.calendar.StudyItem
import com.android.sample.model.calendar.TaskType
import com.android.sample.ui.theme.BackgroundDark
import com.android.sample.ui.theme.BackgroundGradientEnd
import com.android.sample.ui.theme.Blue
import com.android.sample.ui.theme.DarkBlue
import com.android.sample.ui.theme.DarkerBlue
import com.android.sample.ui.theme.LightBlue
import com.android.sample.ui.theme.LightViolet
import com.android.sample.ui.theme.Pink
import com.android.sample.ui.theme.PurpleBorder
import com.android.sample.ui.theme.PurpleCalendar
import com.android.sample.ui.theme.PurplePrimary
import com.android.sample.ui.theme.VioletLilas
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

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
                          onDayClick = { vm.onDateSelected(it) },
                          onPrevClick = { vm.onPreviousMonthWeekClicked() },
                          onNextClick = { vm.onNextMonthWeekClicked() })
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

@Composable
fun MonthGrid(
    currentMonth: YearMonth,
    selectedDate: LocalDate,
    allTasks: List<StudyItem>,
    onDateClick: (LocalDate) -> Unit,
    onPrevClick: () -> Unit,
    onNextClick: () -> Unit
) {
  Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
    /* --- Month Header --- */
    CalendarHeader(
        title =
            "${currentMonth.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${currentMonth.year}",
        onPrevClick = onPrevClick,
        onNextClick = onNextClick)

    /* --- Weekday Header --- */
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween) {
          val daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
          daysOfWeek.forEach { day ->
            Text(
                text = day,
                style =
                    MaterialTheme.typography.labelMedium.copy(
                        color = PurpleCalendar, fontWeight = FontWeight.Medium),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center)
          }
        }

    /* --- Calendar Days Grid --- */
    val firstOfMonth = currentMonth.atDay(1)
    val firstDayOfWeek = DayOfWeek.MONDAY
    val offset = (firstOfMonth.dayOfWeek.value - firstDayOfWeek.value + 7) % 7
    val daysInMonth = currentMonth.lengthOfMonth()

    val displayDays =
        buildList<LocalDate?> {
          repeat(offset) { add(null) }
          (1..daysInMonth).forEach { add(currentMonth.atDay(it)) }
        }

    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()) {
          items(displayDays.size) { index ->
            val day = displayDays[index]
            if (day != null) {
              val tasksForDay = allTasks.filter { it.date == day }
              DayCell(
                  date = day,
                  tasks = tasksForDay,
                  isSelected = day == selectedDate,
                  onDateClick = onDateClick)
            } else {
              Spacer(modifier = Modifier.aspectRatio(1f))
            }
          }
        }
  }
}

@Composable
fun WeekRow(
    startOfWeek: LocalDate,
    selectedDate: LocalDate,
    allTasks: List<StudyItem>,
    onDayClick: (LocalDate) -> Unit,
    onPrevClick: () -> Unit,
    onNextClick: () -> Unit
) {
  val weekDays = remember(startOfWeek) { (0..6).map { startOfWeek.plusDays(it.toLong()) } }

  val weekTitle =
      "${weekDays.first().month.name.lowercase().replaceFirstChar { it.uppercase() }} " +
          "${weekDays.first().dayOfMonth} - ${weekDays.last().dayOfMonth}"

  CalendarHeader(title = weekTitle, onPrevClick = onPrevClick, onNextClick = onNextClick)

  LazyRow(
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      contentPadding = PaddingValues(horizontal = 8.dp),
      modifier =
          Modifier.fillMaxWidth()
              .padding(vertical = 8.dp)
              .testTag(CalendarScreenTestTags.WEEK_ROW)) {
        items(weekDays) { day ->
          val tasksForDay =
              allTasks
                  .filter { it.date == day }
                  .sortedWith(compareBy<StudyItem> { it.time }.thenBy { it.title })

          val isSelected = day == selectedDate

          Box(
              modifier =
                  Modifier.width(140.dp)
                      .height(160.dp)
                      .clip(RoundedCornerShape(18.dp))
                      .background(brush = Brush.verticalGradient(listOf(Blue, DarkerBlue)))
                      .border(
                          width = if (isSelected) 2.dp else 1.dp,
                          color =
                              if (isSelected) PurpleBorder else PurplePrimary.copy(alpha = 0.4f),
                          shape = RoundedCornerShape(18.dp))
                      .clickable { onDayClick(day) }
                      .padding(10.dp)
                      .testTag("${CalendarScreenTestTags.WEEK_DAY_BOX_PREFIX}${day.dayOfMonth}")) {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Top) {
                  // Day number
                  Text(
                      text = day.dayOfMonth.toString(),
                      style =
                          MaterialTheme.typography.bodyLarge.copy(
                              color = Color.White,
                              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium))

                  Spacer(Modifier.height(6.dp))

                  // Show up to 2 events
                  tasksForDay.take(2).forEach { task ->
                    val tagColor =
                        when (task.type) {
                          TaskType.STUDY -> LightBlue
                          TaskType.WORK -> Pink
                          TaskType.PERSONAL -> VioletLilas
                        }

                    Surface(
                        color = tagColor.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(8.dp),
                        modifier =
                            Modifier.padding(vertical = 3.dp)
                                .testTag("${CalendarScreenTestTags.WEEK_EVENT_PREFIX}${task.id}")) {
                          Text(
                              text = task.title.take(22) + if (task.title.length > 22) "…" else "",
                              color = tagColor,
                              style =
                                  MaterialTheme.typography.labelSmall.copy(
                                      fontWeight = FontWeight.Medium),
                              modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                        }
                  }

                  // “+N more” indicator
                  if (tasksForDay.size > 2) {
                    Text(
                        text = "+${tasksForDay.size - 2} more",
                        color = PurpleCalendar,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = 4.dp))
                  }
                }
              }
        }
      }
}

@Composable
private fun CalendarHeader(
    title: String,
    onPrevClick: () -> Unit,
    onNextClick: () -> Unit,
    modifier: Modifier = Modifier
) {
  Row(
      modifier = modifier.fillMaxWidth().padding(bottom = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            text = title,
            style =
                MaterialTheme.typography.titleLarge.copy(
                    color = LightViolet, fontWeight = FontWeight.Bold))

        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(onClick = onPrevClick) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = stringResource(R.string.previous),
                tint = LightViolet)
          }
          IconButton(onClick = onNextClick) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(R.string.next),
                tint = LightViolet)
          }
        }
      }
}
