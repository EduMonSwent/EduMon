package com.android.sample.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import com.android.sample.feature.schedule.data.calendar.Priority
import com.android.sample.feature.schedule.data.calendar.StudyItem
import com.android.sample.ui.calendar.CalendarHeader
import com.android.sample.ui.calendar.CalendarScreenTestTags
import com.android.sample.ui.calendar.MonthGrid
import com.android.sample.ui.theme.DarkBlue
import java.time.LocalDate
import java.time.YearMonth

/** This class was implemented with the help of ai (chatgbt) */
const val MONTH_CONTENT_TEST_TAG = "MonthContent"

@Composable
fun MonthTabContent(
    allTasks: List<StudyItem>,
    selectedDate: LocalDate,
    currentMonth: YearMonth,
    onPreviousMonthClick: () -> Unit,
    onNextMonthClick: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
) {
  val monthName = currentMonth.month.name.lowercase().replaceFirstChar { it.uppercase() }
  val headerTitle = stringResource(id = R.string.calendar_month_year, monthName, currentMonth.year)

  LazyColumn(
      modifier = Modifier.fillMaxSize().testTag(MONTH_CONTENT_TEST_TAG),
      verticalArrangement = Arrangement.spacedBy(16.dp),
      contentPadding = PaddingValues(bottom = 96.dp)) {
        item("month-big-frame") {

          // ONE AND ONLY big frame — like WeekTabContent
          SectionBox(title = null, header = null) {

            // ───────────── Header ─────────────
            CalendarHeader(
                title = headerTitle,
                onPrevClick = onPreviousMonthClick,
                onNextClick = onNextMonthClick)

            Spacer(Modifier.height(8.dp))

            // ───────────── Card with MonthGrid ─────────────
            Card(
                modifier =
                    Modifier.fillMaxWidth()
                        .shadow(8.dp)
                        .testTag(CalendarScreenTestTags.CALENDAR_CARD),
                colors = CardDefaults.cardColors(containerColor = DarkBlue.copy(alpha = 0.85f)),
                shape = RoundedCornerShape(24.dp)) {
                  Box(
                      modifier =
                          Modifier.fillMaxWidth()
                              .heightIn(min = 260.dp, max = 420.dp)
                              .padding(horizontal = 8.dp, vertical = 6.dp)) {
                        MonthGrid(
                            currentMonth = currentMonth,
                            selectedDate = selectedDate,
                            allTasks = allTasks,
                            onDateClick = { onDateSelected })
                      }
                }

            Spacer(Modifier.height(16.dp))

            // ───────────── Most important this month (HIGH priority only) ─────────────
            val monthStart = currentMonth.atDay(1)
            val monthEnd = monthStart.plusMonths(1).minusDays(1)

            val highPriorityMonthTasks =
                allTasks
                    .filter { it.date in monthStart..monthEnd && it.priority == Priority.HIGH }
                    .distinctBy { it.id }
                    .sortedWith(
                        compareBy<StudyItem>({ it.date }, { it.time ?: java.time.LocalTime.MIN }))

            SectionBox(
                header = {
                  Text(
                      text = stringResource(R.string.schedule_month_important_title),
                      style =
                          MaterialTheme.typography.titleMedium.copy(
                              fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White),
                      modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp))
                }) {
                  val cs = MaterialTheme.colorScheme

                  if (highPriorityMonthTasks.isEmpty()) {
                    Text(
                        text = stringResource(R.string.schedule_month_no_important),
                        color = cs.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth())
                  } else {
                    Column(modifier = Modifier.fillMaxWidth()) {
                      highPriorityMonthTasks.forEach { task ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {

                          // small colored bar on the left
                          Box(
                              modifier =
                                  Modifier.width(5.dp)
                                      .height(32.dp)
                                      .background(cs.primary, RoundedCornerShape(999.dp)))

                          Spacer(Modifier.width(10.dp))

                          Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = task.title,
                                style =
                                    MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold, color = cs.onSurface))
                            Text(
                                text = task.date.toString(), // format later if you want
                                style =
                                    MaterialTheme.typography.labelSmall.copy(
                                        color = cs.onSurfaceVariant))
                          }
                        }
                      }
                    }
                  }
                }
          }
        }
      }
}
