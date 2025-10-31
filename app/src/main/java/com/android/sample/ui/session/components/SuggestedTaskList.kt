package com.android.sample.ui.session.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.android.sample.ui.session.Task

// Parts of this code were written using ChatGPT and AndroidStudio Gemini tool.

@Composable
fun SuggestedTasksList(
    tasks: List<Task>,
    selectedTask: Task?,
    onTaskSelected: (Task) -> Unit,
    modifier: Modifier = Modifier
) {
  LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = modifier.fillMaxWidth()) {
    items(tasks) { task ->
      val isSelected = selectedTask?.id == task.id
      AssistChip(
          onClick = { onTaskSelected(task) },
          label = { Text(task.title) },
          colors =
              AssistChipDefaults.assistChipColors(
                  containerColor =
                      if (isSelected)
                          androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer
                      else androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant))
    }
  }
}
