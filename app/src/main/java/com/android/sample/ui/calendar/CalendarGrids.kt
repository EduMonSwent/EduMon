package com.android.sample.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.android.sample.R
import com.android.sample.feature.schedule.data.calendar.StudyItem
import com.android.sample.feature.schedule.data.calendar.TaskType
import com.android.sample.ui.theme.Blue
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

@Composable
fun MonthGrid(
    currentMonth: YearMonth,
    selectedDate: LocalDate,
    allTasks: List<StudyItem>,
    onDateClick: (LocalDate) -> Unit
) {
  val tasksByDate = remember(allTasks) { allTasks.groupBy { it.date } }

  Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween) {
          val daysOfWeek =
              listOf(
                  R.string.weekday_mon_short,
                  R.string.weekday_tue_short,
                  R.string.weekday_wed_short,
                  R.string.weekday_thu_short,
                  R.string.weekday_fri_short,
                  R.string.weekday_sat_short,
                  R.string.weekday_sun_short)

          daysOfWeek.forEach { dayResId ->
            Text(
                text = stringResource(dayResId),
                style =
                    MaterialTheme.typography.labelMedium.copy(
                        color = PurpleCalendar, fontWeight = FontWeight.Medium),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center)
          }
        }

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
              // O(1) lookup instead of filter
              val tasksForDay = tasksByDate[day].orEmpty()
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
    onDayClick: (LocalDate) -> Unit
) {
  val weekDays = remember(startOfWeek) { (0..6).map { startOfWeek.plusDays(it.toLong()) } }

  // Group + sort once, recompute only when allTasks changes
  val tasksByDate =
      remember(allTasks) {
        allTasks
            .groupBy { it.date }
            .mapValues { (_, list) ->
              list.sortedWith(compareBy<StudyItem> { it.time }.thenBy { it.title })
            }
      }

  LazyRow(
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      contentPadding = PaddingValues(horizontal = 8.dp),
      modifier =
          Modifier.fillMaxWidth()
              .padding(vertical = 8.dp)
              .testTag(CalendarScreenTestTags.WEEK_ROW)) {
        items(weekDays) { day ->
          // O(1) lookup; already sorted
          val tasksForDay = tasksByDate[day].orEmpty()
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
                  Text(
                      text = day.dayOfMonth.toString(),
                      style =
                          MaterialTheme.typography.bodyLarge.copy(
                              color = MaterialTheme.colorScheme.onPrimary,
                              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium))

                  Spacer(Modifier.height(6.dp))

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
                          val maxLength = 22
                          val truncatedTitle =
                              if (task.title.length > maxLength) {
                                task.title.take(maxLength) + "…"
                              } else {
                                task.title
                              }

                          Text(
                              text = truncatedTitle,
                              color = tagColor,
                              style =
                                  MaterialTheme.typography.labelSmall.copy(
                                      fontWeight = FontWeight.Medium),
                              modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                        }
                  }

                  if (tasksForDay.size > 2) {
                    val remainingCount = tasksForDay.size - 2
                    Text(
                        text = stringResource(id = R.string.calendar_more_events, remainingCount),
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
fun CalendarHeader(
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
