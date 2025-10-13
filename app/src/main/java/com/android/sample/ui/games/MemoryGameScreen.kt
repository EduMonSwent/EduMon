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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.sample.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun MemoryGameScreen() {
  // Icônes Material à la place des emojis
  val icons =
      listOf(
          Icons.Filled.School,
          Icons.Filled.Book,
          Icons.Filled.Psychology,
          Icons.Filled.Lightbulb,
          Icons.Filled.AutoAwesome,
          Icons.Filled.Science,
          Icons.Filled.SportsEsports,
          Icons.Filled.TravelExplore,
          Icons.Filled.Bolt)

  var cards by remember { mutableStateOf(generateCards(icons)) }
  var flipped by remember { mutableStateOf(listOf<Int>()) }
  var score by remember { mutableStateOf(0) }
  var timer by remember { mutableStateOf(90) }
  var isGameOver by remember { mutableStateOf(false) }
  var isWin by remember { mutableStateOf(false) }

  // Timer
  LaunchedEffect(isGameOver, isWin) {
    if (!isGameOver && !isWin) {
      while (timer > 0) {
        delay(1000)
        timer--
      }
      if (timer == 0) isGameOver = true
    }
  }

  // Victoire
  LaunchedEffect(cards) {
    if (cards.all { it.isMatched }) {
      isWin = true
    }
  }

  Column(
      modifier =
          Modifier.fillMaxSize()
              .background(BackgroundDark)
              .padding(horizontal = 16.dp, vertical = 8.dp),
      horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "Memory Game",
            color = AccentViolet,
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
        Text("Time: ${timer}s | Score: $score", color = TextLight, fontSize = 16.sp)
        Spacer(Modifier.height(16.dp))

        // Grille du jeu
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
        }

        // Logique de matching
        if (flipped.size == 2) {
          LaunchedEffect(flipped) {
            delay(700)
            val first = cards.first { it.id == flipped[0] }
            val second = cards.first { it.id == flipped[1] }

            if (first.icon == second.icon) {
              score += 10
              cards = cards.map { if (it.id in flipped) it.copy(isMatched = true) else it }
            } else {
              cards = cards.map { if (it.id in flipped) it.copy(isFlipped = false) else it }
            }
            flipped = listOf()
          }
        }

        // Overlay victoire / défaite
        if (isWin || isGameOver) {
          Box(
              modifier = Modifier.fillMaxSize().background(Color(0xAA000000)),
              contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Text(
                      text = if (isWin) "Well done!" else "Time’s up!",
                      color = if (isWin) AccentViolet else Color.Red,
                      fontSize = 26.sp,
                      fontWeight = FontWeight.Bold)
                  Spacer(Modifier.height(8.dp))
                  Text("Score: $score", color = TextLight, fontSize = 18.sp)
                  Spacer(Modifier.height(20.dp))
                  Button(
                      onClick = {
                        cards = generateCards(icons)
                        flipped = listOf()
                        score = 0
                        timer = 90
                        isWin = false
                        isGameOver = false
                      },
                      colors = ButtonDefaults.buttonColors(containerColor = AccentViolet)) {
                        Text("Restart", color = TextLight, fontWeight = FontWeight.Bold)
                      }
                }
              }
        }
      }
}

@Composable
fun MemoryCardView(card: MemoryCard, onClick: () -> Unit) {
  val bg by
      animateColorAsState(
          when {
            card.isMatched -> Glow
            card.isFlipped -> MidDarkCard
            else -> Color(0xFF1C1C2E)
          })

  Box(
      modifier =
          Modifier.aspectRatio(1f)
              .fillMaxWidth()
              .shadow(10.dp, RoundedCornerShape(18.dp))
              .background(bg, RoundedCornerShape(18.dp))
              .clickable(enabled = !card.isMatched) { onClick() },
      contentAlignment = Alignment.Center) {
        if (card.isFlipped || card.isMatched) {
          Icon(
              imageVector = card.icon,
              contentDescription = null,
              tint = TextLight,
              modifier = Modifier.size(36.dp))
        } else {
          Text("?", fontSize = 34.sp, fontWeight = FontWeight.Bold, color = TextLight)
        }
      }
}

data class MemoryCard(
    val id: Int,
    val icon: ImageVector,
    val isFlipped: Boolean = false,
    val isMatched: Boolean = false
)

private fun generateCards(icons: List<ImageVector>): List<MemoryCard> {
  val pairs = (icons.shuffled().take(9) * 2).shuffled()
  return pairs.mapIndexed { i, icon -> MemoryCard(i, icon) }
}

private operator fun <T> List<T>.times(n: Int): List<T> = List(n) { this }.flatten()
