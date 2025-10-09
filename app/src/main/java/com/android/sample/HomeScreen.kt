package com.android.sample

import android.content.res.Configuration
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import androidx.compose.ui.text.style.TextDecoration
import com.android.sample.ui.theme.AccentViolet
import com.android.sample.ui.theme.MidDarkCard


// =============================================================
// Models & Mock Data (replace with your ViewModel as needed)
// =============================================================

data class Todo(
    val id: String,
    val title: String,
    val due: String,
    val done: Boolean = false,
)

data class CreatureStats(
    val happiness: Int = 85,
    val health: Int = 90,
    val energy: Int = 70,
    val level: Int = 5,
)

data class UserStats(
    val streakDays: Int = 7,
    val points: Int = 1250,
    val studyTodayMin: Int = 45,
    val dailyGoalMin: Int = 180,
)

private val sampleTodos = listOf(
    Todo("1", "CS-101: Finish exercise sheet", "Today 18:00", true),
    Todo("2", "Math review: sequences", "Today 20:00"),
    Todo("3", "Pack lab kit for tomorrow", "Tomorrow"),
)

private val quotes = listOf(
    "Small consistent steps beat intense sprints.",
    "Study now, thank yourself later.",
    "Progress over perfection, always.",
    "You don't have to do it fast — just do it.",
    "Your future self is watching. Keep going.",
)

// =============================================================
// Destinations for drawer & bottom bar
// =============================================================

enum class AppDestination(val route: String, val label: String, val icon: ImageVector) {
    Home("home", "Home", Icons.Outlined.Home),
    Profile("profile", "Profile", Icons.Outlined.Person),
    Shop("shop", "Shop", Icons.Outlined.ShoppingBag),
    Calendar("calendar", "Calendar", Icons.Outlined.CalendarMonth),
    Planner("planner", "Planner", Icons.Outlined.EventNote),
    Study("study", "Study Session", Icons.Outlined.Timer),
    Rankings("rankings", "Rankings", Icons.Outlined.Leaderboard),
    Settings("settings", "Settings", Icons.Outlined.Settings),
    Games("games", "Games", Icons.Outlined.Extension),
}

// =============================================================
// Public API
// =============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EduMonHomeScreen(
    creatureResId: Int, // e.g. R.drawable.creature
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    todos: List<Todo> = sampleTodos,
    creatureStats: CreatureStats = CreatureStats(),
    userStats: UserStats = UserStats(),
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                drawerContentColor   = MaterialTheme.colorScheme.onSurface
            ) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Edumon",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Text(
                    text = "EPFL Companion",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp)
                )
                Spacer(Modifier.height(8.dp))
                AppDestination.values().forEach { dest ->
                    NavigationDrawerItem(
                        label = { Text(dest.label) },
                        selected = dest == AppDestination.Home,
                        onClick = { onNavigate(dest.route) },
                        icon = { Icon(dest.icon, contentDescription = dest.label) },
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedTextColor     = MaterialTheme.colorScheme.onSurface,
                            selectedTextColor       = MaterialTheme.colorScheme.onSurface,
                            unselectedContainerColor= Color.Transparent,
                            selectedContainerColor  = MaterialTheme.colorScheme.surfaceVariant,
                            unselectedIconColor     = MaterialTheme.colorScheme.primary,
                            selectedIconColor       = MaterialTheme.colorScheme.primary,
                        ),
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    ) {
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
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            bottomBar = {
                BottomNavBar(onNavigate = onNavigate)
            }
        ) { padding ->
            Column(
                modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CreatureHouseCard(
                    creatureResId = creatureResId,
                    level = creatureStats.level,
                    environmentResId = R.drawable.home
                )

                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CreatureStatsCard(
                        stats = creatureStats,
                        modifier = Modifier.weight(1f)
                                            .fillMaxHeight()
                    )
                    UserStatsCard(
                        stats = userStats,
                        modifier = Modifier.weight(1f) .fillMaxHeight()
                    )
                }

                AffirmationsAndRemindersCard(
                    quote = dailyQuote(),
                    onOpenPlanner = { onNavigate(AppDestination.Planner.route) },
                )

                TodayTodosCard(
                    todos = todos,
                    onSeeAll = { onNavigate(AppDestination.Planner.route) }
                )

                QuickActionsCard(
                    onStudy = { onNavigate(AppDestination.Study.route) },
                    onTakeBreak = { /* start break */ },
                    onExercise = { /* open exercise tips */ },
                    onSocial = { /* open social time */ },
                )
                Spacer(Modifier.height(72.dp))
            }
        }
    }
}

// =============================================================
// Bottom Navigation
// =============================================================

@Composable
private fun BottomNavBar(onNavigate: (String) -> Unit) {
    val items = listOf(
        AppDestination.Home,
        AppDestination.Calendar,
        AppDestination.Shop,
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
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor      = MaterialTheme.colorScheme.surfaceVariant,
                    selectedIconColor   = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = .65f),
                    selectedTextColor   = MaterialTheme.colorScheme.onSurface,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = .65f),
                )
            )
        }
    }
}

// =============================================================
// Cards
// =============================================================

@Composable
fun CreatureHouseCard(
    creatureResId: Int,
    level: Int,
    environmentResId: Int,
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
                .height(220.dp)         // tweak as you like
        ) {
            // BACK: environment image
            Image(
                painter = painterResource(environmentResId),
                contentDescription = "Creature environment",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // optional floor glow
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(0.7f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = .18f))
            )

            // FRONT: creature (superposed)
            CreatureSprite(
                resId = creatureResId,
                size = 120.dp,                             // fixed size
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = (-28).dp)                  // lift above cushion
            )

            // Level chip
            AssistChip(
                onClick = {},
                label = { Text("Lv $level", color = MaterialTheme.colorScheme.primary) },
                leadingIcon = {
                    Icon(Icons.Outlined.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary,)
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor          = MaterialTheme.colorScheme.surfaceVariant,
                    labelColor              = MaterialTheme.colorScheme.onSurface,
                    leadingIconContentColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.align(Alignment.TopStart).padding(10.dp)
            )
        }
    }
}



@Composable
private fun CreatureSprite(resId: Int, modifier: Modifier = Modifier, size: Dp = 120.dp) {
    val inf = rememberInfiniteTransition(label = "float")
    val offset by inf.animateFloat(
        initialValue = 0f,
        targetValue = -10f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "offset"
    )
    val glowAlpha by inf.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "glow"
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
            modifier = Modifier
                .size(size)
                .offset(y = offset.dp)
        )
    }
}

@Composable
fun CreatureStatsCard(stats: CreatureStats, modifier: Modifier = Modifier) {
    ElevatedCard(
        modifier,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor   = MaterialTheme.colorScheme.onSurface
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Buddy Stats", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            StatRow("Happiness", stats.happiness, Icons.Outlined.FavoriteBorder, barColor = MaterialTheme.colorScheme.primary)
            StatRow("Health", stats.health, Icons.Outlined.LocalHospital, barColor = MaterialTheme.colorScheme.secondary)
            StatRow("Energy", stats.energy, Icons.Outlined.Bolt, barColor = MaterialTheme.colorScheme.tertiary)
        }
    }
}

@Composable
fun UserStatsCard(stats: UserStats, modifier: Modifier = Modifier) {
    ElevatedCard(
        modifier,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor   = MaterialTheme.colorScheme.onSurface
        ),        shape = RoundedCornerShape(20.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Your Stats", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column { Label("Streak"); BigNumber("${stats.streakDays}d") }
                Column { Label("Points"); BigNumber("${stats.points}") }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column { Label("Study Today"); BigNumber("${stats.studyTodayMin}m") }
                Column { Label("Daily Goal"); BigNumber("${stats.dailyGoalMin}m") }
            }
        }
    }
}

@Composable private fun Label(text: String) {
    Text(text, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
}
@Composable private fun BigNumber(text: String) {
    Text(text, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
}

@Composable
fun TodayTodosCard(
    todos: List<Todo>,
    onSeeAll: () -> Unit,
) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor   = MaterialTheme.colorScheme.onSurface
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Today",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "See all",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onSeeAll() }
                )
            }
            Spacer(Modifier.height(6.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                todos.take(3).forEach { t ->
                    val secondary = MaterialTheme.colorScheme.onSurface.copy(alpha = .70f)

                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Leading badge
                        if (t.done) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = .15f),
                                shape = CircleShape,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Outlined.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        } else {
                            Surface(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = .12f),
                                shape = CircleShape,
                                modifier = Modifier.size(28.dp)
                            ) {}
                        }

                        // Title + meta
                        Column(
                            Modifier
                                .padding(start = 10.dp)
                                .weight(1f)
                        ) {
                            Text(
                                t.title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface,
                                textDecoration = if (t.done) TextDecoration.LineThrough else null
                            )
                            Text(
                                if (t.done) "Completed" else t.due,
                                color = if (t.done) MaterialTheme.colorScheme.secondary else secondary,
                                fontSize = 12.sp
                            )
                        }

                        // Trailing icon
                        Icon(
                            imageVector = if (t.done) Icons.Outlined.CheckCircle else Icons.Outlined.ChevronRight,
                            contentDescription = null,
                            tint = if (t.done) MaterialTheme.colorScheme.secondary
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = .65f)
                        )
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
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor   = MaterialTheme.colorScheme.onSurface
        ),        shape = RoundedCornerShape(20.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Affirmation", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Text(quote)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AssistChip(onClick = onOpenPlanner, label = { Text("Open Planner") }, leadingIcon = { Icon(Icons.Outlined.EventNote, contentDescription = null) })
                AssistChip(onClick = {}, label = { Text("Focus Mode") }, leadingIcon = { Icon(Icons.Outlined.DoNotDisturbOn, contentDescription = null) })
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
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor   = MaterialTheme.colorScheme.onSurface
        ),        shape = RoundedCornerShape(20.dp)
    ) {
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
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        color        = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(icon, contentDescription = null)
            Text(text)
        }
    }
}

// =============================================================
// Small UI bits
// =============================================================

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
            Text("${value}%", color = MaterialTheme.colorScheme.onSurface)
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = value / 100f,
            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = .20f),
            color      = barColor,
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(8.dp))
        )
    }
}

// =============================================================
// Helpers
// =============================================================

@Composable
private fun dailyQuote(): String {
    val idx = remember {
        // rotate by day
        (System.currentTimeMillis() / 86_400_000L % quotes.size).toInt()
    }
    return quotes[idx]
}

// Optional preview (requires a drawable named creature_epfl)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun HomePreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        EduMonHomeScreen(
            creatureResId = R.drawable.edumon,
            onNavigate = {}
        )
    }
}
@Composable
fun GlowCard(content: @Composable () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "glowAnim")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .shadow(16.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MidDarkCard)
    ) {
        Box(
            modifier = Modifier.background(
                Brush.radialGradient(
                    colors = listOf(
                        AccentViolet.copy(alpha = glowAlpha),
                        Color.Transparent
                    )
                )
            )
        ) {
            content()
        }
    }
}