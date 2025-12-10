package com.android.sample.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.EduMonNavHost
import com.android.sample.NavigationTestTags
import com.android.sample.feature.homeScreen.AppDestination
import com.android.sample.repos_providors.AppRepositories
import com.android.sample.repos_providors.FakeRepositoriesProvider
import com.android.sample.ui.theme.EduMonTheme
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests navigation back behavior from different screens.
 *
 * Home has a TOP_BAR_TITLE with text "Home" and no GO_BACK_BUTTON (uses drawer menu instead).
 */
@RunWith(AndroidJUnit4::class)
class NavigationDeepLinkBackTest {

  @get:Rule val composeTestRule = createComposeRule()

  private var originalRepositories = AppRepositories

  @Before
  fun setUp() {
    // Use fake repositories to avoid Firestore crashes in CI (no logged-in user)
    AppRepositories = FakeRepositoriesProvider
  }

  @After
  fun tearDown() {
    AppRepositories = originalRepositories
  }

  @Test
  fun statsScreen_backUsesBackStackWhenHistoryExists() {
    var navController: NavHostController? = null

    composeTestRule.setContent {
      EduMonTheme {
        val nav = rememberNavController()
        navController = nav
        EduMonNavHost(navController = nav, startDestination = AppDestination.Home.route)
      }
    }

    composeTestRule.waitForIdle()

    // Navigate programmatically Home -> Stats
    composeTestRule.runOnIdle { navController!!.navigate(AppDestination.Stats.route) }
    composeTestRule.waitForIdle()

    // We should now be on Stats screen with top bar + back button
    composeTestRule.onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.GO_BACK_BUTTON).assertIsDisplayed()

    // Press back: popBackStack() should return true, navigating back to Home
    composeTestRule.onNodeWithTag(NavigationTestTags.GO_BACK_BUTTON).performClick()
    composeTestRule.waitForIdle()

    // Assert: We are back on Home
    composeTestRule
        .onNode(
            hasText("Home") and hasTestTag(NavigationTestTags.TOP_BAR_TITLE),
        )
        .assertIsDisplayed()

    // Home doesn't have a back button (uses menu drawer instead)
    composeTestRule.onNodeWithTag(NavigationTestTags.GO_BACK_BUTTON).assertDoesNotExist()
  }

  @Test
  fun profileScreen_backNavigatesToHome() {
    var navController: NavHostController? = null

    composeTestRule.setContent {
      EduMonTheme {
        val nav = rememberNavController()
        navController = nav
        EduMonNavHost(navController = nav, startDestination = AppDestination.Home.route)
      }
    }

    composeTestRule.waitForIdle()

    // Navigate programmatically Home -> Profile
    composeTestRule.runOnIdle { navController!!.navigate(AppDestination.Profile.route) }
    composeTestRule.waitForIdle()

    // We should now be on Profile screen with top bar + back button
    composeTestRule
        .onNode(hasText("Profile") and hasTestTag(NavigationTestTags.TOP_BAR_TITLE))
        .assertIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.GO_BACK_BUTTON).assertIsDisplayed()

    // Press back
    composeTestRule.onNodeWithTag(NavigationTestTags.GO_BACK_BUTTON).performClick()
    composeTestRule.waitForIdle()

    // Assert: We are back on Home
    composeTestRule
        .onNode(
            hasText("Home") and hasTestTag(NavigationTestTags.TOP_BAR_TITLE),
        )
        .assertIsDisplayed()

    composeTestRule.onNodeWithTag(NavigationTestTags.GO_BACK_BUTTON).assertDoesNotExist()
  }
}
