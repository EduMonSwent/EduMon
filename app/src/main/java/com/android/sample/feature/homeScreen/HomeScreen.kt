package com.android.sample.feature.homeScreen

// This code has been written partially using A.I (LLM).

// 🔽 Only dependency on creature UI:

import androidx.annotation.DrawableRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.sample.R
import com.android.sample.data.Status
import com.android.sample.data.ToDo
import com.android.sample.data.UserStats
import com.android.sample.screens.CreatureHouseCard

private const val DAYS_PER_WEEK = 7

/** ---------- Test tags used by UI tests ---------- */
object HomeTestTags {
  const val MENU_BUTTON = "home_menu_button"

  // Drawer & bottom bar items (use route so tests can be generic)
  fun drawerTag(route: String) = "drawer_$route"

  // Cards / chips / actions
  const val TODAY_SEE_ALL = "today_see_all"
  const val CHIP_OPEN_PLANNER = "chip_open_planner"
  const val CHIP_FOCUS_MODE = "chip_focus_mode"
  const val CHIP_MOOD = "chip_mood" // NEW

  const val QUICK_STUDY = "quick_study"
  const val QUICK_BREAK = "quick_break"
  const val QUICK_EXERCISE = "quick_exercise"
  const val QUICK_SOCIAL = "quick_social"
}

// ---------- Destinations ----------
enum class AppDestination(val route: String, val label: String, val icon: ImageVector) {
  Home("home", "Home", Icons.Outlined.Home),
  Profile("profile", "Profile", Icons.Outlined.Person),
  Schedule("schedule", "Schedule", Icons.Outlined.CalendarMonth),
  Study("study", "Study", Icons.Outlined.Timer),
  Games("games", "Games", Icons.Outlined.Extension),
  Stats("stats", "Stats", Icons.Outlined.ShowChart),
  Flashcards("flashcards", "Flashcards", Icons.Outlined.FlashOn),
  Todo("todo", "To-Do", Icons.Outlined.CheckBox),
  // NEW: Daily Reflection / Mood
  Mood("mood", "Daily Reflection", Icons.Outlined.Mood),
  StudyTogether("study_together", "Study Together", Icons.Outlined.Group),
  Shop("shop", "Shop", Icons.Outlined.ShoppingCart),
}

// ---------- Route (hooks up VM to UI) ----------
@Composable
fun EduMonHomeRoute(
    modifier: Modifier = Modifier,
    @DrawableRes creatureResId: Int = R.drawable.edumon,
    @DrawableRes environmentResId: Int = R.drawable.bg_pyrmon,
    onNavigate: (String) -> Unit,
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
  Column(
      modifier
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 16.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Creature + environment fully driven by parameters (no hard-coded sprite)
        CreatureHouseCard(
            creatureResId = creatureResId,
            level = state.creatureStats.level,
            environmentResId = environmentResId,
        )

        Row(
            Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
              UserStatsCard(stats = state.userStats, modifier = Modifier.weight(1f).fillMaxHeight())
            }

        AffirmationsAndRemindersCard(
            quote = state.quote,
            onOpenPlanner = { onNavigate(AppDestination.Schedule.route) },
            onOpenMood = { onNavigate(AppDestination.Mood.route) },
        )

        TodosObjectivesCarouselCard(
            todos = state.todos, // <-- use your actual HomeUiState field
            objectives = state.objectives, // <-- use your actual HomeUiState field
            onOpenTodos = { onNavigate(AppDestination.Todo.route) },
            onOpenObjectives = {
              onNavigate(AppDestination.Schedule.route)
            }, // adjust to your objectives screen
        )

        QuickActionsCard(
            onStudy = { onNavigate(AppDestination.Study.route) },
            onTakeBreak = { onNavigate(AppDestination.Games.route) },
            onExercise = { /* open exercise tips */},
            onSocial = { onNavigate(AppDestination.StudyTogether.route) },
        )

        Spacer(Modifier.height(72.dp))
      }
}

// ---------- Components ----------

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
              BigNumber("${stats.streak}d")
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
              BigNumber("${stats.todayStudyMinutes}m")
            }
            Column {
              Label("Weekly Goal")
              BigNumber("${stats.weeklyGoal}m")
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
fun AffirmationsAndRemindersCard(
    quote: String,
    onOpenPlanner: () -> Unit,
    onOpenMood: () -> Unit, // NEW
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
                label = { Text("Open Schedule") },
                leadingIcon = { Icon(Icons.Outlined.EventNote, contentDescription = null) },
                modifier = Modifier.testTag(HomeTestTags.CHIP_OPEN_PLANNER))
            AssistChip(
                onClick = onOpenMood,
                label = { Text("Daily Reflection") },
                leadingIcon = { Icon(Icons.Outlined.Mood, contentDescription = null) },
                modifier = Modifier.testTag(HomeTestTags.CHIP_MOOD))
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
              QuickButton(
                  "Study 30m",
                  Icons.Outlined.MenuBook,
                  Modifier.weight(1f).testTag(HomeTestTags.QUICK_STUDY),
                  onStudy)
              QuickButton(
                  "Take Break",
                  Icons.Outlined.Coffee,
                  Modifier.weight(1f).testTag(HomeTestTags.QUICK_BREAK),
                  onTakeBreak)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              QuickButton(
                  "Exercise",
                  Icons.Outlined.FitnessCenter,
                  Modifier.weight(1f).testTag(HomeTestTags.QUICK_EXERCISE),
                  onExercise)
              QuickButton(
                  "Social Time",
                  Icons.Outlined.Groups2,
                  Modifier.weight(1f).testTag(HomeTestTags.QUICK_SOCIAL),
                  onSocial)
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TodosObjectivesCarouselCard(
    todos: List<ToDo>,
    objectives: List<com.android.sample.feature.weeks.model.Objective>,
    onOpenTodos: () -> Unit,
    onOpenObjectives: () -> Unit,
    modifier: Modifier = Modifier,
) {
  val pendingTodos = remember(todos) { todos.filter { it.status != Status.DONE }.take(2) }
  val pendingObjectives = remember(objectives) { objectives.filter { !it.completed }.take(2) }

  val pagerState = rememberPagerState(pageCount = { 2 })

  ElevatedCard(
      modifier = modifier.fillMaxWidth().testTag("home_todo_objective_carousel"),
      colors =
          CardDefaults.elevatedCardColors(
              containerColor = MaterialTheme.colorScheme.surface,
              contentColor = MaterialTheme.colorScheme.onSurface),
      shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          HorizontalPager(
              state = pagerState, modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp)) { page
                ->
                when (page) {
                  0 -> PendingTodosSlide(todos = pendingTodos, onSeeAll = onOpenTodos)
                  1 -> ObjectivesSlide(objectives = pendingObjectives, onSeeAll = onOpenObjectives)
                }
              }

          PagerDots(
              count = 2,
              selectedIndex = pagerState.currentPage,
              modifier = Modifier.align(Alignment.CenterHorizontally))
        }
      }
}

@Composable
private fun PendingTodosSlide(todos: List<ToDo>, onSeeAll: () -> Unit) {
  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    SlideHeader(title = "To-dos (Pending)", actionText = "See all", onAction = onSeeAll)

    if (todos.isEmpty()) {
      Text("No pending to-dos.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    } else {
      todos.forEach { todo ->
        CompactRow(
            title = todo.title, // uses your ToDoForm field
            subtitle = todo.status.name.replace('_', ' '))
      }
    }
  }
}

@Composable
private fun ObjectivesSlide(
    objectives: List<com.android.sample.feature.weeks.model.Objective>,
    onSeeAll: () -> Unit
) {
  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    SlideHeader(title = "Objectives", actionText = "See all", onAction = onSeeAll)

    if (objectives.isEmpty()) {
      Text("No objectives to show.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    } else {
      objectives.forEach { obj ->
        val subtitle = buildString {
          append(obj.course)
          if (obj.estimateMinutes > 0) append(" • ${obj.estimateMinutes} min")
          append(" • ${obj.day.name.lowercase().replaceFirstChar { it.titlecase() }}")
        }
        CompactRow(title = obj.title, subtitle = subtitle)
      }
    }
  }
}

@Composable
private fun SlideHeader(title: String, actionText: String, onAction: () -> Unit) {
  Row(
      Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween) {
        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        TextButton(onClick = onAction, contentPadding = PaddingValues(0.dp)) { Text(actionText) }
      }
}

@Composable
private fun CompactRow(
    title: String,
    subtitle: String,
) {
  Surface(
      modifier = Modifier.fillMaxWidth(),
      color = MaterialTheme.colorScheme.surfaceVariant,
      contentColor = MaterialTheme.colorScheme.onSurface,
      shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Text(
              title, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
          Text(
              subtitle,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              fontSize = 12.sp,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis)
        }
      }
}

@Composable
private fun PagerDots(count: Int, selectedIndex: Int, modifier: Modifier = Modifier) {
  Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    repeat(count) { i ->
      val selected = i == selectedIndex
      Box(
          Modifier.size(if (selected) 10.dp else 8.dp)
              .clip(CircleShape)
              .background(
                  if (selected) MaterialTheme.colorScheme.primary
                  else MaterialTheme.colorScheme.outlineVariant))
    }
  }
}
