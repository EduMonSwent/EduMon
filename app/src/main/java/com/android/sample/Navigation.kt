package com.android.sample

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.android.sample.ui.calendar.CalendarScreen
import com.android.sample.ui.flashcards.FlashcardsApp
import com.android.sample.ui.games.GamesScreen
import com.android.sample.ui.planner.PlannerScreen
import com.android.sample.ui.pomodoro.PomodoroScreen
import com.android.sample.ui.profile.ProfileScreen
import com.android.sample.ui.session.StudySessionScreen
import com.android.sample.ui.stats.StatsScreen
import com.android.sample.ui.viewmodel.ObjectivesViewModel
import com.android.sample.ui.viewmodel.WeekDotsViewModel
import com.android.sample.ui.viewmodel.WeeksViewModel
import com.android.sample.ui.widgets.WeekProgDailyObj

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EduMonNavHost(modifier: Modifier = Modifier) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val dest = backStack?.destination

    Scaffold(
        modifier = modifier,
        topBar = { CenterAlignedTopAppBar(title = { Text(titleForDestination(dest)) }) },
        bottomBar = {
            BottomBar(
                current = dest,
                onNavigate = { route ->
                    nav.navigate(route) {
                        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            NavHost(navController = nav, startDestination = Routes.Stats) {
                // Home / Stats
                composable(Routes.Stats) { StatsScreen() }

                // Planner
                composable(Routes.Planner) { PlannerScreen() }

                // Study session (Pomodoro inside)
                composable(Routes.Study) { StudySessionScreen() }

                // Standalone Pomodoro (optional direct route)
                composable(Routes.Pomodoro) { PomodoroScreen() }

                // Calendar
                composable(Routes.Calendar) { CalendarScreen() }

                // Flashcards
                composable(Routes.Flashcards) { FlashcardsApp() }

                // Brain games (needs NavController in your API)
                composable(Routes.Games) { GamesScreen(nav) }

                // Profile
                composable(Routes.Profile) { ProfileScreen() }

                // Weekly progress widget (legacy slot)
                composable(Routes.WeekProgress) {
                    val weeksVM: WeeksViewModel = viewModel()
                    val objectivesVM: ObjectivesViewModel = viewModel()
                    val dotsVM: WeekDotsViewModel = viewModel()
                    WeekProgDailyObj(
                        weeksViewModel = weeksVM,
                        objectivesViewModel = objectivesVM,
                        dotsViewModel = dotsVM
                    )
                }
            }
        }
    }
}

/** App routes (single source of truth) */
private object Routes {
    const val Stats = "stats"
    const val Planner = "planner"
    const val Study = "study"
    const val Pomodoro = "pomodoro"
    const val Calendar = "calendar"
    const val Flashcards = "flashcards"
    const val Games = "games"
    const val Profile = "profile"
    const val WeekProgress = "week_progress"
}

/** Top bar title per screen */
@Composable
private fun titleForDestination(dest: NavDestination?): String =
    when (dest?.route) {
        Routes.Stats -> "Statistics"
        Routes.Planner -> "Planner"
        Routes.Study -> "Study Session"
        Routes.Pomodoro -> "Pomodoro"
        Routes.Calendar -> "Calendar"
        Routes.Flashcards -> "Flashcards"
        Routes.Games -> "Brain Games"
        Routes.Profile -> "Profile"
        Routes.WeekProgress -> "Weekly Progress"
        else -> "EduMon"
    }

/** Bottom navigation with the main 4 tabs */
@Composable
private fun BottomBar(
    current: NavDestination?,
    onNavigate: (String) -> Unit
) {
    val items = listOf(
        BottomItem(Routes.Stats, "Stats", Icons.Outlined.BarChart),
        BottomItem(Routes.Planner, "Planner", Icons.Outlined.EventNote),
        BottomItem(Routes.Study, "Study", Icons.Outlined.Timer),
        BottomItem(Routes.Profile, "Profile", Icons.Outlined.Person),
    )
    NavigationBar {
        items.forEach { item ->
            val selected = current?.route == item.route
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(item.route) },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) }
            )
        }
    }
}

private data class BottomItem(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)
