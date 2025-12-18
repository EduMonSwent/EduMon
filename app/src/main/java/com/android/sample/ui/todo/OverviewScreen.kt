package com.android.sample.ui.todo

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.android.sample.data.Priority
import com.android.sample.data.Status
import com.android.sample.repos_providors.AppRepositories

/**
 * The Overview screen shows the list of all To-Dos.
 * - Displays each To-Do in a card
 * - Lets you delete or open an item for editing
 * - Includes a FloatingActionButton to add new To-Dos Some parts of this code have been generated
 *   by AI.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewScreen(onAddClicked: () -> Unit, onEditClicked: (String) -> Unit) {
  val vm: OverviewViewModel =
      viewModel(
          factory =
              object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                  return OverviewViewModel(AppRepositories.toDoRepository) as T
                }
              })

  val state by vm.uiState.collectAsState()

  Scaffold(
      modifier = Modifier.testTag(TestTags.OverviewScreen),
      containerColor = TodoColors.Background,
      topBar = {
        TopAppBar(
            title = { Text("Your To-Dos", color = TodoColors.OnBackground) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = TodoColors.Background))
      },
      floatingActionButton = {
        FloatingActionButton(
            onClick = onAddClicked,
            containerColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.testTag(TestTags.FabAdd)) {
              Icon(
                  Icons.Default.Add,
                  contentDescription = "Add",
                  tint = MaterialTheme.colorScheme.onPrimary)
            }
      }) { padding ->
        if (state.items.isEmpty()) {
          Box(
              Modifier.fillMaxSize().padding(padding).background(TodoColors.Background),
              contentAlignment = Alignment.Center) {
                Text("No tasks yet. Tap + to add one.", color = TodoColors.OnBackground)
              }
        } else {
          LazyColumn(
              modifier =
                  Modifier.padding(padding)
                      .padding(16.dp)
                      .background(TodoColors.Background)
                      .testTag(TestTags.List),
              verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(state.items, key = { it.id }) { item ->
                  Card(
                      colors = todoCardColors(),
                      shape = MaterialTheme.shapes.large,
                      border = BorderStroke(1.dp, TodoColors.CardStroke),
                      modifier = Modifier.fillMaxWidth().testTag(TestTags.card(item.id))) {
                        Column(
                            Modifier.clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() },
                                    onClick = { onEditClicked(item.id) })
                                .padding(16.dp)) {
                              Row(verticalAlignment = Alignment.CenterVertically) {
                                StatusChip(
                                    status = item.status,
                                    onClick = { vm.cycleStatus(item.id) },
                                    modifier = Modifier.testTag(TestTags.status(item.id)))
                                Spacer(Modifier.width(8.dp))
                                PriorityChip(
                                    priority = item.priority,
                                    modifier = Modifier.testTag(TestTags.priority(item.id)))
                                Spacer(Modifier.weight(1f))
                                IconButton(
                                    onClick = { vm.delete(item.id) },
                                    modifier = Modifier.testTag(TestTags.delete(item.id))) {
                                      Icon(
                                          Icons.Default.Delete,
                                          contentDescription = "Delete",
                                          tint = MaterialTheme.colorScheme.error)
                                    }
                              }

                              Spacer(Modifier.height(4.dp))
                              Text(
                                  item.title,
                                  style = MaterialTheme.typography.titleMedium,
                                  fontWeight = FontWeight.SemiBold)

                              Spacer(Modifier.height(2.dp))
                              Text(
                                  "Due: ${item.dueDateFormatted()}",
                                  style = MaterialTheme.typography.bodySmall,
                                  color = TodoColors.OnCard.copy(alpha = 0.85f))

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
private fun StatusChip(status: Status, onClick: () -> Unit, modifier: Modifier = Modifier) {
  val bg =
      when (status) {
        Status.TODO -> TodoColors.ChipViolet
        Status.IN_PROGRESS -> TodoColors.ChipPink
        Status.DONE -> TodoColors.ChipGreen
      }
  AssistChip(
      onClick = onClick,
      label = { Text(status.name.replace('_', ' ')) },
      modifier = modifier,
      colors =
          AssistChipDefaults.assistChipColors(
              containerColor = bg, labelColor = MaterialTheme.colorScheme.onPrimary))
}

/** A chip that displays the priority level (LOW / MEDIUM / HIGH) */
@Composable
private fun PriorityChip(priority: Priority, modifier: Modifier = Modifier) {
  val chipColor =
      when (priority) {
        Priority.HIGH -> TodoColors.Accent
        Priority.MEDIUM -> TodoColors.ChipPink
        Priority.LOW -> TodoColors.ChipGreen
      }
  AssistChip(
      onClick = {},
      label = { Text("Priority: ${priority.name}") },
      modifier = modifier,
      colors =
          AssistChipDefaults.assistChipColors(
              containerColor = chipColor.copy(alpha = 0.25f), labelColor = TodoColors.OnCard))
}

// routes etc. (unchanged) ...

private const val ROUTE_OVERVIEW = "overview"
private const val ROUTE_ADD = "todo/add"
private const val ROUTE_EDIT = "todo/edit/{id}"

private fun editRoute(id: String) = "todo/edit/$id"

@Composable
fun TodoNavHostInThisFile() {
  val nav = rememberNavController()

  NavHost(navController = nav, startDestination = ROUTE_OVERVIEW) {
    composable(ROUTE_OVERVIEW) {
      OverviewScreen(
          onAddClicked = { nav.navigate(ROUTE_ADD) },
          onEditClicked = { id -> nav.navigate(editRoute(id)) })
    }

    composable(ROUTE_ADD) { AddToDoScreen(onBack = { nav.popBackStack() }) }

    composable(
        route = ROUTE_EDIT, arguments = listOf(navArgument("id") { type = NavType.StringType })) {
            backStackEntry ->
          val id = requireNotNull(backStackEntry.arguments?.getString("id"))
          EditToDoScreen(id = id, onBack = { nav.popBackStack() })
        }
  }
}
