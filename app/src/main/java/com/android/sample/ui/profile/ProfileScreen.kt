package com.android.sample.ui.profile

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.sample.R


private val MidDarkCard = Color(0xFF232445)
private val TextLight = Color(0xFFE0E0E0)
private val AccentViolet = Color(0xFF9333EA)

@Composable
fun ProfileScreen(viewModel: ProfileViewModel = viewModel()) {
    val user by viewModel.userProfile.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F1A))
            .verticalScroll(rememberScrollState())
            .padding(bottom = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PetSection(level = user.level)
        Spacer(modifier = Modifier.height(16.dp))
        GlowCard { ProfileCard(user = user) }
        Spacer(modifier = Modifier.height(16.dp))
        GlowCard { StatsCard(user = user) }
        Spacer(modifier = Modifier.height(16.dp))
        GlowCard { CustomizePetSection() }
        Spacer(modifier = Modifier.height(16.dp))
        GlowCard { SettingsCard(user, viewModel) }
        Spacer(modifier = Modifier.height(16.dp))
        GlowCard { AccountActionsSection() }
    }
}

@Composable
fun PetSection(level: Int) {

    val pulseAlpha by rememberInfiniteTransition().animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .background(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF0B0C24), Color(0xFF151737))
                )
            ),
        contentAlignment = Alignment.Center
    ) {

        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 20.dp, top = 40.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatBar(icon = "❤️", percent = 0.9f, color = Color(0xFFFF69B4))
            StatBar(icon = "💡", percent = 0.85f, color = Color(0xFFFFC107))
            StatBar(icon = "⚡", percent = 0.7f, color = Color(0xFF03A9F4))
        }


        Box(
            modifier = Modifier
                .size(130.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF5CE1E6).copy(alpha = pulseAlpha * 0.6f),
                            Color.Transparent
                        )
                    ),
                    shape = RoundedCornerShape(100.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.edumon),
                contentDescription = "EduMon",
                modifier = Modifier
                    .size(100.dp)
                    .background(Color.Transparent, RoundedCornerShape(20.dp))
            )
        }


        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = 70.dp)
                .background(Color(0xFF9333EA), RoundedCornerShape(50.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                "Lv $level",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }


        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 20.dp, top = 40.dp)
                .background(
                    color = Color(0xFF1A1B2E),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Text(" Edumon", color = Color(0xFFE0E0E0), fontSize = 13.sp)
        }
    }
}


@Composable
fun StatBar(icon: String, percent: Float, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(icon, fontSize = 16.sp)
        Spacer(modifier = Modifier.width(4.dp))
        Box(
            modifier = Modifier
                .width(70.dp)
                .height(10.dp)
                .background(Color(0xFF202233), RoundedCornerShape(10.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(percent)
                    .background(color, RoundedCornerShape(10.dp))
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text("${(percent * 100).toInt()}%", color = TextLight.copy(alpha = 0.8f), fontSize = 12.sp)
    }
}


@Composable
fun GlowCard(content: @Composable () -> Unit) {
    val glowAlpha by rememberInfiniteTransition().animateFloat(
        initialValue = 0.25f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Card(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .shadow(
                elevation = 16.dp,
                ambientColor = AccentViolet.copy(alpha = glowAlpha),
                spotColor = AccentViolet.copy(alpha = glowAlpha),
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MidDarkCard)
    ) {
        content()
    }
}

@Composable
fun ProfileCard(user: UserProfile) {
    Column(
        modifier = Modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {

            Box(
                modifier = Modifier
                    .size(70.dp)
                    .background(AccentViolet, shape = RoundedCornerShape(50.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = user.name.take(2).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )
            }

            Spacer(modifier = Modifier.width(20.dp))


            Image(
                painter = painterResource(id = R.drawable.epfl),
                contentDescription = "EPFL Logo",
                modifier = Modifier
                    .height(28.dp)
                    .width(60.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(user.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextLight)
        Text(user.email, color = TextLight.copy(alpha = 0.7f), fontSize = 14.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Badge(text = "Level ${user.level}", bg = AccentViolet)
            Badge(text = "${user.points} pts", bg = Color.White, textColor = AccentViolet)
        }
    }
}


@Composable
fun Badge(text: String, bg: Color, textColor: Color = Color.White) {
    Box(
        modifier = Modifier
            .background(bg, RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun StatsCard(user: UserProfile) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Your Stats", fontWeight = FontWeight.SemiBold, color = TextLight.copy(alpha = 0.8f))
        Spacer(modifier = Modifier.height(8.dp))
        StatRow("🔥 Current Streak", "${user.streak} days")
        StatRow("⭐ Total Points", "${user.points}")
        StatRow("📚 Study Time Today", "${user.studyTimeToday} min")
        StatRow("🎯 Daily Goal", "${user.dailyGoal} min")
    }
}

@Composable
fun StatRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TextLight.copy(alpha = 0.9f))
        Text(value, color = TextLight, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun CustomizePetSection() {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Customize Pet", color = TextLight.copy(alpha = 0.8f), fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Change your pet's appearance", color = TextLight.copy(alpha = 0.6f), fontSize = 13.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { /* TODO */ },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            contentPadding = PaddingValues()
        ) {
            Box(
                modifier = Modifier
                    .background(
                        brush = Brush.horizontalGradient(
                            listOf(AccentViolet, Color(0xFFFF0080))
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .padding(vertical = 10.dp, horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Customize Buddy", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SettingsCard(user: UserProfile, viewModel: ProfileViewModel) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Settings", color = TextLight.copy(alpha = 0.8f), fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        SettingRow("🔔 Notifications", "Study reminders & updates",
            user.notificationsEnabled, { viewModel.toggleNotifications() })
        Divider(color = Color(0xFF2F2F45))
        SettingRow("📍 Location Services", "Study spot suggestions",
            user.locationEnabled, { viewModel.toggleLocation() })
        Divider(color = Color(0xFF2F2F45))
        SettingRow("🎯 Focus Mode", "Minimize distractions",
            user.focusModeEnabled, { viewModel.toggleFocusMode() })
    }
}

@Composable
fun SettingRow(title: String, desc: String, value: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(title, color = TextLight)
            Text(desc, color = TextLight.copy(alpha = 0.6f), fontSize = 12.sp)
        }
        Switch(checked = value, onCheckedChange = { onToggle() })
    }
}

@Composable
fun AccountActionsSection() {
    Column(modifier = Modifier.padding(12.dp)) {
        ActionButton("Privacy Policy")
        ActionButton("Terms of Service")
        ActionButton("Logout", textColor = Color.Red)
    }
}

@Composable
fun ActionButton(text: String, textColor: Color = TextLight) {
    Text(
        text,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        color = textColor,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center
    )
}
