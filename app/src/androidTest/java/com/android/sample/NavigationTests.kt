package com.android.sample

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import com.android.sample.feature.homeScreen.AppDestination
import com.android.sample.feature.homeScreen.HomeTestTags
import junit.framework.TestCase.assertEquals
import org.junit.Rule
import org.junit.Test

class HomeNavigationTests {

  @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

  private lateinit var nav: TestNavHostController

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
    rule.waitForIdle()
  }

  @Test
  fun navHost_isTagged() {
    setContent()
    rule.onNode(hasTestTag(NavigationTestTags.NAV_HOST)).assertExists()
  }

  @Test
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
            AppDestination.Mood.route to "Daily Reflection")

    cases.forEach { (route, title) ->
      navigateDirect(route)
      assertTopBarTitle(title)
      tapBack()
      assertRoute(AppDestination.Home.route)
    }
  }

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
}
