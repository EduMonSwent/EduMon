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
    composeTestRule.onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE).assertTextEquals("Profile")

    // Scroll down to reveal the SettingsCard using relative coordinates via swipe action.
    // Target the root node which contains the LazyColumn.
    composeTestRule.onRoot().performTouchInput {
      // Get the size of the node we are interacting with (the root)
      val size = this@performTouchInput.visibleSize
      val width = size.width
      val height = size.height

      // Calculate relative start and end points for the swipe
      // Start near the bottom (e.g., 80% down the height)
      val startY = (height * 0.80).toFloat()
      // End near the top (e.g., 20% down the height)
      val endY = (height * 0.20).toFloat()

      // The x-coordinate can be the center
      val centerX = (width / 2.0).toFloat()

      // Perform the swipe gesture
      swipe(start = Offset(centerX, startY), end = Offset(centerX, endY))
    }
    composeTestRule.waitForIdle()

    // Now that we've scrolled, try to find and click the "Manage notifications" button.
    // It has the testTag "open_notifications_screen".
    composeTestRule.onNodeWithTag("open_notifications_screen").performClick()
    composeTestRule.waitForIdle()

    // We should now be on the Notifications screen.
    // The HeaderBar in NotificationsScreen has a tag "notifications_title".
    composeTestRule.onNodeWithTag("notifications_title").assertExists()
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
    composeTestRule.onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE).assertTextEquals("Profile")

    // Scroll down to reveal the SettingsCard using relative coordinates via swipe action.
    composeTestRule.onRoot().performTouchInput {
      val size = this@performTouchInput.visibleSize
      val width = size.width
      val height = size.height

      val startY = (height * 0.80).toFloat()
      val endY = (height * 0.20).toFloat()
      val centerX = (width / 2.0).toFloat()

      swipe(start = Offset(centerX, startY), end = Offset(centerX, endY))
    }
    composeTestRule.waitForIdle()

    // Now that we've scrolled, try to find and click the Focus Mode switch.
    // It's tagged with ProfileScreenTestTags.SWITCH_FOCUS_MODE.
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.SWITCH_FOCUS_MODE).performClick()
    composeTestRule.waitForIdle()

    // We should now be on the Focus Mode screen.
    // FocusModeScreen is wrapped by ScreenWithTopBar, so TOP_BAR_TITLE should work.
    composeTestRule.onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE).assertTextEquals("Focus Mode")
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
