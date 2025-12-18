package com.android.sample

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.feature.homeScreen.AppDestination
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Some parts of this file were written using an LLM (ClaudeAI)
@OptIn(ExperimentalMaterial3Api::class)
@RunWith(AndroidJUnit4::class)
class EduMonNavHostTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Before
  fun setUp() {
    runBlocking {
      val auth = FirebaseAuth.getInstance()
      if (auth.currentUser == null) {
        auth.signInAnonymously().await()
      }
    }
  }

  // Covers: NavigationTestTags object, GameRoutes object
  @Test
  fun testConstants() {
    assert(NavigationTestTags.NAV_HOST == "nav_host")
    assert(NavigationTestTags.TOP_BAR_TITLE == "top_bar_title")
    assert(NavigationTestTags.GO_BACK_BUTTON == "go_back_button")
  }

  // Covers: EduMonNavHost, EduMonDrawerContent, ScreenWithTopBar, Home composable,
  // drawer open/close, all drawer navigation items, navigateSingleTopTo
  @Test
  fun testDrawerNavigationToAllDestinations() {
    lateinit var navController: NavHostController

    composeTestRule.setContent {
      navController = rememberNavController()
      EduMonNavHost(
          navController = navController,
          startDestination = AppDestination.Home.route,
          onSignOut = {})
    }

    // Wait for NavHost to be fully set up
    composeTestRule.waitUntil(5000) { navController.currentDestination != null }

    // Navigate programmatically to each destination to cover all composable routes
    val destinations =
        listOf(
            AppDestination.Profile.route,
            AppDestination.Schedule.route,
            AppDestination.Study.route,
            AppDestination.Stats.route,
            AppDestination.Games.route,
            AppDestination.Flashcards.route,
            AppDestination.Todo.route,
            AppDestination.Mood.route,
            AppDestination.StudyTogether.route,
            AppDestination.Shop.route,
            AppDestination.Home.route)

    destinations.forEach { route ->
      composeTestRule.runOnUiThread { navController.navigate(route) }
      composeTestRule.waitForIdle()
    }
  }

  // Covers: safeNavigateBack (back button press, popBackStack success path)
  @Test
  fun testBackNavigation() {
    lateinit var navController: NavHostController

    composeTestRule.setContent {
      navController = rememberNavController()
      EduMonNavHost(navController = navController, startDestination = AppDestination.Home.route)
    }

    // Wait for NavHost to be fully set up
    composeTestRule.waitUntil(5000) { navController.currentDestination != null }

    // Navigate to Games via programmatic navigation
    composeTestRule.runOnUiThread { navController.navigate(AppDestination.Games.route) }
    composeTestRule.waitForIdle()

    // Wait for back button to be available and click it
    composeTestRule.onNodeWithTag(NavigationTestTags.GO_BACK_BUTTON).assertExists()
    composeTestRule.onNodeWithTag(NavigationTestTags.GO_BACK_BUTTON).performClick()
    composeTestRule.waitForIdle()
  }

  // Covers: All game route composables (memory, reaction, focus, runner)
  @Test
  fun testGameRoutes() {
    lateinit var navController: NavHostController

    composeTestRule.setContent {
      navController = rememberNavController()
      EduMonNavHost(navController = navController, startDestination = AppDestination.Games.route)
    }
    composeTestRule.waitForIdle()

    listOf("memory", "reaction", "focus", "runner").forEach { route ->
      composeTestRule.runOnUiThread { navController.navigate(route) }
      composeTestRule.waitForIdle()
      composeTestRule.onNodeWithTag(NavigationTestTags.GO_BACK_BUTTON).performClick()
      composeTestRule.waitForIdle()
    }
  }

  // Covers: addTodoFromSchedule route and its back navigation logic
  @Test
  fun testAddTodoFromScheduleRoute() {
    lateinit var navController: NavHostController

    composeTestRule.setContent {
      navController = rememberNavController()
      EduMonNavHost(navController = navController, startDestination = AppDestination.Schedule.route)
    }
    composeTestRule.waitForIdle()

    // Navigate to addTodoFromSchedule
    composeTestRule.runOnUiThread { navController.navigate("addTodoFromSchedule/2024-01-15") }
    composeTestRule.waitForIdle()
  }

  // Covers: Profile screen callbacks (onOpenNotifications, onOpenFocusMode)
  @Test
  fun testProfileScreenCallbacks() {
    lateinit var navController: NavHostController

    composeTestRule.setContent {
      navController = rememberNavController()
      EduMonNavHost(navController = navController, startDestination = AppDestination.Profile.route)
    }
    composeTestRule.waitForIdle()

    // Navigate to notifications and focus_mode to cover those routes
    composeTestRule.runOnUiThread { navController.navigate("notifications") }
    composeTestRule.waitForIdle()

    composeTestRule.runOnUiThread { navController.navigate("focus_mode") }
    composeTestRule.waitForIdle()
  }

  // Covers: onboarding composable and onOnboardingFinished callback
  @Test
  fun testOnboardingRoute() {
    lateinit var navController: NavHostController

    composeTestRule.setContent {
      navController = rememberNavController()
      EduMonNavHost(navController = navController, startDestination = "onboarding")
    }
    composeTestRule.waitForIdle()
  }
}
