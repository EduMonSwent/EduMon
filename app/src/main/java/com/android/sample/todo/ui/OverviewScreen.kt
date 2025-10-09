package com.android.sample.todo.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.sample.todo.Priority
import com.android.sample.todo.Status
import com.android.sample.todo.ToDoRepositoryProvider

/**
 * The Overview screen shows the list of all To-Dos.
 * - Displays each To-Do in a card
 * - Lets you delete or open an item for editing
 * - Includes a FloatingActionButton (+) to add new To-Dos
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewScreen(
    onAddClicked: () -> Unit, // Navigate to AddToDoScreen
    onEditClicked: (String) -> Unit // Navigate to EditToDoScreen for a given ID
) {
  // Create a ViewModel and inject the repository
  val vm: OverviewViewModel =
      viewModel(
          factory =
              object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                  return OverviewViewModel(ToDoRepositoryProvider.repository) as T
                }
              })

  // Observe UI state (list of todos)
  val state by vm.uiState.collectAsState()

  // Scaffold provides the top bar and FAB layout
  Scaffold(
      containerColor = TodoColors.Background,
      topBar = {
        TopAppBar(
            title = { Text("Your To-Dos", color = TodoColors.OnBackground) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = TodoColors.Background))
      },
      floatingActionButton = {
        FloatingActionButton(
            onClick = onAddClicked, // open AddToDoScreen
            containerColor = TodoColors.Accent) {
              Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White)
            }
      }) { padding ->

        // If there are no tasks, show a message
        if (state.items.isEmpty()) {
          Box(
              Modifier.fillMaxSize().padding(padding).background(TodoColors.Background),
              contentAlignment = Alignment.Center) {
                Text("No tasks yet. Tap + to add one.", color = TodoColors.OnBackground)
              }

          // Otherwise show the list of To-Dos
        } else {
          LazyColumn(
              modifier = Modifier.padding(padding).padding(16.dp).background(TodoColors.Background),
              verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // For each To-Do in the list
                items(state.items, key = { it.id }) { item ->
                  Card(
                      colors = todoCardColors(),
                      shape = MaterialTheme.shapes.large,
                      border = BorderStroke(1.dp, TodoColors.CardStroke),
                      modifier = Modifier.fillMaxWidth()) {
                        Column(
                            Modifier
                                // Click anywhere on the card to edit
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() },
                                    onClick = { onEditClicked(item.id) })
                                .padding(16.dp)) {
                              // Top row: status, priority, and delete button
                              Row(verticalAlignment = Alignment.CenterVertically) {
                                StatusChip(item.status) { vm.cycleStatus(item.id) }
                                Spacer(Modifier.width(8.dp))
                                PriorityChip(item.priority)
                                Spacer(Modifier.weight(1f))
                                IconButton(onClick = { vm.delete(item.id) }) {
                                  Icon(
                                      Icons.Default.Delete,
                                      contentDescription = "Delete",
                                      tint = Color(0xFFFF3B30))
                                }
                              }

                              Spacer(Modifier.height(4.dp))
                              // To-Do title
                              Text(
                                  item.title,
                                  style = MaterialTheme.typography.titleMedium,
                                  fontWeight = FontWeight.SemiBold)

                              Spacer(Modifier.height(2.dp))
                              // Due date
                              Text(
                                  "Due: ${item.dueDateFormatted()}",
                                  style = MaterialTheme.typography.bodySmall,
                                  color = TodoColors.OnCard.copy(alpha = 0.85f))

                              // Optional note (if present)
                              if (!item.note.isNullOrBlank()) {
                                Spacer(Modifier.height(8.dp))
                                Text(item.note!!, maxLines = 2, overflow = TextOverflow.Ellipsis)
                              }
                            }
                      }
                }
              }
        }
      }
}

/** A colored chip representing the To-Do's status (TODO / IN_PROGRESS / DONE) */
@Composable
private fun StatusChip(status: Status, onClick: () -> Unit) {
  val bg =
      when (status) {
        Status.TODO -> TodoColors.ChipViolet
        Status.IN_PROGRESS -> TodoColors.ChipPink
        Status.DONE -> TodoColors.ChipGreen
      }
  AssistChip(
      onClick = onClick,
      label = { Text(status.name.replace('_', ' ')) },
      colors = AssistChipDefaults.assistChipColors(containerColor = bg, labelColor = Color.White))
}

/** A chip that displays the priority level (LOW / MEDIUM / HIGH) */
@Composable
private fun PriorityChip(priority: Priority) {
  val chipColor =
      when (priority) {
        Priority.HIGH -> TodoColors.Accent
        Priority.MEDIUM -> TodoColors.ChipPink
        Priority.LOW -> TodoColors.ChipGreen
      }
  AssistChip(
      onClick = {},
      label = { Text("Priority: ${priority.name}") },
      colors =
          AssistChipDefaults.assistChipColors(
              containerColor = chipColor.copy(alpha = 0.25f), labelColor = TodoColors.OnCard))
}
