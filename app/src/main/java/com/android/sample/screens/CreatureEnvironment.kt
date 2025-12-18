package com.android.sample.screens

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material3.*
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
import com.android.sample.R
import com.android.sample.data.CreatureStats
import com.android.sample.data.UserStats

@Composable
fun CreatureHouseCard(
    modifier: Modifier = Modifier,
    @DrawableRes creatureResId: Int = R.drawable.edumon,
    level: Int,
    @DrawableRes environmentResId: Int = R.drawable.bg_pyrmon,
    overrideCreature: (@Composable () -> Unit)? = null,
    userStats: UserStats? = null, // ✅ NEW (default keeps old behavior)
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 1.dp,
    ) {
        if (userStats == null) {
            // ✅ Old layout unchanged
            CreatureEnvironmentBox(
                creatureResId = creatureResId,
                level = level,
                environmentResId = environmentResId,
                overrideCreature = overrideCreature,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            )
        } else {
            // ✅ New layout: environment left, stats right
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                CreatureEnvironmentBox(
                    creatureResId = creatureResId,
                    level = level,
                    environmentResId = environmentResId,
                    overrideCreature = overrideCreature,
                    modifier = Modifier.weight(1.35f),
                )

                UserStatsSideCard(
                    stats = userStats,
                    modifier = Modifier.weight(1f).height(220.dp),
                )
            }
        }
    }
}

@Composable
private fun CreatureEnvironmentBox(
    @DrawableRes creatureResId: Int,
    level: Int,
    @DrawableRes environmentResId: Int,
    overrideCreature: (@Composable () -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .clip(RoundedCornerShape(20.dp))
                .height(220.dp),
    ) {
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

        if (overrideCreature != null) {
            Box(modifier = Modifier.align(Alignment.BottomCenter).offset(y = (-28).dp)) { overrideCreature() }
        } else {
            CreatureSprite(
                resId = creatureResId,
                size = 120.dp,
                modifier = Modifier.align(Alignment.BottomCenter).offset(y = (-28).dp))
        }

        AssistChip(
            onClick = {},
            label = { Text("Lv $level", color = MaterialTheme.colorScheme.primary) },
            leadingIcon = { Icon(Icons.Outlined.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary) },
            colors =
                AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    labelColor = MaterialTheme.colorScheme.onSurface,
                    leadingIconContentColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier.align(Alignment.TopStart).padding(10.dp))
    }
}

@Composable
private fun UserStatsSideCard(stats: UserStats, modifier: Modifier = Modifier) {
    ElevatedCard(
        modifier = modifier.fillMaxHeight(),
        colors =
            CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Your Stats", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Spacer(Modifier.height(8.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Streak", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                    Text("${stats.streak}d", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Points", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                    Text("${stats.points}", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Today", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                    Text("${stats.todayStudyMinutes}m", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Goal", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                    Text("${stats.weeklyGoal}m", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

/* ---- rest of your file unchanged ---- */

@Composable
fun CreatureSprite(resId: Int, modifier: Modifier = Modifier, size: Dp = 120.dp) {
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

    val glowColor = MaterialTheme.colorScheme.primary

    Box(modifier.size(size + 40.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush =
                    Brush.radialGradient(
                        0f to glowColor.copy(alpha = glowAlpha), 1f to Color.Transparent),
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
                MaterialTheme.colorScheme.primary)
            StatRow(
                "Health",
                stats.health,
                Icons.Outlined.LocalHospital,
                MaterialTheme.colorScheme.secondary)
            StatRow("Energy", stats.energy, Icons.Outlined.Bolt, MaterialTheme.colorScheme.tertiary)
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
            Text("$value%", color = MaterialTheme.colorScheme.onSurface)
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = value / 100f,
            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = .20f),
            color = barColor,
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(8.dp)))
    }
}
