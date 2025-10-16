package com.android.sample.navigation

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.android.sample.R
import com.android.sample.screens.AppDestination
import com.android.sample.screens.EduMonHomeRoute
import com.android.sample.ui.calendar.CalendarScreen
import com.android.sample.ui.flashcards.FlashcardsApp
import com.android.sample.ui.flashcards.data.StudyScreen
import com.android.sample.ui.games.FlappyEduMonScreen
import com.android.sample.ui.games.FocusBreathingScreen
import com.android.sample.ui.games.GamesScreen
import com.android.sample.ui.games.MemoryGameScreen
import com.android.sample.ui.games.ReactionGameScreen
import com.android.sample.ui.planner.PlannerScreen
import com.android.sample.ui.profile.ProfileScreen
import com.android.sample.ui.viewmodel.ObjectivesViewModel
import com.android.sample.ui.viewmodel.WeekDotsViewModel
import com.android.sample.ui.viewmodel.WeeksViewModel
import com.android.sample.ui.widgets.WeekProgDailyObj
import com.android.sample.ui.session.StudySessionScreen   // <-- NEW import
import com.android.sample.ui.stats.StatsScreen


private object GameRoutes {
    const val Memory = "memory"
    const val Reaction = "reaction"
    const val Focus = "focus"
    const val Runner = "runner"
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EduMonNavHost(modifier: Modifier = Modifier) {
    val nav = rememberNavController()

    NavHost(navController = nav, startDestination = AppDestination.Home.route, modifier = modifier) {

        // HOME
        composable(AppDestination.Home.route) {
            EduMonHomeRoute(
                creatureResId = R.drawable.edumon,
                environmentResId = R.drawable.home,
                onNavigate = { route ->
                    nav.navigate(route) {
                        popUpTo(AppDestination.Home.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }

        // PLANNER -> WeekProgDailyObj
        composable(AppDestination.Planner.route) {

            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Planner") },
                        navigationIcon = {
                            IconButton(onClick = { nav.popBackStack() }) {
                                Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                            }
                        }
                    )
                }
            ) { padding ->
                Box(Modifier.fillMaxSize().padding(padding)) {
                    PlannerScreen()
                }
            }
        }

        // PROFILE
        composable(AppDestination.Profile.route) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Profile") },
                        navigationIcon = {
                            IconButton(onClick = { nav.popBackStack() }) {
                                Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                            }
                        }
                    )
                }
            ) { padding ->
                Box(Modifier.fillMaxSize().padding(padding)) {
                    ProfileScreen()
                }
            }
        }

        //Calendar
        composable(AppDestination.Calendar.route) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Calendar") },
                        navigationIcon = {
                            IconButton(onClick = { nav.popBackStack() }) {
                                Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                            }
                        }
                    )
                }
            ) { padding ->
                Box(Modifier.fillMaxSize().padding(padding)) {
                    CalendarScreen()   // <-- opens your StudySession screen (with Pomodoro inside)
                }
            }
        }

        //Stats
        composable(AppDestination.Stats.route) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Stats") },
                        navigationIcon = {
                            IconButton(onClick = { nav.popBackStack() }) {
                                Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                            }
                        }
                    )
                }
            ) { padding ->
                Box(Modifier.fillMaxSize().padding(padding)) {
                    StatsScreen()   // <-- opens your StudySession screen (with Pomodoro inside)
                }
            }
        }

        //Games
        composable(AppDestination.Games.route) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Games") },
                        navigationIcon = {
                            IconButton(onClick = { nav.popBackStack() }) {
                                Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                            }
                        }
                    )
                }
            ) { padding ->
                Box(Modifier.fillMaxSize().padding(padding)) {
                    GamesScreen(nav)
                }
            }
        }
        // MEMORY
        composable(GameRoutes.Memory) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Memory Game") },
                        navigationIcon = {
                            IconButton(onClick = { nav.popBackStack() }) {
                                Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                            }
                        }
                    )
                }
            ) { padding ->
                Box(Modifier.fillMaxSize().padding(padding)) {
                    MemoryGameScreen()
                }
            }
        }

        // REACTION
        composable(GameRoutes.Reaction) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Reaction Test") },
                        navigationIcon = {
                            IconButton(onClick = { nav.popBackStack() }) {
                                Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                            }
                        }
                    )
                }
            ) { padding ->
                Box(Modifier.fillMaxSize().padding(padding)) {
                    ReactionGameScreen()
                }
            }
        }

        // FOCUS BREATHING
        composable(GameRoutes.Focus) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Focus Breathing") },
                        navigationIcon = {
                            IconButton(onClick = { nav.popBackStack() }) {
                                Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                            }
                        }
                    )
                }
            ) { padding ->
                Box(Modifier.fillMaxSize().padding(padding)) {
                    FocusBreathingScreen()
                }
            }
        }

        // RUNNER
        composable(GameRoutes.Runner) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("EduMon Runner") },
                        navigationIcon = {
                            IconButton(onClick = { nav.popBackStack() }) {
                                Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                            }
                        }
                    )
                }
            ) { padding ->
                Box(Modifier.fillMaxSize().padding(padding)) {
                    FlappyEduMonScreen()
                }
            }
        }


        // STUDY SESSION -> StudySessionScreen
        composable(AppDestination.Study.route) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Study") },
                        navigationIcon = {
                            IconButton(onClick = { nav.popBackStack() }) {
                                Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                            }
                        }
                    )
                }
            ) { padding ->
                Box(Modifier.fillMaxSize().padding(padding)) {
                    StudySessionScreen()   // <-- opens your StudySession screen (with Pomodoro inside)
                }
            }
        }

        //flashcards
        composable(AppDestination.Flashcards.route) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Study") },
                        navigationIcon = {
                            IconButton(onClick = { nav.popBackStack() }) {
                                Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                            }
                        }
                    )
                }
            ) { padding ->
                Box(Modifier.fillMaxSize().padding(padding)) {
                    FlashcardsApp()   // <-- opens your StudySession screen (with Pomodoro inside)
                }
            }
        }
    }
}

@Composable
private fun SimpleStub(title: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(title, style = MaterialTheme.typography.titleLarge)
    }
}