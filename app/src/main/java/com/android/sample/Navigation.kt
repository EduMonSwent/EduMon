package com.android.sample

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.android.sample.feature.homeScreen.AppDestination
import com.android.sample.feature.homeScreen.EduMonHomeRoute
import com.android.sample.ui.calendar.CalendarScreen
import com.android.sample.ui.flashcards.FlashcardsApp
import com.android.sample.ui.games.FlappyEduMonScreen
import com.android.sample.ui.games.FocusBreathingScreen
import com.android.sample.ui.games.GamesScreen
import com.android.sample.ui.games.MemoryGameScreen
import com.android.sample.ui.games.ReactionGameScreen
import com.android.sample.ui.mood.MoodLoggingRoute
import com.android.sample.ui.planner.PlannerScreen
import com.android.sample.ui.profile.ProfileScreen
import com.android.sample.ui.session.StudySessionScreen
import com.android.sample.ui.stats.StatsRoute

/** Stable tags used by UI tests */
object NavigationTestTags {
  const val NAV_HOST = "nav_host"
  const val TOP_BAR_TITLE = "top_bar_title"
  const val GO_BACK_BUTTON = "go_back_button"
}

private object GameRoutes {
  const val Memory = "memory"
  const val Reaction = "reaction"
  const val Focus = "focus"
  const val Runner = "runner"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EduMonNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
  val nav = navController

  NavHost(
      navController = nav,
      startDestination = AppDestination.Home.route,
      modifier = modifier.testTag(NavigationTestTags.NAV_HOST)) {

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
              })
        }

        // PLANNER
        composable(AppDestination.Planner.route) {
          Scaffold(
              topBar = {
                TopAppBar(
                    title = {
                      Text("Planner", modifier = Modifier.testTag(NavigationTestTags.TOP_BAR_TITLE))
                    },
                    navigationIcon = {
                      IconButton(
                          onClick = { nav.popBackStack() },
                          modifier = Modifier.testTag(NavigationTestTags.GO_BACK_BUTTON)) {
                            Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                          }
                    })
              }) { padding ->
                Box(Modifier.fillMaxSize().padding(padding)) { PlannerScreen() }
              }
        }

        // PROFILE
        composable(AppDestination.Profile.route) {
          Scaffold(
              topBar = {
                TopAppBar(
                    title = {
                      Text("Profile", modifier = Modifier.testTag(NavigationTestTags.TOP_BAR_TITLE))
                    },
                    navigationIcon = {
                      IconButton(
                          onClick = { nav.popBackStack() },
                          modifier = Modifier.testTag(NavigationTestTags.GO_BACK_BUTTON)) {
                            Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                          }
                    })
              }) { padding ->
                Box(Modifier.fillMaxSize().padding(padding)) { ProfileScreen() }
              }
        }

        // CALENDAR
        composable(AppDestination.Calendar.route) {
          Scaffold(
              topBar = {
                TopAppBar(
                    title = {
                      Text(
                          "Calendar", modifier = Modifier.testTag(NavigationTestTags.TOP_BAR_TITLE))
                    },
                    navigationIcon = {
                      IconButton(
                          onClick = { nav.popBackStack() },
                          modifier = Modifier.testTag(NavigationTestTags.GO_BACK_BUTTON)) {
                            Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                          }
                    })
              }) { padding ->
                Box(Modifier.fillMaxSize().padding(padding)) { CalendarScreen() }
              }
        }

        // STATS
        composable(AppDestination.Stats.route) {
          Scaffold(
              topBar = {
                TopAppBar(
                    title = {
                      Text("Stats", modifier = Modifier.testTag(NavigationTestTags.TOP_BAR_TITLE))
                    },
                    navigationIcon = {
                      IconButton(
                          onClick = { nav.popBackStack() },
                          modifier = Modifier.testTag(NavigationTestTags.GO_BACK_BUTTON)) {
                            Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                          }
                    })
              }) { padding ->
                Box(Modifier.fillMaxSize().padding(padding)) { StatsRoute() }
              }
        }

        // GAMES (hub)
        composable(AppDestination.Games.route) {
          Scaffold(
              topBar = {
                TopAppBar(
                    title = {
                      Text("Games", modifier = Modifier.testTag(NavigationTestTags.TOP_BAR_TITLE))
                    },
                    navigationIcon = {
                      IconButton(
                          onClick = { nav.popBackStack() },
                          modifier = Modifier.testTag(NavigationTestTags.GO_BACK_BUTTON)) {
                            Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                          }
                    })
              }) { padding ->
                Box(Modifier.fillMaxSize().padding(padding)) { GamesScreen(nav) }
              }
        }

        // Individual games
        composable(GameRoutes.Memory) {
          Scaffold(
              topBar = {
                TopAppBar(
                    title = {
                      Text(
                          "Memory Game",
                          modifier = Modifier.testTag(NavigationTestTags.TOP_BAR_TITLE))
                    },
                    navigationIcon = {
                      IconButton(
                          onClick = { nav.popBackStack() },
                          modifier = Modifier.testTag(NavigationTestTags.GO_BACK_BUTTON)) {
                            Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                          }
                    })
              }) { padding ->
                Box(Modifier.fillMaxSize().padding(padding)) { MemoryGameScreen() }
              }
        }

        composable(GameRoutes.Reaction) {
          Scaffold(
              topBar = {
                TopAppBar(
                    title = {
                      Text(
                          "Reaction Test",
                          modifier = Modifier.testTag(NavigationTestTags.TOP_BAR_TITLE))
                    },
                    navigationIcon = {
                      IconButton(
                          onClick = { nav.popBackStack() },
                          modifier = Modifier.testTag(NavigationTestTags.GO_BACK_BUTTON)) {
                            Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                          }
                    })
              }) { padding ->
                Box(Modifier.fillMaxSize().padding(padding)) { ReactionGameScreen() }
              }
        }

        composable(GameRoutes.Focus) {
          Scaffold(
              topBar = {
                TopAppBar(
                    title = {
                      Text(
                          "Focus Breathing",
                          modifier = Modifier.testTag(NavigationTestTags.TOP_BAR_TITLE))
                    },
                    navigationIcon = {
                      IconButton(
                          onClick = { nav.popBackStack() },
                          modifier = Modifier.testTag(NavigationTestTags.GO_BACK_BUTTON)) {
                            Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                          }
                    })
              }) { padding ->
                Box(Modifier.fillMaxSize().padding(padding)) { FocusBreathingScreen() }
              }
        }

        composable(GameRoutes.Runner) {
          Scaffold(
              topBar = {
                TopAppBar(
                    title = {
                      Text(
                          "EduMon Runner",
                          modifier = Modifier.testTag(NavigationTestTags.TOP_BAR_TITLE))
                    },
                    navigationIcon = {
                      IconButton(
                          onClick = { nav.popBackStack() },
                          modifier = Modifier.testTag(NavigationTestTags.GO_BACK_BUTTON)) {
                            Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                          }
                    })
              }) { padding ->
                Box(Modifier.fillMaxSize().padding(padding)) { FlappyEduMonScreen() }
              }
        }

        // STUDY SESSION
        composable(AppDestination.Study.route) {
          Scaffold(
              topBar = {
                TopAppBar(
                    title = {
                      Text("Study", modifier = Modifier.testTag(NavigationTestTags.TOP_BAR_TITLE))
                    },
                    navigationIcon = {
                      IconButton(
                          onClick = { nav.popBackStack() },
                          modifier = Modifier.testTag(NavigationTestTags.GO_BACK_BUTTON)) {
                            Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                          }
                    })
              }) { padding ->
                Box(Modifier.fillMaxSize().padding(padding)) { StudySessionScreen() }
              }
        }

        // FLASHCARDS
        composable(AppDestination.Flashcards.route) {
          Scaffold(
              topBar = {
                TopAppBar(
                    title = {
                      Text("Study", modifier = Modifier.testTag(NavigationTestTags.TOP_BAR_TITLE))
                    },
                    navigationIcon = {
                      IconButton(
                          onClick = { nav.popBackStack() },
                          modifier = Modifier.testTag(NavigationTestTags.GO_BACK_BUTTON)) {
                            Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                          }
                    })
              }) { padding ->
                Box(Modifier.fillMaxSize().padding(padding)) { FlashcardsApp() }
              }
        }

        // 👉 MOOD (Daily Reflection)
        composable(AppDestination.Mood.route) {
          Scaffold(
              topBar = {
                TopAppBar(
                    title = {
                      Text(
                          "Daily Reflection",
                          modifier = Modifier.testTag(NavigationTestTags.TOP_BAR_TITLE))
                    },
                    navigationIcon = {
                      IconButton(
                          onClick = { nav.popBackStack() },
                          modifier = Modifier.testTag(NavigationTestTags.GO_BACK_BUTTON)) {
                            Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                          }
                    })
              }) { padding ->
                Box(Modifier.fillMaxSize().padding(padding)) { MoodLoggingRoute() }
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
