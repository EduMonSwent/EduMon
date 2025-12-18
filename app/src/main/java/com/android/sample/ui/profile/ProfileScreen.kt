package com.android.sample.ui.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Whatshot
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.sample.R
import com.android.sample.data.AccentVariant
import com.android.sample.data.AccessoryItem
import com.android.sample.data.AccessorySlot
import com.android.sample.data.Rarity
import com.android.sample.data.UserProfile
import com.android.sample.data.UserStats
import com.android.sample.ui.theme.UiValues

// This code has been written partially using A.I (LLM).
object ProfileScreenTestTags {
    const val PROFILE_SCREEN = "profileScreen"
    const val PET_SECTION = "petSection"
    const val PROFILE_CARD = "profileCard"
    const val STATS_CARD = "statsCard"
    const val CUSTOMIZE_PET_SECTION = "customizePetSection"
    const val SETTINGS_CARD = "settingsCard"
    const val ACCOUNT_ACTIONS_SECTION = "accountActionsSection"
    const val SWITCH_LOCATION = "switchLocation"
    const val SWITCH_FOCUS_MODE = "switchFocusMode"
}

private val CARD_CORNER_RADIUS = 16.dp
private val SECTION_SPACING = 16.dp
private val SCREEN_PADDING = 60.dp
private const val STREAK_PLURAL_THRESHOLD = 1
private const val LEVEL_PROGRESS_ANIM_DURATION_MS = 600
private const val LABEL_FONT_SIZE_SP = 12
private const val VALUE_FONT_SIZE_SP = 11
private const val SMALL_FONT_SIZE = 8

private val LEVEL_BAR_HEIGHT = SMALL_FONT_SIZE.dp
private val LEVEL_BAR_CORNER_RADIUS = 12.dp
private val LABEL_ALPHA = 0.7f
private val SPACING_SMALL = 4.dp

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = viewModel(),
    onOpenFocusMode: () -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    onImportIcs: () -> Unit = {},
    onSignOut: () -> Unit = {}
) {
    val user by viewModel.userProfile.collectAsState()
    val stats by viewModel.userStats.collectAsState()
    val accent by viewModel.accentEffective.collectAsState()
    val variant by viewModel.accentVariantFlow.collectAsState()
    val context = LocalContext.current
    val launcher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                viewModel.importIcs(context, uri)
            }
        }

    val snackbarHostState = remember { SnackbarHostState() }

    // LevelUpRewardSnackbarHandler(viewModel = viewModel, snackbarHostState = snackbarHostState)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }, containerColor = Color.Transparent) {
            innerPadding ->
        LazyColumn(
            modifier =
                Modifier.fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.background,
                                MaterialTheme.colorScheme.surface,
                            )))
                    .padding(bottom = SCREEN_PADDING)
                    .padding(innerPadding)
                    .testTag(ProfileScreenTestTags.PROFILE_SCREEN),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(vertical = SECTION_SPACING),
            verticalArrangement = Arrangement.spacedBy(SECTION_SPACING)) {
            item { PetSection(viewModel = viewModel) }
            item {
                GlowCard {
                    Box(Modifier.testTag(ProfileScreenTestTags.PROFILE_CARD)) { ProfileCard(user) }
                }
            }
            item {
                GlowCard {
                    Box(Modifier.testTag(ProfileScreenTestTags.STATS_CARD)) { StatsCard(user, stats) }
                }
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
                            onToggleLocation = viewModel::toggleLocation,
                            onToggleFocusMode = viewModel::toggleFocusMode,
                            onOpenNotifications = onOpenNotifications,
                            onEnterFocusMode = onOpenFocusMode,
                            onImportIcs = { launcher.launch("text/calendar") })
                    }
                }
            }
            item {
                GlowCard {
                    Box(Modifier.testTag(ProfileScreenTestTags.ACCOUNT_ACTIONS_SECTION)) {
                        AccountActionsSection(onSignOut = onSignOut)
                    }
                }
            }
        }
    }
}

@Composable
fun PetSection(viewModel: ProfileViewModel, modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.surfaceVariant,
                            MaterialTheme.colorScheme.background,
                        )))
                .padding(vertical = 20.dp, horizontal = 16.dp)
                .testTag(ProfileScreenTestTags.PET_SECTION)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                StatBar("❤", 0.9f, MaterialTheme.colorScheme.error)
                StatBar("💡", 0.85f, MaterialTheme.colorScheme.tertiary)
                StatBar("⚡", 0.7f, MaterialTheme.colorScheme.secondary)
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center) {
                EduMonAvatar(
                    viewModel = viewModel,
                    showLevelLabel = true,
                    avatarSize = UiValues.AvatarSize)
            }
        }
    }
}

@Composable
fun StatBar(icon: String, percent: Float, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(icon, fontSize = 16.sp)
        Spacer(Modifier.width(4.dp))
        Box(
            Modifier.width(70.dp)
                .height(10.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))) {
            Box(
                Modifier.fillMaxSize()
                    .width(70.dp * percent)
                    .background(color, RoundedCornerShape(10.dp)))
        }
        Spacer(Modifier.width(4.dp))
        Text(
            "${(percent * 100).toInt()}%",
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            fontSize = 12.sp)
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

    val glowColor = MaterialTheme.colorScheme.primary

    Card(
        modifier =
            Modifier.fillMaxWidth(0.9f)
                .shadow(
                    elevation = 16.dp,
                    ambientColor = glowColor.copy(alpha = glow),
                    spotColor = glowColor.copy(alpha = glow),
                    shape = RoundedCornerShape(CARD_CORNER_RADIUS)),
        shape = RoundedCornerShape(CARD_CORNER_RADIUS),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        content()
    }
}

@Composable
fun ProfileCard(user: UserProfile) {
    // Extract initials from the user's name
    val initials =
        user.name
            .split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
            .ifEmpty { user.name.take(2).uppercase() }

    val cs = MaterialTheme.colorScheme

    Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()) {
            Box(
                Modifier.size(70.dp).background(cs.primary, shape = RoundedCornerShape(50.dp)),
                contentAlignment = Alignment.Center) {
                Text(initials, color = cs.onPrimary, fontWeight = FontWeight.Bold, fontSize = 22.sp)
            }
            Spacer(Modifier.width(20.dp))
            Image(
                painter = painterResource(id = R.drawable.epfl),
                contentDescription = "EPFL Logo",
                modifier = Modifier.height(28.dp).width(60.dp))
        }
        Spacer(Modifier.height(SMALL_FONT_SIZE.dp))
        Text(user.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = cs.onSurface)
        Text(user.email, color = cs.onSurface.copy(alpha = 0.7f), fontSize = 14.sp)
        Spacer(Modifier.height(SMALL_FONT_SIZE.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(SMALL_FONT_SIZE.dp)) {
            Badge(text = "Level ${user.level}", bg = cs.primary)
            Badge(text = "${user.points} pts", bg = cs.surface, textColor = cs.primary)
        }
        Spacer(Modifier.height(SMALL_FONT_SIZE.dp))
        LevelProgressBar(level = user.level, points = user.points)
    }
}

@Composable
fun Badge(text: String, bg: Color, textColor: Color = Color.White) {
    Box(
        Modifier.background(bg, RoundedCornerShape(12.dp))
            .padding(horizontal = SMALL_FONT_SIZE.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center) {
        Text(text, color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun StatsCard(
    profile: UserProfile,
    stats: UserStats,
) {
    val cs = MaterialTheme.colorScheme

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = stringResource(id = R.string.stats_title),
            fontWeight = FontWeight.SemiBold,
            color = cs.onSurface.copy(alpha = 0.8f))
        Spacer(modifier = Modifier.height(SMALL_FONT_SIZE.dp))

        val streakUnit =
            if (stats.streak > STREAK_PLURAL_THRESHOLD) {
                stringResource(id = R.string.days)
            } else {
                "day"
            }

        StatRow(Icons.Outlined.Whatshot, stringResource(id = R.string.stats_streak), "${stats.streak} $streakUnit")
        StatRow(Icons.Outlined.Star, stringResource(id = R.string.stats_points), "${stats.points}")
        StatRow(Icons.Outlined.AttachMoney, stringResource(id = R.string.stats_coins), "${stats.coins}")
        StatRow(
            Icons.AutoMirrored.Outlined.MenuBook,
            stringResource(id = R.string.stats_study_time),
            "${stats.todayStudyMinutes} ${stringResource(R.string.minute)}")
        StatRow(
            Icons.Outlined.Flag,
            stringResource(id = R.string.stats_goal),
            "${profile.studyStats.dailyGoalMin} ${stringResource(R.string.minute)}")
    }
}

@Composable
fun StatRow(icon: ImageVector, label: String, value: String) {
    val cs = MaterialTheme.colorScheme

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = cs.primary)
            Spacer(modifier = Modifier.width(SMALL_FONT_SIZE.dp))
            Text(label, color = cs.onSurface.copy(alpha = 0.9f))
        }
        Text(value, color = cs.onSurface, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun CustomizePetSection(viewModel: ProfileViewModel) {
    val user by viewModel.userProfile.collectAsState()
    val currentVariant by viewModel.accentVariantFlow.collectAsState()
    val cs = MaterialTheme.colorScheme

    Column(Modifier.padding(16.dp)) {
        Text("Customize Buddy", color = cs.onSurface.copy(alpha = 0.8f), fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))

        Text("Accent color", color = cs.onSurface.copy(alpha = 0.7f), fontSize = 13.sp)
        Spacer(Modifier.height(SMALL_FONT_SIZE.dp))
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
                                if (selected) cs.onSurface else cs.onSurface.copy(alpha = 0.25f),
                                CircleShape)
                            .clickable { viewModel.setAvatarAccent(c) },
                    contentAlignment = Alignment.Center) {
                    if (selected) Icon(Icons.Outlined.Check, null, tint = cs.onSurface)
                }
            }
        }

        Spacer(Modifier.height(SMALL_FONT_SIZE.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(SMALL_FONT_SIZE.dp)) {
            AccentVariant.values().forEach { v ->
                FilterChip(
                    selected = v == currentVariant,
                    onClick = { viewModel.setAccentVariant(v) },
                    label = { Text(v.name) },
                    modifier = Modifier.widthIn(min = 72.dp))
            }
        }

        Spacer(Modifier.height(20.dp))

        Text("Inventory", color = cs.onSurface.copy(alpha = 0.8f), fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(10.dp))

        var selectedTab by remember { mutableStateOf(AccessorySlot.HEAD) }
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
        Spacer(Modifier.height(SMALL_FONT_SIZE.dp))

        val userState by viewModel.userProfile.collectAsState()

        val slotItems =
            remember(userState, selectedTab) {
                viewModel.accessoryCatalog.filter { it.slot == selectedTab }
            }

        val equippedId =
            remember(userState.accessories, selectedTab) { viewModel.equippedId(selectedTab) }

        AccessoriesGrid(
            items = slotItems,
            selectedId = equippedId,
            onSelect = { id -> viewModel.equip(selectedTab, id) })
    }
}

@Composable
fun AccessoriesGrid(items: List<AccessoryItem>, selectedId: String?, onSelect: (String) -> Unit) {
    val cs = MaterialTheme.colorScheme

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 96.dp),
        modifier = Modifier.fillMaxWidth().height(200.dp),
        userScrollEnabled = false) {
        items(items, key = { "${it.slot}-${it.id}" }) { item ->
            val on = selectedId == item.id || (selectedId == null && item.id == "none")
            val rarityStroke =
                when (item.rarity) {
                    Rarity.COMMON -> cs.outlineVariant
                    Rarity.RARE -> cs.secondary
                    Rarity.EPIC -> cs.primary
                    Rarity.LEGENDARY -> Color(0xFFFFC107)
                }
            val stroke = if (on) rarityStroke else cs.outlineVariant

            Card(
                shape = RoundedCornerShape(16.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            if (on) stroke.copy(alpha = 0.16f) else cs.surfaceVariant.copy(alpha = 0.35f)),
                modifier =
                    Modifier.padding(6.dp)
                        .fillMaxWidth()
                        .height(84.dp)
                        .clickable { onSelect(item.id) }
                        .border(1.dp, stroke, RoundedCornerShape(16.dp))) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
                                AccessorySlot.BACK -> Icons.Filled.BrightnessLow
                            }
                        Icon(
                            fallback,
                            contentDescription = item.label,
                            tint = stroke,
                            modifier = Modifier.size(26.dp))
                    }
                    Text(
                        item.label,
                        color = cs.onSurface,
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
    onToggleLocation: () -> Unit,
    onToggleFocusMode: () -> Unit,
    onOpenNotifications: () -> Unit,
    onEnterFocusMode: () -> Unit,
    onImportIcs: () -> Unit
) {
    val cs = MaterialTheme.colorScheme

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            stringResource(id = R.string.settings_title),
            color = cs.onSurface.copy(alpha = 0.8f),
            fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(SMALL_FONT_SIZE.dp))

        SettingRow(
            stringResource(id = R.string.settings_location),
            stringResource(id = R.string.settings_location_desc),
            user.locationEnabled,
            onToggleLocation,
            modifier = Modifier.testTag(ProfileScreenTestTags.SWITCH_LOCATION))

        Divider(color = cs.outlineVariant)

        SettingRow(
            stringResource(id = R.string.settings_focus),
            stringResource(id = R.string.settings_focus_desc),
            user.focusModeEnabled,
            onToggle = {
                onToggleFocusMode()
                if (!user.focusModeEnabled) {
                    onEnterFocusMode()
                }
            },
            modifier = Modifier.testTag(ProfileScreenTestTags.SWITCH_FOCUS_MODE))

        Divider(color = cs.outlineVariant)
        Spacer(Modifier.height(12.dp))
        TextButton(
            onClick = onOpenNotifications,
            modifier = Modifier.fillMaxWidth().testTag("open_notifications_screen")) {
            Text("Manage notifications")
        }
        Divider(color = cs.outlineVariant)
        Spacer(Modifier.height(12.dp))

        TextButton(onClick = onImportIcs, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.import_timetable),
                color = cs.primary,
                fontWeight = FontWeight.Medium)
        }
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
    val cs = MaterialTheme.colorScheme

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text(title, color = cs.onSurface)
            Text(desc, color = cs.onSurface.copy(alpha = 0.6f), fontSize = 12.sp)
        }
        Switch(checked = value, onCheckedChange = { onToggle() }, modifier = modifier)
    }
}

@Composable
fun AccountActionsSection(onSignOut: () -> Unit = {}) {
    Column(modifier = Modifier.padding(12.dp)) {
        ActionButton(stringResource(id = R.string.account_privacy)) {}
        ActionButton(stringResource(id = R.string.account_terms)) {}
        ActionButton(
            stringResource(id = R.string.account_logout), textColor = MaterialTheme.colorScheme.error, onClick = onSignOut)
    }
}

@Composable
fun ActionButton(text: String, textColor: Color = MaterialTheme.colorScheme.onSurface, onClick: () -> Unit = {}) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(text, color = textColor, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
    }
}

private const val PROGRESS_TO_NEXT_LEVEL = "Progress to next level"

@Composable
fun LevelProgressBar(level: Int, points: Int) {
    val currentLevelBase = LevelingConfig.pointsForLevel(level)
    val nextLevelBase = LevelingConfig.pointsForLevel(level + 1)

    val levelRange = (nextLevelBase - currentLevelBase).coerceAtLeast(1)
    val rawProgressPoints = (points - currentLevelBase).coerceIn(0, levelRange)
    val targetFraction = rawProgressPoints.toFloat() / levelRange.toFloat()

    val animatedFraction by
    animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = tween(durationMillis = 600),
        label = "levelProgressAnim")

    val cs = MaterialTheme.colorScheme

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
        Text(text = PROGRESS_TO_NEXT_LEVEL, color = cs.onSurface.copy(alpha = 0.7f), fontSize = 12.sp)

        Spacer(Modifier.height(4.dp))

        Box(
            modifier =
                Modifier.fillMaxWidth()
                    .height(SMALL_FONT_SIZE.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(cs.surfaceVariant)) {
            Box(
                modifier =
                    Modifier.fillMaxWidth(animatedFraction.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(cs.primary))
        }

        Spacer(Modifier.height(4.dp))

        val remaining = nextLevelBase - points

        Text(
            text = "$rawProgressPoints / $levelRange pts  •  $remaining pts to next level",
            color = cs.onSurface.copy(alpha = 0.7f),
            fontSize = 11.sp)
    }
}
