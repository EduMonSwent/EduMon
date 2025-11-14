package com.android.sample.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.EduMonNavHost
import com.android.sample.NavigationTestTags
import com.android.sample.ui.theme.EduMonTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Covers the fallback branch in Navigation.kt where popBackStack() returns false (deep link start
 * with only one entry), triggering navigation to Home.
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

    // Assert: Study top bar is gone (Home route has no top bar)
    composeTestRule.onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE).assertDoesNotExist()
    // Back button should also be gone
    composeTestRule.onNodeWithTag(NavigationTestTags.GO_BACK_BUTTON).assertDoesNotExist()
  }
}
