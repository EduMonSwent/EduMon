package com.android.sample.feature.homeScreen

// This code has been written partially using A.I (LLM).

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.EventNote
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Mood
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.VideogameAsset
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.sample.data.Status
import com.android.sample.data.ToDo
import com.android.sample.data.UserStats
import com.android.sample.screens.CreatureHouseCard
import com.android.sample.ui.profile.EduMonAvatar

/** ---------- Test tags used by UI tests ---------- */
object HomeTestTags {
  const val MENU_BUTTON = "home_menu_button"

  // Drawer & bottom bar items (use route so tests can be generic)
  fun drawerTag(route: String) = "drawer_$route"

  // Cards / chips / actions
  const val TODAY_SEE_ALL = "today_see_all"
  const val CHIP_OPEN_PLANNER = "chip_open_planner"
  const val CHIP_MOOD = "chip_mood" // NEW

  const val QUICK_STUDY = "quick_study"
  const val QUICK_BREAK = "quick_break"
  const val QUICK_SOCIAL = "quick_social"
}

// ---------- Destinations ----------
enum class AppDestination(val route: String, val label: String, val icon: ImageVector) {
  Home("home", "Home", Icons.Outlined.Home),
  Profile("profile", "Profile", Icons.Outlined.Person),
  Schedule("schedule", "Schedule", Icons.Outlined.CalendarMonth),
  Study("study", "Study", Icons.Outlined.Timer),
  Games("games", "Games", Icons.Outlined.Extension),
  Stats("stats", "Stats", Icons.AutoMirrored.Outlined.ShowChart),
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
  Column(
      modifier
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 16.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CreatureHouseCard(
            creatureResId = creatureResId,
            level = state.userLevel, // Ensure real level is used (synchronized with user stats)
            environmentResId = environmentResId,
            overrideCreature = { EduMonAvatar(showLevelLabel = false) })

        UserStatsCard(stats = state.userStats, modifier = Modifier.fillMaxWidth())

        AffirmationsAndRemindersCard(
            quote = state.quote,
            onOpenPlanner = { onNavigate(AppDestination.Schedule.route) },
            onOpenMood = { onNavigate(AppDestination.Mood.route) },
        )

        TodayTodosCard(
            todos = state.todos, onSeeAll = { onNavigate(AppDestination.Schedule.route) })

        QuickActionsCard(
            onStudy = { onNavigate(AppDestination.Study.route) },
            onTakeBreak = { onNavigate(AppDestination.Games.route) },
            onSocial = {
              onNavigate(AppDestination.StudyTogether.route)
            }, // Updated to navigate to "Study Together"
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
          HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
          Spacer(Modifier.height(12.dp))
          Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            StatsItem("Streak", "${stats.streak}d")
            StatsItem("Points", "${stats.points}")
            StatsItem("Study Today", "${stats.todayStudyMinutes}m")
            StatsItem("Weekly Goal", "${stats.weeklyGoal}m")
          }
        }
      }
}

@Composable
private fun StatsItem(label: String, value: String) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Label(label)
    BigNumber(value)
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
                modifier = Modifier.testTag(HomeTestTags.TODAY_SEE_ALL).clickable { onSeeAll() })
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
                        if (isDone) Icons.Outlined.CheckCircle
                        else Icons.AutoMirrored.Outlined.ArrowForward,
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
                leadingIcon = {
                  Icon(Icons.AutoMirrored.Outlined.EventNote, contentDescription = null)
                },
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
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              QuickAction(
                  Icons.Outlined.Timer,
                  "Study",
                  onStudy,
                  Modifier.testTag(HomeTestTags.QUICK_STUDY))
              QuickAction(
                  Icons.Outlined.VideogameAsset,
                  "Break",
                  onTakeBreak,
                  Modifier.testTag(HomeTestTags.QUICK_BREAK))
              QuickAction(
                  Icons.Outlined.Group,
                  "Social",
                  onSocial,
                  Modifier.testTag(HomeTestTags.QUICK_SOCIAL))
            }
          }
        }
      }
}

@Composable
fun QuickAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
  Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = modifier.clickable { onClick() }) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.size(48.dp)) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer)
              }
            }
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium)
      }
}
