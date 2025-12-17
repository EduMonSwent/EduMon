package com.android.sample.ui.profile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.android.sample.data.UserProfile
import com.android.sample.profile.FakeProfileRepository
import com.android.sample.ui.theme.EduMonTheme
import org.junit.Rule
import org.junit.Test

// The assistance of an AI tool (ChatGPT) was solicited in writing this test file.
class ProfileRewardAndLevelUpTest {

  @get:Rule val composeRule = createComposeRule()

  // ------------------------------------------------------------
  // 1) Progress Bar Test
  // ------------------------------------------------------------
  @Test
  fun levelProgressBar_showsCorrectProgress_forGivenLevelAndPoints() {

    val user = UserProfile(level = 1, points = 0)

    composeRule.setContent { EduMonTheme { ProfileCard(user = user) } }

    // Label is visible
    composeRule.onNodeWithText("Progress to next level").assertIsDisplayed()

    // 50/300 is displayed
    composeRule.onNodeWithText("0 / 80 pts", substring = true).assertIsDisplayed()
  }

  // ------------------------------------------------------------
  // 2) Snackbar Reward Test
  // ------------------------------------------------------------
  @Test
  fun profileScreen_showsSnackbarOnLevelUpReward() {
    val initial = UserProfile(level = 1, points = 0, coins = 0, lastRewardedLevel = 0)
    val repo = FakeProfileRepository(initial)
    val viewModel = ProfileViewModel(profileRepository = repo)

    composeRule.setContent { EduMonTheme { ProfileScreen(viewModel = viewModel) } }

    // Ensure composable tree is ready and the collector is attached.
    composeRule.onNodeWithTag(ProfileSnackbarTestTags.HOST, useUnmergedTree = true).assertExists()
    composeRule.waitForIdle()

    composeRule.runOnIdle { viewModel.addPoints(80) }

    composeRule.waitUntil(timeoutMillis = 60_000) {
      composeRule
          .onAllNodesWithTag(ProfileSnackbarTestTags.MESSAGE, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeRule
        .onNodeWithTag(ProfileSnackbarTestTags.MESSAGE, useUnmergedTree = true)
        .assertExists()
        .assertTextContains("Level 2", substring = true)
  }
}
