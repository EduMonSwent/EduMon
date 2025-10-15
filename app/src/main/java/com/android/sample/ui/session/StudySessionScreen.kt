package com.android.sample.ui.session

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.sample.ui.pomodoro.PomodoroScreen
import com.android.sample.ui.pomodoro.PomodoroViewModel
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
    viewModel: StudySessionViewModel = viewModel(),
    pomodoroViewModel: PomodoroViewModel = viewModel()
) {
  val uiState by viewModel.uiState.collectAsState()

  Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
          Text(
              text = "Study Session",
              style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
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
                text = "Selected: ${task.name}",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.testTag(StudySessionTestTags.SELECTED_TASK))
          }
          Spacer(modifier = Modifier.weight(1f))

          // --- Pomodoro Timer Section ---
          Box(modifier = Modifier.fillMaxWidth().testTag(StudySessionTestTags.TIMER_SECTION)) {
            PomodoroScreen(viewModel = pomodoroViewModel)
          }

          Spacer(modifier = Modifier.weight(1f))

          // --- Stats Panel ---
          SessionStatsPanel(
              pomodoros = uiState.completedPomodoros,
              totalMinutes = uiState.totalMinutes,
              streak = uiState.streakCount,
              modifier = Modifier.testTag(StudySessionTestTags.STATS_PANEL))
        }
  }
}
