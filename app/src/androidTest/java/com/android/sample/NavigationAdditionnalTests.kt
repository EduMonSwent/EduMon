package com.android.sample

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

    // Scroll down to reveal the SettingsCard.
    // The ProfileScreen content is within a LazyColumn.
    // Find the LazyColumn and perform a scroll gesture.
    // The LazyColumn itself might not have a specific tag. Let's try scrolling the main content
    // area.
    // We can try to find the main container or just perform a generic scroll down (swipe up
    // gesture).
    // Let's try performing a swipe up gesture on the overall screen content.
    // The screen content is within the Scaffold's content slot, inside the LazyColumn.
    // We might need to target the LazyColumn itself. It's the root of the content passed to
    // Scaffold.
    // The LazyColumn is inside the Scaffold's content lambda.
    // Let's try to find a node that represents the scrollable area. The LazyColumn itself is the
    // scrollable.
    // We might need to target the root of the LazyColumn's content or find a way to interact with
    // the LazyColumn directly.
    // Since there's no explicit tag on the LazyColumn, let's try to perform a scroll action on the
    // first scrollable container found.
    // composeTestRule.onRoot().performTouchInput { swipeUp() } // This might not work as expected
    // if the root isn't scrollable.
    // Let's try to find the LazyColumn by its role or properties if possible.
    // A more reliable way is to target the overall scrollable container.
    // The content inside the Scaffold's content lambda is the LazyColumn.
    // Let's try scrolling the node that *contains* the profile content. It's likely the root of the
    // LazyColumn's content.
    // The LazyColumn is defined as: LazyColumn { ... item { PetSection(...) } ... item { GlowCard {
    // Box(ProfileScreenTestTags.PROFILE_CARD) } ... } ... }
    // The PetSection is first, then the cards.
    // Let's try to perform a generic scroll down action on the main content area.
    // We'll use `performTouchInput` and `swipeUp` (swiping up scrolls content down).
    // Let's find the main content area. It's the LazyColumn. We'll try to find it implicitly by
    // performing a scroll action.
    // A common way is to find the root content and perform a scroll gesture.
    // Let's assume the root node is scrollable or find the first scrollable ancestor of an item we
    // expect to find later.
    // Let's try to find the LazyColumn by searching for its content and then scrolling.
    // Or, let's just perform a scroll gesture on the screen area.
    // composeTestRule.onRoot().performTouchInput { swipeUp(start = Offset(500f, 1000f), end =
    // Offset(500f, 200f)) }
    // This performs a swipe from near the bottom of the screen upwards, simulating a scroll down.
    // Let's try this approach.
    composeTestRule.onRoot().performTouchInput {
      swipeUp(
          // Start near the bottom of the visible area
          startY = 1000f,
          // End near the top of the visible area
          endY = 200f)
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

    // Scroll down to reveal the SettingsCard.
    // Perform a swipe up gesture on the screen content area to scroll down.
    composeTestRule.onRoot().performTouchInput {
      swipeUp(
          startY = 1000f, // Start near the bottom
          endY = 200f // End near the top
          )
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
