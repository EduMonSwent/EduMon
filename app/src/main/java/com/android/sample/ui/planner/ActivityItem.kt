package com.android.sample.ui.planner

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.sample.R
import com.android.sample.model.planner.Class
import com.android.sample.model.planner.ClassAttendance
import com.android.sample.model.planner.ClassType
import com.android.sample.ui.theme.AccentViolet
import com.android.sample.ui.theme.CustomBlue
import com.android.sample.ui.theme.CustomGreen
import com.android.sample.ui.theme.DarkCardItem
import com.android.sample.ui.theme.LightBlueAccent
import com.android.sample.ui.theme.MidDarkCard
import com.android.sample.ui.theme.TextLight
import java.time.format.DateTimeFormatter
import kotlin.math.max

@Composable
fun ActivityItem(activity: Class, attendanceRecord: ClassAttendance?, onClick: () -> Unit) {
  val infiniteTransition = rememberInfiniteTransition(label = "activityGlowAnim")
  val glowAlpha by
      infiniteTransition.animateFloat(
          initialValue = 0.15f,
          targetValue = 0.35f,
          animationSpec =
              infiniteRepeatable(
                  animation = tween(durationMillis = 2800, easing = LinearEasing),
                  repeatMode = RepeatMode.Reverse),
          label = "activityGlowAlpha")

  Box(
      modifier =
          Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(14.dp))
              .background(
                  Brush.verticalGradient(
                      colors =
                          listOf(
                              MidDarkCard.copy(alpha = 0.95f), DarkCardItem.copy(alpha = 0.98f))))
              .border(
                  width = 1.dp,
                  brush =
                      Brush.verticalGradient(
                          colors = listOf(AccentViolet.copy(alpha = 0.3f), Color.Transparent)),
                  shape = RoundedCornerShape(14.dp))
              .shadow(elevation = 4.dp, shape = RoundedCornerShape(14.dp))
              .clickable(onClick = onClick)
              .padding(14.dp)) {
        Box(
            modifier =
                Modifier.matchParentSize().drawBehind {
                  val center = Offset(size.width / 2f, size.height / 2f)
                  val radius = max(size.width, size.height) * 0.7f

                  drawCircle(
                      brush =
                          Brush.radialGradient(
                              colors =
                                  listOf(AccentViolet.copy(alpha = glowAlpha), Color.Transparent),
                              center = center,
                              radius = radius),
                      radius = radius,
                      center = center)
                })

        Column {
          Row(verticalAlignment = Alignment.CenterVertically) {
            val iconColor =
                when (activity.type) {
                  ClassType.LECTURE -> LightBlueAccent
                  ClassType.EXERCISE -> CustomGreen
                  ClassType.LAB -> AccentViolet
                }

            val classTypeText =
                when (activity.type) {
                  ClassType.LECTURE -> stringResource(R.string.lecture_type)
                  ClassType.EXERCISE -> stringResource(R.string.exercise_type)
                  ClassType.LAB -> stringResource(R.string.lab_type)
                }

            Icon(
                painter =
                    painterResource(
                        id =
                            when (activity.type) {
                              ClassType.LECTURE -> R.drawable.ic_lecture
                              ClassType.EXERCISE -> R.drawable.ic_exercise
                              ClassType.LAB -> R.drawable.ic_lab
                            }),
                contentDescription = classTypeText, // Content description from string resource
                tint = iconColor.copy(alpha = 0.9f),
                modifier = Modifier.size(28.dp).shadow(6.dp, shape = CircleShape))

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = "${activity.courseName} (${classTypeText})",
                color = TextLight,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp)
          }

          Spacer(modifier = Modifier.height(6.dp))

          Text(
              text =
                  "${activity.startTime.format(DateTimeFormatter.ofPattern("HH:mm"))} - ${activity.endTime.format(DateTimeFormatter.ofPattern("HH:mm"))}",
              color = TextLight.copy(alpha = 0.75f),
              fontSize = 13.sp)
          Text(
              text = "${activity.location} • ${activity.instructor}",
              color = TextLight.copy(alpha = 0.7f),
              fontSize = 12.sp)

          attendanceRecord?.let {
            Spacer(modifier = Modifier.height(10.dp))
            Divider(
                color =
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), // Using theme color
                thickness = 0.5.dp,
                modifier = Modifier.padding(vertical = 4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_check_circle),
                        contentDescription =
                            stringResource(
                                R.string.attended_status, ""), // Content desc for attended
                        tint = CustomGreen, // Custom green color
                        modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text =
                            stringResource(
                                R.string.attended_status,
                                it.attendance.name.replace("_", " ").lowercase().replaceFirstChar {
                                    c ->
                                  c.uppercase()
                                }),
                        color = CustomGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium)
                  }

                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_done_all),
                        contentDescription =
                            stringResource(
                                R.string.completed_status, ""), // Content desc for completed
                        tint = CustomBlue, // Custom blue color
                        modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text =
                            stringResource(
                                R.string.completed_status,
                                it.completion.name.lowercase().replaceFirstChar { c ->
                                  c.uppercase()
                                }),
                        color = CustomBlue,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium)
                  }
                }
          }
        }
      }
}
