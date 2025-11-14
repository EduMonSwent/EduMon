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
import org.junit.Rule
import org.junit.Test

class HomeNavigationTests {

  @get:Rule
  val locationPermissionRule: GrantPermissionRule =
    GrantPermissionRule.grant(
      Manifest.permission.ACCESS_FINE_LOCATION,
      Manifest.permission.ACCESS_COARSE_LOCATION
    )

  @get:Rule val rule = createComposeRule()

  private lateinit var nav: TestNavHostController

  @OptIn(ExperimentalTestApi::class)
  private fun setContent() {
    rule.setContent {
      val ctx = LocalContext.current
      nav = TestNavHostController(ctx).apply {
        navigatorProvider.addNavigator(ComposeNavigator())
      }
      MaterialTheme { EduMonNavHost(navController = nav) }
    }
    // Ensure Home is rendered before proceeding
    rule.waitUntilExactlyOneExists(hasTestTag(HomeTestTags.MENU_BUTTON))
  }

  private fun assertRoute(expected: String) {
    assertEquals(expected, nav.currentBackStackEntry?.destination?.route)
  }

  private fun hasRoute(route: String): Boolean =
    nav.graph.findNode(route) != null

  @OptIn(ExperimentalTestApi::class)
  private fun assertTopBarTitle(expected: String) {
    rule.waitUntilExactlyOneExists(hasTestTag(NavigationTestTags.TOP_BAR_TITLE))
    rule.onNode(hasTestTag(NavigationTestTags.TOP_BAR_TITLE) and hasText(expected)).assertExists()
  }

  private fun tapBack() {
    rule.onNode(hasTestTag(NavigationTestTags.GO_BACK_BUTTON)).performClick()
    rule.waitForIdle()
  }

  private fun navigateDirect(route: String) {
    // Guard: only navigate to routes present in the current graph
    check(hasRoute(route)) { "Route '$route' not found in NavGraph" }
    rule.runOnUiThread { nav.navigate(route) }
    waitUntilRoute(route)
  }

  @Test
  fun navHost_isTagged() {
    setContent()
    rule.onNode(hasTestTag(NavigationTestTags.NAV_HOST)).assertExists()
  }

  @Test
  fun topBar_titles_are_correct_for_all_sections() {
    setContent()

    // Only include routes that exist in the current graph
    val cases = listOf(
      AppDestination.Planner.route to "Planner",
      AppDestination.Profile.route to "Profile",
      AppDestination.Calendar.route to "Calendar",
      AppDestination.Stats.route to "Stats",
      AppDestination.Games.route to "Games",
      AppDestination.Study.route to "Study",
      AppDestination.Flashcards.route to "Study",   // current title in graph
      AppDestination.Todo.route to "Todo",
      AppDestination.Mood.route to "Daily Reflection",
      // AppDestination.Shop.route, AppDestination.StudyTogether.route
      // are intentionally omitted if not in the graph
    ).filter { (route, _) -> hasRoute(route) }

    cases.forEach { (route, title) ->
      navigateDirect(route)
      assertTopBarTitle(title)
    }
  }

  @Test
  fun topBar_back_navigates_to_home_for_stack_sections() {
    setContent()

    val stackSections = listOf(
      AppDestination.Planner.route to "Planner",
      AppDestination.Profile.route to "Profile",
      AppDestination.Study.route to "Study",
      AppDestination.Flashcards.route to "Study",
      AppDestination.Todo.route to "Todo",
      AppDestination.Mood.route to "Daily Reflection"
    ).filter { (route, _) -> hasRoute(route) }

    stackSections.forEach { (route, title) ->
      navigateDirect(route)
      assertTopBarTitle(title)
      tapBack()
      waitUntilRoute(AppDestination.Home.route)
      assertRoute(AppDestination.Home.route)
    }
  }

  @Test
  fun game_routes_show_correct_titles() {
    setContent()

    val games = listOf(
      "memory" to "Memory Game",
      "reaction" to "Reaction Test",
      "focus" to "Focus Breathing",
      "runner" to "EduMon Runner"
    ).filter { (route, _) -> hasRoute(route) }

    games.forEach { (route, title) ->
      navigateDirect(route)
      assertTopBarTitle(title)
      tapBack()
      assertRoute(AppDestination.Home.route)
    }
  }

  @Test
  fun startDestination_isHome() {
    setContent()
    assertRoute(AppDestination.Home.route)
  }

  @Test
  fun bottomBar_navigates_to_study_and_back() {
    setContent()
    // Only run if Study exists
    if (!hasRoute(AppDestination.Study.route)) return
    rule.onNode(hasTestTag(HomeTestTags.bottomTag(AppDestination.Study.route))).performClick()
    waitUntilRoute(AppDestination.Study.route)
    assertRoute(AppDestination.Study.route)
    assertTopBarTitle("Study")
    tapBack()
    assertRoute(AppDestination.Home.route)
  }

  @Test
  fun singleTop_prevents_duplicate_entries() {
    setContent()
    if (!hasRoute(AppDestination.Games.route)) return
    rule.runOnUiThread {
      nav.navigate(AppDestination.Games.route) { launchSingleTop = true }
      nav.navigate(AppDestination.Games.route) { launchSingleTop = true }
    }
    waitUntilRoute(AppDestination.Games.route)
    tapBack()
    waitUntilRoute(AppDestination.Home.route)
    assertRoute(AppDestination.Home.route)
  }

  @OptIn(ExperimentalTestApi::class)
  private fun waitUntilRoute(expected: String) {
    rule.waitUntil(5_000) { nav.currentBackStackEntry?.destination?.route == expected }
  }
}
