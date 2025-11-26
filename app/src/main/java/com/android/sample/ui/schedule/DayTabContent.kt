package com.android.sample.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.sample.R
import com.android.sample.feature.schedule.data.planner.AttendanceStatus
import com.android.sample.feature.schedule.data.planner.Class
import com.android.sample.feature.schedule.data.planner.ClassAttendance
import com.android.sample.feature.schedule.data.planner.ClassType
import com.android.sample.feature.schedule.data.planner.CompletionStatus
import com.android.sample.feature.schedule.data.planner.WellnessEventType
import com.android.sample.feature.schedule.viewmodel.ScheduleUiState
import com.android.sample.feature.schedule.viewmodel.ScheduleViewModel
import com.android.sample.feature.weeks.ui.DailyObjectivesSection
import com.android.sample.feature.weeks.ui.GlassSurface
import com.android.sample.feature.weeks.viewmodel.ObjectiveNavigation
import com.android.sample.feature.weeks.viewmodel.ObjectivesViewModel
import com.android.sample.ui.planner.ClassAttendanceModal
import com.android.sample.ui.planner.WellnessEventItem
import com.android.sample.ui.theme.AccentViolet
import com.android.sample.ui.theme.CustomGreen
import com.android.sample.ui.theme.LightBlueAccent
import com.android.sample.ui.theme.Pink
import com.android.sample.ui.theme.PurplePrimary
import com.android.sample.ui.theme.StatBarLightbulb
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** This class was implemented with the help of ai (chatgbt) */
@Composable
fun DayTabContent(
    vm: ScheduleViewModel,
    state: ScheduleUiState,
    objectivesVm: ObjectivesViewModel,
    onObjectiveNavigation: (ObjectiveNavigation) -> Unit = {},
) {
  // Attendance modal (wired to VM state)
  if (state.showAttendanceModal && state.selectedClass != null) {
    val initial = state.attendanceRecords.firstOrNull { it.classId == state.selectedClass.id }
    ClassAttendanceModal(
        classItem = state.selectedClass,
        initialAttendance = initial?.attendance,
        initialCompletion = initial?.completion,
        onDismiss = { vm.onDismissClassAttendanceModal() },
        onSave = { attendance, completion ->
          vm.saveClassAttendance(state.selectedClass, attendance, completion)
        })
  }

  LazyColumn(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.spacedBy(16.dp),
      contentPadding = PaddingValues(bottom = 96.dp) // leave room for FAB
      ) {
        item {
          TodayCard(
              classes = state.todayClasses,
              attendance = state.attendanceRecords,
              objectivesVm = objectivesVm,
              onClassClick = { vm.onClassClicked(it) },
              onObjectiveNavigate = onObjectiveNavigation,
              modifier = Modifier.fillMaxWidth())
        }
      }
}

/* ---------- UI building blocks ---------- */

@Composable
private fun TodayCard(
    classes: List<Class>,
    attendance: List<ClassAttendance>,
    objectivesVm: ObjectivesViewModel,
    onClassClick: (Class) -> Unit,
    onObjectiveNavigate: (ObjectiveNavigation) -> Unit,
    modifier: Modifier = Modifier
) {
  val cs = MaterialTheme.colorScheme
  val today = LocalDate.now()
  val dateText = DateTimeFormatter.ofPattern("EEEE, MMM d").format(today)

  GlassSurface(modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
    // Header
    Text(
        text = stringResource(R.string.today_title_fmt, dateText),
        style =
            MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold, fontSize = 20.sp, color = cs.onSurface),
        modifier = Modifier.padding(bottom = 6.dp))
    Spacer(Modifier.height(12.dp))

    // ---- Classes section (card inside card) ----
    SectionBox(
        header = {
          Text(
              stringResource(R.string.classes_label),
              style =
                  MaterialTheme.typography.titleMedium.copy(
                      fontSize = 18.sp,
                      fontWeight = FontWeight.SemiBold,
                      color = MaterialTheme.colorScheme.onSurface),
              modifier = Modifier.padding(bottom = 8.dp))
        }) {
          if (classes.isEmpty()) {
            Text(
                stringResource(R.string.no_classes_today),
                color = cs.onSurface.copy(alpha = 0.7f),
                modifier = modifier.fillMaxWidth())
          } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
              classes.forEachIndexed { idx, c ->
                val record = attendance.firstOrNull { it.classId == c.id }
                CompactClassRow(
                    clazz = c,
                    record = record,
                    modifier = Modifier.fillMaxWidth().clickable { onClassClick(c) })
                if (idx != classes.lastIndex) {
                  HorizontalDivider(color = cs.onSurface.copy(alpha = 0.08f))
                }
              }
            }
          }
        }

    Spacer(Modifier.height(14.dp))

    // ---- Daily objectives (your existing composable already looks like the mock) ----
    MaterialTheme(
        colorScheme =
            MaterialTheme.colorScheme.copy(
                // ensure it uses your app purple for its internal Icons/Text tints
                primary = PurplePrimary),
        typography =
            MaterialTheme.typography.copy(
                // DailyObjectivesSection uses titleMedium; bump it here locally
                titleMedium =
                    MaterialTheme.typography.titleMedium.copy(
                        fontSize = 18.sp, fontWeight = FontWeight.SemiBold))) {
          DailyObjectivesSection(
              viewModel = objectivesVm,
              modifier = Modifier.fillMaxWidth(),
              onNavigate = onObjectiveNavigate)
        }

    Spacer(Modifier.height(14.dp))

    // ---- Wellness (match Classes/DailyObjectives look) ----
    SectionBox(
        header = {
          Text(
              stringResource(R.string.wellness_events_label),
              style =
                  MaterialTheme.typography.titleMedium.copy(
                      fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = cs.onSurface),
              modifier = Modifier.padding(bottom = 8.dp))
        }) {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            WellnessEventItem(
                title = stringResource(R.string.wellness_event_yoga_title),
                time = stringResource(R.string.wellness_event_yoga_time),
                description = stringResource(R.string.wellness_event_yoga_description),
                eventType = WellnessEventType.YOGA,
                onClick = {})
            HorizontalDivider(color = cs.onSurface.copy(alpha = 0.08f))
            WellnessEventItem(
                title = stringResource(R.string.wellness_event_lecture_title),
                time = stringResource(R.string.wellness_event_lecture_time),
                description = stringResource(R.string.wellness_event_lecture_description),
                eventType = WellnessEventType.LECTURE,
                onClick = {})
          }
        }
  }
}

@Composable
private fun CompactClassRow(clazz: Class, record: ClassAttendance?, modifier: Modifier = Modifier) {
  val cs = MaterialTheme.colorScheme
  val (iconRes, badgeColor) =
      when (clazz.type) {
        ClassType.LECTURE -> R.drawable.ic_lecture to LightBlueAccent
        ClassType.EXERCISE -> R.drawable.ic_exercise to CustomGreen
        ClassType.LAB -> R.drawable.ic_lab to AccentViolet
      }
  val typeLabel =
      when (clazz.type) {
        ClassType.LECTURE -> stringResource(R.string.lecture_type)
        ClassType.EXERCISE -> stringResource(R.string.exercise_type)
        ClassType.LAB -> stringResource(R.string.lab_type)
      }
  val timeFmt = DateTimeFormatter.ofPattern("HH:mm")

  Row(modifier, verticalAlignment = Alignment.CenterVertically) {
    Box(
        modifier =
            Modifier.size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(badgeColor.copy(alpha = .16f)) // subtle filled badge
                .border(1.dp, badgeColor.copy(alpha = .32f), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center) {
          Icon(painterResource(iconRes), null, tint = badgeColor, modifier = Modifier.size(20.dp))
        }

    Spacer(Modifier.width(10.dp))

    Column(Modifier.weight(1f)) {
      Text(
          "${clazz.courseName} ($typeLabel)",
          color = cs.onSurface,
          fontWeight = FontWeight.SemiBold,
          fontSize = 16.sp)
      Text("(${typeLabel})", color = cs.onSurface.copy(alpha = 0.65f), fontSize = 12.sp)
      Spacer(Modifier.height(2.dp))
      Text(
          "${clazz.startTime.format(timeFmt)} - ${clazz.endTime.format(timeFmt)}",
          color = cs.onSurface.copy(alpha = 0.75f),
          fontSize = 12.sp)
      Text(
          "${clazz.location} • ${clazz.instructor}",
          color = cs.onSurface.copy(alpha = 0.65f),
          fontSize = 12.sp)
    }

    Spacer(Modifier.width(10.dp))

    if (record != null) {
      Column(
          horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Attendance
            val attColor =
                when (record.attendance) {
                  AttendanceStatus.YES -> CustomGreen
                  AttendanceStatus.ARRIVED_LATE -> StatBarLightbulb
                  AttendanceStatus.NO -> Pink
                }
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                  painterResource(R.drawable.ic_check_circle),
                  null,
                  tint = attColor,
                  modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(6.dp))
              Text(
                  text =
                      when (record.attendance) {
                        AttendanceStatus.YES -> stringResource(R.string.attendance_attended)
                        AttendanceStatus.ARRIVED_LATE ->
                            stringResource(R.string.attendance_arrived_late)
                        AttendanceStatus.NO -> stringResource(R.string.attendance_missed)
                      },
                  color = attColor,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Medium)
            }
            // Completion
            val compColor =
                when (record.completion) {
                  CompletionStatus.YES -> PurplePrimary
                  CompletionStatus.PARTIALLY -> StatBarLightbulb
                  CompletionStatus.NO -> Pink
                }
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                  painterResource(R.drawable.ic_done_all),
                  null,
                  tint = compColor,
                  modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(6.dp))
              Text(
                  text =
                      when (record.completion) {
                        CompletionStatus.YES -> stringResource(R.string.completion_done)
                        CompletionStatus.PARTIALLY -> stringResource(R.string.completion_partially)
                        CompletionStatus.NO -> stringResource(R.string.completion_not_done)
                      },
                  color = compColor,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Medium)
            }
          }
    }
  }
}
