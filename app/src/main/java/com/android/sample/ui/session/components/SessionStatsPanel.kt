package com.android.sample.ui.session.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Parts of this code were written using ChatGPT and AndroidStudio Gemini tool.

@Composable
fun SessionStatsPanel(
    pomodoros: Int,
    totalMinutes: Int,
    streak: Int,
    modifier: Modifier = Modifier
) {
  Card(
      elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
      modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
              Text(text = "Pomodoros Completed: $pomodoros")
              Text(text = "Total Study Time: $totalMinutes min")
              Text(text = "Current Streak: $streak days")
            }
      }
}
