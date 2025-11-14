package com.android.sample

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import com.android.sample.feature.homeScreen.AppDestination
import com.android.sample.feature.homeScreen.HomeTestTags
import com.android.sample.ui.location.StudyTogetherScreen
import com.android.sample.ui.location.StudyTogetherViewModel
import com.android.sample.ui.login.LoginScreen
import com.android.sample.ui.theme.EduMonTheme
import org.junit.Rule
import org.junit.Test

/**
 * End-to-end style UI smoke tests that exercise the major features via navigation, plus
 * direct composition for screens not wired in the NavHost (login, StudyTogether).
 *
 * These tests are intentionally light on fragile UI details; they assert stable titles/tags so they
 * remain robust in CI and provide broad coverage across the app.
 */
class AllFeaturesE2ETest {

  @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

  private lateinit var nav: TestNavHostController

  private fun setNavHost() {
    compose.setContent {
      EduMonTheme {
        val ctx = androidx.compose.ui.platform.LocalContext.current
        nav = TestNavHostController(ctx).apply { navigatorProvider.addNavigator(ComposeNavigator()) }
        EduMonNavHost(navController = nav)
      }
    }
    // Wait until Home is visible by checking a stable tag
    compose.waitUntilAtLeastOneExists(hasTestTag(HomeTestTags.MENU_BUTTON))
  }

  private fun assertTopBarTitle(expected: String) {
    compose.onNode(hasTestTag(NavigationTestTags.TOP_BAR_TITLE) and hasText(expected)).assertIsDisplayed()
  }

  private fun navigateDirect(route: String) {
    compose.runOnUiThread { nav.navigate(route) }
    compose.waitForIdle()
  }

  @Test
  fun loginScreen_shows_core_controls() {
    compose.setContent { EduMonTheme { LoginScreen(onLoggedIn = {}) } }
    compose.onNodeWithText("Connect yourself to EduMon.").assertIsDisplayed()
    compose.onNodeWithText("Continue with Google").assertIsDisplayed()
  }

  @Test
  fun home_core_sections_and_nav_titles() {
    setNavHost()

    // Home sanity
    compose.onNodeWithTag(HomeTestTags.MENU_BUTTON).assertIsDisplayed()
    compose.onNodeWithTag(HomeTestTags.TODAY_SEE_ALL).assertIsDisplayed()

    // Visit core destinations and assert their top bar titles
    val cases = listOf(
        AppDestination.Planner.route to "Planner",
        AppDestination.Profile.route to "Profile",
        AppDestination.Calendar.route to "Calendar",
        AppDestination.Stats.route to "Stats",
        AppDestination.Games.route to "Games",
        AppDestination.Study.route to "Study",
        AppDestination.Flashcards.route to "Study",
        AppDestination.Todo.route to "Todo",
        AppDestination.Mood.route to "Daily Reflection",
    )

    cases.forEach { (route, title) ->
      navigateDirect(route)
      assertTopBarTitle(title)
      // back to Home
      compose.onNodeWithTag(NavigationTestTags.GO_BACK_BUTTON).performClick()
      compose.waitForIdle()
      // ensure Home menu button is visible again
      compose.onNodeWithTag(HomeTestTags.MENU_BUTTON).assertIsDisplayed()
    }
  }

  @Test
  fun studyTogether_core_controls_render() {
    // Not wired in NavHost; render directly and test core controls using testTags
    compose.setContent { EduMonTheme { StudyTogetherScreen(viewModel = StudyTogetherViewModel(), showMap = false) } }
    compose.onNodeWithTag("map_stub").assertIsDisplayed()
    compose.onNodeWithTag("btn_friends").assertIsDisplayed()
    compose.onNodeWithTag("fab_add_friend").assertIsDisplayed()

    // Expand friends panel (button text area), then collapse
    compose.onNodeWithTag("btn_friends").performClick()
    // We don’t assert the inner content text to keep this test robust across locales
  }
}

