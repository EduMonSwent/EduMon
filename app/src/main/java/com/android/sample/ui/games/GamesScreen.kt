package com.android.sample.ui.games

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.android.sample.ui.theme.*

@Composable
fun GamesScreen(navController: NavController) {
  Box(
      modifier = Modifier.fillMaxSize().background(BackgroundDark).padding(16.dp),
      contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                  text = "EduMon Games",
                  color = TextLight,
                  fontSize = 28.sp,
                  fontWeight = FontWeight.ExtraBold,
                  modifier = Modifier.padding(bottom = 24.dp))

              GameCard("Memory Game", "Train your memory", Icons.Default.Memory, Blue) {
                navController.navigate("memory")
              }
              GameCard("Reaction Test", "Test your reflexes", Icons.Default.FlashOn, AccentViolet) {
                navController.navigate("reaction")
              }
              GameCard(
                  "Focus Breathing", "Relax and breathe", Icons.Default.SelfImprovement, Blue) {
                    navController.navigate("focus")
                  }
              GameCard(
                  "EduMon Runner",
                  "Jump over obstacles",
                  Icons.Default.SportsEsports,
                  AccentViolet) {
                    navController.navigate("runner")
                  }
            }
      }
}

@Composable
fun GameCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit
) {
  Box(
      modifier =
          Modifier.fillMaxWidth()
              .padding(vertical = 8.dp)
              .shadow(8.dp, RoundedCornerShape(20.dp))
              .background(MidDarkCard, RoundedCornerShape(20.dp))
              .clickable { onClick() }
              .padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
              modifier =
                  Modifier.size(48.dp)
                      .background(color.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
              contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(28.dp))
              }
          Spacer(Modifier.width(16.dp))
          Column {
            Text(title, color = TextLight, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = TextLight.copy(alpha = 0.6f), fontSize = 14.sp)
          }
        }
      }
}
