package com.android.sample.ui.session

import FakeStudySessionRepository
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.android.sample.R
import com.android.sample.ui.pomodoro.PomodoroScreen
import com.android.sample.ui.pomodoro.PomodoroViewModelContract
import com.android.sample.ui.session.components.SessionStatsPanel
import com.android.sample.ui.session.components.SuggestedTasksList

// Parts of this code were written using ChatGPT and AndroidStudio Gemini tool.

// --- Test Tags for UI tests ---
object StudySessionTestTags {
  const val TITLE = "study_session_title"
  const val TASK_LIST = "study_session_task_list"
  const val SELECTED_TASK = "study_session_selected_task"
  const val TIMER_SECTION = "study_session_timer_section"
  const val STATS_PANEL = "study_session_stats_panel"
}

@Composable
fun StudySessionScreen(
    viewModel: StudySessionViewModel =
        StudySessionViewModel(repository = FakeStudySessionRepository()),
    pomodoroViewModel: PomodoroViewModelContract = viewModel.pomodoroViewModel
) {
  val uiState by viewModel.uiState.collectAsState()

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

                // --- Suggested Tasks List ---
                SuggestedTasksList(
                    tasks = uiState.suggestedTasks,
                    selectedTask = uiState.selectedTask,
                    onTaskSelected = { viewModel.selectTask(it) },
                    modifier = Modifier.testTag(StudySessionTestTags.TASK_LIST))

                // --- Selected Task ---
                uiState.selectedTask?.let { task ->
                  Text(
                      text = stringResource(R.string.selected_task_txt) + " " + task.name,
                      style = MaterialTheme.typography.bodyLarge,
                      modifier = Modifier.testTag(StudySessionTestTags.SELECTED_TASK))
                }
              }

          // --- Pomodoro Timer Section ---
          Card(modifier = Modifier.fillMaxWidth().testTag(StudySessionTestTags.TIMER_SECTION)) {
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
