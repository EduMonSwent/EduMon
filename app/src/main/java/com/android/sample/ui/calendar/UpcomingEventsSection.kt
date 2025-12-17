package com.android.sample.ui.calendar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.android.sample.R
import com.android.sample.feature.schedule.data.calendar.StudyItem
import com.android.sample.feature.schedule.data.calendar.TaskType
import com.android.sample.feature.weeks.ui.GlassSurface
import com.android.sample.ui.theme.CustomGreen
import com.android.sample.ui.theme.EventViolet
import com.android.sample.ui.theme.LightBlue
import com.android.sample.ui.theme.Pink
import com.android.sample.ui.theme.PurplePrimary
import com.android.sample.ui.theme.VioletLilas
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun UpcomingEventsSection(
    tasks: List<StudyItem>,
    selectedDate: LocalDate,
    onAddTaskClick: (LocalDate) -> Unit,
    onTaskClick: (StudyItem) -> Unit,
    title: String = stringResource(R.string.upcoming_events)
) {
  val cs = MaterialTheme.colorScheme
  val sortedTasks = remember(tasks) { tasks.sortedBy { it.date } }

  Column(modifier = Modifier.fillMaxWidth()) {

    // Header: tighter gap under title
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically) {
          Text(
              text = title,
              style =
                  MaterialTheme.typography.titleLarge.copy(
                      fontWeight = FontWeight.ExtraBold, color = cs.onSurface),
              modifier = Modifier.weight(1f))
          FilledTonalButton(
              onClick = { onAddTaskClick(selectedDate) },
              shape = RoundedCornerShape(14.dp),
              colors =
                  ButtonDefaults.filledTonalButtonColors(
                      containerColor = PurplePrimary, contentColor = Color.White),
              contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
              modifier = Modifier.height(44.dp)) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_event))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.add_event))
              }
        }

    if (sortedTasks.isEmpty()) {
      Text(
          text = stringResource(R.string.no_upcoming_events),
          color = EventViolet,
          style = MaterialTheme.typography.bodyMedium,
          modifier = Modifier.align(Alignment.CenterHorizontally))
    } else {
      Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        sortedTasks.forEach { task -> EventRowGlass(task = task, onTaskClick = onTaskClick) }
      }
    }
  }
}
// Item row styled like your other “glass” pills, full-width
@Composable
private fun EventRowGlass(task: StudyItem, onTaskClick: (StudyItem) -> Unit) {
  val cs = MaterialTheme.colorScheme
  val tagColor =
      when (task.type) {
        TaskType.STUDY -> LightBlue
        TaskType.WORK -> Pink
        TaskType.PERSONAL -> VioletLilas
      }

  GlassSurface(
      modifier = Modifier.fillMaxWidth().clickable { onTaskClick(task) },
      shape = RoundedCornerShape(10.dp)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp)) {
          Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = task.date.format(DateTimeFormatter.ofPattern("EEE, MMM dd")),
                style =
                    MaterialTheme.typography.titleMedium.copy(
                        color = EventViolet, fontWeight = FontWeight.Bold),
                modifier = Modifier.weight(1f))
            Surface(color = tagColor.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp)) {
              Text(
                  text = task.type.name.lowercase(),
                  color = tagColor,
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
            }
          }

          Spacer(Modifier.height(4.dp))

          Text(
              text = task.title,
              style =
                  MaterialTheme.typography.bodyLarge.copy(
                      color = cs.onSurface, fontWeight = FontWeight.SemiBold))

          task.time?.let { start ->
            val end = task.durationMinutes?.let { minutes -> start.plusMinutes(minutes.toLong()) }

            val formatter = DateTimeFormatter.ofPattern("HH:mm")

            Spacer(Modifier.height(1.dp))
            Text(
                text =
                    if (end != null) {
                      "${start.format(formatter)} – ${end.format(formatter)}"
                    } else {
                      start.format(formatter)
                    },
                style = MaterialTheme.typography.labelSmall.copy(color = CustomGreen))
          }
        }
      }
}
