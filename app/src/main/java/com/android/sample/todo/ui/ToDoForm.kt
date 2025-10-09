package com.android.sample.todo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.android.sample.todo.Priority
import com.android.sample.todo.Status
import com.android.sample.todo.ui.DueDateField
import com.android.sample.todo.ui.TodoColors
import java.time.LocalDate

/**
 * Shared form used by both AddToDoScreen and EditToDoScreen. Handles all UI fields (title, date,
 * priority, etc.) and save button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoForm(
    // --- Screen header configuration ---
    titleTopBar: String, // title of the top bar ("New To-Do" / "Edit To-Do")
    saveButtonText: String, // text for the save button ("Save To-Do" / "Save changes")
    onBack: () -> Unit, // action for back button

    // --- Required fields ---
    title: String,
    onTitleChange: (String) -> Unit,
    dueDate: LocalDate,
    onDueDateChange: (LocalDate) -> Unit,
    priority: Priority,
    onPriorityChange: (Priority) -> Unit,
    status: Status,
    onStatusChange: (Status) -> Unit,

    // --- Optional fields ---
    showOptionalInitial: Boolean = false,
    location: String?,
    onLocationChange: (String?) -> Unit,
    linksText: String,
    onLinksTextChange: (String) -> Unit,
    note: String?,
    onNoteChange: (String?) -> Unit,
    notificationsEnabled: Boolean,
    onNotificationsChange: (Boolean) -> Unit,

    // --- Save logic ---
    canSave: Boolean, // enable/disable save button
    onSave: () -> Unit // action to save data
) {
  // Controls whether optional fields are shown
  var showOptional by remember { mutableStateOf(showOptionalInitial) }

  // Consistent high-contrast text field colors for dark background
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

        // Main form layout
        Column(
            Modifier.padding(padding).padding(16.dp).background(TodoColors.Background),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
              // --- Required fields ---
              OutlinedTextField(
                  value = title,
                  onValueChange = onTitleChange,
                  label = { Text("Title*") },
                  singleLine = true,
                  colors = fieldColors,
                  modifier = Modifier.fillMaxWidth())

              // Date field (uses custom component with date picker)
              DueDateField(
                  date = dueDate,
                  onDateChange = onDueDateChange,
                  colors = fieldColors,
                  modifier = Modifier.fillMaxWidth())

              // Priority dropdown
              EnumDropdown(
                  label = "Priority*",
                  current = priority,
                  values = Priority.values().toList(),
                  onSelect = onPriorityChange,
                  fieldColors = fieldColors)

              // Status dropdown
              EnumDropdown(
                  label = "Status*",
                  current = status,
                  values = Status.values().toList(),
                  onSelect = onStatusChange,
                  fieldColors = fieldColors)

              // --- Optional section toggle ---
              ElevatedButton(
                  onClick = { showOptional = !showOptional }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (showOptional) "Hide optional" else "Show optional")
                  }

              // --- Optional fields (conditionally visible) ---
              if (showOptional) {
                OutlinedTextField(
                    value = location.orEmpty(),
                    onValueChange = onLocationChange,
                    label = { Text("Location") },
                    singleLine = true,
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth())

                OutlinedTextField(
                    value = linksText,
                    onValueChange = onLinksTextChange,
                    label = { Text("Useful links (comma-separated)") },
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth())

                OutlinedTextField(
                    value = note.orEmpty(),
                    onValueChange = onNoteChange,
                    label = { Text("Note / Description") },
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp))

                Row {
                  Text("Notifications", color = TodoColors.OnBackground)
                  Spacer(Modifier.height(0.dp))
                  Switch(checked = notificationsEnabled, onCheckedChange = onNotificationsChange)
                }
              }

              Spacer(Modifier.height(8.dp))

              // --- Save button ---
              Button(
                  onClick = onSave,
                  enabled = canSave,
                  colors =
                      ButtonDefaults.buttonColors(
                          containerColor = TodoColors.Accent,
                          contentColor = MaterialTheme.colorScheme.onPrimary),
                  modifier = Modifier.fillMaxWidth()) {
                    Text(saveButtonText)
                  }
            }
      }
}

/* ---- Shared helper: Dropdown menu for Enums ---- */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T : Enum<T>> EnumDropdown(
    label: String,
    current: T,
    values: List<T>,
    onSelect: (T) -> Unit,
    fieldColors: TextFieldColors
) {
  var expanded by remember { mutableStateOf(false) }
  ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
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
