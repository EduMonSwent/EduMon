package com.android.sample.ui.profile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
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
    // Given: level 2, 350 points
    // Level 2 base = 300 → progress = 50/300
    val user = UserProfile(level = 2, points = 350)

    composeRule.setContent { EduMonTheme { ProfileCard(user = user) } }

    // Label is visible
    composeRule.onNodeWithText("Progress to next level").assertIsDisplayed()

    // 50/300 is displayed
    composeRule.onNodeWithText("50 / 300 pts", substring = true).assertIsDisplayed()
  }

  // ------------------------------------------------------------
  // 2) Snackbar Reward Test
  // ------------------------------------------------------------
  @Test
  fun profileScreen_showsSnackbarOnLevelUpReward() {
    val initial = UserProfile(level = 1, points = 0, coins = 0, lastRewardedLevel = 0)
    val repo = FakeProfileRepository(initial)
    val viewModel = ProfileViewModel(repo)

    composeRule.setContent { EduMonTheme { ProfileScreen(viewModel = viewModel) } }

    // Trigger level-up: +300 points = level 2
    composeRule.runOnIdle { viewModel.addPoints(300) }

    // Wait for snackbar text
    composeRule.waitUntil(timeoutMillis = 5000) {
      composeRule.onAllNodesWithText("Level 2", substring = true).fetchSemanticsNodes().isNotEmpty()
    }

    // Assert snackbar actually visible
    composeRule.onAllNodesWithText("Level 2", substring = true).onFirst().assertIsDisplayed()
  }
}
