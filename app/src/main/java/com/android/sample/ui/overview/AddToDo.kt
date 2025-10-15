package com.android.sample.ui.overview

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
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
import com.android.sample.ui.navigation.NavigationTestTags
import com.android.sample.ui.navigation.Screen

object AddToDoScreenTestTags {
  const val INPUT_TODO_TITLE = "inputTodoTitle"
  const val INPUT_TODO_DESCRIPTION = "inputTodoDescription"
  const val INPUT_TODO_ASSIGNEE = "inputTodoAssignee"
  const val INPUT_TODO_LOCATION = "inputTodoLocation"
  const val LOCATION_SUGGESTION = "locationSuggestion"
  const val INPUT_TODO_DATE = "inputTodoDate"
  const val TODO_SAVE = "todoSave"
  const val ERROR_MESSAGE = "errorMessage"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTodoScreen(
    addTodoViewModel: AddTodoViewModel = viewModel(),
    onSaved: () -> Unit = {},
    goBack: () -> Unit = {},
) {
  val todoUIState by addTodoViewModel.uiState.collectAsState()
  val errorMsg = todoUIState.errorMsg

  var locationText by remember { mutableStateOf("") }

  var hasTouchedTitle by remember { mutableStateOf(false) }
  var hasTouchedDescription by remember { mutableStateOf(false) }
  var hasTouchedAssignee by remember { mutableStateOf(false) }
  var hasTouchedDate by remember { mutableStateOf(false) }

  val context = LocalContext.current

  LaunchedEffect(errorMsg) {
    if (errorMsg != null) {
      Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
      addTodoViewModel.clearErrorMsg()
    }
  }

  Scaffold(
      topBar = {
        TopAppBar(
            title = {
              Text(Screen.AddToDo.name, Modifier.testTag(NavigationTestTags.TOP_BAR_TITLE))
            },
            navigationIcon = {
              IconButton(
                  onClick = { goBack() }, Modifier.testTag(NavigationTestTags.GO_BACK_BUTTON)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back")
                  }
            })
      },
      content = { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp).padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
              // Title Input
              OutlinedTextField(
                  value = todoUIState.title,
                  onValueChange = { addTodoViewModel.setTitle(it) },
                  label = { Text("Title") },
                  placeholder = { Text("Name the task") },
                  modifier =
                      Modifier.fillMaxWidth()
                          .testTag(AddToDoScreenTestTags.INPUT_TODO_TITLE)
                          .onFocusChanged({ focusState ->
                            if (focusState.isFocused) {
                              hasTouchedTitle = true
                            }
                          }))
              if (todoUIState.title.isBlank() && hasTouchedTitle) {
                Text(
                    text = "Title cannot be empty",
                    color = Color.Red,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.testTag(AddToDoScreenTestTags.ERROR_MESSAGE))
              }

              // Description Input
              OutlinedTextField(
                  value = todoUIState.description,
                  onValueChange = { addTodoViewModel.setDescription(it) },
                  label = { Text("Description") },
                  placeholder = { Text("Describe the task") },
                  modifier =
                      Modifier.fillMaxWidth()
                          .height(200.dp)
                          .testTag(AddToDoScreenTestTags.INPUT_TODO_DESCRIPTION)
                          .onFocusChanged({ focusState ->
                            if (focusState.isFocused) {
                              hasTouchedDescription = true
                            }
                          }))
              if (todoUIState.description.isBlank() && hasTouchedDescription) {
                Text(
                    text = "Description cannot be empty",
                    color = Color.Red,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.testTag(AddToDoScreenTestTags.ERROR_MESSAGE))
              }

              // Assignee Input
              OutlinedTextField(
                  value = todoUIState.assigneeName,
                  onValueChange = { addTodoViewModel.setAssigneeName(it) },
                  label = { Text("Assignee") },
                  placeholder = { Text("Assign a person") },
                  modifier =
                      Modifier.fillMaxWidth()
                          .testTag(AddToDoScreenTestTags.INPUT_TODO_ASSIGNEE)
                          .onFocusChanged({ focusState ->
                            if (focusState.isFocused) {
                              hasTouchedAssignee = true
                            }
                          }))
              if (todoUIState.assigneeName.isBlank() && hasTouchedAssignee) {
                Text(
                    text = "Assignee cannot be empty",
                    color = Color.Red,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.testTag(AddToDoScreenTestTags.ERROR_MESSAGE))
              }

              // Placeholder Location Input, does nothing for now
              OutlinedTextField(
                  value = locationText,
                  onValueChange = { locationText = it },
                  label = { Text("Location") },
                  placeholder = { Text("Enter an Address or Location") },
                  modifier =
                      Modifier.fillMaxWidth().testTag(AddToDoScreenTestTags.INPUT_TODO_LOCATION),
              )

              // Due Date Input
              OutlinedTextField(
                  value = todoUIState.dueDate,
                  onValueChange = { addTodoViewModel.setDueDate(it) },
                  label = { Text("Due date") },
                  placeholder = { Text("01/01/1970") },
                  modifier =
                      Modifier.fillMaxWidth()
                          .testTag(AddToDoScreenTestTags.INPUT_TODO_DATE)
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
                    modifier = Modifier.testTag(AddToDoScreenTestTags.ERROR_MESSAGE))
              }
              Spacer(modifier = Modifier.height(16.dp))

              // Save Button
              Button(
                  onClick = {
                    addTodoViewModel.addTodo()
                    onSaved()
                  },
                  modifier = Modifier.fillMaxWidth().testTag(AddToDoScreenTestTags.TODO_SAVE),
                  enabled =
                      todoUIState.title.isNotBlank() &&
                          todoUIState.description.isNotBlank() &&
                          todoUIState.assigneeName.isNotBlank() &&
                          dateRegex.matches(todoUIState.dueDate)) {
                    Text("Save")
                  }
            }
      })
}
