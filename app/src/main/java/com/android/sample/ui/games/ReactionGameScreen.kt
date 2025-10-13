package com.android.sample.ui.games

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.android.sample.ui.theme.*
import kotlin.random.Random
import kotlinx.coroutines.delay

@Composable
fun ReactionGameScreen() {
  var started by remember { mutableStateOf(false) }
  var waiting by remember { mutableStateOf(false) }
  var ready by remember { mutableStateOf(false) }
  var message by remember { mutableStateOf("Tap to start!") }
  var startTime by remember { mutableStateOf(0L) }
  var reactionTime by remember { mutableStateOf<Long?>(null) }

  val bgColor =
      when {
        waiting -> Color(0xFF9333EA)
        ready -> Color(0xFF00C853) // Green
        else -> Color(0xFF141526)
      }

  Box(
      modifier =
          Modifier.fillMaxSize().background(bgColor).clickable {
            when {
              !started -> {
                started = true
                waiting = true
                message = "Wait for green..."
              }
              waiting -> {
                message = "Too soon! Tap to retry."
                started = false
                waiting = false
                ready = false
              }
              ready -> {
                reactionTime = System.currentTimeMillis() - startTime
                message = "Reaction: ${reactionTime}ms\nTap to restart."
                started = false
                ready = false
              }
            }
          },
      contentAlignment = Alignment.Center) {
        if (started && waiting) {
          LaunchedEffect(Unit) {
            delay(Random.nextLong(1500, 4000))
            waiting = false
            ready = true
            startTime = System.currentTimeMillis()
            message = "TAP NOW!"
          }
        }
        Text(text = message, color = TextLight, fontSize = 24.sp, fontWeight = FontWeight.Bold)
      }
}
