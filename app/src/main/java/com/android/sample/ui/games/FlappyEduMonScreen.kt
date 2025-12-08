package com.android.sample.ui.games

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.sample.R
import com.android.sample.ui.profile.EduMonAvatar
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

// The assistance of an AI tool (ChatGPT) was solicited in writing this file.

@Composable
fun FlappyEduMonScreen(
    modifier: Modifier = Modifier,
    @DrawableRes avatarResId: Int = R.drawable.edumon, // 👈 new parameter
    onExit: (() -> Unit)? = null,
) {
  val colorScheme = MaterialTheme.colorScheme

  // Game physics
  val gravity = 0.5f
  val flapVelocity = -10f
  val pipeWidth = 60f
  val pipeGap = 320f
  val pipeSpeed = 3f

  // World
  var worldW by remember { mutableIntStateOf(0) }
  var worldH by remember { mutableIntStateOf(0) }
  val density = LocalDensity.current

  // Game state
  var started by remember { mutableStateOf(false) }
  var gameOver by remember { mutableStateOf(false) }
  var score by remember { mutableFloatStateOf(0f) }

  // Player
  val playerSizeDp = 52.dp
  val playerSizePx = with(density) { playerSizeDp.toPx() }
  val hitboxScale = 0.7f
  val halfHitbox = (playerSizePx * hitboxScale) / 2f

  var playerX by remember { mutableFloatStateOf(0f) }
  var playerY by remember { mutableFloatStateOf(0f) }
  var playerVy by remember { mutableFloatStateOf(0f) }

  // Pipes
  data class Pipe(val x: Float, val topHeight: Float)
  var pipes by remember { mutableStateOf(listOf<Pipe>()) }

  fun resetGame() {
    started = false
    gameOver = false
    score = 0f
    playerX = worldW / 4f
    playerY = worldH / 2f
    playerVy = 0f
    pipes =
        List(3) { i ->
          val x = worldW + i * (pipeWidth + pipeGap)
          val topH = (worldH * 0.35f) + Random.nextFloat() * (worldH * 0.3f)
          Pipe(x, topH)
        }
  }

  fun flap() {
    if (!started) started = true
    if (!gameOver) playerVy = flapVelocity
  }

  // Game loop
  LaunchedEffect(started, worldW, worldH) {
    if (worldW == 0 || worldH == 0) return@LaunchedEffect
    val frameDelay = 16L
    while (isActive) {
      if (started && !gameOver) {
        playerVy += gravity
        playerY += playerVy

        // Move pipes
        pipes = pipes.map { it.copy(x = it.x - pipeSpeed) }

        // Spawn new pipe
        if (pipes.isNotEmpty() && pipes.last().x < worldW) {
          val newX = pipes.last().x + pipeWidth + pipeGap
          val topH = (worldH * 0.35f) + Random.nextFloat() * (worldH * 0.3f)
          pipes = pipes + Pipe(newX, topH)
        }

        pipes = pipes.filter { it.x + pipeWidth > 0 }

        // Collision detection
        for (p in pipes) {
          val left = p.x
          val right = p.x + pipeWidth
          val gapTop = p.topHeight
          val gapBottom = p.topHeight + pipeGap
          if (playerX + halfHitbox > left && playerX - halfHitbox < right) {
            if (playerY - halfHitbox < gapTop || playerY + halfHitbox > gapBottom) {
              gameOver = true
              break
            }
          }
        }

        if (playerY < 0f || playerY > worldH.toFloat()) gameOver = true
        score += 0.01f
      }
      delay(frameDelay)
    }
  }

  val backgroundBrush =
      Brush.verticalGradient(
          colors = listOf(colorScheme.background, colorScheme.surfaceVariant, colorScheme.surface))

  Box(
      modifier
          .fillMaxSize()
          .pointerInput(Unit) { detectTapGestures(onTap = { flap() }) }
          .onSizeChanged {
            worldW = it.width
            worldH = it.height
            resetGame()
          }
          .background(brush = backgroundBrush)) {
        val pipeTopColor = colorScheme.primary
        val pipeBottomColor = colorScheme.primaryContainer

        Canvas(Modifier.fillMaxSize()) {
          pipes.forEach { p ->
            // Top pipe
            drawRect(
                color = pipeTopColor.copy(alpha = 0.85f),
                topLeft = Offset(p.x + 0.5f, 0f),
                size = Size(pipeWidth - 1f, p.topHeight))

            // Bottom pipe
            val bottomY = p.topHeight + pipeGap
            drawRect(
                color = pipeBottomColor.copy(alpha = 0.85f),
                topLeft = Offset(p.x + 0.5f, bottomY),
                size = Size(pipeWidth - 1f, worldH - bottomY))
          }
        }

        Box(
            modifier =
                Modifier.offset {
                      IntOffset(
                          (playerX - playerSizePx / 2f).toInt(),
                          (playerY - playerSizePx / 2f).toInt())
                    }
                    .size(playerSizeDp)) {
              // 👇 Now uses the chosen Edumon sprite
              EduMonAvatar(
                  showLevelLabel = false,
                  avatarResId = avatarResId,
              )
            }

        Text(
            text = "Score: ${score.toInt()}",
            color = colorScheme.onBackground,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 24.dp))

        // “Tap to start”
        if (!started && !gameOver) {
          Text(
              text = "Tap anywhere to start",
              color = colorScheme.secondary,
              fontSize = 18.sp,
              fontWeight = FontWeight.Medium,
              modifier = Modifier.align(Alignment.Center))
        }

        // Game over screen
        if (gameOver) {
          Column(
              Modifier.align(Alignment.Center)
                  .background(
                      colorScheme.scrim.copy(alpha = 0.75f), shape = MaterialTheme.shapes.medium)
                  .padding(24.dp),
              horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Game Over",
                    color = colorScheme.primary,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold)
                Text("Score: ${score.toInt()}", color = colorScheme.onSurface, fontSize = 18.sp)
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { resetGame() },
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = colorScheme.primary,
                            contentColor = colorScheme.onPrimary)) {
                      Text("Restart", fontSize = 16.sp)
                    }
              }
        }
      }
}
