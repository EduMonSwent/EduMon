package com.android.sample.ui.schedule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.sample.R
import com.android.sample.feature.schedule.data.calendar.StudyItem
import com.android.sample.feature.schedule.viewmodel.ScheduleViewModel
import com.android.sample.feature.weeks.ui.WeekDotsRow
import com.android.sample.feature.weeks.ui.WeekProgDailyObjTags
import com.android.sample.feature.weeks.viewmodel.ObjectivesViewModel
import com.android.sample.feature.weeks.viewmodel.WeeksViewModel
import com.android.sample.ui.calendar.CalendarHeader
import com.android.sample.ui.calendar.CalendarScreenTestTags
import com.android.sample.ui.calendar.UpcomingEventsSection
import com.android.sample.ui.calendar.WeekRow
import com.android.sample.ui.theme.DarkBlue
import java.time.LocalDate

/**
 * Weekly tab content adapted to the existing Day/GlassSurface styling. Place this in
 * ui/schedule/WeekTabContent.kt
 */
@Composable
fun WeekTabContent(
    vm: ScheduleViewModel,
    weeksVm: WeeksViewModel,
    objectivesVm: ObjectivesViewModel,
    allTasks: List<StudyItem>,
    selectedDate: LocalDate
) {
  val weekStart = vm.startOfWeek(selectedDate)
  LazyColumn(
      modifier = Modifier.fillMaxSize().testTag("WeekContent"),
      verticalArrangement = Arrangement.spacedBy(16.dp),
      contentPadding = PaddingValues(bottom = 96.dp)) {
        item("week-big-frame") {
          SectionBox(title = null, header = null) {
            val weekDays = (0..6).map { weekStart.plusDays(it.toLong()) }
            val weekTitle =
                "${weekDays.first().month.name.lowercase().replaceFirstChar { it.uppercase() }} " +
                    "${weekDays.first().dayOfMonth} - ${weekDays.last().dayOfMonth}"

            // Header INSIDE the big SectionBox, but OUTSIDE the tiles frame
            CalendarHeader(
                title = weekTitle,
                onPrevClick = { vm.onPreviousMonthWeekClicked() },
                onNextClick = { vm.onNextMonthWeekClicked() })

            Spacer(Modifier.height(8.dp))

            // ---- Week header + day tiles (WeekRow renders its own title/arrows) ----
            Card(
                modifier =
                    Modifier.fillMaxWidth()
                        .shadow(8.dp, RoundedCornerShape(24.dp))
                        .testTag(CalendarScreenTestTags.CALENDAR_CARD),
                colors = CardDefaults.cardColors(containerColor = DarkBlue.copy(alpha = 0.85f)),
                shape = RoundedCornerShape(24.dp)) {
                  WeekRow(
                      startOfWeek = weekStart,
                      selectedDate = selectedDate,
                      allTasks = allTasks,
                      onDayClick = { vm.onDateSelected(it) })
                }

            Spacer(Modifier.height(16.dp))

            // ---- Frame: "Week progression" ----
            SectionBox(
                header = {
                  Text(
                      text = stringResource(R.string.week_progression),
                      style =
                          MaterialTheme.typography.titleMedium.copy(
                              fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White),
                      modifier = Modifier.padding(bottom = 14.dp))
                }) {
                  WeekDotsRow(
                      objectivesVm,
                      modifier =
                          Modifier.fillMaxWidth()
                              .padding(top = 6.dp)
                              .testTag(WeekProgDailyObjTags.WEEK_DOTS_ROW))
                }

            Spacer(Modifier.height(16.dp))

            // ---- Frame: Upcoming events (embedded; no inner GlassSurface) ----
            SectionBox(title = null) {
              val end = weekStart.plusDays(6)
              val from = if (selectedDate.isBefore(weekStart)) weekStart else selectedDate
              val weekTasks = allTasks.filter { it.date in from..end }

              UpcomingEventsSection(
                  tasks =
                      weekTasks.sortedWith(
                          compareBy({ it.date }, { it.time ?: java.time.LocalTime.MIN })),
                  selectedDate = selectedDate,
                  onAddTaskClick = { /* handled by FAB */ _ -> },
                  onTaskClick = {},
                  title = stringResource(R.string.upcoming_events),
              )
            }
          }
        }
      }
}
