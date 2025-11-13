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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.android.sample.R
import com.android.sample.feature.schedule.data.calendar.StudyItem
import com.android.sample.feature.schedule.data.calendar.TaskType
import com.android.sample.feature.weeks.ui.GlassSurface
import com.android.sample.ui.theme.Blue
import com.android.sample.ui.theme.Pink
import com.android.sample.ui.theme.PurpleCalendar
import com.android.sample.ui.theme.VioletLilas
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun UpcomingEventsSection(
    tasks: List<StudyItem>,
    selectedDate: LocalDate,
    onAddTaskClick: (LocalDate) -> Unit,
    onTaskClick: (StudyItem) -> Unit,
    modifier: Modifier = Modifier, // Added modifier
    title: String = stringResource(R.string.upcoming_events)
) {
  val sortedTasks = remember(tasks) { tasks.sortedBy { it.date } }
  val cs = MaterialTheme.colorScheme

  GlassSurface(
      modifier = modifier.fillMaxWidth(), // Use the passed modifier
      shape = RoundedCornerShape(20.dp)) {
        Column(modifier = Modifier.padding(20.dp)) {
          // Header Row
          Row(
              modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style =
                        MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold, color = cs.onSurface))

                // Properly sized and aligned "Add Event" button
                Button( // Changed to Button for more control over background
                    onClick = { onAddTaskClick(selectedDate) },
                    shape = RoundedCornerShape(12.dp),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = cs.primary, // Use primary color
                            contentColor = cs.onPrimary),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                    modifier = Modifier.height(38.dp).wrapContentWidth()) {
                      Icon(
                          Icons.Default.Add,
                          contentDescription = stringResource(R.string.add_event),
                          modifier = Modifier.size(18.dp))
                      Spacer(Modifier.width(6.dp))
                      Text(
                          text = stringResource(R.string.add_event),
                          style =
                              MaterialTheme.typography.labelMedium.copy(
                                  fontWeight = FontWeight.SemiBold))
                    }
              }

          Spacer(Modifier.height(12.dp))

          // Task list
          if (sortedTasks.isEmpty()) {
            Text(
                text = stringResource(R.string.no_upcoming_events),
                color = cs.onSurface.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.align(Alignment.CenterHorizontally))
          } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
              sortedTasks.forEach { task -> EventCard(task = task, onTaskClick = onTaskClick) }
            }
          }
        }
      }
}

@Composable
private fun EventCard(task: StudyItem, onTaskClick: (StudyItem) -> Unit) {
  val tagColor =
      when (task.type) {
        TaskType.STUDY -> Blue
        TaskType.WORK -> Pink
        TaskType.PERSONAL -> VioletLilas
        else -> PurpleCalendar
      }
  val cs = MaterialTheme.colorScheme

  GlassSurface( // Use GlassSurface for individual event cards
      modifier = Modifier.fillMaxWidth().clickable { onTaskClick(task) },
      shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = task.date.format(DateTimeFormatter.ofPattern("MMM dd")),
                style =
                    MaterialTheme.typography.labelLarge.copy(
                        color = cs.primary, // Use theme primary
                        fontWeight = FontWeight.Medium),
                modifier = Modifier.weight(1f))
            Surface(color = tagColor.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp)) {
              Text(
                  text = task.type.name.lowercase(),
                  color = tagColor,
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
            }
          }

          Spacer(Modifier.height(6.dp))

          Text(
              text = task.title,
              style =
                  MaterialTheme.typography.bodyLarge.copy(
                      color = cs.onSurface, // Use theme onSurface
                      fontWeight = FontWeight.SemiBold))

          val taskTime =
              when (task) {
                is StudyItem ->
                    try {
                      task.time?.toString()?.takeIf { it.isNotBlank() }
                    } catch (e: Exception) {
                      null
                    }
                else -> null
              }

          if (taskTime != null) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = taskTime,
                style =
                    MaterialTheme.typography.labelSmall.copy(
                        color = cs.onSurface.copy(alpha = 0.7f)) // Use theme onSurface
                )
          }
        }
      }
}
