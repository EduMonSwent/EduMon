package com.android.sample.ui.planner

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.sample.R
import com.android.sample.model.planner.WellnessEventType
import com.android.sample.ui.theme.DarkCardItem
import com.android.sample.ui.theme.MidDarkCard
import com.android.sample.ui.theme.TextLight
import kotlin.math.max

@Composable
fun WellnessEventItem(
    title: String,
    time: String,
    description: String,
    eventType: WellnessEventType,
    onClick: () -> Unit
) {
  val infiniteTransition = rememberInfiniteTransition(label = "wellnessGlowAnim")
  val glowAlpha by
      infiniteTransition.animateFloat(
          initialValue = 0.15f,
          targetValue = 0.35f,
          animationSpec =
              infiniteRepeatable(
                  animation = tween(durationMillis = 2800, easing = LinearEasing),
                  repeatMode = RepeatMode.Reverse),
          label = "wellnessGlowAlpha")

  val iconColor = eventType.primaryColor
  val containerColor = iconColor.copy(alpha = 0.1f)
  val borderColor = iconColor.copy(alpha = 0.3f)

  Box(
      modifier =
          Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(14.dp))
              .background(
                  Brush.verticalGradient(
                      colors =
                          listOf(
                              MidDarkCard.copy(alpha = 0.95f), DarkCardItem.copy(alpha = 0.98f))))
              .border(
                  width = 1.dp,
                  brush =
                      Brush.verticalGradient(
                          colors = listOf(iconColor.copy(alpha = 0.3f), Color.Transparent)),
                  shape = RoundedCornerShape(14.dp))
              .shadow(elevation = 4.dp, shape = RoundedCornerShape(14.dp))
              .clickable(onClick = onClick)
              .padding(14.dp)) {
        Box(
            modifier =
                Modifier.matchParentSize().drawBehind {
                  val center = Offset(size.width / 2f, size.height / 2f)
                  val radius = max(size.width, size.height) * 0.7f

                  drawCircle(
                      brush =
                          Brush.radialGradient(
                              colors =
                                  listOf(
                                      iconColor.copy(alpha = glowAlpha * 0.8f), Color.Transparent),
                              center = center,
                              radius = radius),
                      radius = radius,
                      center = center)
                })

        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
              modifier =
                  Modifier.size(40.dp)
                      .background(color = containerColor, shape = RoundedCornerShape(10.dp))
                      .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(10.dp)),
              contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(id = eventType.iconRes), // Using enum icon
                    contentDescription = title, // Content description as title
                    tint = iconColor,
                    modifier = Modifier.size(24.dp))
              }

          Spacer(modifier = Modifier.width(12.dp))

          Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = TextLight, fontWeight = FontWeight.Bold, fontSize = 16.sp)

            Spacer(modifier = Modifier.height(2.dp))

            Text(text = time, color = iconColor, fontSize = 13.sp, fontWeight = FontWeight.Medium)

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = description,
                color = TextLight.copy(alpha = 0.7f),
                fontSize = 12.sp,
                lineHeight = 14.sp)
          }

          Icon(
              painter = painterResource(id = R.drawable.ic_arrow_right),
              contentDescription = stringResource(R.string.details),
              tint = TextLight.copy(alpha = 0.5f),
              modifier = Modifier.size(20.dp))
        }
      }
}
