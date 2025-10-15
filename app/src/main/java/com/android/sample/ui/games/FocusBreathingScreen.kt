package com.android.sample.ui.games

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun FocusBreathingScreen() {
  var phase by remember { mutableStateOf("Inhale...") }
  val scale = remember { Animatable(1f) }

  LaunchedEffect(Unit) {
    while (true) {
      phase = "Inhale..."
      scale.animateTo(1.5f, tween(4000))
      phase = "Hold..."
      delay(2000)
      phase = "Exhale..."
      scale.animateTo(1f, tween(4000))
      delay(1000)
    }
  }

  Box(
      modifier = Modifier.fillMaxSize().background(Color(0xFF141526)),
      contentAlignment = Alignment.Center) {
        Box(
            modifier =
                Modifier.size(150.dp).scale(scale.value).background(Color(0xFF4C7EFF), CircleShape))
        Text(
            text = phase,
            color = Color(0xFFE2E3F3),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 120.dp))
      }
}
