package com.android.sample.ui.games

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// ✅ Shared implementation for game & tests
@Composable
fun MemoryGameScreenBase(
    icons: List<ImageVector> =
        listOf(
            Icons.Filled.School,
            Icons.Filled.Book,
            Icons.Filled.Psychology,
            Icons.Filled.Lightbulb,
            Icons.Filled.AutoAwesome,
            Icons.Filled.Science,
            Icons.Filled.SportsEsports,
            Icons.Filled.TravelExplore,
            Icons.Filled.Bolt),
    initialCards: List<MemoryCard>? = null,
    initialWin: Boolean = false,
    initialGameOver: Boolean = false,
    enableTimer: Boolean = true
) {
  val colorScheme = MaterialTheme.colorScheme

  var cards by remember { mutableStateOf(initialCards ?: generateCards(icons)) }
  var flipped by remember { mutableStateOf(listOf<Int>()) }
  var score by remember { mutableStateOf(0) }
  var timer by remember { mutableStateOf(90) }
  var isGameOver by remember { mutableStateOf(initialGameOver) }
  var isWin by remember { mutableStateOf(initialWin) }

  // Timer (disabled in tests if needed)
  LaunchedEffect(enableTimer, isGameOver, isWin) {
    if (enableTimer && !isGameOver && !isWin) {
      while (timer > 0) {
        delay(1000)
        timer--
      }
      if (timer == 0) isGameOver = true
    }
  }

  // Win condition
  LaunchedEffect(cards) { if (cards.all { it.isMatched }) isWin = true }

  // Matching logic
  if (flipped.size == 2) {
    LaunchedEffect(flipped) {
      delay(700)
      val (first, second) = flipped.map { id -> cards.first { it.id == id } }
      if (first.icon == second.icon) {
        score += 10
        cards = cards.map { if (it.id in flipped) it.copy(isMatched = true) else it }
      } else {
        cards = cards.map { if (it.id in flipped) it.copy(isFlipped = false) else it }
      }
      flipped = emptyList()
    }
  }

  // UI
  Column(
      modifier =
          Modifier.fillMaxSize()
              .background(colorScheme.background)
              .padding(horizontal = 16.dp, vertical = 8.dp),
      horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "Memory Game",
            color = colorScheme.primary,
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
        Text("Time: ${timer}s | Score: $score", color = colorScheme.onBackground, fontSize = 16.sp)
        Spacer(Modifier.height(16.dp))

        Box(modifier = Modifier.weight(1f)) {
          LazyVerticalGrid(
              columns = GridCells.Fixed(3),
              modifier = Modifier.fillMaxSize(),
              horizontalArrangement = Arrangement.spacedBy(12.dp),
              verticalArrangement = Arrangement.spacedBy(12.dp),
              contentPadding = PaddingValues(6.dp)) {
                items(cards) { card ->
                  MemoryCardView(card) {
                    if (!isGameOver &&
                        !isWin &&
                        flipped.size < 2 &&
                        !card.isFlipped &&
                        !card.isMatched) {
                      flipped = flipped + card.id
                      cards = cards.map { if (it.id == card.id) it.copy(isFlipped = true) else it }
                    }
                  }
                }
              }

          // End-of-game overlay
          if (isWin || isGameOver) {
            GameEndOverlay(
                isWin = isWin,
                score = score,
                onRestart = {
                  cards = generateCards(icons)
                  flipped = emptyList()
                  score = 0
                  timer = 90
                  isWin = false
                  isGameOver = false
                })
          }
        }
      }
}

@Composable
fun MemoryGameScreen() {
  MemoryGameScreenBase()
}

@Composable
fun MemoryGameScreenTestable(
    initialCards: List<MemoryCard> =
        generateCards(
            listOf(
                Icons.Filled.School,
                Icons.Filled.Book,
                Icons.Filled.Psychology,
                Icons.Filled.Lightbulb,
                Icons.Filled.AutoAwesome,
                Icons.Filled.Science,
                Icons.Filled.SportsEsports,
                Icons.Filled.TravelExplore,
                Icons.Filled.Bolt)),
    initialWin: Boolean = false,
    initialGameOver: Boolean = false
) {
  MemoryGameScreenBase(
      initialCards = initialCards,
      initialWin = initialWin,
      initialGameOver = initialGameOver,
      enableTimer = false)
}

// 🎭 Game end overlay
@Composable
fun GameEndOverlay(isWin: Boolean, score: Int, onRestart: () -> Unit) {
  val colorScheme = MaterialTheme.colorScheme

  Box(
      modifier = Modifier.fillMaxSize().background(colorScheme.scrim.copy(alpha = 0.8f)),
      contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(
              text = if (isWin) "Well done!" else "Time’s up!",
              color = if (isWin) colorScheme.primary else colorScheme.error,
              fontSize = 26.sp,
              fontWeight = FontWeight.Bold)
          Spacer(Modifier.height(8.dp))
          Text("Score: $score", color = colorScheme.onBackground, fontSize = 18.sp)
          Spacer(Modifier.height(20.dp))
          Button(
              onClick = onRestart,
              colors =
                  ButtonDefaults.buttonColors(
                      containerColor = colorScheme.primary, contentColor = colorScheme.onPrimary)) {
                Text("Restart", fontWeight = FontWeight.Bold)
              }
        }
      }
}

// -----------------------------------------------------------
// Cards & helpers
// -----------------------------------------------------------
@Composable
fun MemoryCardView(card: MemoryCard, onClick: () -> Unit) {
  val colorScheme = MaterialTheme.colorScheme

  val bg by
      animateColorAsState(
          targetValue =
              when {
                card.isMatched -> colorScheme.secondaryContainer
                card.isFlipped -> colorScheme.surfaceVariant
                else -> colorScheme.surface
              },
          label = "memoryCardBg")

  Box(
      modifier =
          Modifier.aspectRatio(1f)
              .fillMaxWidth()
              .shadow(10.dp, RoundedCornerShape(18.dp))
              .background(bg, RoundedCornerShape(18.dp))
              .clickable(enabled = !card.isMatched) { onClick() },
      contentAlignment = Alignment.Center) {
        if (card.isFlipped || card.isMatched) {
          androidx.compose.material3.Icon(
              imageVector = card.icon,
              contentDescription = null,
              tint = colorScheme.onSurface,
              modifier = Modifier.size(36.dp))
        } else {
          Text("?", fontSize = 34.sp, fontWeight = FontWeight.Bold, color = colorScheme.onSurface)
        }
      }
}

data class MemoryCard(
    val id: Int,
    val icon: ImageVector,
    val isFlipped: Boolean = false,
    val isMatched: Boolean = false
)

fun generateCards(icons: List<ImageVector>): List<MemoryCard> {
  val pairs = (icons.shuffled().take(9) * 2).shuffled()
  return pairs.mapIndexed { i, icon -> MemoryCard(i, icon) }
}

operator fun <T> List<T>.times(n: Int): List<T> = List(n) { this }.flatten()
