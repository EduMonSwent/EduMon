package com.android.sample.todo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.android.sample.ui.todo.DueDateField
import com.android.sample.ui.todo.TestTags
import com.android.sample.ui.todo.TodoColors
import com.android.sample.ui.todo.model.Priority
import com.android.sample.ui.todo.model.Status
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoForm(
    titleTopBar: String,
    saveButtonText: String,
    onBack: () -> Unit,
    title: String,
    onTitleChange: (String) -> Unit,
    dueDate: LocalDate,
    onDueDateChange: (LocalDate) -> Unit,
    priority: Priority,
    onPriorityChange: (Priority) -> Unit,
    status: Status,
    onStatusChange: (Status) -> Unit,
    showOptionalInitial: Boolean = false,
    location: String?,
    onLocationChange: (String?) -> Unit,
    linksText: String,
    onLinksTextChange: (String) -> Unit,
    note: String?,
    onNoteChange: (String?) -> Unit,
    notificationsEnabled: Boolean,
    onNotificationsChange: (Boolean) -> Unit,
    canSave: Boolean,
    onSave: () -> Unit
) {
  var showOptional by remember { mutableStateOf(showOptionalInitial) }

  val fieldColors =
      OutlinedTextFieldDefaults.colors(
          focusedTextColor = TodoColors.OnCard,
          unfocusedTextColor = TodoColors.OnCard,
          disabledTextColor = TodoColors.OnCard.copy(alpha = 0.7f),
          focusedLabelColor = TodoColors.OnCard,
          unfocusedLabelColor = TodoColors.OnCard.copy(alpha = 0.85f),
          focusedBorderColor = TodoColors.OnCard.copy(alpha = 0.6f),
          unfocusedBorderColor = TodoColors.OnCard.copy(alpha = 0.35f),
          cursorColor = TodoColors.Accent)

  Scaffold(
      containerColor = TodoColors.Background,
      topBar = {
        TopAppBar(
            title = { Text(titleTopBar, color = TodoColors.OnBackground) },
            navigationIcon = {
              IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TodoColors.OnBackground)
              }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = TodoColors.Background))
      }) { padding ->
        Column(
            Modifier.padding(padding).padding(16.dp).background(TodoColors.Background),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
              OutlinedTextField(
                  value = title,
                  onValueChange = onTitleChange,
                  label = { Text("Title*") },
                  singleLine = true,
                  colors = fieldColors,
                  modifier = Modifier.fillMaxWidth().testTag(TestTags.TitleField))

              DueDateField(
                  date = dueDate,
                  onDateChange = onDueDateChange,
                  colors = fieldColors,
                  modifier = Modifier.fillMaxWidth().testTag(TestTags.DueDateField))

              EnumDropdown(
                  label = "Priority*",
                  current = priority,
                  values = Priority.values().toList(),
                  onSelect = onPriorityChange,
                  fieldColors = fieldColors,
                  modifier = Modifier.testTag(TestTags.PriorityDropdown))

              EnumDropdown(
                  label = "Status*",
                  current = status,
                  values = Status.values().toList(),
                  onSelect = onStatusChange,
                  fieldColors = fieldColors,
                  modifier = Modifier.testTag(TestTags.StatusDropdown))

              ElevatedButton(
                  onClick = { showOptional = !showOptional },
                  modifier = Modifier.fillMaxWidth().testTag(TestTags.OptionalToggle)) {
                    Text(if (showOptional) "Hide optional" else "Show optional")
                  }

              if (showOptional) {
                OutlinedTextField(
                    value = location.orEmpty(),
                    onValueChange = onLocationChange,
                    label = { Text("Location") },
                    singleLine = true,
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth().testTag(TestTags.LocationField))

                OutlinedTextField(
                    value = linksText,
                    onValueChange = onLinksTextChange,
                    label = { Text("Useful links (comma-separated)") },
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth().testTag(TestTags.LinksField))

                OutlinedTextField(
                    value = note.orEmpty(),
                    onValueChange = onNoteChange,
                    label = { Text("Note / Description") },
                    colors = fieldColors,
                    modifier =
                        Modifier.fillMaxWidth().heightIn(min = 120.dp).testTag(TestTags.NoteField))

                Row {
                  Text("Notifications", color = TodoColors.OnBackground)
                  Spacer(Modifier.height(0.dp))
                  Switch(
                      checked = notificationsEnabled,
                      onCheckedChange = onNotificationsChange,
                      modifier = Modifier.testTag(TestTags.NotificationsSwitch))
                }
              }

              Spacer(Modifier.height(8.dp))

              Button(
                  onClick = onSave,
                  enabled = canSave,
                  colors =
                      ButtonDefaults.buttonColors(
                          containerColor = TodoColors.Accent,
                          contentColor = MaterialTheme.colorScheme.onPrimary),
                  modifier = Modifier.fillMaxWidth().testTag(TestTags.SaveButton)) {
                    Text(saveButtonText)
                  }
            }
      }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T : Enum<T>> EnumDropdown(
    label: String,
    current: T,
    values: List<T>,
    onSelect: (T) -> Unit,
    fieldColors: TextFieldColors,
    modifier: Modifier = Modifier
) {
  var expanded by remember { mutableStateOf(false) }
  ExposedDropdownMenuBox(
      expanded = expanded, onExpandedChange = { expanded = !expanded }, modifier = modifier) {
        OutlinedTextField(
            readOnly = true,
            value = current.name.replace('_', ' '),
            onValueChange = {},
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            colors = fieldColors,
            modifier = Modifier.menuAnchor().fillMaxWidth())
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
          values.forEach { v ->
            DropdownMenuItem(
                text = { Text(v.name.replace('_', ' ')) },
                onClick = {
                  onSelect(v)
                  expanded = false
                })
          }
        }
      }
}
