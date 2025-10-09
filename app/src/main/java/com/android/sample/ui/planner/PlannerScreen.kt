package com.android.sample.ui.planner

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.sample.R
import com.android.sample.model.planner.*
import com.android.sample.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.max
import kotlinx.coroutines.flow.collectLatest

// Define local test tags if ProfileScreenTestTags is unavailable
object PlannerScreenTestTags {
  const val PLANNER_SCREEN = "plannerScreen"
  const val PET_HEADER = "petHeader" // Renamed from PET_SECTION to avoid conflict
  const val TODAY_CLASSES_SECTION = "todayClassesSection" // Renamed for broader scope
  const val WELLNESS_CAMPUS_SECTION = "wellnessCampusSection" // New section
  const val ADD_TASK_MODAL = "addTaskModal"
  const val CLASS_ATTENDANCE_MODAL = "classAttendanceModal"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerScreen(viewModel: PlannerViewModel = viewModel()) {
  val todayClasses by viewModel.todayClasses.collectAsState()
  val todayAttendanceRecords by viewModel.todayAttendanceRecords.collectAsState()
  val showAddStudyTaskModal by viewModel.showAddStudyTaskModal.collectAsState()
  val showClassAttendanceModal by viewModel.showClassAttendanceModal.collectAsState()
  val selectedClassForAttendance by viewModel.selectedClassForAttendance.collectAsState()
  val snackbarHostState = remember { SnackbarHostState() }

  Scaffold(
      floatingActionButton = {
        FloatingActionButton(
            onClick = { viewModel.onAddStudyTaskClicked() },
            containerColor = AccentViolet,
            contentColor = Color.White) {
              Icon(Icons.Filled.Add, "Add Study Task")
            }
      },
      containerColor = Color.Transparent, // Ensure background gradient is visible
      modifier =
          Modifier.background(Brush.verticalGradient(listOf(BackgroundDark, BackgroundGradientEnd)))
              .testTag(PlannerScreenTestTags.PLANNER_SCREEN),
      snackbarHost = { SnackbarHost(snackbarHostState) }) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
              item {
                // Your preferred Pet Header
                PetHeader(
                    level = 5, // Placeholder level
                    modifier = Modifier.testTag(PlannerScreenTestTags.PET_HEADER),
                    onEdumonNameClick = { /* Navigate to pet profile / customization */})
              }

              item {
                PlannerGlowCard {
                  AIRecommendationCard(
                      recommendationText =
                          "Based on your schedule, start with the Calculus problem set (high priority, due tomorrow). Take a 10-minute break after 45 minutes.",
                      onActionClick = { /* Handle action for AI recommendation, e.g., open task details */})
                }
              }

              item {
                PlannerGlowCard {
                  Column(
                      modifier =
                          Modifier.fillMaxWidth()
                              .padding(16.dp)
                              .testTag(PlannerScreenTestTags.TODAY_CLASSES_SECTION)) {
                        Text(
                            text = "Today's Classes", // Renamed
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextLight)
                        Text(
                            text =
                                LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMM d")),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextLight.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 8.dp))
                        Spacer(modifier = Modifier.height(8.dp))

                        if (todayClasses
                            .isEmpty()) { // Assuming todayClasses will eventually include study
                          // tasks
                          Text(
                              text = "No activities scheduled for today.",
                              color = TextLight.copy(alpha = 0.7f),
                              modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                              textAlign = TextAlign.Center)
                        } else {
                          todayClasses.forEach { classItem
                            -> // You'll need to adapt this to a generic 'Activity' type
                            val attendanceRecord =
                                todayAttendanceRecords.find { it.classId == classItem.id }
                            ActivityItem( // Renamed from ClassItem to be more generic
                                activity = classItem, // Pass a generic activity type
                                attendanceRecord = attendanceRecord,
                                onClick = {
                                  viewModel.onClassClicked(classItem)
                                } // Adapt to onActivityClicked
                                )
                            Spacer(modifier = Modifier.height(8.dp))
                          }
                        }
                      }
                }
              }

              item {
                // New Section: Wellness & Campus Life
                PlannerGlowCard {
                  Column(
                      modifier =
                          Modifier.fillMaxWidth()
                              .padding(16.dp)
                              .testTag(PlannerScreenTestTags.WELLNESS_CAMPUS_SECTION)) {
                        Text(
                            text = "Wellness & Campus Life",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextLight)
                        Text(
                            text = "Balance your studies with well-being!",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextLight.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 8.dp))
                        Spacer(modifier = Modifier.height(8.dp))

                        WellnessEventItem(
                            title = "Yoga Session @ Sports Center",
                            time = "17:00 - 18:00",
                            description = "Relax and recharge after classes. Sign up now!",
                            iconRes = R.drawable.ic_yoga,
                            onClick = { /* Handle navigation to wellness details */})
                        Spacer(modifier = Modifier.height(8.dp))
                        WellnessEventItem(
                            title = "Guest Lecture: Future of AI",
                            time = "19:30 - 21:00",
                            description = "EPFL Auditorium. Don't miss this insightful talk!",
                            iconRes = R.drawable.ic_event,
                            onClick = { /* Handle navigation to event details */})
                      }
                }
              }
            }

        if (showAddStudyTaskModal) {
          AddStudyTaskModal(
              onDismiss = { viewModel.onDismissAddStudyTaskModal() },
              onAddTask = { subject, title, duration, deadline, priority ->
                // Handle adding the task - ViewModel would typically handle this
                // TODO: Call viewModel.addStudyTask(subject, title, duration, deadline, priority)
                viewModel.onDismissAddStudyTaskModal()
              },
              modifier = Modifier.testTag(PlannerScreenTestTags.ADD_TASK_MODAL))
        }

        selectedClassForAttendance?.let { classItem ->
          if (showClassAttendanceModal) {
            val existingAttendance = todayAttendanceRecords.find { it.classId == classItem.id }
            ClassAttendanceModal(
                classItem = classItem,
                initialAttendance = existingAttendance?.attendance,
                initialCompletion = existingAttendance?.completion,
                onDismiss = { viewModel.onDismissClassAttendanceModal() },
                onSave = { attendanceStatus, completionStatus ->
                  viewModel.saveClassAttendance(classItem, attendanceStatus, completionStatus)
                },
                modifier = Modifier.testTag(PlannerScreenTestTags.CLASS_ATTENDANCE_MODAL))
          }
        }
      }
  LaunchedEffect(key1 = true) {
    viewModel.eventFlow.collectLatest { event ->
      when (event) {
        is PlannerViewModel.UiEvent.ShowSnackbar -> {
          snackbarHostState.showSnackbar(event.message)
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetHeader(level: Int, modifier: Modifier = Modifier, onEdumonNameClick: () -> Unit) {
  val pulseAlpha by
      rememberInfiniteTransition(label = "petGlow")
          .animateFloat(
              initialValue = 0.3f,
              targetValue = 0.9f,
              animationSpec =
                  infiniteRepeatable(
                      animation = tween(durationMillis = 2500, easing = LinearEasing),
                      repeatMode = RepeatMode.Reverse),
              label = "pulseAlpha")

  Box(modifier = modifier.fillMaxWidth().height(200.dp).testTag(PlannerScreenTestTags.PET_HEADER)) {
    // Background Image
    Image(
        painter = painterResource(id = R.drawable.epfl_amphi_background),
        contentDescription = "EPFL Amphitheater Background",
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop)

    // Overlay content for pet, stats, and profile button
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .padding(vertical = 20.dp, horizontal = 16.dp)
                .align(Alignment.Center)) {
          Box(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 20.dp)) {
            // Stats (Left aligned)
            Column(
                modifier = Modifier.align(Alignment.CenterStart), // Give stats a consistent width
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.Start // Align stats to the start
                ) {
                  StatBar(icon = "❤️", percent = 0.9f, color = Color(0xFFFF69B4))
                  StatBar(icon = "💡", percent = 0.85f, color = Color(0xFFFFC107))
                  StatBar(icon = "⚡", percent = 0.7f, color = Color(0xFF03A9F4))
                }

            // Pet (More centrally aligned, slightly left)
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center) {
                  Box(
                      modifier =
                          Modifier.size(130.dp)
                              .background(
                                  brush =
                                      Brush.radialGradient(
                                          colors =
                                              listOf(
                                                  Color(0xFF5CE1E6).copy(alpha = pulseAlpha * 0.6f),
                                                  Color.Transparent)),
                                  shape = RoundedCornerShape(100.dp)),
                      contentAlignment = Alignment.Center) {
                        Image(
                            painter = painterResource(id = R.drawable.edumon),
                            contentDescription = "EduMon",
                            modifier = Modifier.size(100.dp))
                      }
                  Spacer(modifier = Modifier.height(8.dp))

                  // Level Chip (positioned more like CreatureHouseCard's level)
                  AssistChip(
                      onClick = { /* Not clickable for primary action, just info */},
                      label = { Text("Lv $level", color = AccentViolet) },
                      leadingIcon = {
                        Icon(
                            Icons.Outlined.Star,
                            null,
                            tint = AccentViolet,
                            modifier = Modifier.size(16.dp) // Adjust icon size
                            )
                      },
                      colors =
                          AssistChipDefaults.assistChipColors(
                              containerColor =
                                  DarkCardItem.copy(alpha = 0.8f), // Use a dark card color
                              labelColor = AccentViolet,
                              leadingIconContentColor = AccentViolet),
                      border = BorderStroke(1.dp, AccentViolet.copy(alpha = 0.5f)),
                      modifier = Modifier.offset(y = (-5).dp) // Slightly lift it
                      )
                }
          }
        }

    // Edumon Profile (Top Right)
    Box(
        modifier =
            Modifier.align(Alignment.TopEnd) // Align to top end
                .padding(top = 16.dp, end = 16.dp) // Adjust padding
                .background(Color(0xFF1A1B2E), RoundedCornerShape(20.dp))
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .clickable(onClick = onEdumonNameClick)) {
          Text("Edumon Profile", color = TextLight.copy(alpha = 0.8f), fontSize = 13.sp)
        }
  }
}

@Composable
fun StatBar(icon: String, percent: Float, color: Color) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Text(icon, fontSize = 16.sp)
    Spacer(modifier = Modifier.width(4.dp))
    Box(
        modifier =
            Modifier.width(70.dp)
                .height(10.dp)
                .background(Color(0xFF202233), RoundedCornerShape(10.dp))) {
          Box(
              modifier =
                  Modifier.fillMaxHeight()
                      .fillMaxWidth(percent)
                      .background(color, RoundedCornerShape(10.dp)))
        }
    Spacer(modifier = Modifier.width(4.dp))
    Text("${(percent * 100).toInt()}%", color = TextLight.copy(alpha = 0.8f), fontSize = 12.sp)
  }
}

// Local GlowCard equivalent for PlannerScreen
@Composable
fun PlannerGlowCard(content: @Composable () -> Unit) {
  val infiniteTransition = rememberInfiniteTransition(label = "glowAnim")
  val glowAlpha by
      infiniteTransition.animateFloat(
          initialValue = 0.25f,
          targetValue = 0.6f,
          animationSpec =
              infiniteRepeatable(
                  animation = tween(durationMillis = 2500, easing = LinearEasing),
                  repeatMode = RepeatMode.Reverse),
          label = "glowAlpha")

  Card(
      modifier = Modifier.fillMaxWidth(0.9f).shadow(16.dp, RoundedCornerShape(16.dp)),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MidDarkCard)) {
        Box(
            modifier =
                Modifier.background(
                    Brush.radialGradient(
                        colors =
                            listOf(AccentViolet.copy(alpha = glowAlpha), Color.Transparent)))) {
              content()
            }
      }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIRecommendationCard(
    recommendationText: String =
        "Based on your schedule, start with the Calculus problem set (high priority, due tomorrow). Take a 10-minute break after 45 minutes.",
    onActionClick: () -> Unit = {}
) {
  val infiniteTransition = rememberInfiniteTransition(label = "infiniteTransition")

  val buttonScale by
      infiniteTransition.animateFloat(
          initialValue = 1f,
          targetValue = 1.02f,
          animationSpec =
              infiniteRepeatable(
                  animation = tween(durationMillis = 2000, easing = LinearEasing),
                  repeatMode = RepeatMode.Reverse),
          label = "buttonScale")

  val glowAlpha by
      infiniteTransition.animateFloat(
          initialValue = 0.3f,
          targetValue = 0.6f,
          animationSpec =
              infiniteRepeatable(
                  animation = tween(durationMillis = 3000, easing = LinearEasing),
                  repeatMode = RepeatMode.Reverse),
          label = "glowAlpha")

  // Background with gradient and glow
  Box(
      modifier =
          Modifier.fillMaxWidth()
              .background(
                  brush =
                      Brush.verticalGradient(
                          colors =
                              listOf(
                                  Color(0xFF2A1B3D), // Deep purple
                                  Color(0xFF1F1B2D) // Dark blue
                                  ),
                          startY = 0f,
                          endY = Float.POSITIVE_INFINITY),
                  shape = RoundedCornerShape(20.dp))
              .border(
                  width = 1.dp,
                  brush =
                      Brush.horizontalGradient(
                          colors =
                              listOf(
                                  AccentViolet.copy(alpha = 0.6f),
                                  Color(0xFF82B1FF).copy(alpha = 0.4f),
                                  AccentViolet.copy(alpha = 0.6f))),
                  shape = RoundedCornerShape(20.dp))
              .drawBehind {
                // Outer glow effect
                drawCircle(
                    brush =
                        Brush.radialGradient(
                            colors =
                                listOf(
                                    AccentViolet.copy(alpha = glowAlpha * 0.5f), Color.Transparent),
                            center = center.copy(x = size.width * 0.8f),
                            radius = size.height * 1.5f),
                    center = center.copy(x = size.width * 0.8f),
                    radius = size.height * 1.5f)
              })

  Column(
      modifier = Modifier.fillMaxWidth().padding(20.dp),
      horizontalAlignment = Alignment.CenterHorizontally) {
        // Header with AI icon and title
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
              Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                // Animated AI icon container
                Box(
                    modifier =
                        Modifier.size(44.dp)
                            .background(
                                brush =
                                    Brush.radialGradient(
                                        colors =
                                            listOf(
                                                AccentViolet.copy(alpha = 0.3f),
                                                Color.Transparent)),
                                shape = CircleShape)
                            .border(
                                width = 1.dp,
                                brush =
                                    Brush.radialGradient(
                                        colors =
                                            listOf(
                                                AccentViolet.copy(alpha = 0.6f),
                                                Color.Transparent)),
                                shape = CircleShape),
                    contentAlignment = Alignment.Center) {
                      // Placeholder for AI icon - you might replace this with an actual icon
                      Icon(
                          painter =
                              painterResource(
                                  id = R.drawable.ic_sparkle), // Using sparkle as a placeholder
                          contentDescription = "AI Icon",
                          tint = Color.White.copy(alpha = 0.8f),
                          modifier =
                              Modifier.size(24.dp).scale(1.2f) // Slightly larger to fill the circle
                          )
                    }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                  Text(
                      text = "AI Study Assistant",
                      color = Color(0xFF82B1FF), // Light blue
                      fontWeight = FontWeight.Bold,
                      fontSize = 16.sp)
                  Text(
                      text = "Personalized Recommendation",
                      color = TextLight.copy(alpha = 0.7f),
                      fontSize = 12.sp)
                }
              }
            }

        Spacer(modifier = Modifier.height(16.dp))

        // Recommendation text in a speech bubble style
        Box(
            modifier =
                Modifier.fillMaxWidth()
                    .background(
                        color = Color(0xFF2D2B42).copy(alpha = 0.8f),
                        shape = RoundedCornerShape(16.dp))
                    .padding(16.dp)) {
              Text(
                  text = "💡 $recommendationText",
                  color = TextLight.copy(alpha = 0.9f),
                  fontSize = 14.sp,
                  lineHeight = 18.sp,
                  modifier = Modifier.fillMaxWidth())
            }

        Spacer(modifier = Modifier.height(20.dp))

        // Animated action button
        Box(
            modifier =
                Modifier.scale(buttonScale).shadow(8.dp, RoundedCornerShape(12.dp), clip = true)) {
              Button(
                  onClick = onActionClick,
                  modifier = Modifier.fillMaxWidth(0.9f).height(44.dp),
                  shape = RoundedCornerShape(12.dp),
                  colors =
                      ButtonDefaults.buttonColors(
                          containerColor = AccentViolet, contentColor = Color.White),
                  border =
                      BorderStroke(
                          1.dp,
                          Brush.verticalGradient(
                              colors = listOf(Color.White.copy(alpha = 0.3f), Color.Transparent))),
                  contentPadding = PaddingValues(0.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center) {
                          Icon(
                              painter =
                                  painterResource(id = R.drawable.ic_play), // Play or start icon
                              contentDescription = "Start",
                              tint = Color.White,
                              modifier = Modifier.size(18.dp))
                          Spacer(modifier = Modifier.width(8.dp))
                          Text(
                              "Start Studying Session",
                              color = Color.White,
                              fontWeight = FontWeight.Bold,
                              fontSize = 14.sp)
                        }
                  }
            }

        // Footer note
        Text(
            text = "Based on your schedule and performance patterns",
            color = TextLight.copy(alpha = 0.5f),
            fontSize = 10.sp,
            modifier = Modifier.padding(top = 8.dp))
      }
}

@Composable
fun ActivityItem(activity: Class, attendanceRecord: ClassAttendance?, onClick: () -> Unit) {
  // Animated subtle inner glow
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
                              Color(0xFF24223B).copy(alpha = 0.95f),
                              Color(0xFF1C1A2B).copy(alpha = 0.98f))))
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
                  // compute center & radius from the actual size
                  val center = Offset(size.width / 2f, size.height / 2f)
                  val radius = max(size.width, size.height) * 0.7f

                  // draw a radial glow centered on the box
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
          // --- Header (icon + title)
          Row(verticalAlignment = Alignment.CenterVertically) {
            val iconColor =
                when (activity.type) {
                  ClassType.LECTURE -> Color(0xFF82B1FF)
                  ClassType.EXERCISE -> Color(0xFF69F0AE)
                  ClassType.LAB -> Color(0xFFB388FF)
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
                contentDescription = activity.type.name,
                tint = iconColor.copy(alpha = 0.9f),
                modifier = Modifier.size(28.dp).shadow(6.dp, shape = CircleShape))

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text =
                    "${activity.courseName} (${activity.type.name.lowercase().replaceFirstChar { it.uppercase() }})",
                color = TextLight,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp)
          }

          Spacer(modifier = Modifier.height(6.dp))

          // --- Time and details
          Text(
              text =
                  "${activity.startTime.format(DateTimeFormatter.ofPattern("HH:mm"))} - ${activity.endTime.format(DateTimeFormatter.ofPattern("HH:mm"))}",
              color = TextLight.copy(alpha = 0.75f),
              fontSize = 13.sp)
          Text(
              text = "${activity.location} • ${activity.instructor}",
              color = TextLight.copy(alpha = 0.7f),
              fontSize = 12.sp)

          // --- Attendance info
          attendanceRecord?.let {
            Spacer(modifier = Modifier.height(10.dp))
            Divider(
                color = Color.White.copy(alpha = 0.08f),
                thickness = 0.5.dp,
                modifier = Modifier.padding(vertical = 4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_check_circle),
                        contentDescription = "Attended",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text =
                            "Attended: ${it.attendance.name.replace("_", " ").lowercase().replaceFirstChar { c -> c.uppercase() }}",
                        color = Color(0xFF4CAF50),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium)
                  }

                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_done_all),
                        contentDescription = "Completed",
                        tint = Color(0xFF2196F3),
                        modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text =
                            "Completed: ${it.completion.name.lowercase().replaceFirstChar { c -> c.uppercase() }}",
                        color = Color(0xFF2196F3),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium)
                  }
                }
          }
        }
      }
}

// New Composable for Wellness/Event items
@Composable
fun WellnessEventItem(
    title: String,
    time: String,
    description: String,
    iconRes: Int,
    onClick: () -> Unit
) {
  // Animated subtle inner glow
  val infiniteTransition = rememberInfiniteTransition(label = "wellnessGlowAnim")
  val glowAlpha by
      infiniteTransition.animateFloat(
          initialValue = 0.15f,
          targetValue = 0.35f,
          animationSpec =
              infiniteRepeatable(
                  animation = tween(durationMillis = 2800, easing = LinearEasing),
                  repeatMode = RepeatMode.Reverse),
          label = "wellnessGlowAlpha")
  val (iconColor, containerColor, borderColor) =
      when {
        title.contains("Yoga", ignoreCase = true) ->
            Triple(
                Color(0xFF4CAF50), // Green for yoga/meditation
                Color(0xFF4CAF50).copy(alpha = 0.1f),
                Color(0xFF4CAF50).copy(alpha = 0.3f))
        title.contains("Lecture", ignoreCase = true) || title.contains("Talk", ignoreCase = true) ->
            Triple(
                Color(0xFF2196F3), // Blue for educational events
                Color(0xFF2196F3).copy(alpha = 0.1f),
                Color(0xFF2196F3).copy(alpha = 0.3f))
        title.contains("Sports", ignoreCase = true) ||
            title.contains("Fitness", ignoreCase = true) ->
            Triple(
                Color(0xFFFF9800), // Orange for sports
                Color(0xFFFF9800).copy(alpha = 0.1f),
                Color(0xFFFF9800).copy(alpha = 0.3f))
        title.contains("Social", ignoreCase = true) || title.contains("Party", ignoreCase = true) ->
            Triple(
                Color(0xFFE91E63), // Pink for social events
                Color(0xFFE91E63).copy(alpha = 0.1f),
                Color(0xFFE91E63).copy(alpha = 0.3f))
        title.contains("Music", ignoreCase = true) ||
            title.contains("Concert", ignoreCase = true) ->
            Triple(
                Color(0xFF9C27B0), // Purple for music/arts
                Color(0xFF9C27B0).copy(alpha = 0.1f),
                Color(0xFF9C27B0).copy(alpha = 0.3f))
        else ->
            Triple(
                Color(0xFF00BCD4), // Cyan as default
                Color(0xFF00BCD4).copy(alpha = 0.1f),
                Color(0xFF00BCD4).copy(alpha = 0.3f))
      }

  Box(
      modifier =
          Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(14.dp))
              .background(
                  Brush.verticalGradient(
                      colors =
                          listOf(
                              Color(0xFF24223B).copy(alpha = 0.95f),
                              Color(0xFF1C1A2B).copy(alpha = 0.98f))))
              .border(
                  width = 1.dp,
                  brush =
                      Brush.verticalGradient(
                          colors = listOf(iconColor.copy(alpha = 0.3f), Color.Transparent)),
                  shape = RoundedCornerShape(14.dp))
              .shadow(elevation = 4.dp, shape = RoundedCornerShape(14.dp))
              .clickable(onClick = onClick)
              .padding(14.dp)) {
        Box(
            modifier =
                Modifier.matchParentSize().drawBehind {
                  // compute center & radius from the actual size
                  val center = Offset(size.width / 2f, size.height / 2f)
                  val radius = max(size.width, size.height) * 0.7f

                  // draw a radial glow centered on the box
                  drawCircle(
                      brush =
                          Brush.radialGradient(
                              colors =
                                  listOf(
                                      iconColor.copy(alpha = glowAlpha * 0.8f), Color.Transparent),
                              center = center,
                              radius = radius),
                      radius = radius,
                      center = center)
                })

        Row(verticalAlignment = Alignment.CenterVertically) {
          // Yoga/Wellness Icon with proper styling
          Box(
              modifier =
                  Modifier.size(40.dp)
                      .background(color = containerColor, shape = RoundedCornerShape(10.dp))
                      .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(10.dp)),
              contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = title,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp))
              }

          Spacer(modifier = Modifier.width(12.dp))

          // Content
          Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = TextLight, fontWeight = FontWeight.Bold, fontSize = 16.sp)

            Spacer(modifier = Modifier.height(2.dp))

            // Time with accent color
            Text(text = time, color = iconColor, fontSize = 13.sp, fontWeight = FontWeight.Medium)

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = description,
                color = TextLight.copy(alpha = 0.7f),
                fontSize = 12.sp,
                lineHeight = 14.sp)
          }

          // Arrow indicator
          Icon(
              painter = painterResource(id = R.drawable.ic_arrow_right),
              contentDescription = "Details",
              tint = TextLight.copy(alpha = 0.5f),
              modifier = Modifier.size(20.dp))
        }
      }
}

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
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1B2D))) {
              Column(
                  modifier =
                      Modifier.background(
                              brush =
                                  Brush.verticalGradient(
                                      colors = listOf(Color(0xFF2A1B3D), Color(0xFF1F1B2D))))
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
                                  Modifier.size(40.dp).background(Color(0xFF2F2F45), CircleShape)) {
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
                          onValueChange = { subject = it })

                      // Task Title Field
                      FormFieldSection(
                          label = "Task Title *",
                          placeholder = "e.g., Complete homework exercises",
                          value = taskTitle,
                          onValueChange = { taskTitle = it })

                      // Duration Field
                      FormFieldSection(
                          label = "Duration (minutes) *",
                          placeholder = "60",
                          value = duration,
                          onValueChange = { duration = it },
                          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))

                      // Divider
                      Box(
                          modifier =
                              Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF3A3750)))

                      // Deadline Field
                      FormFieldSection(
                          label = "Deadline",
                          placeholder = "dd.mm.yyyy",
                          value = deadline,
                          onValueChange = { deadline = it })

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
                              border = BorderStroke(1.5.dp, Color(0xFF3A3750)),
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
                              modifier = Modifier.weight(1f).height(52.dp),
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
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
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
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = keyboardOptions,
        colors =
            OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AccentViolet,
                unfocusedBorderColor = Color(0xFF3A3750),
                focusedTextColor = TextLight,
                unfocusedTextColor = TextLight,
                focusedLabelColor = Color.Transparent,
                unfocusedLabelColor = Color.Transparent,
                focusedContainerColor = Color(0xFF2D2B42).copy(alpha = 0.6f),
                unfocusedContainerColor = Color(0xFF2D2B42).copy(alpha = 0.4f),
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
                      unfocusedBorderColor = Color(0xFF3A3750),
                      focusedTextColor = TextLight,
                      unfocusedTextColor = TextLight,
                      focusedContainerColor = Color(0xFF2D2B42).copy(alpha = 0.6f),
                      unfocusedContainerColor = Color(0xFF2D2B42).copy(alpha = 0.4f),
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
              modifier = Modifier.background(Color(0xFF2D2B42))) {
                listOf("Low", "Medium", "High").forEach { selectionOption ->
                  DropdownMenuItem(
                      text = { Text(selectionOption, color = TextLight, fontSize = 15.sp) },
                      onClick = {
                        onPriorityChange(selectionOption)
                        expanded = false
                      },
                      modifier = Modifier.background(Color(0xFF2D2B42)))
                }
              }
        }
  }
}

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
                  Color(0xFF2A1B3D).copy(alpha = 0.95f) // Slightly darker purple
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
          targetValue = if (isSelected) AccentViolet.copy(alpha = 0.9f) else Color(0xFF2F2F45),
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

@Preview(showBackground = true, showSystemUi = true, name = "Planner Screen Preview")
@Composable
fun PlannerScreenPreview() {
  SampleAppTheme { PlannerScreen() }
}
