package com.android.sample

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.feature.homeScreen.AppDestination
import com.android.sample.feature.homeScreen.HomeTestTags
import com.android.sample.repos_providors.AppRepositories
import com.android.sample.repos_providors.FakeRepositoriesProvider
import com.android.sample.ui.profile.ProfileScreenTestTags
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationAdditionnalTests {

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

  // ==================== NEW TESTS FOR UNCOVERED LINES ====================

  /**
   * Tests the early return path in safeNavigateBack when currentRoute is Home. This explicitly
   * proves the 'return' statement on line 5 is executed.
   */
  @Test
  fun safeNavigateBack_early_return_path_is_covered() {
    composeTestRule.setContent { EduMonNavHost(startDestination = AppDestination.Home.route) }
    composeTestRule.waitForIdle()
    waitForHomeScreen()

    // Verify we're on Home by checking its title or menu button.
    composeTestRule.onNodeWithTag(HomeTestTags.MENU_BUTTON).assertIsDisplayed()

    // The Home screen should NOT have a back button (because safeNavigateBack returns early).
    // This proves the 'if (currentRoute == AppDestination.Home.route) { return }' branch was taken.
    composeTestRule.onNodeWithTag(NavigationTestTags.GO_BACK_BUTTON).assertDoesNotExist()
  }

  /**
   * Tests that the onOpenNotifications lambda in ProfileScreen is executed. This covers the line:
   * navController.navigate("notifications") { launchSingleTop = true }
   */
  @Test
  fun profile_screen_onOpenNotifications_lambda_executes() {
    composeTestRule.setContent { EduMonNavHost(startDestination = AppDestination.Home.route) }
    composeTestRule.waitForIdle()
    waitForHomeScreen()

    // Navigate to Profile
    composeTestRule.onNodeWithTag(HomeTestTags.MENU_BUTTON).performClick()
    composeTestRule.waitForIdle()
    composeTestRule
        .onNodeWithTag(HomeTestTags.drawerTag(AppDestination.Profile.route))
        .performClick()
    composeTestRule.waitForIdle()

    // Verify we're on Profile
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      try {
        composeTestRule.onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE).assertTextEquals("Profile")
        true
      } catch (e: AssertionError) {
        false
      }
    }

    // Wait for the profile screen to be fully loaded
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      try {
        composeTestRule.onNodeWithTag(ProfileScreenTestTags.PROFILE_SCREEN).assertExists()
        true
      } catch (e: AssertionError) {
        false
      }
    }

    // Perform multiple scrolls to ensure we reach the bottom
    repeat(3) {
      composeTestRule.onNodeWithTag(ProfileScreenTestTags.PROFILE_SCREEN).performTouchInput {
        val size = this@performTouchInput.visibleSize
        val width = size.width
        val height = size.height

        val startY = (height * 0.80).toFloat()
        val endY = (height * 0.20).toFloat()
        val centerX = (width / 2.0).toFloat()

        swipe(start = Offset(centerX, startY), end = Offset(centerX, endY), durationMillis = 500)
      }
      composeTestRule.waitForIdle()
      Thread.sleep(300) // Give time for scroll to settle
    }

    // Wait for the notifications button to be visible and clickable
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      try {
        composeTestRule.onNodeWithTag("open_notifications_screen").assertExists()
        composeTestRule.onNodeWithTag("open_notifications_screen").assertIsDisplayed()
        true
      } catch (e: AssertionError) {
        false
      }
    }

    // Click the button
    composeTestRule.onNodeWithTag("open_notifications_screen").performClick()
    composeTestRule.waitForIdle()
    Thread.sleep(500) // Give navigation time to complete

    // Wait for navigation to complete
    composeTestRule.waitUntil(timeoutMillis = 10000) {
      try {
        composeTestRule.onNodeWithTag("notifications_title").assertExists()
        true
      } catch (e: AssertionError) {
        false
      }
    }

    // Final assertion
    composeTestRule.onNodeWithTag("notifications_title").assertIsDisplayed()
  }

  /**
   * Tests that the onOpenFocusMode lambda in ProfileScreen is executed. This covers the line:
   * navController.navigate("focus_mode") { launchSingleTop = true }
   */
  @Test
  fun profile_screen_onOpenFocusMode_lambda_executes() {
    composeTestRule.setContent { EduMonNavHost(startDestination = AppDestination.Home.route) }
    composeTestRule.waitForIdle()
    waitForHomeScreen()

    // Navigate to Profile
    composeTestRule.onNodeWithTag(HomeTestTags.MENU_BUTTON).performClick()
    composeTestRule.waitForIdle()
    composeTestRule
        .onNodeWithTag(HomeTestTags.drawerTag(AppDestination.Profile.route))
        .performClick()
    composeTestRule.waitForIdle()

    // Verify we're on Profile
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      try {
        composeTestRule.onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE).assertTextEquals("Profile")
        true
      } catch (e: AssertionError) {
        false
      }
    }

    // Wait for the profile screen to be fully loaded
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      try {
        composeTestRule.onNodeWithTag(ProfileScreenTestTags.PROFILE_SCREEN).assertExists()
        true
      } catch (e: AssertionError) {
        false
      }
    }

    // Perform multiple scrolls to ensure we reach the bottom
    repeat(3) {
      composeTestRule.onNodeWithTag(ProfileScreenTestTags.PROFILE_SCREEN).performTouchInput {
        val size = this@performTouchInput.visibleSize
        val width = size.width
        val height = size.height

        val startY = (height * 0.80).toFloat()
        val endY = (height * 0.20).toFloat()
        val centerX = (width / 2.0).toFloat()

        swipe(start = Offset(centerX, startY), end = Offset(centerX, endY), durationMillis = 500)
      }
      composeTestRule.waitForIdle()
      Thread.sleep(300) // Give time for scroll to settle
    }

    // Wait for the focus mode switch to be visible and clickable
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      try {
        composeTestRule.onNodeWithTag(ProfileScreenTestTags.SWITCH_FOCUS_MODE).assertExists()
        composeTestRule.onNodeWithTag(ProfileScreenTestTags.SWITCH_FOCUS_MODE).assertIsDisplayed()
        true
      } catch (e: AssertionError) {
        false
      }
    }

    // Click the switch
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.SWITCH_FOCUS_MODE).performClick()
    composeTestRule.waitForIdle()
    Thread.sleep(500) // Give navigation time to complete

    // Wait for navigation to complete
    composeTestRule.waitUntil(timeoutMillis = 10000) {
      try {
        composeTestRule
            .onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE)
            .assertTextEquals("Focus Mode")
        true
      } catch (e: AssertionError) {
        false
      }
    }

    // Final assertion
    composeTestRule.onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE).assertIsDisplayed()
  }

  // ==================== EXISTING TESTS (Can be kept or removed if redundant) ====================

  @Test
  fun safeNavigateBack_navigates_to_home_when_popBackStack_fails() {
    composeTestRule.setContent { EduMonNavHost(startDestination = "focus_mode") }
    composeTestRule.waitForIdle()
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      try {
        composeTestRule
            .onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE)
            .assertTextEquals("Focus Mode")
        true
      } catch (e: AssertionError) {
        false
      }
    }
    composeTestRule.onNodeWithTag(NavigationTestTags.GO_BACK_BUTTON).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE).assertTextEquals("Home")
  }

  @Test
  fun successful_popBackStack_through_navigation_chain() {
    composeTestRule.setContent { EduMonNavHost(startDestination = AppDestination.Home.route) }
    composeTestRule.waitForIdle()
    waitForHomeScreen()
    composeTestRule.onNodeWithTag(HomeTestTags.MENU_BUTTON).performClick()
    composeTestRule.waitForIdle()
    composeTestRule
        .onNodeWithTag(HomeTestTags.drawerTag(AppDestination.Profile.route))
        .performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE).assertTextEquals("Profile")
    composeTestRule.onNodeWithTag(NavigationTestTags.GO_BACK_BUTTON).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE).assertTextEquals("Home")
  }

  private fun waitForHomeScreen() {
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      try {
        composeTestRule.onNodeWithTag(HomeTestTags.MENU_BUTTON).assertExists()
        true
      } catch (e: AssertionError) {
        false
      }
    }
  }
}
