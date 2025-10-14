package com.android.sample.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.android.sample.R
import com.android.sample.screens.AppDestination
import com.android.sample.screens.EduMonHomeRoute
import com.android.sample.ui.viewmodel.ObjectivesViewModel
import com.android.sample.ui.viewmodel.WeekDotsViewModel
import com.android.sample.ui.viewmodel.WeeksViewModel
import com.android.sample.ui.widgets.WeekProgDailyObj

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EduMonNavHost(modifier: Modifier = Modifier) {
    val nav = rememberNavController()

    NavHost(
        navController = nav,
        startDestination = AppDestination.Home.route,
        modifier = modifier
    ) {
        // HOME
        composable(AppDestination.Home.route) {
            EduMonHomeRoute(
                creatureResId = R.drawable.edumon,
                environmentResId = R.drawable.home,
                onNavigate = { route ->
                    nav.navigate(route) {
                        // avoid building a tall stack and enable state restore
                        popUpTo(AppDestination.Home.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }

        // PLANNER -> WeekProgDailyObj
        composable(AppDestination.Planner.route) {
            val weeksVM: WeeksViewModel = viewModel()
            val objectivesVM: ObjectivesViewModel = viewModel()
            val dotsVM: WeekDotsViewModel = viewModel()

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
                    WeekProgDailyObj(
                        weeksViewModel = weeksVM,
                        objectivesViewModel = objectivesVM,
                        dotsViewModel = dotsVM
                    )
                }
            }
        }

        // --- Safe stubs so BottomNav clicks don’t crash even if not implemented yet ---
        composable(AppDestination.Calendar.route) { SimpleStub("Calendar") }
        composable(AppDestination.Shop.route)     { SimpleStub("Shop") }
        composable(AppDestination.Profile.route)  { SimpleStub("Profile") }
        composable(AppDestination.Games.route)    { SimpleStub("Games") }
        composable(AppDestination.Settings.route) { SimpleStub("Settings") }
        composable(AppDestination.Study.route)    { SimpleStub("Study") }
    }
}

@Composable
private fun SimpleStub(title: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(title, style = MaterialTheme.typography.titleLarge)
    }
}
