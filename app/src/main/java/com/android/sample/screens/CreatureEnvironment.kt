package com.android.sample

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
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.sample.data.CreatureStats

/**
 * Creature-only entry:
 * - House card with environment + sprite
 * - Top-left: name/level pill
 * - Right side (centered): vertical stack of colored % + label (Variant B right-side style)
 */
@Composable
fun CreatureEnvironmentSection(
    creatureResId: Int,
    environmentResId: Int,
    level: Int,
    happiness: Int,
    health: Int,
    energy: Int,
    modifier: Modifier = Modifier,
    userStats: @Composable (Modifier) -> Unit = {} // kept for API stability; not used
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CreatureHouseCard(
            creatureResId = creatureResId,
            environmentResId = environmentResId,
            level = level,
            name = "EduMon",
            stats = CreatureStats(level = level, happiness = happiness, health = health, energy = energy)
        )
    }
}

@Composable
fun CreatureHouseCard(
    creatureResId: Int,
    level: Int,
    name: String = "EduMon",
    environmentResId: Int,
    stats: CreatureStats,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 1.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(20.dp))
                .height(220.dp)
        ) {

            // Environment
            Image(
                painter = painterResource(environmentResId),
                contentDescription = "Creature environment",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Ground shadow
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(0.7f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = .18f))
            )

            // Name + Level pill (top-left)
            NameLevelPill(
                name = name,
                level = level,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 8.dp, top = 10.dp)
            )

            // Right side vertical stats (Variant B style)
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 30.dp, top = 50.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                RightSideStat(
                    label = "Happiness",
                    value = stats.happiness,
                    color = MaterialTheme.colorScheme.primary
                )
                RightSideStat(
                    label = "Health",
                    value = stats.health,
                    color = MaterialTheme.colorScheme.secondary
                )
                RightSideStat(
                    label = "Energy",
                    value = stats.energy,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            // Sprite (centered, drawn last)
            CreatureSprite(
                resId = creatureResId,
                size = 120.dp,
                modifier = Modifier.align(Alignment.BottomCenter).offset(y = (-28).dp)
            )
        }
    }
}

@Composable
fun CreatureSprite(resId: Int, modifier: Modifier = Modifier, size: Dp = 120.dp) {
    val inf = rememberInfiniteTransition(label = "float")
    val offset by inf.animateFloat(
        initialValue = 0f,
        targetValue = -10f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset"
    )
    val glowAlpha by inf.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Box(modifier = modifier.size(size + 40.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    0.0f to Color(0xFFA26BF2).copy(alpha = glowAlpha),
                    1.0f to Color.Transparent
                ),
                radius = (size.value * 0.9f) * density,
                center = center
            )
        }
        Image(
            painter = painterResource(id = resId),
            contentDescription = "Creature",
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(size).offset(y = offset.dp)
        )
    }
}

/** Top-left level + name pill */
@Composable
private fun NameLevelPill(name: String, level: Int, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f),
        tonalElevation = 2.dp
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text("Lv $level", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(10.dp))
            Text("•")
            Spacer(Modifier.width(10.dp))
            Text(name, fontWeight = FontWeight.Bold)
        }
    }
}

/** Right-side item: colored % number with a small label underneath (right aligned). */
@Composable
private fun RightSideStat(label: String, value: Int, color: Color) {
    Column(horizontalAlignment = Alignment.End) {
        Text(
            text = "${value}%",
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
