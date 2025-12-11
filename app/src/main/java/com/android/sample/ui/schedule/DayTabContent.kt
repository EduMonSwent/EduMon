package com.android.sample.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.sample.R
import com.android.sample.data.ToDo
import com.android.sample.feature.schedule.data.planner.AttendanceStatus
import com.android.sample.feature.schedule.data.planner.Class
import com.android.sample.feature.schedule.data.planner.ClassAttendance
import com.android.sample.feature.schedule.data.planner.ClassType
import com.android.sample.feature.schedule.data.planner.CompletionStatus
import com.android.sample.feature.schedule.data.planner.DayScheduleItem
import com.android.sample.feature.schedule.data.planner.ScheduleClassItem
import com.android.sample.feature.schedule.data.planner.ScheduleEventItem
import com.android.sample.feature.schedule.data.planner.ScheduleGapItem
import com.android.sample.feature.schedule.data.planner.WellnessEventType
import com.android.sample.feature.schedule.data.schedule.ScheduleEvent
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
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** This class was implemented with the help of ai (chatgbt) */
data class ScheduleActions(
    val onClassClick: (Class) -> Unit,
    val onGapClick: (ScheduleGapItem) -> Unit,
    val onEventClick: (ScheduleEvent) -> Unit,
    val onEventDelete: (ScheduleEvent) -> Unit
)

@Composable
fun DayTabContent(
    vm: ScheduleViewModel,
    state: ScheduleUiState,
    objectivesVm: ObjectivesViewModel,
    onObjectiveNavigation: (ObjectiveNavigation) -> Unit = {},
    onTodoClicked: (String) -> Unit = {}
) {
  // 1. Attendance modal (wired to VM state)
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
  // 2. Gap Step 1: Study or Relax?
  if (state.showGapOptionsModal && state.selectedGap != null) {
    AlertDialog(
        onDismissRequest = { vm.onDismissGapModal() },
        title = {
          Text(stringResource(R.string.smart_gap_modal_title, state.selectedGap.durationMinutes))
        },
        text = { Text(stringResource(R.string.smart_gap_modal_text)) },
        confirmButton = {
          Button(onClick = { vm.onGapTypeSelected(true) }) {
            Text(stringResource(R.string.smart_gap_action_study))
          }
        },
        dismissButton = {
          TextButton(onClick = { vm.onGapTypeSelected(false) }) {
            Text(stringResource(R.string.smart_gap_action_relax))
          }
        })
  }
  // 3. Gap Step 2: Specific Propositions (The "Dialect")
  if (state.showGapPropositionsModal && state.selectedGap != null) {
    AlertDialog(
        onDismissRequest = { vm.onDismissGapModal() },
        title = { Text(stringResource(R.string.smart_gap_suggestions_title)) },
        text = {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            state.gapPropositions.forEach { prop ->
              OutlinedButton(
                  onClick = { vm.onGapPropositionClicked(prop) },
                  modifier = Modifier.fillMaxWidth()) {
                    Text(prop)
                  }
            }
          }
        },
        confirmButton = {}, // Clicking an option is the confirm action
        dismissButton = {
          TextButton(onClick = { vm.onDismissGapModal() }) { Text(stringResource(R.string.cancel)) }
        })
  }

  Column(
      modifier = Modifier.fillMaxSize().padding(bottom = Dimensions.bottomBarPadding),
      verticalArrangement = Arrangement.spacedBy(Dimensions.spacingLarge)) {
        val today = LocalDate.now()
        val dayTodos = state.todos.filter { it.dueDate == today }
        TodayCard(
            classes = state.todaySchedule,
            attendance = state.attendanceRecords,
            objectivesVm = objectivesVm,
            onClassClick = { vm.onClassClicked(it) },
            onObjectiveNavigate = onObjectiveNavigation,
            todos = dayTodos,
            onTodoClicked = onTodoClicked,
            allClassesFinished = state.allClassesFinished,
            onGapClick = { vm.onGapClicked(it) },
            onEventClick = { vm.onScheduleEventClicked(it) },
            onEventDelete = { vm.onDeleteScheduleEvent(it) },
            modifier = Modifier.fillMaxWidth())
      }
}

/* ---------- UI building blocks ---------- */

@Composable
private fun TodayCard(
    classes: List<DayScheduleItem>,
    attendance: List<ClassAttendance>,
    objectivesVm: ObjectivesViewModel,
    onClassClick: (Class) -> Unit,
    onGapClick: (ScheduleGapItem) -> Unit,
    onEventClick: (ScheduleEvent) -> Unit,
    onEventDelete: (ScheduleEvent) -> Unit,
    onObjectiveNavigate: (ObjectiveNavigation) -> Unit,
    todos: List<ToDo>,
    onTodoClicked: (String) -> Unit,
    modifier: Modifier = Modifier,
    allClassesFinished: Boolean
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
        modifier = Modifier.padding(bottom = Dimensions.spacingXSmall))
    Spacer(Modifier.height(Dimensions.spacingMedium))

    ClassesSection(
        classes = classes,
        attendance = attendance,
        actions =
            ScheduleActions(
                onClassClick = onClassClick,
                onGapClick = onGapClick,
                onEventClick = onEventClick,
                onEventDelete = onEventDelete),
        allClassesFinished = allClassesFinished,
        modifier = modifier)

    Spacer(Modifier.height(14.dp))

    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(primary = PurplePrimary),
        typography =
            MaterialTheme.typography.copy(
                titleMedium =
                    MaterialTheme.typography.titleMedium.copy(
                        fontSize = 18.sp, fontWeight = FontWeight.SemiBold))) {
          DailyObjectivesSection(
              viewModel = objectivesVm,
              modifier = Modifier.fillMaxWidth(),
              onNavigate = onObjectiveNavigate)
        }

    Spacer(Modifier.height(14.dp))

    TodosSection(todos = todos, onTodoClicked = onTodoClicked, modifier = modifier)

    Spacer(Modifier.height(14.dp))

    WellnessSection()
  }
}

@Composable
private fun ClassesSection(
    classes: List<DayScheduleItem>,
    attendance: List<ClassAttendance>,
    actions: ScheduleActions,
    allClassesFinished: Boolean,
    modifier: Modifier
) {
  val cs = MaterialTheme.colorScheme

  SectionBox(
      header = {
        Text(
            stringResource(R.string.classes_label),
            style =
                MaterialTheme.typography.titleMedium.copy(
                    fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = cs.onSurface),
            modifier = Modifier.padding(bottom = Dimensions.spacingSmall))
      }) {
        if (classes.isEmpty() && !allClassesFinished) {
          Text(
              stringResource(R.string.no_classes_today),
              color = cs.onSurface.copy(alpha = 0.7f),
              modifier = modifier.fillMaxWidth())
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          classes.forEachIndexed { idx, item ->
            when (item) {
              is ScheduleClassItem -> {
                val c = item.classData
                val record = attendance.firstOrNull { it.classId == c.id }
                CompactClassRow(
                    clazz = c,
                    record = record,
                    modifier = Modifier.fillMaxWidth().clickable { actions.onClassClick(c) })
              }
              is ScheduleGapItem -> {
                GapItemRow(gap = item, onClick = { actions.onGapClick(item) })
              }
              is ScheduleEventItem -> {
                EventItemRow(
                    event = item.eventData,
                    onClick = { actions.onEventClick(item.eventData) },
                    onDelete = { actions.onEventDelete(item.eventData) })
              }
            }

            if (idx != classes.lastIndex) {
              HorizontalDivider(color = cs.onSurface.copy(alpha = 0.08f))
            }
          }
        }
      }
}

@Composable
private fun TodosSection(todos: List<ToDo>, onTodoClicked: (String) -> Unit, modifier: Modifier) {
  val cs = MaterialTheme.colorScheme

  SectionBox(
      header = {
        Text(
            text = stringResource(R.string.schedule_day_todos_title),
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = cs.onSurface,
            modifier = Modifier.padding(bottom = Dimensions.spacingSmall))
      }) {
        if (todos.isEmpty()) {
          Text(
              text = stringResource(R.string.schedule_day_todos_empty),
              color = cs.onSurface.copy(alpha = 0.7f),
              modifier = modifier.fillMaxWidth())
        } else {
          Column(verticalArrangement = Arrangement.spacedBy(Dimensions.spacingXSmall)) {
            todos.forEach { todo ->
              Row(
                  modifier =
                      Modifier.fillMaxWidth()
                          .padding(vertical = Dimensions.spacingXSmall)
                          .clickable { onTodoClicked(todo.id) }) {
                    Box(
                        modifier =
                            Modifier.width(5.dp)
                                .height(32.dp)
                                .background(cs.primary, RoundedCornerShape(999.dp)))

                    Spacer(Modifier.width(10.dp))

                    Column {
                      Text(
                          text = todo.title,
                          fontWeight = FontWeight.SemiBold,
                          fontSize = 14.sp,
                          color = cs.onSurface)
                      Text(
                          text = todo.dueDateFormatted(),
                          fontSize = 12.sp,
                          color = cs.onSurface.copy(alpha = 0.7f))
                    }
                  }
            }
          }
        }
      }
}

@Composable
private fun WellnessSection() {
  val cs = MaterialTheme.colorScheme

  SectionBox(
      header = {
        Text(
            stringResource(R.string.wellness_events_label),
            style =
                MaterialTheme.typography.titleMedium.copy(
                    fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = cs.onSurface),
            modifier = Modifier.padding(bottom = Dimensions.spacingSmall))
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

@Composable
private fun CompactClassRow(clazz: Class, record: ClassAttendance?, modifier: Modifier = Modifier) {
  val cs = MaterialTheme.colorScheme
  val (iconRes, badgeColor) =
      when (clazz.type) {
        ClassType.LECTURE -> R.drawable.ic_lecture to LightBlueAccent
        ClassType.EXERCISE -> R.drawable.ic_exercise to CustomGreen
        ClassType.LAB -> R.drawable.ic_lab to AccentViolet
        ClassType.PROJECT -> R.drawable.ic_lab to Pink
      }
  val typeLabel =
      when (clazz.type) {
        ClassType.LECTURE -> stringResource(R.string.lecture_type)
        ClassType.EXERCISE -> stringResource(R.string.exercise_type)
        ClassType.LAB -> stringResource(R.string.lab_type)
        ClassType.PROJECT -> stringResource(R.string.project_type)
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
          clazz.courseName,
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
          horizontalAlignment = Alignment.End,
          verticalArrangement = Arrangement.spacedBy(Dimensions.spacingSmall)) {
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
                  modifier = Modifier.size(Dimensions.spacingLarge))
              Spacer(Modifier.width(Dimensions.spacingXSmall))
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
                  modifier = Modifier.size(Dimensions.spacingLarge))
              Spacer(Modifier.width(Dimensions.spacingXSmall))
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

@Composable
private fun GapItemRow(gap: ScheduleGapItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
  val timeFmt = DateTimeFormatter.ofPattern("HH:mm")

  // Dashed border or light background to indicate "open slot"
  Box(
      modifier =
          modifier
              .fillMaxWidth()
              .height(50.dp)
              .clip(RoundedCornerShape(12.dp))
              .border(
                  1.dp,
                  Color.Gray.copy(alpha = 0.5f),
                  RoundedCornerShape(
                      12.dp)) // Dashed effect requires simpler custom modifier usually, plain
              // border is fine
              .background(Color.Transparent)
              .clickable { onClick() }
              .padding(horizontal = 12.dp),
      contentAlignment = Alignment.CenterStart) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
              Column {
                Text(stringResource(R.string.gap_row_title, gap.durationMinutes))
                Text(
                    text = "${gap.start.format(timeFmt)} - ${gap.end.format(timeFmt)}",
                    style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray))
              }

              Icon(
                  imageVector = Icons.Default.Add,
                  contentDescription = stringResource(R.string.gap_row_add_activity))
            }
      }
}

@Composable
private fun EventItemRow(event: ScheduleEvent, onClick: () -> Unit, onDelete: () -> Unit) {
  val timeFmt = DateTimeFormatter.ofPattern("HH:mm")
  Box(
      modifier =
          Modifier.fillMaxWidth()
              .height(50.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(PurplePrimary.copy(alpha = 0.15f))
              .clickable { onClick() }
              .padding(start = 12.dp, end = 4.dp),
      contentAlignment = Alignment.CenterStart) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
              Column {
                Text(
                    text = event.title,
                    style =
                        MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold, color = PurplePrimary))
                val start = event.time ?: LocalTime.MIN
                val end = start.plusMinutes((event.durationMinutes ?: 60).toLong())
                Text(
                    text = "${start.format(timeFmt)} - ${end.format(timeFmt)}",
                    style =
                        MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(0.6f)))
              }

              // "Extreme right" button to remove the event
              IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.event_remove),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp))
              }
            }
      }
}
