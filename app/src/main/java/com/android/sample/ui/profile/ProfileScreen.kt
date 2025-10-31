package com.android.sample.ui.profile

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.sample.R
import com.android.sample.ui.theme.AccentViolet
import com.android.sample.ui.theme.MidDarkCard
import com.android.sample.ui.theme.TextLight

object ProfileScreenTestTags {
  const val PROFILE_SCREEN = "profileScreen"
  const val PET_SECTION = "petSection"
  const val PROFILE_CARD = "profileCard"
  const val STATS_CARD = "statsCard"
  const val CUSTOMIZE_PET_SECTION = "customizePetSection"
  const val SETTINGS_CARD = "settingsCard"
  const val ACCOUNT_ACTIONS_SECTION = "accountActionsSection"
  const val SWITCH_NOTIFICATIONS = "switchNotifications"
  const val SWITCH_LOCATION = "switchLocation"
  const val SWITCH_FOCUS_MODE = "switchFocusMode"
}

// === constants ===
private val CARD_CORNER_RADIUS = 16.dp
private val SECTION_SPACING = 16.dp
private val SCREEN_PADDING = 60.dp
private val GRADIENT_COLORS = listOf(Color(0xFF12122A), Color(0xFF181830))

@Composable
fun ProfileScreen(viewModel: ProfileViewModel = viewModel()) {
  val user by viewModel.userProfile.collectAsState()

  LazyColumn(
      modifier =
          Modifier.fillMaxSize()
              .background(Brush.verticalGradient(GRADIENT_COLORS))
              .padding(bottom = SCREEN_PADDING)
              .testTag(ProfileScreenTestTags.PROFILE_SCREEN),
      horizontalAlignment = Alignment.CenterHorizontally,
      contentPadding = PaddingValues(vertical = SECTION_SPACING),
      verticalArrangement = Arrangement.spacedBy(SECTION_SPACING)) {
        item {
          PetSection(
              level = user.level, modifier = Modifier.testTag(ProfileScreenTestTags.PET_SECTION))
        }
        item {
          GlowCard {
            Box(Modifier.testTag(ProfileScreenTestTags.PROFILE_CARD)) { ProfileCard(user) }
          }
        }
        item {
          GlowCard { Box(Modifier.testTag(ProfileScreenTestTags.STATS_CARD)) { StatsCard(user) } }
        }
        item {
          GlowCard {
            Box(Modifier.testTag(ProfileScreenTestTags.CUSTOMIZE_PET_SECTION)) {
              CustomizePetSection()
            }
          }
        }
        item {
          GlowCard {
            Box(Modifier.testTag(ProfileScreenTestTags.SETTINGS_CARD)) {
              SettingsCard(
                  user = user,
                  onToggleNotifications = viewModel::toggleNotifications,
                  onToggleLocation = viewModel::toggleLocation,
                  onToggleFocusMode = viewModel::toggleFocusMode)
            }
          }
        }
        item {
          GlowCard {
            Box(Modifier.testTag(ProfileScreenTestTags.ACCOUNT_ACTIONS_SECTION)) {
              AccountActionsSection()
            }
          }
        }
      }
}

@Composable
fun PetSection(level: Int, modifier: Modifier = Modifier) {
  val pulseAlpha by
      rememberInfiniteTransition()
          .animateFloat(
              initialValue = 0.3f,
              targetValue = 0.9f,
              animationSpec =
                  infiniteRepeatable(
                      animation = tween(durationMillis = 2500, easing = LinearEasing),
                      repeatMode = RepeatMode.Reverse))

  Box(
      modifier =
          modifier
              .fillMaxWidth()
              .background(Brush.verticalGradient(listOf(Color(0xFF0B0C24), Color(0xFF151737))))
              .padding(vertical = 20.dp, horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
              Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                StatBar(icon = "❤️", percent = 0.9f, color = Color(0xFFFF69B4))
                StatBar(icon = "💡", percent = 0.85f, color = Color(0xFFFFC107))
                StatBar(icon = "⚡", percent = 0.7f, color = Color(0xFF03A9F4))
              }

              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier =
                        Modifier.size(130.dp)
                            .background(
                                brush =
                                    Brush.radialGradient(
                                        colors =
                                            listOf(
                                                Color(0xFF5CE1E6).copy(alpha = pulseAlpha * 0.6f),
                                                Color.Transparent)),
                                shape = RoundedCornerShape(100.dp)),
                    contentAlignment = Alignment.Center) {
                      Image(
                          painter = painterResource(id = R.drawable.edumon),
                          contentDescription = "EduMon",
                          modifier = Modifier.size(100.dp))
                    }

                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier =
                        Modifier.background(Color(0xFF9333EA), RoundedCornerShape(50.dp))
                            .padding(horizontal = 12.dp, vertical = 5.dp)) {
                      Text(
                          "Lv $level",
                          color = Color.White,
                          fontWeight = FontWeight.Bold,
                          fontSize = 12.sp)
                    }
              }

              Box(
                  modifier =
                      Modifier.background(Color(0xFF1A1B2E), RoundedCornerShape(20.dp))
                          .padding(horizontal = 16.dp, vertical = 6.dp)) {
                    Text("Edumon", color = Color(0xFFE0E0E0), fontSize = 13.sp)
                  }
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
                .background(Color(0xFF202233), RoundedCornerShape(10.dp))) {
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

@Composable
fun GlowCard(content: @Composable () -> Unit) {
  val glowAlpha by
      rememberInfiniteTransition()
          .animateFloat(
              initialValue = 0.25f,
              targetValue = 0.6f,
              animationSpec =
                  infiniteRepeatable(
                      animation = tween(durationMillis = 2500, easing = LinearEasing),
                      repeatMode = RepeatMode.Reverse))

  Card(
      modifier =
          Modifier.fillMaxWidth(0.9f)
              .shadow(
                  elevation = 16.dp,
                  ambientColor = AccentViolet.copy(alpha = glowAlpha),
                  spotColor = AccentViolet.copy(alpha = glowAlpha),
                  shape = RoundedCornerShape(CARD_CORNER_RADIUS)),
      shape = RoundedCornerShape(CARD_CORNER_RADIUS),
      colors = CardDefaults.cardColors(containerColor = MidDarkCard)) {
        content()
      }
}

@Composable
fun ProfileCard(user: UserProfile) {
  Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()) {
          Box(
              modifier =
                  Modifier.size(70.dp).background(AccentViolet, shape = RoundedCornerShape(50.dp)),
              contentAlignment = Alignment.Center) {
                Text(
                    text = user.name.take(2).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp)
              }
          Spacer(modifier = Modifier.width(20.dp))
          Image(
              painter = painterResource(id = R.drawable.epfl),
              contentDescription = "EPFL Logo",
              modifier = Modifier.height(28.dp).width(60.dp))
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
      modifier =
          Modifier.background(bg, RoundedCornerShape(12.dp))
              .padding(horizontal = 8.dp, vertical = 4.dp),
      contentAlignment = Alignment.Center) {
        Text(text, color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Medium)
      }
}

@Composable
fun StatsCard(user: UserProfile) {
  Column(modifier = Modifier.padding(16.dp)) {
    Text(
        text = stringResource(id = R.string.stats_title),
        fontWeight = FontWeight.SemiBold,
        color = TextLight.copy(alpha = 0.8f))
    Spacer(modifier = Modifier.height(8.dp))
    StatRow(
        Icons.Outlined.Whatshot, stringResource(id = R.string.stats_streak), "${user.streak} days")
    StatRow(Icons.Outlined.Star, stringResource(id = R.string.stats_points), "${user.points}")
    StatRow(Icons.Outlined.AttachMoney, stringResource(id = R.string.stats_coins), "${user.coins}")
    StatRow(
        Icons.AutoMirrored.Outlined.MenuBook,
        stringResource(id = R.string.stats_study_time),
        "${user.studyTimeToday} min")
    StatRow(Icons.Outlined.Flag, stringResource(id = R.string.stats_goal), "${user.dailyGoal} min")
  }
}

@Composable
fun StatRow(icon: ImageVector, label: String, value: String) {
  Row(
      Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(icon, contentDescription = null, tint = AccentViolet)
          Spacer(modifier = Modifier.width(8.dp))
          Text(label, color = TextLight.copy(alpha = 0.9f))
        }
        Text(value, color = TextLight, fontWeight = FontWeight.Medium)
      }
}

@Composable
fun CustomizePetSection() {
  Column(modifier = Modifier.padding(16.dp)) {
    Text(
        text = stringResource(id = R.string.customize_pet_title),
        color = TextLight.copy(alpha = 0.8f),
        fontWeight = FontWeight.SemiBold)
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = stringResource(id = R.string.customize_pet_desc),
        color = TextLight.copy(alpha = 0.6f),
        fontSize = 13.sp)
    Spacer(modifier = Modifier.height(12.dp))
    Button(
        onClick = { /* navigateToCustomize() */},
        modifier = Modifier.fillMaxWidth().height(46.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = PaddingValues(0.dp)) {
          Box(
              modifier =
                  Modifier.fillMaxSize()
                      .background(
                          brush =
                              Brush.horizontalGradient(
                                  listOf(Color(0xFF9333EA), Color(0xFFFF0080))),
                          shape = RoundedCornerShape(12.dp)),
              contentAlignment = Alignment.Center) {
                Text(
                    stringResource(id = R.string.customize_pet_button),
                    color = Color.White,
                    fontWeight = FontWeight.Bold)
              }
        }
  }
}

@Composable
fun SettingsCard(
    user: UserProfile,
    onToggleNotifications: () -> Unit,
    onToggleLocation: () -> Unit,
    onToggleFocusMode: () -> Unit
) {
  Column(modifier = Modifier.padding(16.dp)) {
    Text(
        stringResource(id = R.string.settings_title),
        color = TextLight.copy(alpha = 0.8f),
        fontWeight = FontWeight.SemiBold)
    Spacer(modifier = Modifier.height(8.dp))
    SettingRow(
        stringResource(id = R.string.settings_notifications),
        stringResource(id = R.string.settings_notifications_desc),
        user.notificationsEnabled,
        onToggleNotifications)
    Divider(color = Color(0xFF2F2F45))
    SettingRow(
        stringResource(id = R.string.settings_location),
        stringResource(id = R.string.settings_location_desc),
        user.locationEnabled,
        onToggleLocation)
    Divider(color = Color(0xFF2F2F45))
    SettingRow(
        stringResource(id = R.string.settings_focus),
        stringResource(id = R.string.settings_focus_desc),
        user.focusModeEnabled,
        onToggleFocusMode)
  }
}

@Composable
fun SettingRow(title: String, desc: String, value: Boolean, onToggle: () -> Unit) {
  Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically) {
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
    ActionButton(stringResource(id = R.string.account_privacy)) { /* navigateToPrivacy() */}
    ActionButton(stringResource(id = R.string.account_terms)) { /* navigateToTerms() */}
    ActionButton(
        stringResource(id = R.string.account_logout), textColor = Color.Red) { /* logout() */}
  }
}

@Composable
fun ActionButton(text: String, textColor: Color = TextLight, onClick: () -> Unit = {}) {
  TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
    Text(text, color = textColor, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
  }
}
