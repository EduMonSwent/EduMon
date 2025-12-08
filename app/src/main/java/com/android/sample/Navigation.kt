package com.android.sample

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.android.sample.feature.homeScreen.AppDestination
import com.android.sample.feature.homeScreen.EduMonHomeRoute
import com.android.sample.feature.homeScreen.HomeTestTags
import com.android.sample.ui.flashcards.FlashcardsApp
import com.android.sample.ui.focus.FocusModeScreen
import com.android.sample.ui.games.FlappyEduMonScreen
import com.android.sample.ui.games.FocusBreathingScreen
import com.android.sample.ui.games.GamesScreen
import com.android.sample.ui.games.MemoryGameScreen
import com.android.sample.ui.games.ReactionGameScreen
import com.android.sample.ui.location.StudyTogetherScreen
import com.android.sample.ui.mood.MoodLoggingRoute
import com.android.sample.ui.notifications.NotificationsScreen
import com.android.sample.ui.profile.ProfileScreen
import com.android.sample.ui.schedule.ScheduleScreen
import com.android.sample.ui.session.StudySessionScreen
import com.android.sample.ui.shop.ShopScreen
import com.android.sample.ui.stats.StatsRoute
import com.android.sample.ui.todo.AddToDoScreen
import com.android.sample.ui.todo.TodoNavHostInThisFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

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

/** Drawer content reused for all screens. Same items as the old home drawer. */
@Composable
private fun EduMonDrawerContent(
    currentRoute: String?,
    onDestinationClick: (String) -> Unit,
) {
  ModalDrawerSheet(
      modifier = Modifier.fillMaxHeight().verticalScroll(rememberScrollState()),
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
          val selected =
              currentRoute == dest.route || (currentRoute?.startsWith(dest.route) == true)

          NavigationDrawerItem(
              label = { Text(dest.label) },
              selected = selected,
              onClick = { onDestinationClick(dest.route) },
              icon = { Icon(dest.icon, contentDescription = dest.label) },
              colors =
                  NavigationDrawerItemDefaults.colors(
                      unselectedTextColor = MaterialTheme.colorScheme.onSurface,
                      selectedTextColor = MaterialTheme.colorScheme.onSurface,
                      unselectedContainerColor = Color.Transparent,
                      selectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                      unselectedIconColor = MaterialTheme.colorScheme.primary,
                      selectedIconColor = MaterialTheme.colorScheme.primary),
              modifier =
                  Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                      .testTag(HomeTestTags.drawerTag(dest.route)))
        }

        Spacer(Modifier.height(8.dp))
      }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScreenWithTopBar(
    title: String,
    drawerState: DrawerState,
    scope: CoroutineScope,
    onBack: () -> Unit,
    content: @Composable () -> Unit,
) {
  Scaffold(
      topBar = {
        TopAppBar(
            title = { Text(title, modifier = Modifier.testTag(NavigationTestTags.TOP_BAR_TITLE)) },
            navigationIcon = {
              IconButton(
                  onClick = onBack,
                  modifier = Modifier.testTag(NavigationTestTags.GO_BACK_BUTTON)) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                  }
            },
            actions = {
              IconButton(onClick = { scope.launch { drawerState.open() } }) {
                Icon(Icons.Outlined.Menu, contentDescription = "Menu")
              }
            })
      }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) { content() }
      }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EduMonNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = AppDestination.Home.route,
    creatureResId: Int = R.drawable.edumon,
    environmentResId: Int = R.drawable.home,
) {
  val nav = navController

  val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
  val scope = rememberCoroutineScope()

  val backStackEntry by nav.currentBackStackEntryAsState()
  val currentRoute = backStackEntry?.destination?.route

  ModalNavigationDrawer(
      drawerState = drawerState,
      drawerContent = {
        EduMonDrawerContent(
            currentRoute = currentRoute,
            onDestinationClick = { route ->
              scope.launch { drawerState.close() }
              nav.navigateSingleTopTo(route)
            })
      }) {
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = modifier.testTag(NavigationTestTags.NAV_HOST)) {

              // HOME
              composable(AppDestination.Home.route) {
                Scaffold(
                    topBar = {
                      TopAppBar(
                          title = {
                            Text(
                                "Home",
                                modifier = Modifier.testTag(NavigationTestTags.TOP_BAR_TITLE))
                          },
                          navigationIcon = {
                            IconButton(
                                onClick = { scope.launch { drawerState.open() } },
                                modifier = Modifier.testTag(HomeTestTags.MENU_BUTTON)) {
                                  Icon(Icons.Outlined.Menu, contentDescription = "Menu")
                                }
                          })
                    }) { padding ->
                      Box(Modifier.fillMaxSize().padding(padding)) {
                        EduMonHomeRoute(
                            creatureResId = creatureResId,
                            environmentResId = environmentResId,
                            onNavigate = { route -> nav.navigateSingleTopTo(route) })
                      }
                    }
              }

              // PROFILE
              composable(AppDestination.Profile.route) {
                ScreenWithTopBar(
                    title = "Profile",
                    drawerState = drawerState,
                    scope = scope,
                    onBack = { nav.popBackStack() }) {
                      ProfileScreen(
                          onOpenNotifications = { nav.navigate("notifications") },
                          onOpenFocusMode = { nav.navigate("focus_mode") },
                          avatarResId = creatureResId, // 👈 use chosen Edumon here
                      )
                    }
              }

              // SCHEDULE
              composable(AppDestination.Schedule.route) {
                ScreenWithTopBar(
                    title = "Schedule",
                    drawerState = drawerState,
                    scope = scope,
                    onBack = { nav.popBackStack() }) {
                      ScheduleScreen(
                          onAddTodoClicked = { date ->
                            nav.navigate("addTodoFromSchedule/$date") { launchSingleTop = true }
                          },
                          onOpenTodo = { _ -> nav.navigateSingleTopTo(AppDestination.Todo.route) },
                          avatarResId = creatureResId, // already dynamic
                          environmentResId =
                              environmentResId, // 👈 pass appearance.environmentResId
                      )
                    }
              }

              composable(
                  route = "addTodoFromSchedule/{date}",
                  arguments = listOf(navArgument("date") { type = NavType.StringType })) {
                    AddToDoScreen(
                        onBack = {
                          nav.popBackStack(route = AppDestination.Schedule.route, inclusive = false)
                        })
                  }

              // STATS
              composable(AppDestination.Stats.route) {
                ScreenWithTopBar(
                    title = "Stats",
                    drawerState = drawerState,
                    scope = scope,
                    onBack = { nav.popBackStack() }) {
                      StatsRoute()
                    }
              }

              // GAMES (hub)
              composable(AppDestination.Games.route) {
                ScreenWithTopBar(
                    title = "Games",
                    drawerState = drawerState,
                    scope = scope,
                    onBack = { nav.popBackStack() }) {
                      GamesScreen(nav)
                    }
              }

              // Individual games
              composable(GameRoutes.Memory) {
                ScreenWithTopBar(
                    title = "Memory Game",
                    drawerState = drawerState,
                    scope = scope,
                    onBack = { nav.popBackStack() }) {
                      MemoryGameScreen()
                    }
              }

              composable(GameRoutes.Reaction) {
                ScreenWithTopBar(
                    title = "Reaction Test",
                    drawerState = drawerState,
                    scope = scope,
                    onBack = { nav.popBackStack() }) {
                      ReactionGameScreen()
                    }
              }

              composable(GameRoutes.Focus) {
                ScreenWithTopBar(
                    title = "Focus Breathing",
                    drawerState = drawerState,
                    scope = scope,
                    onBack = { nav.popBackStack() }) {
                      FocusBreathingScreen()
                    }
              }

              // Runner
              composable(GameRoutes.Runner) {
                ScreenWithTopBar(
                    title = "EduMon Runner",
                    drawerState = drawerState,
                    scope = scope,
                    onBack = { nav.popBackStack() }) {
                      FlappyEduMonScreen(
                          avatarResId = creatureResId, // 👈 use the selected Edumon
                      )
                    }
              }

              // STUDY SESSION
              composable(AppDestination.Study.route) {
                ScreenWithTopBar(
                    title = "Study",
                    drawerState = drawerState,
                    scope = scope,
                    onBack = {
                      val popped = nav.popBackStack()
                      if (!popped) {
                        nav.navigateSingleTopTo(AppDestination.Home.route)
                      }
                    }) {
                      StudySessionScreen()
                    }
              }

              // Deep-linkable study route: study/{eventId}
              composable(
                  route = "study/{eventId}",
                  arguments = listOf(navArgument("eventId") { type = NavType.StringType })) {
                      backStackEntry ->
                    val eventId = backStackEntry.arguments?.getString("eventId")
                    ScreenWithTopBar(
                        title = "Study",
                        drawerState = drawerState,
                        scope = scope,
                        onBack = {
                          val popped = nav.popBackStack()
                          if (!popped) {
                            nav.navigateSingleTopTo(AppDestination.Home.route)
                          }
                        }) {
                          StudySessionScreen(eventId)
                        }
                  }

              // FLASHCARDS
              composable(AppDestination.Flashcards.route) {
                ScreenWithTopBar(
                    title = "Study",
                    drawerState = drawerState,
                    scope = scope,
                    onBack = { nav.popBackStack() }) {
                      FlashcardsApp()
                    }
              }

              // TODOS LIST
              composable(AppDestination.Todo.route) {
                ScreenWithTopBar(
                    title = "Todo",
                    drawerState = drawerState,
                    scope = scope,
                    onBack = { nav.popBackStack() }) {
                      TodoNavHostInThisFile()
                    }
              }

              // MOOD (Daily Reflection)
              composable(AppDestination.Mood.route) {
                ScreenWithTopBar(
                    title = "Daily Reflection",
                    drawerState = drawerState,
                    scope = scope,
                    onBack = { nav.popBackStack() }) {
                      MoodLoggingRoute()
                    }
              }

              // STUDY TOGETHER
              composable(AppDestination.StudyTogether.route) {
                ScreenWithTopBar(
                    title = "Study Together",
                    drawerState = drawerState,
                    scope = scope,
                    onBack = { nav.popBackStack() }) {
                      StudyTogetherScreen()
                    }
              }

              // SHOP
              composable(AppDestination.Shop.route) {
                ScreenWithTopBar(
                    title = "Shop",
                    drawerState = drawerState,
                    scope = scope,
                    onBack = { nav.popBackStack() }) {
                      ShopScreen()
                    }
              }

              // Notifications keeps its own internal top bar callbacks
              composable("notifications") {
                NotificationsScreen(
                    onBack = { nav.popBackStack() },
                    onGoHome = { nav.navigateSingleTopTo(AppDestination.Home.route) })
              }

              // FOCUS MODE
              composable("focus_mode") {
                ScreenWithTopBar(
                    title = "Focus Mode",
                    drawerState = drawerState,
                    scope = scope,
                    onBack = { nav.popBackStack() }) {
                      FocusModeScreen()
                    }
              }
            }
      }
}

/**
 * Helper to avoid stacking multiple copies of the same destination and to reduce race conditions
 * when navigating very fast.
 */
private fun NavHostController.navigateSingleTopTo(route: String) {
  val currentRoute = this.currentDestination?.route
  if (currentRoute == route) return

  this.navigate(route) {
    popUpTo(this@navigateSingleTopTo.graph.startDestinationId) { saveState = true }
    launchSingleTop = true
    restoreState = true
  }
}

@Composable
private fun SimpleStub(title: String) {
  Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    Text(title, style = MaterialTheme.typography.titleLarge)
  }
}
