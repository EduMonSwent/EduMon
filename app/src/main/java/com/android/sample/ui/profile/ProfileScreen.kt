package com.android.sample.ui.profile

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.sample.R
import com.android.sample.ui.login.UserProfile
import com.android.sample.ui.theme.*

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

@Composable
fun ProfileScreen(viewModel: ProfileViewModel = viewModel()) {
  val user by viewModel.userProfile.collectAsState()
  val accent by viewModel.accentEffective.collectAsState()
  val variant by viewModel.accentVariantFlow.collectAsState()

  LazyColumn(
      modifier =
          Modifier.fillMaxSize()
              .background(Brush.verticalGradient(listOf(Color(0xFF12122A), Color(0xFF181830))))
              .padding(bottom = 60.dp)
              .testTag(ProfileScreenTestTags.PROFILE_SCREEN),
      horizontalAlignment = Alignment.CenterHorizontally,
      contentPadding = PaddingValues(vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
          PetSection(
              level = user.level,
              accent = accent,
              accessories = user.accessories,
              variant = variant)
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
              CustomizePetSection(viewModel)
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
fun PetSection(
    level: Int,
    accent: Color,
    accessories: List<String>,
    variant: AccentVariant,
    modifier: Modifier = Modifier
) {

  Box(
      modifier =
          modifier
              .fillMaxWidth()
              .background(
                  brush = Brush.verticalGradient(listOf(Color(0xFF0B0C24), Color(0xFF151737))))
              .padding(vertical = 20.dp, horizontal = 16.dp)
              .testTag(ProfileScreenTestTags.PET_SECTION)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
              Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                StatBar("❤️", 0.9f, StatBarHeart)
                StatBar("💡", 0.85f, StatBarLightbulb)
                StatBar("⚡", 0.7f, StatBarLightning)
              }

              Column(
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.Center) {
                    Box(
                        modifier = Modifier.size(130.dp).clip(RoundedCornerShape(100.dp)),
                        contentAlignment = Alignment.Center) {
                          // aura (fond)
                          Box(
                              Modifier.fillMaxSize()
                                  .background(
                                      brush =
                                          Brush.radialGradient(
                                              colors =
                                                  listOf(
                                                      accent.copy(alpha = 0.55f),
                                                      Color.Transparent))))
                          // avatar au-dessus
                          Image(
                              painter = painterResource(id = R.drawable.edumon),
                              contentDescription = "EduMon",
                              modifier = Modifier.size(100.dp).zIndex(1f))

                          // --- overlays par slot (dans le Box du pet) ---
                          val equipped =
                              remember(accessories) {
                                fun norm(s: String) =
                                    when (s) {
                                      "hand" -> "hands"
                                      "leg" -> "legs"
                                      else -> s
                                    }
                                accessories.associate {
                                  val p = it.split(":")
                                  norm(p.getOrNull(0) ?: "") to (p.getOrNull(1) ?: "")
                                }
                              }

                          // LEGS (sol)
                          equipped["legs"]?.let { _: String ->
                            Box(
                                Modifier.align(Alignment.BottomCenter)
                                    .padding(bottom = 6.dp)
                                    .size(width = 42.dp, height = 6.dp)
                                    .background(
                                        Color.White.copy(alpha = 0.85f), RoundedCornerShape(3.dp))
                                    .zIndex(2f))
                          }

                          // TORSO
                          equipped["torso"]?.let { _: String ->
                            Icon(
                                Icons.Filled.AutoAwesome,
                                contentDescription = "torso",
                                tint = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.align(Alignment.Center).size(20.dp).zIndex(2f))
                          }

                          // HEAD
                          equipped["head"]?.let { _: String ->
                            Icon(
                                Icons.Filled.Star,
                                contentDescription = "head",
                                tint = Color(0xFFFFD54F),
                                modifier =
                                    Modifier.align(Alignment.TopCenter)
                                        .padding(top = 2.dp)
                                        .size(22.dp)
                                        .zIndex(2f))
                          }
                        }
                    Spacer(Modifier.height(6.dp))
                    Text("Level $level", color = TextLight, fontWeight = FontWeight.SemiBold)
                  }
            }
      }
}

@Composable
fun StatBar(icon: String, percent: Float, color: Color) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Text(icon, fontSize = 16.sp)
    Spacer(Modifier.width(4.dp))
    Box(Modifier.width(70.dp).height(10.dp).background(DarkCardItem, RoundedCornerShape(10.dp))) {
      Box(
          Modifier.fillMaxSize()
              .width(70.dp * percent)
              .background(color, RoundedCornerShape(10.dp)))
    }
    Spacer(Modifier.width(4.dp))
    Text("${(percent * 100).toInt()}%", color = TextLight.copy(alpha = 0.8f), fontSize = 12.sp)
  }
}

@Composable
fun GlowCard(content: @Composable () -> Unit) {
  val glow by
      rememberInfiniteTransition(label = "glow")
          .animateFloat(
              initialValue = 0.25f,
              targetValue = 0.6f,
              animationSpec =
                  infiniteRepeatable(tween(2500, easing = LinearEasing), RepeatMode.Reverse),
              label = "glowVal")
  Card(
      modifier =
          Modifier.fillMaxWidth(0.9f)
              .shadow(
                  elevation = 16.dp,
                  ambientColor = AccentViolet.copy(alpha = glow),
                  spotColor = AccentViolet.copy(alpha = glow),
                  shape = RoundedCornerShape(16.dp)),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MidDarkCard)) {
        content()
      }
}

@Composable
fun ProfileCard(user: UserProfile) {
  Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()) {
          Box(
              Modifier.size(70.dp).background(AccentViolet, shape = RoundedCornerShape(50.dp)),
              contentAlignment = Alignment.Center) {
                Text(
                    user.name.take(2).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp)
              }
          Spacer(Modifier.width(20.dp))
          Image(
              painter = painterResource(id = R.drawable.epfl),
              contentDescription = "EPFL Logo",
              modifier = Modifier.height(28.dp).width(60.dp))
        }
    Spacer(Modifier.height(8.dp))
    Text(user.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextLight)
    Text(user.email, color = TextLight.copy(alpha = 0.7f), fontSize = 14.sp)
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      Badge(text = "Level ${user.level}", bg = AccentViolet)
      Badge(text = "${user.points} pts", bg = Color.White, textColor = AccentViolet)
    }
  }
}

@Composable
fun Badge(text: String, bg: Color, textColor: Color = Color.White) {
  Box(
      Modifier.background(bg, RoundedCornerShape(12.dp))
          .padding(horizontal = 8.dp, vertical = 4.dp),
      contentAlignment = Alignment.Center) {
        Text(text, color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Medium)
      }
}

@Composable
fun StatsCard(user: UserProfile) {
  // Samples en fallback si zéro
  val sampleStreak = if (user.streak > 0) user.streak else 7
  val samplePoints = if (user.points > 0) user.points else 1280
  val sampleToday = if (user.studyTimeToday > 0) user.studyTimeToday else 54
  val sampleGoal = if (user.dailyGoal > 0) user.dailyGoal else 60

  Column(Modifier.padding(16.dp)) {
    Text("Your Stats", fontWeight = FontWeight.SemiBold, color = TextLight.copy(alpha = 0.8f))
    Spacer(Modifier.height(8.dp))
    StatRow("🔥 Current Streak", "$sampleStreak days")
    StatRow("⭐ Total Points", "$samplePoints")
    StatRow("📚 Study Time Today", "$sampleToday min")
    StatRow("🎯 Daily Goal", "$sampleGoal min")
  }
}

@Composable
fun StatRow(label: String, value: String) {
  Row(
      Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = TextLight.copy(alpha = 0.9f))
        Text(value, color = TextLight, fontWeight = FontWeight.Medium)
      }
}

@Composable
fun CustomizePetSection(viewModel: ProfileViewModel) {
  val user by viewModel.userProfile.collectAsState()
  val currentVariant by viewModel.accentVariantFlow.collectAsState()

  Column(Modifier.padding(16.dp)) {
    Text("Customize Buddy", color = TextLight.copy(alpha = 0.8f), fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(12.dp))

    // Accent base
    Text("Accent color", color = TextLight.copy(alpha = 0.7f), fontSize = 13.sp)
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      viewModel.accentPalette.forEach { c ->
        val selected = user.avatarAccent == c.toArgb().toLong()
        Box(
            modifier =
                Modifier.size(32.dp)
                    .clip(CircleShape)
                    .background(c)
                    .border(
                        if (selected) 3.dp else 1.dp,
                        if (selected) Color.White else Color.White.copy(alpha = 0.25f),
                        CircleShape)
                    .clickable { viewModel.setAvatarAccent(c) },
            contentAlignment = Alignment.Center) {
              if (selected)
                  androidx.compose.material3.Icon(Icons.Outlined.Check, null, tint = Color.White)
            }
      }
    }

    // Variations
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      AccentVariant.values().forEach { v ->
        FilterChip(
            selected = v == currentVariant,
            onClick = { viewModel.setAccentVariant(v) },
            label = { Text(v.name) },
            modifier = Modifier.widthIn(min = 72.dp))
      }
    }

    Spacer(Modifier.height(20.dp))

    // Inventaire par slot
    Text("Inventory", color = TextLight.copy(alpha = 0.8f), fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(10.dp))

    var selectedTab by
        androidx.compose.runtime.remember {
          androidx.compose.runtime.mutableStateOf(AccessorySlot.HEAD)
        }
    val tabs = AccessorySlot.values()
    TabRow(selectedTabIndex = tabs.indexOf(selectedTab)) {
      tabs.forEach { slot ->
        val title = slot.name.lowercase().replaceFirstChar { it.titlecase() }
        Tab(
            selected = selectedTab == slot,
            onClick = { selectedTab = slot },
            text = { Text(title) })
      }
    }
    Spacer(Modifier.height(8.dp))

    val slotItems =
        remember(viewModel.accessoryCatalog, selectedTab) {
          viewModel.accessoryCatalog.filter { it.slot == selectedTab }
        }
    val equippedId = remember(user.accessories, selectedTab) { viewModel.equippedId(selectedTab) }

    AccessoriesGrid(
        items = slotItems,
        selectedId = equippedId,
        onSelect = { id -> viewModel.equip(selectedTab, id) })
  }
}

@Composable
private fun AccessoriesGrid(
    items: List<AccessoryItem>,
    selectedId: String?,
    onSelect: (String) -> Unit
) {
  LazyVerticalGrid(
      columns = GridCells.Adaptive(minSize = 96.dp),
      modifier = Modifier.fillMaxWidth().height(200.dp), // borné pour éviter scroll infini
      userScrollEnabled = false) {
        items(items, key = { "${it.slot}-${it.id}" }) { item ->
          val on = selectedId == item.id || (selectedId == null && item.id == "none")
          val rarityStroke =
              when (item.rarity) {
                Rarity.COMMON -> Color.White.copy(alpha = 0.25f)
                Rarity.RARE -> AccentBlue
                Rarity.EPIC -> AccentViolet
                Rarity.LEGENDARY -> Color(0xFFFFC107)
              }
          val stroke = if (on) rarityStroke else Color.White.copy(alpha = 0.25f)

          Card(
              shape = RoundedCornerShape(16.dp),
              colors =
                  CardDefaults.cardColors(
                      containerColor =
                          if (on) stroke.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.06f)),
              modifier =
                  Modifier.padding(6.dp)
                      .fillMaxWidth()
                      .height(84.dp)
                      .clickable { onSelect(item.id) }
                      .border(1.dp, stroke, RoundedCornerShape(16.dp))) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                  // Icône (drawable si fourni, sinon fallback)
                  if (item.iconRes != null) {
                    Image(
                        painter = painterResource(id = item.iconRes),
                        contentDescription = item.label,
                        modifier = Modifier.size(28.dp))
                  } else {
                    val fallback =
                        when (item.slot) {
                          AccessorySlot.HEAD -> Icons.Filled.Star
                          AccessorySlot.TORSO -> Icons.Filled.AutoAwesome
                          AccessorySlot.LEGS -> Icons.Filled.Star
                        }
                    androidx.compose.material3.Icon(
                        fallback,
                        contentDescription = item.label,
                        tint = stroke,
                        modifier = Modifier.size(26.dp))
                  }

                  // Label
                  Text(
                      item.label,
                      color = TextLight,
                      fontSize = 12.sp,
                      modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp))
                }
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
  Column(Modifier.padding(16.dp)) {
    Text("Settings", color = TextLight.copy(alpha = 0.8f), fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(8.dp))
    SettingRow(
        "🔔 Notifications",
        "Study reminders & updates",
        user.notificationsEnabled,
        onToggleNotifications,
        Modifier.testTag(ProfileScreenTestTags.SWITCH_NOTIFICATIONS))
    androidx.compose.material3.Divider(color = DarkDivider)
    SettingRow(
        "📍 Location Services",
        "Study spot suggestions",
        user.locationEnabled,
        onToggleLocation,
        Modifier.testTag(ProfileScreenTestTags.SWITCH_LOCATION))
    androidx.compose.material3.Divider(color = DarkDivider)
    SettingRow(
        "🎯 Focus Mode",
        "Minimize distractions",
        user.focusModeEnabled,
        onToggleFocusMode,
        Modifier.testTag(ProfileScreenTestTags.SWITCH_FOCUS_MODE))
  }
}

@Composable
fun SettingRow(
    title: String,
    desc: String,
    value: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
  Row(
      Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically) {
        Column {
          Text(title, color = TextLight)
          Text(desc, color = TextLight.copy(alpha = 0.6f), fontSize = 12.sp)
        }
        Switch(checked = value, onCheckedChange = { onToggle() }, modifier = modifier)
      }
}

@Composable
fun AccountActionsSection() {
  Column(Modifier.padding(12.dp)) {
    ActionButton("Privacy Policy") {}
    ActionButton("Terms of Service") {}
    ActionButton("Logout", textColor = Color.Red) {}
  }
}

@Composable
fun ActionButton(text: String, textColor: Color = TextLight, onClick: () -> Unit = {}) {
  androidx.compose.material3.TextButton(
      onClick = onClick, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(text, color = textColor, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
      }
}
