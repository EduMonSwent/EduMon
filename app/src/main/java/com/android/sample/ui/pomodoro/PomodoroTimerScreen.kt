package com.android.sample.ui.pomodoro

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

// Parts of this code were written using ChatGPT and AndroidStudio Gemini tool.

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
        PomodoroPhase.WORK -> MaterialTheme.colorScheme.background
        PomodoroPhase.SHORT_BREAK -> MaterialTheme.colorScheme.secondaryContainer
        PomodoroPhase.LONG_BREAK -> MaterialTheme.colorScheme.tertiaryContainer
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
                PomodoroButton(
                    "Start",
                    { viewModel.startTimer() },
                    Modifier.testTag(PomodoroScreenTestTags.START_BUTTON))
            PomodoroState.RUNNING ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  PomodoroButton(
                      "Pause",
                      { viewModel.pauseTimer() },
                      Modifier.testTag(PomodoroScreenTestTags.PAUSE_BUTTON))

                  PomodoroButton(
                      "Skip",
                      { viewModel.nextPhase() },
                      Modifier.testTag(PomodoroScreenTestTags.SKIP_BUTTON))
                }
            PomodoroState.PAUSED ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  PomodoroButton(
                      "Resume",
                      { viewModel.resumeTimer() },
                      Modifier.testTag(PomodoroScreenTestTags.RESUME_BUTTON))
                  PomodoroButton(
                      "Reset",
                      { viewModel.resetTimer() },
                      Modifier.testTag(PomodoroScreenTestTags.RESET_BUTTON))
                }
            PomodoroState.FINISHED ->
                PomodoroButton(
                    "Next Phase",
                    { viewModel.nextPhase() },
                    Modifier.testTag(PomodoroScreenTestTags.NEXT_PHASE_BUTTON))
          }
        }
  }
}

@Composable
fun PomodoroButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    debounceTimeMs: Long = 1000L
) {
  var lastClickTime by remember { mutableLongStateOf(0L) }

  Button(
      onClick = {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastClickTime > debounceTimeMs) { // prevent double click
          lastClickTime = currentTime
          onClick()
        }
      },
      modifier = modifier) {
        Text(text)
      }
}
