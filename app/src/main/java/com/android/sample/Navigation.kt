package com.android.sample

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
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
import com.android.sample.ui.onBoarding.EduMonOnboardingScreen
import com.android.sample.ui.profile.ProfileScreen
import com.android.sample.ui.profile.ProfileViewModel
import com.android.sample.ui.schedule.ScheduleScreen
import com.android.sample.ui.session.StudySessionScreen
import com.android.sample.ui.shop.ShopScreen
import com.android.sample.ui.stats.StatsRoute
import com.android.sample.ui.todo.AddToDoScreen
import com.android.sample.ui.todo.TodoNavHostInThisFile
import com.android.sample.profile.FirestoreProfileRepository
import com.android.sample.repos_providors.AppRepositories
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

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

@Composable
private fun EduMonDrawerContent(
    currentRoute: String?,
    onDestinationClick: (String) -> Unit,
) {
    ModalDrawerSheet(
        modifier = Modifier
            .fillMaxHeight()
            .verticalScroll(rememberScrollState()),
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerContentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Edumon",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Text(
            text = "EPFL Companion",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Spacer(Modifier.height(24.dp))

        NavigationDrawerItem(
            label = { Text("Home") },
            selected = currentRoute == AppDestination.Home.route,
            onClick = { onDestinationClick(AppDestination.Home.route) },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
        NavigationDrawerItem(
            label = { Text("Profile") },
            selected = currentRoute == AppDestination.Profile.route,
            onClick = { onDestinationClick(AppDestination.Profile.route) },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
        NavigationDrawerItem(
            label = { Text("Schedule") },
            selected = currentRoute == AppDestination.Schedule.route,
            onClick = { onDestinationClick(AppDestination.Schedule.route) },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
        NavigationDrawerItem(
            label = { Text("Stats") },
            selected = currentRoute == AppDestination.Stats.route,
            onClick = { onDestinationClick(AppDestination.Stats.route) },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
        NavigationDrawerItem(
            label = { Text("Games") },
            selected = currentRoute == AppDestination.Games.route,
            onClick = { onDestinationClick(AppDestination.Games.route) },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
        NavigationDrawerItem(
            label = { Text("Flashcards") },
            selected = currentRoute == AppDestination.Flashcards.route,
            onClick = { onDestinationClick(AppDestination.Flashcards.route) },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
        NavigationDrawerItem(
            label = { Text("Todo") },
            selected = currentRoute == AppDestination.Todo.route,
            onClick = { onDestinationClick(AppDestination.Todo.route) },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
        NavigationDrawerItem(
            label = { Text("Daily Reflection") },
            selected = currentRoute == AppDestination.Mood.route,
            onClick = { onDestinationClick(AppDestination.Mood.route) },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
        NavigationDrawerItem(
            label = { Text("Study Together") },
            selected = currentRoute == AppDestination.StudyTogether.route,
            onClick = { onDestinationClick(AppDestination.StudyTogether.route) },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
        NavigationDrawerItem(
            label = { Text("Shop") },
            selected = currentRoute == AppDestination.Shop.route,
            onClick = { onDestinationClick(AppDestination.Shop.route) },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScreenWithTopBar(
    title: String,
    drawerState: androidx.compose.material3.DrawerState,
    scope: CoroutineScope,
    onBack: () -> Unit,
    content: @Composable () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, modifier = Modifier.testTag(NavigationTestTags.TOP_BAR_TITLE)) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag(NavigationTestTags.GO_BACK_BUTTON)
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                        Icon(Icons.Outlined.Menu, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) { content() }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EduMonNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = AppDestination.Home.route,
    onSignOut: () -> Unit = {}
) {
    val profileViewModel: ProfileViewModel = viewModel()
    val userProfile by profileViewModel.userProfile.collectAsState()

    val repo = AppRepositories.profileRepository
    val isLoaded = if (repo is FirestoreProfileRepository) {
        repo.isLoaded.collectAsState().value
    } else {
        true
    }

    var hasDecided by remember { mutableStateOf(false) }
    var needsOnboarding by remember { mutableStateOf(false) }

    LaunchedEffect(isLoaded, userProfile) {
        Log.d("EduMonNavHost", "isLoaded=$isLoaded, starterId='${userProfile.starterId}', hasDecided=$hasDecided")

        if (isLoaded && !hasDecided) {
            needsOnboarding = userProfile.starterId.isBlank()
            hasDecided = true
            Log.d("EduMonNavHost", "Decision: needsOnboarding=$needsOnboarding")
        }
    }

    if (!hasDecided) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    val actualStartDestination = if (needsOnboarding) "onboarding" else AppDestination.Home.route

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            EduMonDrawerContent(
                currentRoute = currentRoute,
                onDestinationClick = { route ->
                    scope.launch { drawerState.close() }
                    navController.navigateSingleTopTo(route)
                }
            )
        }
    ) {
        NavHost(
            navController = navController,
            startDestination = actualStartDestination,
            modifier = modifier.testTag(NavigationTestTags.NAV_HOST)
        ) {

            composable("onboarding") {
                EduMonOnboardingScreen(
                    onOnboardingFinished = { _, starterId ->
                        Log.d("EduMonNavHost", "Onboarding finished: $starterId")
                        profileViewModel.setStarter(starterId)
                        navController.navigate(AppDestination.Home.route) {
                            popUpTo("onboarding") { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(AppDestination.Home.route) {
                val creatureResId = profileViewModel.starterDrawable()
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    "Home",
                                    modifier = Modifier.testTag(NavigationTestTags.TOP_BAR_TITLE)
                                )
                            },
                            navigationIcon = {
                                IconButton(
                                    onClick = { scope.launch { drawerState.open() } },
                                    modifier = Modifier.testTag(HomeTestTags.MENU_BUTTON)
                                ) {
                                    Icon(Icons.Outlined.Menu, contentDescription = null)
                                }
                            }
                        )
                    }
                ) { padding ->
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(padding)
                    ) {
                        EduMonHomeRoute(
                            creatureResId = creatureResId,
                            environmentResId = R.drawable.home,
                            onNavigate = { route -> navController.navigateSingleTopTo(route) }
                        )
                    }
                }
            }

            composable(AppDestination.Profile.route) {
                ScreenWithTopBar(
                    title = "Profile",
                    drawerState = drawerState,
                    scope = scope,
                    onBack = { navController.popBackStack() }
                ) {
                    ProfileScreen(
                        onOpenNotifications = { navController.navigate("notifications") },
                        onOpenFocusMode = { navController.navigate("focus_mode") },
                        onSignOut = onSignOut
                    )
                }
            }

            composable(AppDestination.Schedule.route) {
                ScreenWithTopBar(
                    title = "Schedule",
                    drawerState = drawerState,
                    scope = scope,
                    onBack = { navController.popBackStack() }
                ) {
                    ScheduleScreen(
                        onAddTodoClicked = { date ->
                            navController.navigate("addTodoFromSchedule/$date") {
                                launchSingleTop = true
                            }
                        },
                        onOpenTodo = { _ ->
                            navController.navigateSingleTopTo(AppDestination.Todo.route)
                        }
                    )
                }
            }

            composable(
                route = "addTodoFromSchedule/{date}",
                arguments = listOf(navArgument("date") { type = NavType.StringType })
            ) {
                AddToDoScreen(
                    onBack = {
                        navController.popBackStack(
                            route = AppDestination.Schedule.route,
                            inclusive = false
                        )
                    }
                )
            }

            composable(AppDestination.Stats.route) {
                ScreenWithTopBar(
                    title = "Stats",
                    drawerState = drawerState,
                    scope = scope,
                    onBack = { navController.popBackStack() }
                ) {
                    StatsRoute()
                }
            }

            composable(AppDestination.Games.route) {
                ScreenWithTopBar(
                    title = "Games",
                    drawerState = drawerState,
                    scope = scope,
                    onBack = { navController.popBackStack() }
                ) {
                    GamesScreen(navController)
                }
            }

            composable(GameRoutes.Memory) {
                ScreenWithTopBar(
                    title = "Memory Game",
                    drawerState = drawerState,
                    scope = scope,
                    onBack = { navController.popBackStack() }
                ) {
                    MemoryGameScreen()
                }
            }

            composable(GameRoutes.Reaction) {
                ScreenWithTopBar(
                    title = "Reaction Test",
                    drawerState = drawerState,
                    scope = scope,
                    onBack = { navController.popBackStack() }
                ) {
                    ReactionGameScreen()
                }
            }

            composable(GameRoutes.Focus) {
                ScreenWithTopBar(
                    title = "Focus Breathing",
                    drawerState = drawerState,
                    scope = scope,
                    onBack = { navController.popBackStack() }
                ) {
                    FocusBreathingScreen()
                }
            }

            composable(GameRoutes.Runner) {
                ScreenWithTopBar(
                    title = "Flappy EduMon",
                    drawerState = drawerState,
                    scope = scope,
                    onBack = { navController.popBackStack() }
                ) {
                    FlappyEduMonScreen()
                }
            }

            composable(AppDestination.Flashcards.route) {
                ScreenWithTopBar(
                    title = "Flashcards",
                    drawerState = drawerState,
                    scope = scope,
                    onBack = { navController.popBackStack() }
                ) {
                    FlashcardsApp()
                }
            }

            composable(AppDestination.Todo.route) {
                ScreenWithTopBar(
                    title = "Todo",
                    drawerState = drawerState,
                    scope = scope,
                    onBack = { navController.popBackStack() }
                ) {
                    TodoNavHostInThisFile()
                }
            }

            composable(AppDestination.Mood.route) {
                ScreenWithTopBar(
                    title = "Daily Reflection",
                    drawerState = drawerState,
                    scope = scope,
                    onBack = { navController.popBackStack() }
                ) {
                    MoodLoggingRoute()
                }
            }

            composable(AppDestination.StudyTogether.route) {
                ScreenWithTopBar(
                    title = "Study Together",
                    drawerState = drawerState,
                    scope = scope,
                    onBack = { navController.popBackStack() }
                ) {
                    StudyTogetherScreen()
                }
            }

            composable(AppDestination.Shop.route) {
                ScreenWithTopBar(
                    title = "Shop",
                    drawerState = drawerState,
                    scope = scope,
                    onBack = { navController.popBackStack() }
                ) {
                    ShopScreen()
                }
            }

            composable("notifications") {
                NotificationsScreen(
                    onBack = { navController.popBackStack() },
                    onGoHome = { navController.navigateSingleTopTo(AppDestination.Home.route) }
                )
            }

            composable("focus_mode") {
                ScreenWithTopBar(
                    title = "Focus Mode",
                    drawerState = drawerState,
                    scope = scope,
                    onBack = { navController.popBackStack() }
                ) {
                    FocusModeScreen()
                }
            }

            composable(
                route = "study/{id}",
                arguments = listOf(navArgument("id") { type = NavType.StringType })
            ) {
                ScreenWithTopBar(
                    title = "Study Session",
                    drawerState = drawerState,
                    scope = scope,
                    onBack = { navController.popBackStack() }
                ) {
                    StudySessionScreen()
                }
            }
        }
    }
}

private fun NavHostController.navigateSingleTopTo(route: String) {
    val currentRoute = this.currentDestination?.route
    if (currentRoute == route) return

    this.navigate(route) {
        popUpTo(this@navigateSingleTopTo.graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}