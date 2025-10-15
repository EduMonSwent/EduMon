package com.android.sample.ui.planner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Gray
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.android.sample.R
import com.android.sample.ui.theme.AccentViolet
import com.android.sample.ui.theme.MidDarkCard
import com.android.sample.ui.theme.TextLight
import com.android.sample.ui.theme.VioletSoft

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddStudyTaskModal(
    onDismiss: () -> Unit,
    onAddTask:
        (subject: String, title: String, duration: Int, deadline: String, priority: String) -> Unit,
    modifier: Modifier = Modifier
) {
  var subject by remember { mutableStateOf("") }
  var taskTitle by remember { mutableStateOf("") }
  var duration by remember { mutableStateOf("60") }
  var deadline by remember { mutableStateOf("") }
  var priority by remember { mutableStateOf("Medium") }

  Dialog(
      onDismissRequest = onDismiss,
      properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(
            modifier = modifier.fillMaxWidth(0.9f).shadow(32.dp, RoundedCornerShape(28.dp)),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = VioletSoft)) {
              Column(
                  modifier =
                      Modifier.background(
                              brush =
                                  Brush.verticalGradient(colors = listOf(AccentViolet, VioletSoft)))
                          .padding(24.dp)) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                          Column {
                            Text(
                                text = "Add Study Task",
                                color = TextLight,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp)
                            Text(
                                text = "Plan your study session",
                                color = TextLight.copy(alpha = 0.7f),
                                fontSize = 14.sp,
                                modifier = Modifier.padding(top = 4.dp))
                          }
                          IconButton(
                              onClick = onDismiss,
                              modifier =
                                  Modifier.size(40.dp).background(MidDarkCard, CircleShape)) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_close),
                                    contentDescription = "Close",
                                    tint = TextLight.copy(alpha = 0.7f),
                                    modifier = Modifier.size(20.dp))
                              }
                        }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Form Content
                    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                      // Subject Field
                      FormFieldSection(
                          label = "Subject *",
                          placeholder = "e.g., Data Structures",
                          value = subject,
                          onValueChange = { subject = it },
                          testTag = PlannerScreenTestTags.SUBJECT_FIELD)

                      // Task Title Field
                      FormFieldSection(
                          label = "Task Title *",
                          placeholder = "e.g., Complete homework exercises",
                          value = taskTitle,
                          onValueChange = { taskTitle = it },
                          testTag = PlannerScreenTestTags.TASK_TITLE_FIELD)

                      // Duration Field
                      FormFieldSection(
                          label = "Duration (minutes) *",
                          placeholder = "60",
                          value = duration,
                          onValueChange = { duration = it },
                          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                          testTag = PlannerScreenTestTags.DURATION_FIELD)

                      // Divider
                      Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MidDarkCard))

                      // Deadline Field
                      FormFieldSection(
                          label = "Deadline",
                          placeholder = "dd.mm.yyyy",
                          value = deadline,
                          onValueChange = { deadline = it },
                          testTag = PlannerScreenTestTags.DEADLINE_FIELD)

                      // Priority Dropdown
                      PriorityDropdownSection(
                          priority = priority, onPriorityChange = { priority = it })
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                          OutlinedButton(
                              onClick = onDismiss,
                              modifier = Modifier.weight(1f).height(52.dp),
                              shape = RoundedCornerShape(14.dp),
                              border = BorderStroke(1.5.dp, MidDarkCard),
                              colors =
                                  ButtonDefaults.outlinedButtonColors(
                                      contentColor = TextLight.copy(alpha = 0.8f))) {
                                Text("Cancel", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                              }

                          Button(
                              onClick = {
                                onAddTask(
                                    subject,
                                    taskTitle,
                                    duration.toIntOrNull() ?: 60,
                                    deadline,
                                    priority)
                              },
                              modifier =
                                  Modifier.weight(1f).height(52.dp).testTag("aaddTaskButton"),
                              shape = RoundedCornerShape(14.dp),
                              colors =
                                  ButtonDefaults.buttonColors(
                                      containerColor = AccentViolet, contentColor = Color.White),
                              enabled =
                                  subject.isNotBlank() &&
                                      taskTitle.isNotBlank() &&
                                      duration.isNotBlank(),
                              elevation =
                                  ButtonDefaults.buttonElevation(
                                      defaultElevation = 6.dp, pressedElevation = 3.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center) {
                                      Icon(
                                          painter = painterResource(R.drawable.ic_done_all),
                                          contentDescription = "Add Task",
                                          modifier = Modifier.size(18.dp))
                                      Spacer(modifier = Modifier.width(8.dp))
                                      Text(
                                          "Add Task",
                                          fontWeight = FontWeight.Bold,
                                          fontSize = 15.sp)
                                    }
                              }
                        }
                  }
            }
      }
}

@Composable
fun FormFieldSection(
    label: String,
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    testTag: String = ""
) {
  Column {
    Text(
        text = label,
        color = TextLight.copy(alpha = 0.9f),
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        modifier = Modifier.padding(bottom = 8.dp))

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = TextLight.copy(alpha = 0.4f), fontSize = 16.sp) },
        modifier = Modifier.fillMaxWidth().testTag(testTag),
        keyboardOptions = keyboardOptions,
        colors =
            OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AccentViolet,
                unfocusedBorderColor = MidDarkCard,
                focusedTextColor = TextLight,
                unfocusedTextColor = TextLight,
                focusedLabelColor = Color.Transparent,
                unfocusedLabelColor = Color.Transparent,
                focusedContainerColor = Gray.copy(alpha = 0.6f),
                unfocusedContainerColor = Gray.copy(alpha = 0.4f),
                cursorColor = AccentViolet,
                focusedLeadingIconColor = AccentViolet,
                unfocusedLeadingIconColor = TextLight.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
        textStyle = LocalTextStyle.current.copy(color = TextLight, fontSize = 16.sp))
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriorityDropdownSection(priority: String, onPriorityChange: (String) -> Unit) {
  var expanded by remember { mutableStateOf(false) }

  Column {
    Text(
        text = "Priority",
        color = TextLight.copy(alpha = 0.9f),
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        modifier = Modifier.padding(bottom = 8.dp))

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()) {
          OutlinedTextField(
              value = priority,
              onValueChange = {},
              readOnly = true,
              modifier = Modifier.menuAnchor().fillMaxWidth(),
              placeholder = {
                Text("Select priority", color = TextLight.copy(alpha = 0.4f), fontSize = 16.sp)
              },
              colors =
                  OutlinedTextFieldDefaults.colors(
                      focusedBorderColor = AccentViolet,
                      unfocusedBorderColor = MidDarkCard,
                      focusedTextColor = TextLight,
                      unfocusedTextColor = TextLight,
                      focusedContainerColor = Gray.copy(alpha = 0.6f),
                      unfocusedContainerColor = Gray.copy(alpha = 0.4f),
                      cursorColor = AccentViolet),
              shape = RoundedCornerShape(12.dp),
              leadingIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_star),
                    contentDescription = "Priority",
                    modifier = Modifier.size(20.dp))
              },
              trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
              textStyle = LocalTextStyle.current.copy(color = TextLight, fontSize = 16.sp))

          ExposedDropdownMenu(
              expanded = expanded,
              onDismissRequest = { expanded = false },
              modifier = Modifier.background(Gray)) {
                listOf("Low", "Medium", "High").forEach { selectionOption ->
                  DropdownMenuItem(
                      text = { Text(selectionOption, color = TextLight, fontSize = 15.sp) },
                      onClick = {
                        onPriorityChange(selectionOption)
                        expanded = false
                      },
                      modifier = Modifier.background(Gray))
                }
              }
        }
  }
}
