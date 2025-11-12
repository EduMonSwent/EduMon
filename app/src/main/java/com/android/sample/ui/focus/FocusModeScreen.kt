package com.android.sample.ui.focus

import android.annotation.SuppressLint
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.sample.ui.theme.*
import kotlin.time.Duration.Companion.minutes

@SuppressLint("DefaultLocale")
@Composable
fun FocusModeScreen(viewModel: FocusModeViewModel = viewModel()) {
  val context = LocalContext.current
  val isRunning by viewModel.isRunning.collectAsState()
  val remaining by viewModel.remainingTime.collectAsState()

  val minutes = (remaining.inWholeMinutes % 60)
  val seconds = (remaining.inWholeSeconds % 60)

  val backgroundColor by
      animateColorAsState(
          if (isRunning) BackgroundGradientEnd else BackgroundDark,
          animationSpec = tween(durationMillis = 800, easing = LinearEasing))

  val accentColor by
      animateColorAsState(
          if (isRunning) AccentMint else AccentViolet, animationSpec = tween(durationMillis = 600))

  val infiniteTransition = rememberInfiniteTransition(label = "pulse")
  val pulseAlpha by
      infiniteTransition.animateFloat(
          initialValue = 0.5f,
          targetValue = 1f,
          animationSpec =
              infiniteRepeatable(
                  animation = tween(1500, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
          label = "pulseAlpha")

  Box(
      modifier = Modifier.fillMaxSize().background(backgroundColor).padding(24.dp),
      contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
              Text(
                  text = "Focus Mode",
                  style =
                      MaterialTheme.typography.headlineMedium.copy(
                          color = PurpleText, fontWeight = FontWeight.Bold))

              Spacer(Modifier.height(24.dp))

              Text(
                  text = String.format("%02d:%02d", minutes, seconds),
                  style =
                      MaterialTheme.typography.displayLarge.copy(
                          color = accentColor,
                          fontSize = 72.sp,
                          fontWeight = FontWeight.ExtraBold,
                          textAlign = TextAlign.Center))

              Spacer(Modifier.height(48.dp))

              // 🔘 Bouton stylé avec glow
              Button(
                  onClick = {
                    if (!isRunning) viewModel.startFocus(context, 25)
                    else viewModel.stopFocus(context)
                  },
                  colors =
                      ButtonDefaults.buttonColors(
                          containerColor = accentColor, contentColor = Color.White),
                  modifier =
                      Modifier.size(160.dp)
                          .shadow(12.dp, CircleShape)
                          .background(accentColor.copy(alpha = pulseAlpha), CircleShape),
                  shape = CircleShape) {
                    Text(
                        text = if (!isRunning) "Start" else "Stop",
                        style =
                            MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.SemiBold, fontSize = 22.sp))
                  }

              Spacer(Modifier.height(20.dp))

              Text(
                  text =
                      if (!isRunning) "Start a 25-minute deep focus session"
                      else "Stay focused — you got this 💪",
                  color = TextLight,
                  style = MaterialTheme.typography.bodyLarge,
                  textAlign = TextAlign.Center)
            }
      }
}
