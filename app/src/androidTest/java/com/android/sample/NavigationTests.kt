package com.android.sample

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import com.android.sample.screens.AppDestination
import com.android.sample.screens.HomeTestTags
import junit.framework.TestCase.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * These tests validate navigation from the Home screen:
 * - Drawer items (left burger menu)
 * - Bottom bar items
 * - In-card actions (See all / Open Planner / Quick Study)
 */
class HomeNavigationTests {

  @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

  private lateinit var nav: TestNavHostController

  @OptIn(ExperimentalTestApi::class)
  private fun setContent() {
    rule.setContent {
      val ctx = LocalContext.current
      nav = TestNavHostController(ctx).apply { navigatorProvider.addNavigator(ComposeNavigator()) }
      // Your NavHost composable should accept a navController parameter.
      MaterialTheme { EduMonNavHost(navController = nav) }
    }
    // Wait until the menu button is in the tree (home rendered)
    rule.waitUntilExactlyOneExists(hasTestTag(HomeTestTags.MENU_BUTTON))
  }

  private fun openDrawer() {
    rule.onNode(hasTestTag(HomeTestTags.MENU_BUTTON)).performClick()
  }

  @OptIn(ExperimentalTestApi::class)
  private fun tapDrawer(route: String) {
    openDrawer()
    val tag = HomeTestTags.drawerTag(route)
    rule.waitUntilExactlyOneExists(hasTestTag(tag))
    rule.onNode(hasTestTag(tag)).performClick()
    rule.waitForIdle()
  }

  @OptIn(ExperimentalTestApi::class)
  private fun tapBottom(route: String) {
    val tag = HomeTestTags.bottomTag(route)
    rule.waitUntilExactlyOneExists(hasTestTag(tag))
    rule.onNode(hasTestTag(tag)).performClick()
    rule.waitForIdle()
  }

  private fun assertRoute(expected: String) {
    assertEquals(expected, nav.currentBackStackEntry?.destination?.route)
  }

  // -------- Drawer: one test per item --------

  @Test
  fun drawer_Home() {
    setContent()
    tapDrawer(AppDestination.Home.route)
    assertRoute(AppDestination.Home.route)
  }

  @Test
  fun drawer_Profile() {
    setContent()
    tapDrawer(AppDestination.Profile.route)
    assertRoute(AppDestination.Profile.route)
  }

  @Test
  fun drawer_Calendar() {
    setContent()
    tapDrawer(AppDestination.Calendar.route)
    assertRoute(AppDestination.Calendar.route)
  }

  @Test
  fun drawer_Planner() {
    setContent()
    tapDrawer(AppDestination.Planner.route)
    assertRoute(AppDestination.Planner.route)
  }

  @Test
  fun drawer_Study() {
    setContent()
    tapDrawer(AppDestination.Study.route)
    assertRoute(AppDestination.Study.route)
  }

  @Test
  fun drawer_Games() {
    setContent()
    tapDrawer(AppDestination.Games.route)
    assertRoute(AppDestination.Games.route)
  }

  @Test
  fun drawer_Stats() {
    setContent()
    tapDrawer(AppDestination.Stats.route)
    assertRoute(AppDestination.Stats.route)
  }

  @Test
  fun drawer_Flashcards() {
    setContent()
    tapDrawer(AppDestination.Flashcards.route)
    assertRoute(AppDestination.Flashcards.route)
  }

  // -------- Bottom bar: one test per tab --------

  @Test
  fun bottom_Home() {
    setContent()
    tapBottom(AppDestination.Home.route)
    assertRoute(AppDestination.Home.route)
  }

  @Test
  fun bottom_Calendar() {
    setContent()
    tapBottom(AppDestination.Calendar.route)
    assertRoute(AppDestination.Calendar.route)
  }

  @Test
  fun bottom_Study() {
    setContent()
    tapBottom(AppDestination.Study.route)
    assertRoute(AppDestination.Study.route)
  }

  @Test
  fun bottom_Profile() {
    setContent()
    tapBottom(AppDestination.Profile.route)
    assertRoute(AppDestination.Profile.route)
  }

  @Test
  fun bottom_Games() {
    setContent()
    tapBottom(AppDestination.Games.route)
    assertRoute(AppDestination.Games.route)
  }

  // -------- In-card actions (home content) --------

  // (Optional) make sure game cards are clickable when you are on the Games screen.
  // We use text match because the cards are inside that screen, not the drawer/bottom bar.
  @Test
  fun from_Games_each_card_is_clickable() {
    setContent()
    tapBottom(AppDestination.Games.route)

    val cards =
        listOf(
            "Memory Game" to "memory",
            "Reaction Test" to "reaction",
            "Focus Breathing" to "focus",
            "EduMon Runner" to "runner",
        )

    // Click each card and verify the route; then go back to Games to click the next one.
    cards.forEach { (label, route) ->
      rule.onAllNodes(hasText(label) and hasClickAction()).onFirst().performClick()
      assertEquals(route, nav.currentBackStackEntry?.destination?.route)
      // Use the top app bar back button provided in the game routes
      rule.onNode(hasTestTag(NavigationTestTags.GO_BACK_BUTTON)).performClick()
      assertRoute(AppDestination.Games.route)
    }
  }
}
