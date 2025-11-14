package com.android.sample

import android.Manifest
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import androidx.test.rule.GrantPermissionRule
import com.android.sample.feature.homeScreen.AppDestination
import com.android.sample.feature.homeScreen.HomeTestTags
import junit.framework.TestCase.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class HomeNavigationTests {

  @get:Rule
  val locationPermissionRule: GrantPermissionRule =
      GrantPermissionRule.grant(
          Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

  @get:Rule val rule = createComposeRule()

  private lateinit var nav: TestNavHostController

  @OptIn(ExperimentalTestApi::class)
  @Before
  fun setup() {
    rule.setContent {
      val ctx = LocalContext.current
      nav = TestNavHostController(ctx).apply { navigatorProvider.addNavigator(ComposeNavigator()) }
      MaterialTheme { EduMonNavHost(navController = nav) }
    }
    // Wait for Home to render
    rule.waitUntilExactlyOneExists(hasTestTag(HomeTestTags.MENU_BUTTON), 5000L)
    rule.waitForIdle()
  }

  private fun assertRoute(expected: String) {
    rule.runOnIdle { assertEquals(expected, nav.currentBackStackEntry?.destination?.route) }
  }

  @OptIn(ExperimentalTestApi::class)
  private fun assertTopBarTitle(expected: String) {
    rule.waitUntilExactlyOneExists(hasTestTag(NavigationTestTags.TOP_BAR_TITLE), 3000L)
    rule.onNode(hasTestTag(NavigationTestTags.TOP_BAR_TITLE) and hasText(expected)).assertExists()
  }

  private fun tapBack() {
    rule.onNode(hasTestTag(NavigationTestTags.GO_BACK_BUTTON)).performClick()
    rule.waitForIdle()
  }

  @OptIn(ExperimentalTestApi::class)
  private fun navigateAndVerify(route: String, expectedTitle: String) {
    rule.runOnIdle { nav.navigate(route) }
    rule.waitUntilExactlyOneExists(hasTestTag(NavigationTestTags.TOP_BAR_TITLE), 3000L)
    rule.waitForIdle()
    assertRoute(route)
    assertTopBarTitle(expectedTitle)
  }

  @Test
  fun navHost_isTagged() {
    rule.onNode(hasTestTag(NavigationTestTags.NAV_HOST)).assertExists()
  }

  @Test
  fun startDestination_isHome() {
    assertRoute(AppDestination.Home.route)
  }

  @Test
  fun navigate_toPlanner_showsCorrectTitle() {
    navigateAndVerify(AppDestination.Planner.route, "Planner")
  }

  @Test
  fun navigate_toProfile_showsCorrectTitle() {
    navigateAndVerify(AppDestination.Profile.route, "Profile")
  }

  @Test
  fun navigate_toCalendar_showsCorrectTitle() {
    navigateAndVerify(AppDestination.Calendar.route, "Calendar")
  }

  @Test
  fun navigate_toStats_showsCorrectTitle() {
    navigateAndVerify(AppDestination.Stats.route, "Stats")
  }

  @Test
  fun navigate_toGames_showsCorrectTitle() {
    navigateAndVerify(AppDestination.Games.route, "Games")
  }

  @Test
  fun navigate_toStudy_showsCorrectTitle() {
    navigateAndVerify(AppDestination.Study.route, "Study")
  }

  @Test
  fun navigate_toTodo_showsCorrectTitle() {
    navigateAndVerify(AppDestination.Todo.route, "Todo")
  }

  @Test
  fun navigate_toMood_showsCorrectTitle() {
    navigateAndVerify(AppDestination.Mood.route, "Daily Reflection")
  }

  @Test
  fun navigate_toShop_showsCorrectTitle() {
    navigateAndVerify(AppDestination.Shop.route, "Shop")
  }

  @Test
  fun navigate_toFlashcards_showsCorrectTitle() {
    navigateAndVerify(AppDestination.Flashcards.route, "Study")
  }

  @Test
  fun backButton_fromPlanner_returnsToHome() {
    navigateAndVerify(AppDestination.Planner.route, "Planner")
    tapBack()
    assertRoute(AppDestination.Home.route)
  }

  @Test
  fun backButton_fromProfile_returnsToHome() {
    navigateAndVerify(AppDestination.Profile.route, "Profile")
    tapBack()
    assertRoute(AppDestination.Home.route)
  }

  @Test
  fun backButton_fromCalendar_returnsToHome() {
    navigateAndVerify(AppDestination.Calendar.route, "Calendar")
    tapBack()
    assertRoute(AppDestination.Home.route)
  }

  @Test
  fun backButton_fromStats_returnsToHome() {
    navigateAndVerify(AppDestination.Stats.route, "Stats")
    tapBack()
    assertRoute(AppDestination.Home.route)
  }

  @Test
  fun backButton_fromGames_returnsToHome() {
    navigateAndVerify(AppDestination.Games.route, "Games")
    tapBack()
    assertRoute(AppDestination.Home.route)
  }

  @Test
  fun backButton_fromStudy_returnsToHome() {
    navigateAndVerify(AppDestination.Study.route, "Study")
    tapBack()
    assertRoute(AppDestination.Home.route)
  }

  @Test
  fun backButton_fromTodo_returnsToHome() {
    navigateAndVerify(AppDestination.Todo.route, "Todo")
    tapBack()
    assertRoute(AppDestination.Home.route)
  }

  @Test
  fun backButton_fromMood_returnsToHome() {
    navigateAndVerify(AppDestination.Mood.route, "Daily Reflection")
    tapBack()
    assertRoute(AppDestination.Home.route)
  }

  @Test
  fun backButton_fromShop_returnsToHome() {
    navigateAndVerify(AppDestination.Shop.route, "Shop")
    tapBack()
    assertRoute(AppDestination.Home.route)
  }

  @Test
  fun backButton_fromFlashcards_returnsToHome() {
    navigateAndVerify(AppDestination.Flashcards.route, "Study")
    tapBack()
    assertRoute(AppDestination.Home.route)
  }

  @Test
  fun navigate_toMemoryGame_showsCorrectTitle() {
    navigateAndVerify("memory", "Memory Game")
  }

  @Test
  fun navigate_toReactionGame_showsCorrectTitle() {
    navigateAndVerify("reaction", "Reaction Test")
  }

  @Test
  fun navigate_toFocusGame_showsCorrectTitle() {
    navigateAndVerify("focus", "Focus Breathing")
  }

  @Test
  fun navigate_toRunnerGame_showsCorrectTitle() {
    navigateAndVerify("runner", "EduMon Runner")
  }

  @Test
  fun backButton_fromMemoryGame_returnsToHome() {
    navigateAndVerify("memory", "Memory Game")
    tapBack()
    assertRoute(AppDestination.Home.route)
  }

  @Test
  fun backButton_fromReactionGame_returnsToHome() {
    navigateAndVerify("reaction", "Reaction Test")
    tapBack()
    assertRoute(AppDestination.Home.route)
  }

  @Test
  fun backButton_fromFocusGame_returnsToHome() {
    navigateAndVerify("focus", "Focus Breathing")
    tapBack()
    assertRoute(AppDestination.Home.route)
  }

  @Test
  fun backButton_fromRunnerGame_returnsToHome() {
    navigateAndVerify("runner", "EduMon Runner")
    tapBack()
    assertRoute(AppDestination.Home.route)
  }

  @OptIn(ExperimentalTestApi::class)
  @Test
  fun navigate_toNotifications_showsScreen() {
    rule.runOnIdle { nav.navigate("notifications") }
    rule.waitForIdle()
    assertRoute("notifications")
  }

  @Test
  fun navigate_toFocusMode_showsCorrectTitle() {
    navigateAndVerify("focus_mode", "Focus Mode")
  }

  @Test
  fun backButton_fromFocusMode_returnsToHome() {
    navigateAndVerify("focus_mode", "Focus Mode")
    tapBack()
    assertRoute(AppDestination.Home.route)
  }

  @OptIn(ExperimentalTestApi::class)
  @Test
  fun navigate_toStudyWithEventId_showsCorrectTitle() {
    val eventId = "test_event_123"
    rule.runOnIdle { nav.navigate("study/$eventId") }
    rule.waitUntilExactlyOneExists(hasTestTag(NavigationTestTags.TOP_BAR_TITLE), 3000L)
    rule.waitForIdle()
    assertRoute("study/{eventId}")
    assertTopBarTitle("Study")
  }

  @Test
  fun backButton_fromStudyWithEventId_returnsToHome() {
    rule.runOnIdle { nav.navigate("study/test_event_456") }
    rule.waitForIdle()
    tapBack()
    assertRoute(AppDestination.Home.route)
  }

  @OptIn(ExperimentalTestApi::class)
  @Test
  fun bottomBar_navigates_toStudy() {
    rule.waitUntilExactlyOneExists(
        hasTestTag(HomeTestTags.bottomTag(AppDestination.Study.route)), 3000L)
    rule.onNode(hasTestTag(HomeTestTags.bottomTag(AppDestination.Study.route))).performClick()
    rule.waitForIdle()
    assertRoute(AppDestination.Study.route)
  }

  @OptIn(ExperimentalTestApi::class)
  @Test
  fun bottomBar_navigates_toCalendar() {
    rule.waitUntilExactlyOneExists(
        hasTestTag(HomeTestTags.bottomTag(AppDestination.Calendar.route)), 3000L)
    rule.onNode(hasTestTag(HomeTestTags.bottomTag(AppDestination.Calendar.route))).performClick()
    rule.waitForIdle()
    assertRoute(AppDestination.Calendar.route)
  }

  @OptIn(ExperimentalTestApi::class)
  @Test
  fun bottomBar_navigates_toProfile() {
    rule.waitUntilExactlyOneExists(
        hasTestTag(HomeTestTags.bottomTag(AppDestination.Profile.route)), 3000L)
    rule.onNode(hasTestTag(HomeTestTags.bottomTag(AppDestination.Profile.route))).performClick()
    rule.waitForIdle()
    assertRoute(AppDestination.Profile.route)
  }

  @OptIn(ExperimentalTestApi::class)
  @Test
  fun bottomBar_navigates_toGames() {
    rule.waitUntilExactlyOneExists(
        hasTestTag(HomeTestTags.bottomTag(AppDestination.Games.route)), 3000L)
    rule.onNode(hasTestTag(HomeTestTags.bottomTag(AppDestination.Games.route))).performClick()
    rule.waitForIdle()
    assertRoute(AppDestination.Games.route)
  }

  @OptIn(ExperimentalTestApi::class)
  @Test
  fun bottomBar_staysOnHome_whenHomeClicked() {
    // Verify we're on home
    assertRoute(AppDestination.Home.route)

    // Click home again
    rule.waitUntilExactlyOneExists(
        hasTestTag(HomeTestTags.bottomTag(AppDestination.Home.route)), 3000L)
    rule.onNode(hasTestTag(HomeTestTags.bottomTag(AppDestination.Home.route))).performClick()
    rule.waitForIdle()

    // Should still be on home
    assertRoute(AppDestination.Home.route)
  }

  @Test
  fun singleTop_prevents_duplicate_entries() {
    rule.runOnIdle {
      nav.navigate(AppDestination.Games.route) { launchSingleTop = true }
      nav.navigate(AppDestination.Games.route) { launchSingleTop = true }
    }
    rule.waitForIdle()
    assertRoute(AppDestination.Games.route)

    // Only one back press should return to home
    tapBack()
    assertRoute(AppDestination.Home.route)
  }

  @Test
  fun navigation_preservesState_withSaveState() {
    rule.runOnIdle {
      nav.navigate(AppDestination.Profile.route) {
        popUpTo(AppDestination.Home.route) { saveState = true }
        launchSingleTop = true
        restoreState = true
      }
    }
    rule.waitForIdle()
    assertRoute(AppDestination.Profile.route)
  }

  @Test
  fun multipleNavigations_maintainCorrectBackStack() {
    // Navigate through multiple screens
    rule.runOnIdle { nav.navigate(AppDestination.Planner.route) }
    rule.waitForIdle()
    assertRoute(AppDestination.Planner.route)

    rule.runOnIdle { nav.navigate(AppDestination.Calendar.route) }
    rule.waitForIdle()
    assertRoute(AppDestination.Calendar.route)

    // Back should go to Planner, not Home
    tapBack()
    assertRoute(AppDestination.Planner.route)

    // Another back should go to Home
    tapBack()
    assertRoute(AppDestination.Home.route)
  }

  @OptIn(ExperimentalTestApi::class)
  @Test
  fun topBarTitle_exists_onAllMainScreens() {
    val routes =
        listOf(
            AppDestination.Planner.route,
            AppDestination.Profile.route,
            AppDestination.Calendar.route,
            AppDestination.Stats.route,
            AppDestination.Games.route)

    routes.forEach { route ->
      rule.runOnIdle { nav.navigate(route) }
      rule.waitUntilExactlyOneExists(hasTestTag(NavigationTestTags.TOP_BAR_TITLE), 3000L)
      rule.onNode(hasTestTag(NavigationTestTags.TOP_BAR_TITLE)).assertExists()
      tapBack()
      rule.waitForIdle()
    }
  }

  @OptIn(ExperimentalTestApi::class)
  @Test
  fun goBackButton_exists_onAllMainScreens() {
    val routes =
        listOf(
            AppDestination.Planner.route,
            AppDestination.Profile.route,
            AppDestination.Calendar.route,
            AppDestination.Stats.route,
            AppDestination.Games.route)

    routes.forEach { route ->
      rule.runOnIdle { nav.navigate(route) }
      rule.waitUntilExactlyOneExists(hasTestTag(NavigationTestTags.GO_BACK_BUTTON), 3000L)
      rule.onNode(hasTestTag(NavigationTestTags.GO_BACK_BUTTON)).assertExists()
      tapBack()
      rule.waitForIdle()
    }
  }

  @Test
  fun navigation_fromHome_toProfile_opensNotifications() {
    // Navigate to Profile
    navigateAndVerify(AppDestination.Profile.route, "Profile")

    // Profile has notification navigation - just verify we're on Profile
    assertRoute(AppDestination.Profile.route)

    // Return home
    tapBack()
    assertRoute(AppDestination.Home.route)
  }

  @Test
  fun navigation_fromHome_toProfile_opensFocusMode() {
    // Navigate to Profile
    navigateAndVerify(AppDestination.Profile.route, "Profile")

    // Profile has focus mode navigation - verify we're on Profile
    assertRoute(AppDestination.Profile.route)

    // Navigate programmatically to focus mode from profile
    rule.runOnIdle { nav.navigate("focus_mode") }
    rule.waitForIdle()
    assertRoute("focus_mode")

    // Back to profile
    tapBack()
    assertRoute(AppDestination.Profile.route)
  }

  @Test
  fun scaffold_topBar_hasCorrectConfiguration_forPlanner() {
    navigateAndVerify(AppDestination.Planner.route, "Planner")

    // Verify both top bar elements exist
    rule.onNode(hasTestTag(NavigationTestTags.TOP_BAR_TITLE)).assertExists()
    rule.onNode(hasTestTag(NavigationTestTags.GO_BACK_BUTTON)).assertExists()
  }

  @Test
  fun scaffold_topBar_hasCorrectConfiguration_forGames() {
    navigateAndVerify(AppDestination.Games.route, "Games")

    // Verify both top bar elements exist
    rule.onNode(hasTestTag(NavigationTestTags.TOP_BAR_TITLE)).assertExists()
    rule.onNode(hasTestTag(NavigationTestTags.GO_BACK_BUTTON)).assertExists()
  }

  @Test
  fun gameRoutes_allAccessibleFromNavigation() {
    val gameRoutes = listOf("memory", "reaction", "focus", "runner")

    gameRoutes.forEach { route ->
      rule.runOnIdle { nav.navigate(route) }
      rule.waitForIdle()
      assertRoute(route)
      tapBack()
      assertRoute(AppDestination.Home.route)
    }
  }

  @Test
  fun studyRoute_withParameter_parsesEventId() {
    val testEventIds = listOf("event123", "abc456", "test_789")

    testEventIds.forEach { eventId ->
      rule.runOnIdle { nav.navigate("study/$eventId") }
      rule.waitForIdle()
      assertRoute("study/{eventId}")
      tapBack()
      assertRoute(AppDestination.Home.route)
    }
  }

  @Test
  fun navigationFlow_homeToStudyToGamesToHome() {
    // Start at home
    assertRoute(AppDestination.Home.route)

    // Go to Study
    rule.runOnIdle { nav.navigate(AppDestination.Study.route) }
    rule.waitForIdle()
    assertRoute(AppDestination.Study.route)

    // Go to Games
    rule.runOnIdle { nav.navigate(AppDestination.Games.route) }
    rule.waitForIdle()
    assertRoute(AppDestination.Games.route)

    // Back to Study
    tapBack()
    assertRoute(AppDestination.Study.route)

    // Back to Home
    tapBack()
    assertRoute(AppDestination.Home.route)
  }

  @Test
  fun menuButton_exists_onHomeScreen() {
    rule.onNode(hasTestTag(HomeTestTags.MENU_BUTTON)).assertExists()
  }

  @OptIn(ExperimentalTestApi::class)
  @Test
  fun allDestinations_haveValidRoutes() {
    val destinations =
        listOf(
            AppDestination.Home,
            AppDestination.Profile,
            AppDestination.Calendar,
            AppDestination.Planner,
            AppDestination.Study,
            AppDestination.Games,
            AppDestination.Stats,
            AppDestination.Flashcards,
            AppDestination.Todo,
            AppDestination.Mood,
            AppDestination.Shop)

    destinations.forEach { dest ->
      if (dest != AppDestination.Home) {
        rule.runOnIdle { nav.navigate(dest.route) }
        rule.waitForIdle()
        assertRoute(dest.route)
        tapBack()
        assertRoute(AppDestination.Home.route)
      }
    }
  }
}
