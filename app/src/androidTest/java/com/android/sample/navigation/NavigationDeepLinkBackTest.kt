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
import com.android.sample.ui.theme.EduMonTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Covers the fallback branch in Navigation.kt where popBackStack() returns false (deep link start
 * with only one entry), triggering navigation to Home.
 *
 * In the new navigation, Home now *does* have a top bar with TOP_BAR_TITLE, but no GO_BACK_BUTTON.
 */
@RunWith(AndroidJUnit4::class)
class NavigationDeepLinkBackTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun deepLinkStudyScreen_backNavigatesToHomeWhenNoHistory() {
    // Start directly on the deep-link study/{eventId} route (no back stack history)
    composeTestRule.setContent { EduMonTheme { EduMonNavHost(startDestination = "study/test123") } }

    // Initially: Study screen top bar and back button are present
    composeTestRule.onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.GO_BACK_BUTTON).assertIsDisplayed()

    // Act: press back (should fail popBackStack and fallback to Home route)
    composeTestRule.onNodeWithTag(NavigationTestTags.GO_BACK_BUTTON).performClick()
    composeTestRule.waitForIdle()

    // Assert: We are now on Home.
    // Home has a TOP_BAR_TITLE with text "Home" and no GO_BACK_BUTTON.
    composeTestRule
        .onNode(
            hasText("Home") and hasTestTag(NavigationTestTags.TOP_BAR_TITLE),
        )
        .assertIsDisplayed()

    composeTestRule.onNodeWithTag(NavigationTestTags.GO_BACK_BUTTON).assertDoesNotExist()
  }

  @Test
  fun studyScreen_backUsesBackStackWhenHistoryExists() {
    var navController: NavHostController? = null

    composeTestRule.setContent {
      EduMonTheme {
        val nav = rememberNavController()
        navController = nav
        EduMonNavHost(navController = nav, startDestination = AppDestination.Home.route)
      }
    }

    // Navigate programmatically Home -> Study
    composeTestRule.runOnIdle { navController!!.navigate(AppDestination.Study.route) }

    // We should now be on Study screen with top bar + back button
    composeTestRule.onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.GO_BACK_BUTTON).assertIsDisplayed()

    // Press back: popBackStack() should return true, so fallback navigation block is skipped,
    // but we still end back on Home via the normal back stack.
    composeTestRule.onNodeWithTag(NavigationTestTags.GO_BACK_BUTTON).performClick()
    composeTestRule.waitForIdle()

    // Assert: We are back on Home (same as above).
    composeTestRule
        .onNode(
            hasText("Home") and hasTestTag(NavigationTestTags.TOP_BAR_TITLE),
        )
        .assertIsDisplayed()

    composeTestRule.onNodeWithTag(NavigationTestTags.GO_BACK_BUTTON).assertDoesNotExist()
  }
}
