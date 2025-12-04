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
import com.android.sample.repos_providors.AppRepositories
import com.android.sample.repos_providors.FakeRepositories
import com.android.sample.ui.schedule.ScheduleScreenTestTags
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

  @Before
  fun setup() {
    AppRepositories = FakeRepositories
  }

  @OptIn(ExperimentalTestApi::class)
  private fun setContent() {
    rule.setContent {
      val ctx = LocalContext.current
      nav = TestNavHostController(ctx).apply { navigatorProvider.addNavigator(ComposeNavigator()) }
      MaterialTheme { EduMonNavHost(navController = nav) }
    }
    // Ensure Home is rendered before proceeding
    rule.waitUntilExactlyOneExists(hasTestTag(HomeTestTags.MENU_BUTTON))
  }

  private fun assertRoute(expected: String) {
    assertEquals(expected, nav.currentBackStackEntry?.destination?.route)
  }

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
    rule.runOnUiThread { nav.navigate(route) }
    waitUntilRoute(route)
    rule.waitForIdle()
  }

  @OptIn(ExperimentalTestApi::class)
  private fun openDrawerAndWait() {
    rule.onNode(hasTestTag(HomeTestTags.MENU_BUTTON)).performClick()
    // Let the drawer animation finish and items appear
    rule.mainClock.advanceTimeBy(1000L)
    rule.waitUntilExactlyOneExists(hasTestTag(HomeTestTags.drawerTag(AppDestination.Home.route)))
  }

  @Test
  fun navHost_isTagged() {
    setContent()
    rule.onNode(hasTestTag(NavigationTestTags.NAV_HOST)).assertExists()
  }

  /*@Test
  fun topBar_and_back_work_for_all_sections() {
    setContent()

    val cases =
        listOf(
            AppDestination.Schedule.route to "Schedule",
            AppDestination.Profile.route to "Profile",
            AppDestination.Stats.route to "Stats",
            AppDestination.Games.route to "Games",
            AppDestination.Study.route to "Study",
            AppDestination.Todo.route to "Todo",
            AppDestination.Mood.route to "Daily Reflection",
            AppDestination.Shop.route to "Shop",
            AppDestination.StudyTogether.route to "Study Together")

    cases.forEach { (route, title) ->
      navigateDirect(route)
      assertTopBarTitle(title)
      tapBack()
      waitUntilRoute(AppDestination.Home.route)
      assertRoute(AppDestination.Home.route)
    }
  }*/

  @Test
  fun game_routes_show_correct_titles() {
    setContent()

    val games =
        listOf(
            "memory" to "Memory Game",
            "reaction" to "Reaction Test",
            "focus" to "Focus Breathing",
            "runner" to "EduMon Runner")

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
  fun singleTop_prevents_duplicate_entries() {
    setContent()
    rule.runOnIdle {
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

  @OptIn(ExperimentalTestApi::class)
  @Test
  fun scheduleFab_navigatesTo_addTodoFromSchedule_route() {
    setContent()
    navigateDirect(AppDestination.Schedule.route)
    waitUntilRoute(AppDestination.Schedule.route)
    assertRoute(AppDestination.Schedule.route)

    rule.waitUntilExactlyOneExists(hasTestTag(ScheduleScreenTestTags.FAB_ADD))
    rule.onNode(hasTestTag(ScheduleScreenTestTags.FAB_ADD)).performClick()
    rule.waitForIdle()

    val route = nav.currentBackStackEntry?.destination?.route
    assert(route != null && route.startsWith("addTodoFromSchedule")) {
      "Expected route starting with addTodoFromSchedule but was: $route"
    }
  }
}
