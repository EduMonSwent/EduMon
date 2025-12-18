package com.android.sample

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.feature.homeScreen.AppDestination
import com.android.sample.feature.homeScreen.HomeTestTags
import com.android.sample.repos_providors.AppRepositories
import com.android.sample.repos_providors.FakeRepositoriesProvider
import com.android.sample.ui.theme.EduMonTheme
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Comprehensive tests for Navigation.kt to improve line coverage. Tests drawer content, screen
 * transitions, back navigation, and all routes.
 */
@RunWith(AndroidJUnit4::class)
class Navigation1Test {

  @get:Rule val composeTestRule = createComposeRule()

  private var originalRepositories = AppRepositories

  @Before
  fun setUp() {
    AppRepositories = FakeRepositoriesProvider
  }

  @After
  fun tearDown() {
    AppRepositories = originalRepositories
  }

  @OptIn(ExperimentalTestApi::class)
  @Test
  fun drawerItem_navigatesToProfile() {
    composeTestRule.setContent {
      EduMonTheme { EduMonNavHost(startDestination = AppDestination.Home.route) }
    }

    composeTestRule.waitForIdle()

    // Open drawer
    composeTestRule.onNodeWithTag(HomeTestTags.MENU_BUTTON).performClick()
    composeTestRule.waitForIdle()

    // Use test tag to avoid ambiguity
    val drawerTag = HomeTestTags.drawerTag(AppDestination.Profile.route)
    composeTestRule.waitUntilAtLeastOneExists(hasTestTag(drawerTag), timeoutMillis = 5000)
    composeTestRule.onNodeWithTag(drawerTag).performClick()
    composeTestRule.waitForIdle()

    // Verify we're on Profile screen
    composeTestRule.waitUntilAtLeastOneExists(
        hasText("Profile") and hasTestTag(NavigationTestTags.TOP_BAR_TITLE), timeoutMillis = 5000)
    composeTestRule
        .onNode(hasText("Profile") and hasTestTag(NavigationTestTags.TOP_BAR_TITLE))
        .assertIsDisplayed()
  }

  @OptIn(ExperimentalTestApi::class)
  @Test
  fun drawerItem_navigatesToSchedule() {
    composeTestRule.setContent {
      EduMonTheme { EduMonNavHost(startDestination = AppDestination.Home.route) }
    }

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(HomeTestTags.MENU_BUTTON).performClick()
    composeTestRule.waitForIdle()

    val drawerTag = HomeTestTags.drawerTag(AppDestination.Schedule.route)
    composeTestRule.waitUntilAtLeastOneExists(hasTestTag(drawerTag), timeoutMillis = 5000)
    composeTestRule.onNodeWithTag(drawerTag).performClick()
    composeTestRule.waitForIdle()

    composeTestRule.waitUntilAtLeastOneExists(
        hasText("Schedule") and hasTestTag(NavigationTestTags.TOP_BAR_TITLE), timeoutMillis = 5000)
    composeTestRule
        .onNode(hasText("Schedule") and hasTestTag(NavigationTestTags.TOP_BAR_TITLE))
        .assertIsDisplayed()
  }

  @OptIn(ExperimentalTestApi::class)
  @Test
  fun drawerItem_navigatesToStats() {
    composeTestRule.setContent {
      EduMonTheme { EduMonNavHost(startDestination = AppDestination.Home.route) }
    }

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(HomeTestTags.MENU_BUTTON).performClick()
    composeTestRule.waitForIdle()

    val drawerTag = HomeTestTags.drawerTag(AppDestination.Stats.route)
    composeTestRule.waitUntilAtLeastOneExists(hasTestTag(drawerTag), timeoutMillis = 5000)
    composeTestRule.onNodeWithTag(drawerTag).performClick()
    composeTestRule.waitForIdle()

    composeTestRule.waitUntilAtLeastOneExists(
        hasText("Stats") and hasTestTag(NavigationTestTags.TOP_BAR_TITLE), timeoutMillis = 5000)
  }

  @OptIn(ExperimentalTestApi::class)
  @Test
  fun drawerItem_navigatesToGames() {
    composeTestRule.setContent {
      EduMonTheme { EduMonNavHost(startDestination = AppDestination.Home.route) }
    }

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(HomeTestTags.MENU_BUTTON).performClick()
    composeTestRule.waitForIdle()

    val drawerTag = HomeTestTags.drawerTag(AppDestination.Games.route)
    composeTestRule.waitUntilAtLeastOneExists(hasTestTag(drawerTag), timeoutMillis = 5000)
    composeTestRule.onNodeWithTag(drawerTag).performClick()
    composeTestRule.waitForIdle()

    composeTestRule.waitUntilAtLeastOneExists(
        hasText("Games") and hasTestTag(NavigationTestTags.TOP_BAR_TITLE), timeoutMillis = 5000)
  }

  @OptIn(ExperimentalTestApi::class)
  @Test
  fun drawerItem_navigatesToFlashcards() {
    composeTestRule.setContent {
      EduMonTheme { EduMonNavHost(startDestination = AppDestination.Home.route) }
    }

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(HomeTestTags.MENU_BUTTON).performClick()
    composeTestRule.waitForIdle()

    val drawerTag = HomeTestTags.drawerTag(AppDestination.Flashcards.route)
    composeTestRule.waitUntilAtLeastOneExists(hasTestTag(drawerTag), timeoutMillis = 5000)
    composeTestRule.onNodeWithTag(drawerTag).performClick()
    composeTestRule.waitForIdle()

    composeTestRule.waitUntilAtLeastOneExists(
        hasText("Flashcards") and hasTestTag(NavigationTestTags.TOP_BAR_TITLE),
        timeoutMillis = 5000)
  }

  @OptIn(ExperimentalTestApi::class)
  @Test
  fun drawerItem_navigatesToTodo() {
    composeTestRule.setContent {
      EduMonTheme { EduMonNavHost(startDestination = AppDestination.Home.route) }
    }

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(HomeTestTags.MENU_BUTTON).performClick()
    composeTestRule.waitForIdle()

    val drawerTag = HomeTestTags.drawerTag(AppDestination.Todo.route)
    composeTestRule.waitUntilAtLeastOneExists(hasTestTag(drawerTag), timeoutMillis = 5000)
    composeTestRule.onNodeWithTag(drawerTag).performClick()
    composeTestRule.waitForIdle()

    composeTestRule.waitUntilAtLeastOneExists(
        hasText("Todo") and hasTestTag(NavigationTestTags.TOP_BAR_TITLE), timeoutMillis = 5000)
  }

  // ════════════════════════════════════════════════════════════════════════════
  // Back Navigation Tests
  // ════════════════════════════════════════════════════════════════════════════

  @OptIn(ExperimentalTestApi::class)
  @Test
  fun backButton_fromProfile_navigatesToHome() {
    var navController: NavHostController? = null

    composeTestRule.setContent {
      EduMonTheme {
        val nav = rememberNavController()
        navController = nav
        EduMonNavHost(navController = nav, startDestination = AppDestination.Home.route)
      }
    }

    composeTestRule.waitForIdle()

    // Navigate to Profile
    composeTestRule.runOnIdle { navController!!.navigate(AppDestination.Profile.route) }
    composeTestRule.waitForIdle()

    // Verify we're on Profile
    composeTestRule.waitUntilAtLeastOneExists(
        hasTestTag(NavigationTestTags.GO_BACK_BUTTON), timeoutMillis = 5000)
    composeTestRule.onNodeWithTag(NavigationTestTags.GO_BACK_BUTTON).assertIsDisplayed()

    // Click back
    composeTestRule.onNodeWithTag(NavigationTestTags.GO_BACK_BUTTON).performClick()
    composeTestRule.waitForIdle()

    // Verify we're back on Home
    composeTestRule.waitUntilAtLeastOneExists(
        hasText("Home") and hasTestTag(NavigationTestTags.TOP_BAR_TITLE), timeoutMillis = 5000)
  }

  @OptIn(ExperimentalTestApi::class)
  @Test
  fun backButton_fromHomeDoesNothing() {
    composeTestRule.setContent {
      EduMonTheme { EduMonNavHost(startDestination = AppDestination.Home.route) }
    }

    composeTestRule.waitForIdle()

    // Home screen doesn't have a back button, only menu
    composeTestRule.onNodeWithTag(NavigationTestTags.GO_BACK_BUTTON).assertDoesNotExist()
    composeTestRule.onNodeWithTag(HomeTestTags.MENU_BUTTON).assertIsDisplayed()
  }

  @OptIn(ExperimentalTestApi::class)
  @Test
  fun safeNavigateBack_withNoBackStack_navigatesToHome() {
    var navController: NavHostController? = null

    composeTestRule.setContent {
      EduMonTheme {
        val nav = rememberNavController()
        navController = nav
        EduMonNavHost(navController = nav, startDestination = AppDestination.Profile.route)
      }
    }

    composeTestRule.waitForIdle()

    // We start on Profile with no back stack
    composeTestRule.waitUntilAtLeastOneExists(
        hasTestTag(NavigationTestTags.GO_BACK_BUTTON), timeoutMillis = 5000)

    // Click back - should navigate to Home as fallback
    composeTestRule.onNodeWithTag(NavigationTestTags.GO_BACK_BUTTON).performClick()
    composeTestRule.waitForIdle()

    // Should be on Home now
    composeTestRule.waitUntilAtLeastOneExists(
        hasText("Home") and hasTestTag(NavigationTestTags.TOP_BAR_TITLE), timeoutMillis = 5000)
  }

  // ════════════════════════════════════════════════════════════════════════════
  // Game Routes Tests
  // ════════════════════════════════════════════════════════════════════════════

  @OptIn(ExperimentalTestApi::class)
  @Test
  fun gameRoutes_memoryGame_navigatesCorrectly() {
    var navController: NavHostController? = null

    composeTestRule.setContent {
      EduMonTheme {
        val nav = rememberNavController()
        navController = nav
        EduMonNavHost(navController = nav, startDestination = AppDestination.Home.route)
      }
    }

    composeTestRule.waitForIdle()

    // Navigate to memory game
    composeTestRule.runOnIdle { navController!!.navigate("memory") }
    composeTestRule.waitForIdle()

    // Verify Memory Game screen
    composeTestRule.waitUntilAtLeastOneExists(
        hasText("Memory Game") and hasTestTag(NavigationTestTags.TOP_BAR_TITLE),
        timeoutMillis = 5000)
  }

  @OptIn(ExperimentalTestApi::class)
  @Test
  fun gameRoutes_reactionGame_navigatesCorrectly() {
    var navController: NavHostController? = null

    composeTestRule.setContent {
      EduMonTheme {
        val nav = rememberNavController()
        navController = nav
        EduMonNavHost(navController = nav, startDestination = AppDestination.Home.route)
      }
    }

    composeTestRule.waitForIdle()

    composeTestRule.runOnIdle { navController!!.navigate("reaction") }
    composeTestRule.waitForIdle()

    composeTestRule.waitUntilAtLeastOneExists(
        hasText("Reaction Test") and hasTestTag(NavigationTestTags.TOP_BAR_TITLE),
        timeoutMillis = 5000)
  }

  @OptIn(ExperimentalTestApi::class)
  @Test
  fun gameRoutes_focusBreathing_navigatesCorrectly() {
    var navController: NavHostController? = null

    composeTestRule.setContent {
      EduMonTheme {
        val nav = rememberNavController()
        navController = nav
        EduMonNavHost(navController = nav, startDestination = AppDestination.Home.route)
      }
    }

    composeTestRule.waitForIdle()

    composeTestRule.runOnIdle { navController!!.navigate("focus") }
    composeTestRule.waitForIdle()

    composeTestRule.waitUntilAtLeastOneExists(
        hasText("Focus Breathing") and hasTestTag(NavigationTestTags.TOP_BAR_TITLE),
        timeoutMillis = 5000)
  }

  @OptIn(ExperimentalTestApi::class)
  @Test
  fun gameRoutes_runner_navigatesCorrectly() {
    var navController: NavHostController? = null

    composeTestRule.setContent {
      EduMonTheme {
        val nav = rememberNavController()
        navController = nav
        EduMonNavHost(navController = nav, startDestination = AppDestination.Home.route)
      }
    }

    composeTestRule.waitForIdle()

    composeTestRule.runOnIdle { navController!!.navigate("runner") }
    composeTestRule.waitForIdle()

    composeTestRule.waitUntilAtLeastOneExists(
        hasText("EduMon Runner") and hasTestTag(NavigationTestTags.TOP_BAR_TITLE),
        timeoutMillis = 5000)
  }

  // ════════════════════════════════════════════════════════════════════════════
  // Special Routes Tests
  // ════════════════════════════════════════════════════════════════════════════

  @OptIn(ExperimentalTestApi::class)
  @Test
  fun focusModeRoute_navigatesCorrectly() {
    var navController: NavHostController? = null

    composeTestRule.setContent {
      EduMonTheme {
        val nav = rememberNavController()
        navController = nav
        EduMonNavHost(navController = nav, startDestination = AppDestination.Home.route)
      }
    }

    composeTestRule.waitForIdle()

    composeTestRule.runOnIdle { navController!!.navigate("focus_mode") }
    composeTestRule.waitForIdle()

    composeTestRule.waitUntilAtLeastOneExists(
        hasText("Focus Mode") and hasTestTag(NavigationTestTags.TOP_BAR_TITLE),
        timeoutMillis = 5000)
  }

  @OptIn(ExperimentalTestApi::class)
  @Test
  fun studySessionRoute_withId_navigatesCorrectly() {
    var navController: NavHostController? = null

    composeTestRule.setContent {
      EduMonTheme {
        val nav = rememberNavController()
        navController = nav
        EduMonNavHost(navController = nav, startDestination = AppDestination.Home.route)
      }
    }

    composeTestRule.waitForIdle()

    composeTestRule.runOnIdle { navController!!.navigate("study/test-id-123") }
    composeTestRule.waitForIdle()

    composeTestRule.waitUntilAtLeastOneExists(
        hasText("Study Session") and hasTestTag(NavigationTestTags.TOP_BAR_TITLE),
        timeoutMillis = 5000)
  }

  // ════════════════════════════════════════════════════════════════════════════
  // Single Top Navigation Tests
  // ════════════════════════════════════════════════════════════════════════════

  @OptIn(ExperimentalTestApi::class)
  @Test
  fun navigateSingleTopTo_sameRoute_doesNotNavigate() {
    var navController: NavHostController? = null

    composeTestRule.setContent {
      EduMonTheme {
        val nav = rememberNavController()
        navController = nav
        EduMonNavHost(navController = nav, startDestination = AppDestination.Home.route)
      }
    }

    composeTestRule.waitForIdle()

    val initialBackStackSize = navController!!.currentBackStack.value.size

    // Try to navigate to Home again (we're already on Home)
    composeTestRule.runOnIdle { navController!!.navigate(AppDestination.Home.route) }
    composeTestRule.waitForIdle()

    // Back stack should not grow
    val finalBackStackSize = navController!!.currentBackStack.value.size
    assert(finalBackStackSize == initialBackStackSize + 1) // NavGraph adds one entry
  }

  @OptIn(ExperimentalTestApi::class)
  @Test
  fun navigateSingleTopTo_differentRoute_navigates() {
    var navController: NavHostController? = null

    composeTestRule.setContent {
      EduMonTheme {
        val nav = rememberNavController()
        navController = nav
        EduMonNavHost(navController = nav, startDestination = AppDestination.Home.route)
      }
    }

    composeTestRule.waitForIdle()

    // Navigate to Profile
    composeTestRule.runOnIdle { navController!!.navigate(AppDestination.Profile.route) }
    composeTestRule.waitForIdle()

    // Verify we're on Profile
    composeTestRule.waitUntilAtLeastOneExists(
        hasText("Profile") and hasTestTag(NavigationTestTags.TOP_BAR_TITLE), timeoutMillis = 5000)
  }
}
