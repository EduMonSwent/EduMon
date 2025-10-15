package com.android.sample.ui.planner

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.sample.R
import com.android.sample.model.planner.AttendanceStatus
import com.android.sample.model.planner.Class
import com.android.sample.model.planner.ClassType
import com.android.sample.model.planner.CompletionStatus
import com.android.sample.ui.theme.AccentViolet
import com.android.sample.ui.theme.DarkCardItem
import com.android.sample.ui.theme.DarknightViolet
import com.android.sample.ui.theme.LightBlueAccent
import com.android.sample.ui.theme.MidDarkCard
import com.android.sample.ui.theme.TextLight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassAttendanceModal(
    classItem: Class,
    initialAttendance: AttendanceStatus?,
    initialCompletion: CompletionStatus?,
    onDismiss: () -> Unit,
    onSave: (attendance: AttendanceStatus, completion: CompletionStatus) -> Unit,
    modifier: Modifier = Modifier
) {
  var attendanceStatus by remember { mutableStateOf(initialAttendance) }
  var completionStatus by remember { mutableStateOf(initialCompletion) }

  // Define modal background gradient
  val modalBackgroundBrush =
      Brush.verticalGradient(
          colors =
              listOf(
                  MidDarkCard.copy(alpha = 0.95f),
                  DarknightViolet.copy(alpha = 0.95f) // Slightly darker purple
                  ))

  AlertDialog(
      onDismissRequest = onDismiss,
      containerColor = Color.Transparent, // Make container transparent to use our custom background
      shape = RoundedCornerShape(16.dp),
      modifier =
          modifier
              .padding(16.dp) // Add padding around the whole dialog
              .background(modalBackgroundBrush, RoundedCornerShape(16.dp))
              .border( // Subtle border for definition
                  width = 1.dp,
                  brush =
                      Brush.verticalGradient(
                          colors =
                              listOf(
                                  AccentViolet.copy(alpha = 0.4f),
                                  LightBlueAccent.copy(alpha = 0.2f))),
                  shape = RoundedCornerShape(16.dp)),
      title = {
        Row(
            modifier =
                Modifier.fillMaxWidth().padding(top = 8.dp), // Add padding to the top of the title
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
              Text(
                  text =
                      "${classItem.courseName} (${classItem.type.name.lowercase().replaceFirstChar { it.uppercase() }})",
                  color = LightBlueAccent, // Use accent color for title
                  fontWeight = FontWeight.ExtraBold,
                  fontSize = 20.sp // Slightly larger title
                  )
              IconButton(onClick = onDismiss) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_close), // Ensure ic_close exists
                    contentDescription = "Close",
                    tint = TextLight.copy(alpha = 0.8f),
                    modifier = Modifier.size(28.dp) // Larger close icon
                    )
              }
            }
      },
      text = {
        Column(modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)) {
          Text(
              text = "Confirm your attendance & completion",
              color = TextLight.copy(alpha = 0.8f),
              fontSize = 14.sp,
              modifier = Modifier.padding(bottom = 20.dp) // More space after subtitle
              )

          // Attendance Section
          Text(
              text = "Did you attend this class?",
              color = TextLight,
              fontWeight = FontWeight.SemiBold,
              fontSize = 15.sp,
              modifier = Modifier.padding(bottom = 12.dp))
          Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp) // Space between buttons
              ) {
                AttendanceStatus.values().forEach { status ->
                  ChoiceButton(
                      text =
                          status.name.replace("_", " ").lowercase().replaceFirstChar {
                            it.uppercase()
                          },
                      isSelected = attendanceStatus == status,
                      onClick = { attendanceStatus = status },
                      modifier = Modifier.weight(1f) // Distribute width evenly
                      )
                }
              }

          Spacer(modifier = Modifier.height(24.dp)) // More vertical space

          // Completion Section
          Text(
              text =
                  when (classItem.type) {
                    ClassType.LECTURE -> "Did you finish reviewing today’s lecture materials?"
                    ClassType.EXERCISE -> "Did you finish the exercise?"
                    ClassType.LAB -> "Did you finish the lab?"
                  },
              color = TextLight,
              fontWeight = FontWeight.SemiBold,
              fontSize = 15.sp,
              modifier = Modifier.padding(bottom = 12.dp))
          Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp) // Space between buttons
              ) {
                CompletionStatus.values().forEach { status ->
                  ChoiceButton(
                      text = status.name.lowercase().replaceFirstChar { it.uppercase() },
                      isSelected = completionStatus == status,
                      onClick = { completionStatus = status },
                      modifier = Modifier.weight(1f) // Distribute width evenly
                      )
                }
              }
        }
      },
      confirmButton = {
        Button(
            onClick = {
              val finalAttendance =
                  attendanceStatus ?: AttendanceStatus.NO // Default to NO if not selected
              val finalCompletion =
                  completionStatus ?: CompletionStatus.NO // Default to NO if not selected
              onSave(finalAttendance, finalCompletion)
            },
            modifier =
                Modifier.fillMaxWidth()
                    .height(50.dp) // Slightly taller button
                    .padding(horizontal = 8.dp) // Padding for button within dialog
                    .shadow(6.dp, RoundedCornerShape(12.dp), clip = false), // Subtle shadow
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentViolet),
            contentPadding = PaddingValues(0.dp)) {
              Text("Save", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
      },
      dismissButton = {
        OutlinedButton(
            onClick = onDismiss,
            modifier =
                Modifier.fillMaxWidth()
                    .height(50.dp) // Slightly taller button
                    .padding(horizontal = 8.dp) // Padding for button within dialog
                    .padding(bottom = 8.dp), // Padding from the bottom of the dialog
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, TextLight.copy(alpha = 0.3f)), // Lighter border
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextLight)) {
              Text(
                  "Cancel",
                  color = TextLight.copy(alpha = 0.7f),
                  fontWeight = FontWeight.SemiBold,
                  fontSize = 16.sp)
            }
      })
}

@Composable
fun ChoiceButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
  val backgroundColor by
      animateColorAsState(
          targetValue = if (isSelected) AccentViolet.copy(alpha = 0.9f) else DarkCardItem,
          animationSpec = tween(durationMillis = 200),
          label = "choiceButtonBgColor")
  val textColor by
      animateColorAsState(
          targetValue = if (isSelected) Color.White else TextLight.copy(alpha = 0.8f),
          animationSpec = tween(durationMillis = 200),
          label = "choiceButtonTextColor")

  Button(
      onClick = onClick,
      colors = ButtonDefaults.buttonColors(containerColor = backgroundColor),
      shape = RoundedCornerShape(10.dp), // Slightly more rounded
      modifier =
          modifier
              .height(48.dp) // Consistent button height
              .shadow(
                  elevation = if (isSelected) 6.dp else 2.dp, // Dynamic shadow for selected state
                  shape = RoundedCornerShape(10.dp),
                  clip = false),
      contentPadding = PaddingValues(vertical = 12.dp, horizontal = 8.dp)) {
        Text(text, color = textColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
      }
}
