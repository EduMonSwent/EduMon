package com.android.sample

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.feature.homeScreen.AppDestination
import com.android.sample.feature.homeScreen.HomeTestTags
import com.android.sample.repos_providors.AppRepositories
import com.android.sample.repos_providors.FakeRepositoriesProvider
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
    originalRepositories = AppRepositories
    AppRepositories = FakeRepositoriesProvider
  }

  @After
  fun tearDown() {
    AppRepositories = originalRepositories
  }

  @Test
  fun safeNavigateBack_early_return_path_is_covered() {
    composeTestRule.setContent { EduMonNavHost(startDestination = AppDestination.Home.route) }
    composeTestRule.waitForIdle()
    waitForHomeScreen()

    composeTestRule.onNodeWithTag(HomeTestTags.MENU_BUTTON).assertExists()
    composeTestRule.onNodeWithTag(NavigationTestTags.GO_BACK_BUTTON).assertDoesNotExist()
  }

  @Test
  fun drawer_study_button_navigates_to_study_screen() {
    composeTestRule.setContent { EduMonNavHost(startDestination = AppDestination.Home.route) }
    composeTestRule.waitForIdle()
    waitForHomeScreen()

    composeTestRule.onNodeWithTag(HomeTestTags.MENU_BUTTON).performClick()
    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(HomeTestTags.drawerTag(AppDestination.Study.route))
        .assertExists()
        .performClick()

    composeTestRule.waitUntil(timeoutMillis = 10_000) {
      runCatching {
            composeTestRule
                .onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE)
                .assertTextEquals("Study")
          }
          .isSuccess
    }

    composeTestRule.onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE).assertExists()
  }

  @Test
  fun safeNavigateBack_navigates_to_home_when_popBackStack_fails() {
    composeTestRule.setContent { EduMonNavHost(startDestination = "focus_mode") }
    composeTestRule.waitForIdle()

    composeTestRule.waitUntil(timeoutMillis = 10_000) {
      runCatching {
            composeTestRule
                .onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE)
                .assertTextEquals("Focus Mode")
          }
          .isSuccess
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
    composeTestRule.waitUntil(timeoutMillis = 10_000) {
      runCatching { composeTestRule.onNodeWithTag(HomeTestTags.MENU_BUTTON).assertExists() }
          .isSuccess
    }
  }

  private fun nodeExists(tag: String): Boolean =
      runCatching { composeTestRule.onNodeWithTag(tag, useUnmergedTree = true).assertExists() }
          .isSuccess

  private fun notificationsScreenReached(): Boolean {
    // Accept either a dedicated notifications tag OR a standard top bar title, if you have it.
    if (nodeExists("notifications_title")) return true
    return runCatching {
          composeTestRule
              .onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE)
              .assertTextEquals("Notifications")
        }
        .isSuccess
  }
}
