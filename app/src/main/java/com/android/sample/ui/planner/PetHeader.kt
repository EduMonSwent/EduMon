package com.android.sample.ui.planner

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.sample.R
import com.android.sample.ui.theme.AccentViolet
import com.android.sample.ui.theme.DarkCardItem
import com.android.sample.ui.theme.LightBlueAccent
import com.android.sample.ui.theme.StatBarHeart
import com.android.sample.ui.theme.StatBarLightbulb
import com.android.sample.ui.theme.StatBarLightning
import com.android.sample.ui.theme.TextLight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetHeader(level: Int, modifier: Modifier = Modifier, onEdumonNameClick: () -> Unit) {
  val pulseAlpha by
      rememberInfiniteTransition(label = "petGlow")
          .animateFloat(
              initialValue = 0.3f,
              targetValue = 0.9f,
              animationSpec =
                  infiniteRepeatable(
                      animation = tween(durationMillis = 2500, easing = LinearEasing),
                      repeatMode = RepeatMode.Reverse),
              label = "pulseAlpha")

  Box(modifier = modifier.fillMaxWidth().height(200.dp).testTag(PlannerScreenTestTags.PET_HEADER)) {
    Image(
        painter = painterResource(id = R.drawable.epfl_amphi_background),
        contentDescription = stringResource(R.string.app_name), // Generic content description
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop)

    Row(
        modifier =
            Modifier.fillMaxWidth()
                .padding(vertical = 20.dp, horizontal = 16.dp)
                .align(Alignment.Center)) {
          Box(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 20.dp)) {
            Column(
                modifier = Modifier.align(Alignment.CenterStart),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.Start) {
                  StatBar(icon = "❤️", percent = 0.9f, color = StatBarHeart)
                  StatBar(icon = "💡", percent = 0.85f, color = StatBarLightbulb)
                  StatBar(icon = "⚡", percent = 0.7f, color = StatBarLightning)
                }

            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center) {
                  Box(
                      modifier =
                          Modifier.size(130.dp)
                              .background(
                                  brush =
                                      Brush.radialGradient(
                                          colors =
                                              listOf(
                                                  LightBlueAccent.copy(alpha = pulseAlpha * 0.6f),
                                                  Color.Transparent)),
                                  shape = RoundedCornerShape(100.dp)),
                      contentAlignment = Alignment.Center) {
                        Image(
                            painter = painterResource(id = R.drawable.edumon),
                            contentDescription = "EduMon",
                            modifier = Modifier.size(100.dp))
                      }
                  Spacer(modifier = Modifier.height(8.dp))

                  AssistChip(
                      onClick = { /* Not clickable for primary action, just info */},
                      label = { Text("Lv $level", color = AccentViolet) },
                      // label = { Text(stringResource(R.string, level), color = AccentViolet) },
                      leadingIcon = {
                        Icon(
                            Icons.Outlined.Star,
                            null,
                            tint = AccentViolet,
                            modifier = Modifier.size(16.dp))
                      },
                      colors =
                          AssistChipDefaults.assistChipColors(
                              containerColor = DarkCardItem.copy(alpha = 0.8f),
                              labelColor = AccentViolet,
                              leadingIconContentColor = AccentViolet),
                      border = BorderStroke(1.dp, AccentViolet.copy(alpha = 0.5f)),
                      modifier = Modifier.offset(y = (-5).dp))
                }
          }
        }

    Box(
        modifier =
            Modifier.align(Alignment.TopEnd)
                .padding(top = 16.dp, end = 16.dp)
                .background(DarkCardItem, RoundedCornerShape(20.dp))
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .clickable(onClick = onEdumonNameClick)
                .testTag("petNameBox")) {
          Text(
              stringResource(R.string.edumon_profile),
              color = TextLight.copy(alpha = 0.8f),
              fontSize = 13.sp)
        }
  }
}

@Composable
fun StatBar(icon: String, percent: Float, color: Color) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Text(icon, fontSize = 16.sp)
    Spacer(modifier = Modifier.width(4.dp))
    Box(
        modifier =
            Modifier.width(70.dp)
                .height(10.dp)
                .background(
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                    RoundedCornerShape(10.dp))) { // Using theme color
          Box(
              modifier =
                  Modifier.fillMaxHeight()
                      .fillMaxWidth(percent)
                      .background(color, RoundedCornerShape(10.dp)))
        }
    Spacer(modifier = Modifier.width(4.dp))
    Text("${(percent * 100).toInt()}%", color = TextLight.copy(alpha = 0.8f), fontSize = 12.sp)
  }
}
