package com.android.sample.ui.pomodoro

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.sample.ui.theme.SampleAppTheme

object PomodoroScreenTestTags {
  const val TIMER = "timer"
  const val TITLE = "title"
  const val START_BUTTON = "start_button"
  const val PAUSE_BUTTON = "pause_button"
  const val RESET_BUTTON = "reset_button"
    const val RESUME_BUTTON = "resume_button"
  const val SKIP_BUTTON = "skip_button"
    const val NEXT_PHASE_BUTTON = "next_phase_button"
  const val PHASE_TEXT = "phase_text"
  const val CYCLE_COUNT = "cycle_count"
}

@Composable
fun PomodoroScreen(viewModel: PomodoroViewModelContract = viewModel<PomodoroViewModel>()) {
  val timeLeft by viewModel.timeLeft.collectAsState()
  val phase by viewModel.phase.collectAsState()
  val state by viewModel.state.collectAsState()
  val cycleCount by viewModel.cycleCount.collectAsState()

  val minutes = timeLeft / 60
  val seconds = timeLeft % 60
  val timeText = String.format("%02d:%02d", minutes, seconds)

  val phaseText =
      when (phase) {
        PomodoroPhase.WORK -> "Work"
        PomodoroPhase.SHORT_BREAK -> "Short Break"
        PomodoroPhase.LONG_BREAK -> "Long Break"
      }

  val backgroundColor =
      when (phase) {
        PomodoroPhase.WORK -> Color.Blue
        PomodoroPhase.SHORT_BREAK -> Color.Yellow
        PomodoroPhase.LONG_BREAK -> Color.Red
      }

  Surface(modifier = Modifier.fillMaxSize(), color = backgroundColor) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally) {
          Text(
              text = "Pomodoro Timer",
              fontSize = 28.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.testTag(PomodoroScreenTestTags.TITLE))
          Spacer(modifier = Modifier.height(8.dp))
          Text(
              "Phase: $phaseText",
              fontSize = 20.sp,
              modifier = Modifier.testTag(PomodoroScreenTestTags.PHASE_TEXT))
          Spacer(modifier = Modifier.height(16.dp))
          Text(
              timeText,
              fontSize = 56.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.testTag(PomodoroScreenTestTags.TIMER))
          Spacer(modifier = Modifier.height(16.dp))
          Text(
              "Cycles Completed: $cycleCount",
              fontSize = 16.sp,
              modifier = Modifier.testTag(PomodoroScreenTestTags.CYCLE_COUNT))
          Spacer(modifier = Modifier.height(24.dp))

          when (state) {
            PomodoroState.IDLE ->
                Button(
                    onClick = { viewModel.startTimer() },
                    modifier = Modifier.testTag(PomodoroScreenTestTags.START_BUTTON)) {
                      Text("Start")
                    }
            PomodoroState.RUNNING ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  Button(
                      onClick = { viewModel.pauseTimer() },
                      modifier = Modifier.testTag(PomodoroScreenTestTags.PAUSE_BUTTON)) {
                        Text("Pause")
                      }
                  Button(
                      onClick = { viewModel.nextPhase() },
                      modifier = Modifier.testTag(PomodoroScreenTestTags.SKIP_BUTTON)) {
                        Text("Skip")
                      }
                }
            PomodoroState.PAUSED ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  Button(onClick = { viewModel.resumeTimer() }, modifier = Modifier.testTag(PomodoroScreenTestTags.RESUME_BUTTON)) { Text("Resume") }
                  Button(
                      onClick = { viewModel.resetTimer() },
                      modifier = Modifier.testTag(PomodoroScreenTestTags.RESET_BUTTON)) {
                        Text("Reset")
                      }
                }
            PomodoroState.FINISHED ->
                Button(onClick = { viewModel.nextPhase() }, modifier = Modifier.testTag(PomodoroScreenTestTags.NEXT_PHASE_BUTTON)) { Text("Next Phase") }
          }
        }
  }
}

