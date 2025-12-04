package com.android.sample.ui.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.android.sample.R
import com.android.sample.data.Status
import com.android.sample.repositories.ToDoRepositoryProvider
import com.android.sample.session.ToDoBackedStudySessionRepository
import com.android.sample.ui.pomodoro.PomodoroScreen
import com.android.sample.ui.pomodoro.PomodoroViewModelContract
import com.android.sample.ui.session.components.SessionStatsPanel
import com.android.sample.ui.session.components.SuggestedTasksList
import kotlinx.coroutines.launch

// Parts of this code were written using ChatGPT and AndroidStudio Gemini tool.

private const val SUBJECT_SECTION_SPACING_DP = 16
private const val SUBJECT_CHIP_SPACING_DP = 8
private const val SUBJECT_TEXT_FIELD_HEIGHT_DP = 8

// --- Test Tags for UI tests ---
object StudySessionTestTags {
  const val TITLE = "study_session_title"
  const val TASK_LIST = "study_session_task_list"
  const val SELECTED_TASK = "study_session_selected_task"
  const val TIMER_SECTION = "study_session_timer_section"
  const val STATS_PANEL = "study_session_stats_panel"
  const val SUBJECTS_SECTION = "study_session_subjects_section"
}

@Composable
fun StudySessionScreen(
    eventId: String? = null,
    viewModel: StudySessionViewModel =
        StudySessionViewModel(repository = ToDoBackedStudySessionRepository()),
    pomodoroViewModel: PomodoroViewModelContract = viewModel.pomodoroViewModel
) {
  val uiState by viewModel.uiState.collectAsState()

  // If a deep-linked eventId is provided, try to pre-select the corresponding task
  val scope = rememberCoroutineScope()
  androidx.compose.runtime.LaunchedEffect(eventId) {
    eventId?.let { id ->
      scope.launch {
        val todo = ToDoRepositoryProvider.repository.getById(id)
        todo?.let { viewModel.selectTask(it) }
      }
    }
  }

  val (newSubjectName, setNewSubjectName) = remember { mutableStateOf("") }

  Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally) {
          // --- Top Block
          Column(
              modifier = Modifier.fillMaxWidth(),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(24.dp)) {
                Text(
                    text = stringResource(R.string.study_session_title),
                    style =
                        MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.testTag(StudySessionTestTags.TITLE))

                // --- Subjects section ---
                Column(
                    modifier =
                        Modifier.fillMaxWidth().testTag(StudySessionTestTags.SUBJECTS_SECTION)) {
                      Text(
                          text = stringResource(R.string.study_session_subject_label),
                          style = MaterialTheme.typography.bodyMedium)

                      Spacer(Modifier.height(SUBJECT_SECTION_SPACING_DP.dp))

                      Row(
                          modifier = Modifier.fillMaxWidth(),
                          verticalAlignment = Alignment.CenterVertically,
                      ) {
                        OutlinedTextField(
                            value = newSubjectName,
                            onValueChange = { setNewSubjectName(it) },
                            modifier = Modifier.weight(1f),
                            label = {
                              Text(
                                  text =
                                      stringResource(
                                          R.string.study_session_new_subject_placeholder))
                            })
                        Spacer(Modifier.height(SUBJECT_TEXT_FIELD_HEIGHT_DP.dp))
                        TextButton(
                            onClick = {
                              viewModel.createSubject(newSubjectName)
                              setNewSubjectName("")
                            }) {
                              Text(text = stringResource(R.string.study_session_add_subject_button))
                            }
                      }

                      Spacer(Modifier.height(SUBJECT_SECTION_SPACING_DP.dp))

                      if (uiState.subjects.isNotEmpty()) {
                        Row(
                            horizontalArrangement =
                                Arrangement.spacedBy(SUBJECT_CHIP_SPACING_DP.dp)) {
                              uiState.subjects.forEach { subject ->
                                AssistChip(
                                    onClick = { viewModel.selectSubject(subject) },
                                    label = { Text(subject.name) },
                                    colors =
                                        AssistChipDefaults.assistChipColors(
                                            containerColor =
                                                if (uiState.selectedSubject?.id == subject.id)
                                                    MaterialTheme.colorScheme.primaryContainer
                                                else MaterialTheme.colorScheme.surfaceVariant),
                                )
                              }
                            }
                      }
                    }

                // --- Suggested Tasks List ---
                SuggestedTasksList(
                    tasks = uiState.suggestedTasks,
                    selectedTask = uiState.selectedTask,
                    onTaskSelected = { viewModel.selectTask(it) },
                    modifier = Modifier.testTag(StudySessionTestTags.TASK_LIST))

                // --- Selected Task ---
                uiState.selectedTask?.let { task ->
                  Text(
                      text = stringResource(R.string.selected_task_txt) + " " + task.title,
                      style = MaterialTheme.typography.bodyLarge,
                      modifier = Modifier.testTag(StudySessionTestTags.SELECTED_TASK))
                  Spacer(Modifier.height(8.dp))

                  Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Status.values().forEach { status ->
                      AssistChip(
                          onClick = { viewModel.setSelectedTaskStatus(status) },
                          label = { Text(status.name.replace('_', ' ')) },
                          colors =
                              AssistChipDefaults.assistChipColors(
                                  containerColor =
                                      if (task.status == status)
                                          MaterialTheme.colorScheme.primaryContainer
                                      else MaterialTheme.colorScheme.surfaceVariant))
                    }
                  }
                }
              }

          // --- Pomodoro Timer Section ---
          androidx.compose.material3.Card(
              modifier = Modifier.fillMaxWidth().testTag(StudySessionTestTags.TIMER_SECTION)) {
                PomodoroScreen(viewModel = pomodoroViewModel)
              }

          // --- Stats Panel ---
          SessionStatsPanel(
              pomodoros = uiState.completedPomodoros,
              totalMinutes = uiState.totalMinutes,
              streak = uiState.streakCount,
              modifier = Modifier.testTag(StudySessionTestTags.STATS_PANEL))
        }
  }
}
