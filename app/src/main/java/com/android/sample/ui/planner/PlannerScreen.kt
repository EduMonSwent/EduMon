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
import androidx.compose.ui.res.stringResource
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
  const val SUBJECT_FIELD = "subject_field"
  const val TASK_TITLE_FIELD = "task_title_field"
  const val DURATION_FIELD = "duration_field"
  const val DEADLINE_FIELD = "deadline_field"
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
      /*floatingActionButton = {
        FloatingActionButton(
            onClick = { viewModel.onAddStudyTaskClicked() },
            containerColor = AccentViolet,
            contentColor = Color.White) {
              Icon(Icons.Filled.Add, stringResource(R.string.add_study_task))
            }
      },*/
      floatingActionButton = {
        FloatingActionButton(
            modifier = Modifier.testTag("addTaskFab"),
            onClick = { viewModel.onAddStudyTaskClicked() },
            containerColor = AccentViolet,
            contentColor = Color.White) {
              Icon(Icons.Filled.Add, contentDescription = "Add Study Task")
            }
      },
      containerColor = Color.Transparent,
      modifier =
          Modifier.background(
              Brush.verticalGradient(listOf(BackgroundDark, BackgroundGradientEnd))),
      snackbarHost = { SnackbarHost(snackbarHostState) }) { paddingValues ->
        LazyColumn(
            modifier =
                Modifier.fillMaxSize()
                    .padding(paddingValues)
                    .testTag(PlannerScreenTestTags.PLANNER_SCREEN),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
              item {
                PetHeader(
                    level = 5, // Placeholder level
                    modifier = Modifier.testTag(PlannerScreenTestTags.PET_HEADER),
                    onEdumonNameClick = { /* Navigate to pet profile / customization */})
              }

              item {
                PlannerGlowCard {
                  AIRecommendationCard(
                      recommendationText =
                          stringResource(R.string.ai_recommendation_calculus_example),
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
                            text = stringResource(R.string.today_classes_title),
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

                        if (todayClasses.isEmpty()) {
                          Text(
                              text = stringResource(R.string.no_activities_scheduled),
                              color = TextLight.copy(alpha = 0.7f),
                              modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                              textAlign = TextAlign.Center)
                        } else {
                          todayClasses.forEach { classItem ->
                            val attendanceRecord =
                                todayAttendanceRecords.find { it.classId == classItem.id }
                            ActivityItem(
                                activity = classItem,
                                attendanceRecord = attendanceRecord,
                                onClick = { viewModel.onClassClicked(classItem) })
                            Spacer(modifier = Modifier.height(8.dp))
                          }
                        }
                      }
                }
              }

              item {
                PlannerGlowCard {
                  Column(
                      modifier =
                          Modifier.fillMaxWidth()
                              .padding(16.dp)
                              .testTag(PlannerScreenTestTags.WELLNESS_CAMPUS_SECTION)) {
                        Text(
                            text = stringResource(R.string.wellness_campus_life_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextLight)
                        Text(
                            text = stringResource(R.string.wellness_campus_life_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextLight.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 8.dp))
                        Spacer(modifier = Modifier.height(8.dp))

                        WellnessEventItem(
                            title = stringResource(R.string.wellness_event_yoga_title),
                            time = stringResource(R.string.wellness_event_yoga_time),
                            description = stringResource(R.string.wellness_event_yoga_description),
                            eventType = WellnessEventType.YOGA, // Using enum
                            onClick = { /* Handle navigation to wellness details */})
                        Spacer(modifier = Modifier.height(8.dp))
                        WellnessEventItem(
                            title = stringResource(R.string.wellness_event_lecture_title),
                            time = stringResource(R.string.wellness_event_lecture_time),
                            description =
                                stringResource(R.string.wellness_event_lecture_description),
                            eventType = WellnessEventType.LECTURE, // Using enum
                            onClick = { /* Handle navigation to event details */})
                      }
                }
              }
            }

        if (showAddStudyTaskModal) {
          AddStudyTaskModal(
              onDismiss = { viewModel.onDismissAddStudyTaskModal() },
              onAddTask = { subject, title, duration, deadline, priority ->
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
    Image(
        painter = painterResource(id = R.drawable.epfl_amphi_background),
        contentDescription = stringResource(R.string.app_name), // Generic content description
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop)

    Row(
        modifier =
            Modifier.fillMaxWidth()
                .padding(vertical = 20.dp, horizontal = 16.dp)
                .align(Alignment.Center)) {
          Box(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 20.dp)) {
            Column(
                modifier = Modifier.align(Alignment.CenterStart),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.Start) {
                  StatBar(icon = "❤️", percent = 0.9f, color = StatBarHeart)
                  StatBar(icon = "💡", percent = 0.85f, color = StatBarLightbulb)
                  StatBar(icon = "⚡", percent = 0.7f, color = StatBarLightning)
                }

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
                                                  LightBlueAccent.copy(alpha = pulseAlpha * 0.6f),
                                                  Color.Transparent)),
                                  shape = RoundedCornerShape(100.dp)),
                      contentAlignment = Alignment.Center) {
                        Image(
                            painter = painterResource(id = R.drawable.edumon),
                            contentDescription = "EduMon",
                            modifier = Modifier.size(100.dp))
                      }
                  Spacer(modifier = Modifier.height(8.dp))

                  AssistChip(
                      onClick = { /* Not clickable for primary action, just info */},
                      label = { Text("Lv $level", color = AccentViolet) },
                      // label = { Text(stringResource(R.string, level), color = AccentViolet) },
                      leadingIcon = {
                        Icon(
                            Icons.Outlined.Star,
                            null,
                            tint = AccentViolet,
                            modifier = Modifier.size(16.dp))
                      },
                      colors =
                          AssistChipDefaults.assistChipColors(
                              containerColor = DarkCardItem.copy(alpha = 0.8f),
                              labelColor = AccentViolet,
                              leadingIconContentColor = AccentViolet),
                      border = BorderStroke(1.dp, AccentViolet.copy(alpha = 0.5f)),
                      modifier = Modifier.offset(y = (-5).dp))
                }
          }
        }

    Box(
        modifier =
            Modifier.align(Alignment.TopEnd)
                .padding(top = 16.dp, end = 16.dp)
                .background(DarkCardItem, RoundedCornerShape(20.dp))
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .clickable(onClick = onEdumonNameClick)
                .testTag("petNameBox")) {
          Text(
              stringResource(R.string.edumon_profile),
              color = TextLight.copy(alpha = 0.8f),
              fontSize = 13.sp)
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
                .background(
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                    RoundedCornerShape(10.dp))) { // Using theme color
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
fun AIRecommendationCard(recommendationText: String, onActionClick: () -> Unit = {}) {
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

  Box(
      modifier =
          Modifier.fillMaxWidth()
              .background(
                  brush =
                      Brush.verticalGradient(
                          colors = listOf(DarknightViolet, DarkViolet),
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
                                  LightBlueAccent.copy(alpha = 0.4f),
                                  AccentViolet.copy(alpha = 0.6f))),
                  shape = RoundedCornerShape(20.dp))
              .drawBehind {
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
              }) { // Content moved inside the Box's lambda
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
              Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)) {
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
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_sparkle),
                                    contentDescription =
                                        "AI Icon", // Consider string resource for content
                                    // description
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(24.dp).scale(1.2f))
                              }

                          Spacer(modifier = Modifier.width(12.dp))

                          Column {
                            Text(
                                text = stringResource(R.string.ai_recommendation_title),
                                color = LightBlueAccent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp)
                            Text(
                                text = stringResource(R.string.ai_recommendation_subtitle),
                                color = TextLight.copy(alpha = 0.7f),
                                fontSize = 12.sp)
                          }
                        }
                  }

              Spacer(modifier = Modifier.height(16.dp))

              Box(
                  modifier =
                      Modifier.fillMaxWidth()
                          .background(
                              color = DarkCardItem.copy(alpha = 0.8f),
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

              Box(
                  modifier =
                      Modifier.scale(buttonScale)
                          .shadow(8.dp, RoundedCornerShape(12.dp), clip = true)) {
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
                                    colors =
                                        listOf(Color.White.copy(alpha = 0.3f), Color.Transparent))),
                        contentPadding = PaddingValues(0.dp)) {
                          Row(
                              verticalAlignment = Alignment.CenterVertically,
                              horizontalArrangement = Arrangement.Center) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_play),
                                    contentDescription =
                                        stringResource(R.string.start_studying_session),
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    stringResource(R.string.start_studying_session),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp)
                              }
                        }
                  }

              Text(
                  text = stringResource(R.string.ai_recommendation_footer),
                  color = TextLight.copy(alpha = 0.5f),
                  fontSize = 10.sp,
                  modifier = Modifier.padding(top = 8.dp))
            }
      }
}

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

@Composable
fun WellnessEventItem(
    title: String,
    time: String,
    description: String,
    eventType: WellnessEventType,
    onClick: () -> Unit
) {
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

  val iconColor = eventType.primaryColor
  val containerColor = iconColor.copy(alpha = 0.1f)
  val borderColor = iconColor.copy(alpha = 0.3f)

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
                          colors = listOf(iconColor.copy(alpha = 0.3f), Color.Transparent)),
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
                                  listOf(
                                      iconColor.copy(alpha = glowAlpha * 0.8f), Color.Transparent),
                              center = center,
                              radius = radius),
                      radius = radius,
                      center = center)
                })

        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
              modifier =
                  Modifier.size(40.dp)
                      .background(color = containerColor, shape = RoundedCornerShape(10.dp))
                      .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(10.dp)),
              contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(id = eventType.iconRes), // Using enum icon
                    contentDescription = title, // Content description as title
                    tint = iconColor,
                    modifier = Modifier.size(24.dp))
              }

          Spacer(modifier = Modifier.width(12.dp))

          Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = TextLight, fontWeight = FontWeight.Bold, fontSize = 16.sp)

            Spacer(modifier = Modifier.height(2.dp))

            Text(text = time, color = iconColor, fontSize = 13.sp, fontWeight = FontWeight.Medium)

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = description,
                color = TextLight.copy(alpha = 0.7f),
                fontSize = 12.sp,
                lineHeight = 14.sp)
          }

          Icon(
              painter = painterResource(id = R.drawable.ic_arrow_right),
              contentDescription = stringResource(R.string.details),
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
            colors = CardDefaults.cardColors(containerColor = DarkViolet)) {
              Column(
                  modifier =
                      Modifier.background(
                              brush =
                                  Brush.verticalGradient(
                                      colors = listOf(DarknightViolet, DarkViolet)))
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
                                  Modifier.size(40.dp).background(DarkCardItem, CircleShape)) {
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
                      Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(DarkDivider))

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
                              border = BorderStroke(1.5.dp, DarkDivider),
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
                unfocusedBorderColor = DarkDivider,
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
                      unfocusedBorderColor = DarkDivider,
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

@Preview(showBackground = true, showSystemUi = true, name = "Planner Screen Preview")
@Composable
fun PlannerScreenPreview() {
  SampleAppTheme { PlannerScreen() }
}
