package com.android.sample.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.sample.data.CreatureStats

@Composable
fun CreatureHouseCard(
    creatureResId: Int,
    level: Int,
    environmentResId: Int,
    modifier: Modifier = Modifier,
) {
  Surface(
      modifier = modifier.fillMaxWidth(),
      color = MaterialTheme.colorScheme.surface,
      shape = RoundedCornerShape(24.dp),
      tonalElevation = 1.dp,
  ) {
    Box(
        modifier =
            Modifier.fillMaxWidth().padding(16.dp).clip(RoundedCornerShape(20.dp)).height(220.dp)) {
          Image(
              painter = painterResource(environmentResId),
              contentDescription = "Creature environment",
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize())
          Box(
              Modifier.align(Alignment.BottomCenter)
                  .fillMaxWidth(0.7f)
                  .height(6.dp)
                  .clip(RoundedCornerShape(50))
                  .background(MaterialTheme.colorScheme.onSurface.copy(alpha = .18f)))
          CreatureSprite(
              resId = creatureResId,
              size = 120.dp,
              modifier = Modifier.align(Alignment.BottomCenter).offset(y = (-28).dp))
          AssistChip(
              onClick = {},
              label = { Text("Lv $level", color = MaterialTheme.colorScheme.primary) },
              leadingIcon = {
                Icon(Icons.Outlined.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary)
              },
              colors =
                  AssistChipDefaults.assistChipColors(
                      containerColor = MaterialTheme.colorScheme.surfaceVariant,
                      labelColor = MaterialTheme.colorScheme.onSurface,
                      leadingIconContentColor = MaterialTheme.colorScheme.primary),
              modifier = Modifier.align(Alignment.TopStart).padding(10.dp))
        }
  }
}

@Composable
private fun CreatureSprite(resId: Int, modifier: Modifier = Modifier, size: Dp = 120.dp) {
  val inf = rememberInfiniteTransition(label = "float")
  val offset by
      inf.animateFloat(
          initialValue = 0f,
          targetValue = -10f,
          animationSpec =
              infiniteRepeatable(
                  animation = tween(1800, easing = FastOutSlowInEasing),
                  repeatMode = RepeatMode.Reverse),
          label = "offset")
  val glowAlpha by
      inf.animateFloat(
          initialValue = 0.25f,
          targetValue = 0.5f,
          animationSpec =
              infiniteRepeatable(
                  animation = tween(1400, easing = FastOutSlowInEasing),
                  repeatMode = RepeatMode.Reverse),
          label = "glow")

  Box(modifier = modifier.size(size + 40.dp), contentAlignment = Alignment.Center) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      drawCircle(
          brush =
              Brush.radialGradient(
                  0.0f to Color(0xFFA26BF2).copy(alpha = glowAlpha), 1.0f to Color.Transparent),
          radius = (size.value * 0.9f) * density,
          center = center)
    }
    Image(
        painter = painterResource(id = resId),
        contentDescription = "Creature",
        contentScale = ContentScale.Fit,
        modifier = Modifier.size(size).offset(y = offset.dp))
  }
}

@Composable
fun CreatureStatsCard(stats: CreatureStats, modifier: Modifier = Modifier) {
  ElevatedCard(
      modifier,
      colors =
          CardDefaults.elevatedCardColors(
              containerColor = MaterialTheme.colorScheme.surface,
              contentColor = MaterialTheme.colorScheme.onSurface),
      shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text("Buddy Stats", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
          StatRow(
              "Happiness",
              stats.happiness,
              Icons.Outlined.FavoriteBorder,
              barColor = MaterialTheme.colorScheme.primary)
          StatRow(
              "Health",
              stats.health,
              Icons.Outlined.LocalHospital,
              barColor = MaterialTheme.colorScheme.secondary)
          StatRow(
              "Energy",
              stats.energy,
              Icons.Outlined.Bolt,
              barColor = MaterialTheme.colorScheme.tertiary)
        }
      }
}

@Composable
private fun StatRow(
    title: String,
    value: Int,
    icon: ImageVector,
    barColor: Color,
) {
  Column {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(icon, contentDescription = null, tint = barColor)
      Spacer(Modifier.width(6.dp))
      Text(title, modifier = Modifier.weight(1f))
      Text("${value}%", color = MaterialTheme.colorScheme.onSurface)
    }
    Spacer(Modifier.height(6.dp))
    LinearProgressIndicator(
        progress = value / 100f,
        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = .20f),
        color = barColor,
        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(8.dp)))
  }
}
