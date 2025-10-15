package com.android.sample.ui.overview

import android.icu.text.SimpleDateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.sample.model.todo.ToDo
import com.android.sample.model.todo.ToDoStatus
import com.android.sample.model.todo.ToDosRepositoryLocal
import com.android.sample.ui.navigation.NavigationActions
import com.android.sample.ui.navigation.NavigationTestTags
import com.android.sample.ui.theme.SampleAppTheme
import java.util.Locale

object OverviewScreenTestTags {
  const val CREATE_TODO_BUTTON = "createTodoFab"
  const val EMPTY_TODO_LIST_MSG = "emptyTodoList"
  const val TODO_LIST = "todoList"

  fun getTestTagForTodoItem(todo: ToDo): String = "todoItem${todo.uid}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewScreen(
    overviewViewModel: OverviewViewModel = viewModel(),
    onAddTodo: () -> Unit = {},
    onSelectTodo: (ToDo) -> Unit = {},
    navigationActions: NavigationActions? = null,
) {

  val uiState by overviewViewModel.uiState.collectAsState()
  val todos = uiState.todos

  // Fetch todos when the screen is recomposed
  LaunchedEffect(Unit) { overviewViewModel.refreshUIState() }

  Scaffold(
      topBar = {
        TopAppBar(
            title = {
              Text("Overview", modifier = Modifier.testTag(NavigationTestTags.TOP_BAR_TITLE))
            })
      },
      floatingActionButton = {
        FloatingActionButton(
            onClick = { onAddTodo() },
            modifier = Modifier.testTag(OverviewScreenTestTags.CREATE_TODO_BUTTON)) {
              Icon(imageVector = Icons.Default.Add, contentDescription = "Add")
            }
      },
      content = { pd ->
        if (todos.isNotEmpty()) {
          LazyColumn(
              contentPadding = PaddingValues(vertical = 8.dp),
              modifier =
                  Modifier.fillMaxWidth()
                      .padding(horizontal = 16.dp)
                      .padding(pd)
                      .testTag(OverviewScreenTestTags.TODO_LIST)) {
                items(todos.size) { index ->
                  ToDoItem(todo = todos[index], onClick = { onSelectTodo(todos[index]) })
                }
              }
        } else {
          Text(
              modifier = Modifier.padding(pd).testTag(OverviewScreenTestTags.EMPTY_TODO_LIST_MSG),
              text = "You have no ToDo yet.")
        }
      })
}

@Composable
fun ToDoItem(todo: ToDo, onClick: () -> Unit) {
  Card(
      modifier =
          Modifier.clickable(onClick = onClick)
              .testTag(OverviewScreenTestTags.getTestTagForTodoItem(todo))
              .fillMaxWidth()
              .padding(vertical = 4.dp)
              .semantics(mergeDescendants = true) {}) {
        Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
          // Date and Status Row
          Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text =
                        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            .format(todo.dueDate.toDate()),
                    style = MaterialTheme.typography.bodySmall)

                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(
                      text =
                          when (todo.status) {
                            ToDoStatus.CREATED -> "Created"
                            ToDoStatus.STARTED -> "Started"
                            ToDoStatus.ENDED -> "Ended"
                            ToDoStatus.ARCHIVED -> "Archived"
                          },
                      style = MaterialTheme.typography.bodySmall,
                      color =
                          when (todo.status) {
                            ToDoStatus.CREATED -> Color.Blue
                            ToDoStatus.STARTED -> Color(0xFFFFA500) // Orange
                            ToDoStatus.ENDED -> Color.Green
                            ToDoStatus.ARCHIVED -> Color.Gray
                          })
                  Icon(
                      imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                      contentDescription = null)
                }
              }

          Spacer(modifier = Modifier.height(4.dp))

          // Task Name
          Text(
              text = todo.name,
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.Bold)

          // Assignee Name
          Text(
              text = todo.assigneeName,
              style = MaterialTheme.typography.bodySmall,
              color = Color.Gray)
        }
      }
}

@Preview
@Composable
fun OverviewScreenPreview() {
  val repository = ToDosRepositoryLocal()
  val viewModel = OverviewViewModel(repository)
  SampleAppTheme { Surface(modifier = Modifier.fillMaxSize()) { OverviewScreen(viewModel) } }
  OverviewScreen(overviewViewModel = viewModel)
}
