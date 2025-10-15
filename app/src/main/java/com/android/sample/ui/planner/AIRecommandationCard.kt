package com.android.sample.ui.planner

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.sample.R
import com.android.sample.ui.theme.AccentViolet
import com.android.sample.ui.theme.DarkCardItem
import com.android.sample.ui.theme.DarkViolet
import com.android.sample.ui.theme.DarknightViolet
import com.android.sample.ui.theme.LightBlueAccent
import com.android.sample.ui.theme.TextLight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun AIRecommendationCard(recommendationText: String, onActionClick: () -> Unit = {}) {
  val infiniteTransition = rememberInfiniteTransition(label = "infiniteTransition")

  val buttonScale by
      infiniteTransition.animateFloat(
          initialValue = 1f,
          targetValue = 1.02f,
          animationSpec =
              infiniteRepeatable(
                  animation = tween(durationMillis = 2000, easing = LinearEasing),
                  repeatMode = RepeatMode.Reverse),
          label = "buttonScale")

  val glowAlpha by
      infiniteTransition.animateFloat(
          initialValue = 0.3f,
          targetValue = 0.6f,
          animationSpec =
              infiniteRepeatable(
                  animation = tween(durationMillis = 3000, easing = LinearEasing),
                  repeatMode = RepeatMode.Reverse),
          label = "glowAlpha")

  Box(
      modifier =
          Modifier.fillMaxWidth()
              .background(
                  brush =
                      Brush.verticalGradient(
                          colors = listOf(DarknightViolet, DarkViolet),
                          startY = 0f,
                          endY = Float.POSITIVE_INFINITY),
                  shape = RoundedCornerShape(20.dp))
              .border(
                  width = 1.dp,
                  brush =
                      Brush.horizontalGradient(
                          colors =
                              listOf(
                                  AccentViolet.copy(alpha = 0.6f),
                                  LightBlueAccent.copy(alpha = 0.4f),
                                  AccentViolet.copy(alpha = 0.6f))),
                  shape = RoundedCornerShape(20.dp))
              .drawBehind {
                drawCircle(
                    brush =
                        Brush.radialGradient(
                            colors =
                                listOf(
                                    AccentViolet.copy(alpha = glowAlpha * 0.5f), Color.Transparent),
                            center = center.copy(x = size.width * 0.8f),
                            radius = size.height * 1.5f),
                    center = center.copy(x = size.width * 0.8f),
                    radius = size.height * 1.5f)
              }) { // Content moved inside the Box's lambda
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
              Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)) {
                          Box(
                              modifier =
                                  Modifier.size(44.dp)
                                      .background(
                                          brush =
                                              Brush.radialGradient(
                                                  colors =
                                                      listOf(
                                                          AccentViolet.copy(alpha = 0.3f),
                                                          Color.Transparent)),
                                          shape = CircleShape)
                                      .border(
                                          width = 1.dp,
                                          brush =
                                              Brush.radialGradient(
                                                  colors =
                                                      listOf(
                                                          AccentViolet.copy(alpha = 0.6f),
                                                          Color.Transparent)),
                                          shape = CircleShape),
                              contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_sparkle),
                                    contentDescription =
                                        "AI Icon", // Consider string resource for content
                                    // description
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(24.dp).scale(1.2f))
                              }

                          Spacer(modifier = Modifier.width(12.dp))

                          Column {
                            Text(
                                text = stringResource(R.string.ai_recommendation_title),
                                color = LightBlueAccent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp)
                            Text(
                                text = stringResource(R.string.ai_recommendation_subtitle),
                                color = TextLight.copy(alpha = 0.7f),
                                fontSize = 12.sp)
                          }
                        }
                  }

              Spacer(modifier = Modifier.height(16.dp))

              Box(
                  modifier =
                      Modifier.fillMaxWidth()
                          .background(
                              color = DarkCardItem.copy(alpha = 0.8f),
                              shape = RoundedCornerShape(16.dp))
                          .padding(16.dp)) {
                    Text(
                        text = "💡 $recommendationText",
                        color = TextLight.copy(alpha = 0.9f),
                        fontSize = 14.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.fillMaxWidth())
                  }

              Spacer(modifier = Modifier.height(20.dp))

              Box(
                  modifier =
                      Modifier.scale(buttonScale)
                          .shadow(8.dp, RoundedCornerShape(12.dp), clip = true)) {
                    Button(
                        onClick = onActionClick,
                        modifier = Modifier.fillMaxWidth(0.9f).height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = AccentViolet, contentColor = Color.White),
                        border =
                            BorderStroke(
                                1.dp,
                                Brush.verticalGradient(
                                    colors =
                                        listOf(Color.White.copy(alpha = 0.3f), Color.Transparent))),
                        contentPadding = PaddingValues(0.dp)) {
                          Row(
                              verticalAlignment = Alignment.CenterVertically,
                              horizontalArrangement = Arrangement.Center) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_play),
                                    contentDescription =
                                        stringResource(R.string.start_studying_session),
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    stringResource(R.string.start_studying_session),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp)
                              }
                        }
                  }

              Text(
                  text = stringResource(R.string.ai_recommendation_footer),
                  color = TextLight.copy(alpha = 0.5f),
                  fontSize = 10.sp,
                  modifier = Modifier.padding(top = 8.dp))
            }
      }
}
