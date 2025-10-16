package com.android.sample.ui.session.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.android.sample.R

// Parts of this code were written using ChatGPT and AndroidStudio Gemini tool.

object SessionStatsPanelTestTags {
  const val POMODOROS = "stats_pomodoros"
  const val TIME = "stats_time"
  const val STREAK = "stats_streak"
}

@Composable
fun SessionStatsPanel(
    pomodoros: Int,
    totalMinutes: Int,
    streak: Int,
    modifier: Modifier = Modifier
) {
  Row(
      modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically) {
        StatCard(
            title = stringResource(R.string.pomodoros_completed_txt),
            value = pomodoros.toString(),
            modifier = Modifier.weight(1f).testTag(SessionStatsPanelTestTags.POMODOROS))

        StatCard(
            title = stringResource(R.string.pomodoro_time_txt),
            value = "$totalMinutes ${stringResource(R.string.minute)}",
            modifier = Modifier.weight(1f).testTag(SessionStatsPanelTestTags.TIME))

        StatCard(
            title = stringResource(R.string.current_streak),
            value = "$streak ${stringResource(R.string.days)}",
            modifier = Modifier.weight(1f).testTag(SessionStatsPanelTestTags.STREAK))
      }
}

@Composable
private fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
  Card(
      modifier =
          modifier
              // .weight(1f) // each card takes equal width
              .aspectRatio(1f), // make it a square7
      elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
              Text(text = title, style = MaterialTheme.typography.bodyMedium)
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                  text = value,
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            }
      }
}
