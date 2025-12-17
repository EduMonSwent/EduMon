package com.android.sample.ui.profile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
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
    // Freeze animations/time so CI speed doesn’t matter.
    composeRule.mainClock.autoAdvance = false

    val initial = UserProfile(level = 1, points = 0, coins = 0, lastRewardedLevel = 0)
    val repo = FakeProfileRepository(initial)
    val viewModel = ProfileViewModel(repo)

    composeRule.setContent { EduMonTheme { ProfileScreen(viewModel = viewModel) } }

    composeRule.runOnIdle { viewModel.addPoints(80) }

    val levelUpText = "Level 2"

    // Give CI more room; also use the unmerged tree for snackbar text.
    composeRule.waitUntil(timeoutMillis = 15_000) {
      composeRule
          .onAllNodesWithText(levelUpText, substring = true, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Advance past snackbar enter animation and let Compose settle.
    composeRule.mainClock.advanceTimeBy(1_500)
    composeRule.waitForIdle()

    // If CI is still flaky on "displayed", prefer assertExists().
    composeRule
        .onNodeWithText(levelUpText, substring = true, useUnmergedTree = true)
        .assertIsDisplayed()
  }
}
