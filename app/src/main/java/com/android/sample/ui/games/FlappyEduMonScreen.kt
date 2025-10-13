package com.android.sample.ui.games

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.sample.R
import com.android.sample.ui.theme.*
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

// The assistance of an AI tool (ChatGPT) was solicited in writing this  file.

@Composable
fun FlappyEduMonScreen(
    modifier: Modifier = Modifier,
    onExit: (() -> Unit)? = null,
) {
  // Physique du jeu
  val gravity = 0.5f
  val flapVelocity = -10f
  val pipeWidth = 60f
  val pipeGap = 320f
  val pipeSpeed = 3f

  // Monde
  var worldW by remember { mutableIntStateOf(0) }
  var worldH by remember { mutableIntStateOf(0) }
  val density = LocalDensity.current

  // État du jeu
  var started by remember { mutableStateOf(false) }
  var gameOver by remember { mutableStateOf(false) }
  var score by remember { mutableFloatStateOf(0f) }

  // Joueur
  val playerSizeDp = 52.dp
  val playerSizePx = with(density) { playerSizeDp.toPx() }
  val hitboxScale = 0.7f
  val halfHitbox = (playerSizePx * hitboxScale) / 2f

  var playerX by remember { mutableFloatStateOf(0f) }
  var playerY by remember { mutableFloatStateOf(0f) }
  var playerVy by remember { mutableFloatStateOf(0f) }

  // Obstacles
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

  // Boucle de jeu
  LaunchedEffect(started, worldW, worldH) {
    if (worldW == 0 || worldH == 0) return@LaunchedEffect
    val frameDelay = 16L
    while (isActive) {
      if (started && !gameOver) {
        playerVy += gravity
        playerY += playerVy

        // Déplacement des tuyaux
        pipes = pipes.map { it.copy(x = it.x - pipeSpeed) }

        // Nouveau tuyau
        if (pipes.isNotEmpty() && pipes.last().x < worldW) {
          val newX = pipes.last().x + pipeWidth + pipeGap
          val topH = (worldH * 0.35f) + Random.nextFloat() * (worldH * 0.3f)
          pipes = pipes + Pipe(newX, topH)
        }

        pipes = pipes.filter { it.x + pipeWidth > 0 }

        // Collision
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
          colors = listOf(Color(0xFF1A1635), Color(0xFF241E49), Color(0xFF322A66)))

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
        Canvas(Modifier.fillMaxSize()) {
          drawRect(color = Color.Transparent, topLeft = Offset.Zero, size = size)

          pipes.forEach { p ->
            val topColor = Color(0xFF6A5ACD)
            val bottomColor = Color(0xFF8A2BE2)

            // ✅ décalage minuscule vers la droite (évite le débordement sur le bord gauche)
            drawRect(
                color = topColor.copy(alpha = 0.85f),
                topLeft = Offset(p.x + 0.5f, 0f),
                size = Size(pipeWidth - 1f, p.topHeight))

            val bottomY = p.topHeight + pipeGap
            drawRect(
                color = bottomColor.copy(alpha = 0.85f),
                topLeft = Offset(p.x + 0.5f, bottomY),
                size = Size(pipeWidth - 1f, worldH - bottomY))
          }
        }

        Image(
            painter = painterResource(R.drawable.edumon),
            contentDescription = "EduMon",
            modifier =
                Modifier.offset {
                      IntOffset(
                          (playerX - playerSizePx / 2f).toInt(),
                          (playerY - playerSizePx / 2f).toInt())
                    }
                    .size(playerSizeDp))

        Text(
            text = "Score: ${score.toInt()}",
            color = TextLight,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 24.dp))

        // État “tap to start”
        if (!started && !gameOver) {
          Text(
              text = "Tap anywhere to start",
              color = AccentBlue,
              fontSize = 18.sp,
              fontWeight = FontWeight.Medium,
              modifier = Modifier.align(Alignment.Center))
        }

        // Game over screen
        if (gameOver) {
          Column(
              Modifier.align(Alignment.Center)
                  .background(Color(0xAA141526), shape = MaterialTheme.shapes.medium)
                  .padding(24.dp),
              horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Game Over",
                    color = AccentViolet,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold)
                Text("Score: ${score.toInt()}", color = TextLight, fontSize = 18.sp)
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { resetGame() },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)) {
                      Text("Restart", color = Color.White, fontSize = 16.sp)
                    }
              }
        }
      }
}
