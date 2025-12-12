package com.android.sample.ui.games

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun FocusBreathingScreen() {
  var phase by remember { mutableStateOf("Inhale...") }
  val scale = remember { Animatable(1f) }
  val colorScheme = MaterialTheme.colorScheme

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
      modifier = Modifier.fillMaxSize().background(colorScheme.background),
      contentAlignment = Alignment.Center) {
        Box(
            modifier =
                Modifier.size(150.dp)
                    .scale(scale.value)
                    .background(colorScheme.primary, CircleShape))

        Text(
            text = phase,
            color = colorScheme.onBackground,
            fontSize = 24.sp,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 120.dp))
      }
}
