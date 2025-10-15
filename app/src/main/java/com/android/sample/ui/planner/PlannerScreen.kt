package com.android.sample.ui.planner

import androidx.compose.animation.core.*
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.sample.R
import com.android.sample.model.planner.*
import com.android.sample.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.collectLatest

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
