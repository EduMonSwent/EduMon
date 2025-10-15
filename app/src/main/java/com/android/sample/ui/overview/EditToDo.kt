package com.android.sample.ui.overview

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.sample.model.todo.ToDoStatus
import com.android.sample.model.todo.displayString
import com.android.sample.ui.navigation.NavigationTestTags
import com.android.sample.ui.navigation.Screen

object EditToDoScreenTestTags {
  const val INPUT_TODO_TITLE = "inputTodoTitle"
  const val INPUT_TODO_DESCRIPTION = "inputTodoDescription"
  const val INPUT_TODO_ASSIGNEE = "inputTodoAssignee"
  const val INPUT_TODO_LOCATION = "inputTodoLocation"
  const val LOCATION_SUGGESTION = "locationSuggestion"
  const val INPUT_TODO_DATE = "inputTodoDate"
  const val INPUT_TODO_STATUS = "inputTodoStatus"
  const val TODO_SAVE = "todoSave"
  const val TODO_DELETE = "todoDelete"
  const val ERROR_MESSAGE = "errorMessage"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditToDoScreen(
    todoUid: String,
    editTodoViewModel: EditTodoViewModel = viewModel(),
    onEdit: () -> Unit = {},
    goBack: () -> Unit = {},
) {
  LaunchedEffect(todoUid) { editTodoViewModel.loadTodo(todoUid) }

  val todoUIState by editTodoViewModel.uiState.collectAsState()
  val errorMsg = todoUIState.errorMsg
  var hasTouchedTitle by remember { mutableStateOf(false) }
  var hasTouchedDescription by remember { mutableStateOf(false) }
  var hasTouchedAssignee by remember { mutableStateOf(false) }
  var hasTouchedDate by remember { mutableStateOf(false) }

  // State for dropdown visibility
  var showDropdown by remember { mutableStateOf(false) }

  // default to the selected location's name or empty string if null
  var locationText by remember { mutableStateOf("") }
  if (todoUIState.selectedLocation != null) {
    locationText = todoUIState.selectedLocation!!.name
  }

  val context = LocalContext.current

  LaunchedEffect(errorMsg) {
    if (errorMsg != null) {
      Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
      editTodoViewModel.clearErrorMsg()
    }
  }

  Scaffold(
      topBar = {
        TopAppBar(
            title = {
              Text(
                  Screen.EditToDo(todoUid).name,
                  modifier = Modifier.testTag(NavigationTestTags.TOP_BAR_TITLE))
            },
            navigationIcon = {
              IconButton(
                  modifier = Modifier.testTag(NavigationTestTags.GO_BACK_BUTTON),
                  onClick = { goBack() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back")
                  }
            })
      },
      content = { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
              // Title Input
              OutlinedTextField(
                  value = todoUIState.title,
                  onValueChange = { editTodoViewModel.setTitle(it) },
                  label = { Text("Title") },
                  placeholder = { Text("Task Title") },
                  modifier =
                      Modifier.fillMaxWidth()
                          .testTag(EditToDoScreenTestTags.INPUT_TODO_TITLE)
                          .onFocusChanged({
                            if (it.isFocused) {
                              hasTouchedTitle = true
                            }
                          }))

              if (todoUIState.title.isBlank() && hasTouchedTitle) {
                Text(
                    text = "Title cannot be empty",
                    color = Color.Red,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.testTag(EditToDoScreenTestTags.ERROR_MESSAGE))
              }

              // Description Input
              OutlinedTextField(
                  value = todoUIState.description,
                  onValueChange = {
                    Log.d(
                        "EditToDoScreen",
                        "ALALALA Description changed: $it and ISBLANK: ${it.isBlank()}")
                    editTodoViewModel.setDescription(it)
                    Log.d(
                        "EditToDoScreen",
                        "ALALALA Current description in state: ${todoUIState.description} and ISBLANK: ${todoUIState.description.isBlank()}")
                  },
                  label = { Text("Description") },
                  placeholder = { Text("Describe the task") },
                  modifier =
                      Modifier.fillMaxWidth()
                          .height(100.dp)
                          .testTag(EditToDoScreenTestTags.INPUT_TODO_DESCRIPTION)
                          .onFocusChanged({
                            if (it.isFocused) {
                              hasTouchedDescription = true
                            }
                          }))
              if (todoUIState.description.isBlank() && hasTouchedDescription) {
                Text(
                    text = "Description cannot be empty",
                    color = Color.Red,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.testTag(EditToDoScreenTestTags.ERROR_MESSAGE))
              }

              // Assignee Input
              OutlinedTextField(
                  value = todoUIState.assigneeName,
                  onValueChange = { editTodoViewModel.setAssigneeName(it) },
                  label = { Text("Assignee") },
                  placeholder = { Text("Assign a person") },
                  modifier =
                      Modifier.fillMaxWidth()
                          .testTag(EditToDoScreenTestTags.INPUT_TODO_ASSIGNEE)
                          .onFocusChanged({
                            if (it.isFocused) {
                              hasTouchedAssignee = true
                            }
                          }))
              if (todoUIState.assigneeName.isBlank() && hasTouchedAssignee) {
                Text(
                    text = "Assignee cannot be empty",
                    color = Color.Red,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.testTag(EditToDoScreenTestTags.ERROR_MESSAGE))
              }

              // Placeholder Location Input
              OutlinedTextField(
                  value = locationText,
                  onValueChange = {
                    locationText = it
                    editTodoViewModel.setLocationName(it)
                  },
                  label = { Text("Location") },
                  placeholder = { Text("Enter an Address or Location") },
                  modifier =
                      Modifier.fillMaxWidth().testTag(EditToDoScreenTestTags.INPUT_TODO_LOCATION),
              )

              // Due Date Input
              OutlinedTextField(
                  value = todoUIState.dueDate,
                  onValueChange = { editTodoViewModel.setDueDate(it) },
                  label = { Text("Due date") },
                  placeholder = { Text("01/01/1970") },
                  modifier =
                      Modifier.fillMaxWidth()
                          .testTag(EditToDoScreenTestTags.INPUT_TODO_DATE)
                          .onFocusChanged({ focusState ->
                            if (focusState.isFocused) {
                              hasTouchedDate = true
                            }
                          }))
              val dateRegex = Regex("""^\d{2}/\d{2}/\d{4}$""")
              if (todoUIState.dueDate.isNotBlank() &&
                  !dateRegex.matches(todoUIState.dueDate) &&
                  hasTouchedDate) {
                Text(
                    text = "Invalid format (must be dd/MM/yyyy)",
                    color = Color.Red,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.testTag(EditToDoScreenTestTags.ERROR_MESSAGE))
              }

              // Status Button
              Button(
                  onClick = {
                    // Update status to the next enum value
                    editTodoViewModel.setStatus(getNextStatus(todoUIState.status))
                  },
                  modifier =
                      Modifier.fillMaxWidth()
                          .padding(vertical = 8.dp)
                          .testTag(EditToDoScreenTestTags.INPUT_TODO_STATUS)) {
                    Text(text = todoUIState.status.displayString())
                  }

              Spacer(modifier = Modifier.height(8.dp))

              // Save Button
              Button(
                  onClick = {
                    editTodoViewModel.editTodo(todoUid)

                    onEdit()
                  },
                  modifier = Modifier.fillMaxWidth().testTag(EditToDoScreenTestTags.TODO_SAVE),
                  enabled =
                      todoUIState.title.isNotBlank() &&
                          todoUIState.description.isNotBlank() &&
                          todoUIState.assigneeName.isNotBlank() &&
                          dateRegex.matches(todoUIState.dueDate)
                  // Ensure location is selected for B3 !!
                  ) {
                    Text("Save")
                  }

              Spacer(modifier = Modifier.height(4.dp))

              // Delete Button
              Button(
                  onClick = {
                    editTodoViewModel.deleteToDo(todoUid)
                    onEdit()
                  },
                  modifier = Modifier.fillMaxWidth().testTag(EditToDoScreenTestTags.TODO_DELETE),
              ) {
                Text("Delete", color = Color.White)
              }
            }
      })
}

// Function to get the next status in the enum sequence
fun getNextStatus(currentStatus: ToDoStatus): ToDoStatus {
  return when (currentStatus) {
    ToDoStatus.CREATED -> ToDoStatus.STARTED
    ToDoStatus.STARTED -> ToDoStatus.ENDED
    ToDoStatus.ENDED -> ToDoStatus.ARCHIVED
    ToDoStatus.ARCHIVED -> ToDoStatus.CREATED
  }
}
