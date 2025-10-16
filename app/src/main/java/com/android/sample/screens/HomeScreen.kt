package com.android.sample.screens

import android.content.res.Configuration
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.sample.R
import com.android.sample.data.Status
import com.android.sample.data.ToDo
import com.android.sample.data.UserStats
import com.android.sample.repositories.HomeUiState
import com.android.sample.repositories.HomeViewModel
import com.android.sample.ui.theme.AccentViolet
import com.android.sample.ui.theme.MidDarkCard
import kotlinx.coroutines.launch

// ---------- Destinations ----------
enum class AppDestination(val route: String, val label: String, val icon: ImageVector) {
    Home("home", "Home", Icons.Outlined.Home),
    Profile("profile", "Profile", Icons.Outlined.Person),
    Calendar("calendar", "Calendar", Icons.Outlined.CalendarMonth),
    Planner("planner", "Planner", Icons.Outlined.EventNote),
    Study("study", "Study", Icons.Outlined.Timer),
    Games("games", "Games", Icons.Outlined.Extension),
    Stats("stats", "Stats", Icons.Outlined.ShowChart),
    Flashcards("flashcards", "Flashcards", Icons.Outlined.FlashOn),
}

// ---------- Route (hooks up VM to UI) ----------
@Composable
fun EduMonHomeRoute(
    creatureResId: Int,
    environmentResId: Int,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    vm: HomeViewModel = viewModel(),
) {
    val state by vm.uiState.collectAsState()

    if (state.isLoading) {
        Box(modifier.fillMaxSize()) { CircularProgressIndicator(Modifier.align(Alignment.Center)) }
        return
    }

    EduMonHomeScreen(
        state = state,
        creatureResId = creatureResId,
        environmentResId = environmentResId,
        onNavigate = onNavigate,
        modifier = modifier)
}

// ---------- Screen ----------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EduMonHomeScreen(
    state: HomeUiState,
    creatureResId: Int,
    environmentResId: Int,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                drawerContentColor = MaterialTheme.colorScheme.onSurface) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Edumon",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 20.dp))
                Text(
                    text = "EPFL Companion",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp))
                Spacer(Modifier.height(8.dp))
                AppDestination.values().forEach { dest ->
                    NavigationDrawerItem(
                        label = { Text(dest.label) },
                        selected = dest == AppDestination.Home,
                        onClick = { onNavigate(dest.route) },
                        icon = { Icon(dest.icon, contentDescription = dest.label) },
                        colors =
                            NavigationDrawerItemDefaults.colors(
                                unselectedTextColor = MaterialTheme.colorScheme.onSurface,
                                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                unselectedContainerColor = Color.Transparent,
                                selectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                unselectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                            ),
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding))
                }
                Spacer(Modifier.height(8.dp))
            }
        }) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {},
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Outlined.Menu, contentDescription = "Menu")
                        }
                    },
                    colors =
                        TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background,
                            navigationIconContentColor = MaterialTheme.colorScheme.onSurface))
            },
            bottomBar = { BottomNavBar(onNavigate = onNavigate) }) { padding ->
            Column(
                modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CreatureHouseCard(
                    creatureResId = creatureResId,
                    level = state.creatureStats.level,
                    environmentResId = environmentResId)

                Row(
                    Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CreatureStatsCard(
                        stats = state.creatureStats,
                        modifier = Modifier.weight(1f).fillMaxHeight())
                    UserStatsCard(
                        stats = state.userStats,
                        modifier = Modifier.weight(1f).fillMaxHeight())
                }

                AffirmationsAndRemindersCard(
                    quote = state.quote,
                    onOpenPlanner = { onNavigate(AppDestination.Planner.route) },
                )

                TodayTodosCard(
                    todos = state.todos,
                    onSeeAll = { onNavigate(AppDestination.Planner.route) })

                QuickActionsCard(
                    onStudy = { onNavigate(AppDestination.Study.route) },
                    onTakeBreak = { /* start break */},
                    onExercise = { /* open exercise tips */},
                    onSocial = { /* open social time */},
                )
                Spacer(Modifier.height(72.dp))
            }
        }
    }
}

// ---------- Components ----------
@Composable
private fun BottomNavBar(onNavigate: (String) -> Unit) {
    val items =
        listOf(
            AppDestination.Home,
            AppDestination.Calendar,
            AppDestination.Study,
            AppDestination.Profile,
            AppDestination.Games,
        )
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        items.forEach { item ->
            NavigationBarItem(
                selected = item == AppDestination.Home,
                onClick = { onNavigate(item.route) },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                colors =
                    NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = .65f),
                        selectedTextColor = MaterialTheme.colorScheme.onSurface,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = .65f),
                    ))
        }
    }
}

@Composable
fun UserStatsCard(stats: UserStats, modifier: Modifier = Modifier) {
    ElevatedCard(
        modifier,
        colors =
            CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface),
        shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text("Your Stats", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Label("Streak")
                    BigNumber("${stats.streakDays}d")
                }
                Column {
                    Label("Points")
                    BigNumber("${stats.points}")
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Label("Study Today")
                    BigNumber("${stats.studyTodayMin}m")
                }
                Column {
                    Label("Daily Goal")
                    BigNumber("${stats.dailyGoalMin}m")
                }
            }
        }
    }
}

@Composable
private fun Label(text: String) {
    Text(text, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
}

@Composable
private fun BigNumber(text: String) {
    Text(
        text,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        color = MaterialTheme.colorScheme.primary)
}

@Composable
fun TodayTodosCard(
    todos: List<ToDo>,
    onSeeAll: () -> Unit,
) {
    ElevatedCard(
        colors =
            CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface),
        shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Today",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f))
                Text(
                    "See all",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onSeeAll() })
            }
            Spacer(Modifier.height(6.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                todos.take(3).forEach { t ->
                    val isDone = t.status == Status.DONE
                    val secondary = MaterialTheme.colorScheme.onSurface.copy(alpha = .70f)

                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        if (isDone) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = .15f),
                                shape = CircleShape,
                                modifier = Modifier.size(28.dp)) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Outlined.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary)
                                }
                            }
                        } else {
                            Surface(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = .12f),
                                shape = CircleShape,
                                modifier = Modifier.size(28.dp)) {}
                        }

                        Column(Modifier.padding(start = 10.dp).weight(1f)) {
                            Text(
                                t.title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface,
                                textDecoration = if (isDone) TextDecoration.LineThrough else null)
                            Text(
                                if (isDone) "Completed" else t.dueDateFormatted(),
                                color = if (isDone) MaterialTheme.colorScheme.secondary else secondary,
                                fontSize = 12.sp)
                        }

                        Icon(
                            imageVector =
                                if (isDone) Icons.Outlined.CheckCircle else Icons.Outlined.ChevronRight,
                            contentDescription = null,
                            tint =
                                if (isDone) MaterialTheme.colorScheme.secondary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = .65f))
                    }
                }
            }
        }
    }
}

@Composable
fun AffirmationsAndRemindersCard(
    quote: String,
    onOpenPlanner: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        colors =
            CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface),
        shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Affirmation", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Text(quote)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AssistChip(
                    onClick = onOpenPlanner,
                    label = { Text("Open Planner") },
                    leadingIcon = { Icon(Icons.Outlined.EventNote, contentDescription = null) })
                AssistChip(
                    onClick = {},
                    label = { Text("Focus Mode") },
                    leadingIcon = { Icon(Icons.Outlined.DoNotDisturbOn, contentDescription = null) })
            }
        }
    }
}

@Composable
fun QuickActionsCard(
    onStudy: () -> Unit,
    onTakeBreak: () -> Unit,
    onExercise: () -> Unit,
    onSocial: () -> Unit,
) {
    ElevatedCard(
        colors =
            CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface),
        shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text("Quick Actions", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Spacer(Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickButton("Study 30m", Icons.Outlined.MenuBook, Modifier.weight(1f), onStudy)
                    QuickButton("Take Break", Icons.Outlined.Coffee, Modifier.weight(1f), onTakeBreak)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickButton("Exercise", Icons.Outlined.FitnessCenter, Modifier.weight(1f), onExercise)
                    QuickButton("Social Time", Icons.Outlined.Groups2, Modifier.weight(1f), onSocial)
                }
            }
        }
    }
}

@Composable
private fun QuickButton(
    text: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier.height(56.dp).clip(RoundedCornerShape(16.dp)).clickable { onClick() },
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(icon, contentDescription = null)
            Text(text)
        }
    }
}

// Optional fancy card (uses your theme colors)
@Composable
fun GlowCard(content: @Composable () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "glowAnim")
    val glowAlpha by
    infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.6f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 2500, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse),
        label = "glowAlpha")

    Card(
        modifier = Modifier.fillMaxWidth(0.9f).shadow(16.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MidDarkCard)) {
        Box(
            modifier =
                Modifier.background(
                    Brush.radialGradient(
                        colors =
                            listOf(AccentViolet.copy(alpha = glowAlpha), Color.Transparent)))) {
            content()
        }
    }
}

// ---------- Preview ----------
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun HomePreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        // Replace with your real drawables
        EduMonHomeRoute(
            creatureResId = R.drawable.edumon, environmentResId = R.drawable.home, onNavigate = {})
    }
}