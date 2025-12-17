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
    // Freeze time so animations and CI slowness don’t break the assertions.
    composeRule.mainClock.autoAdvance = false

    val initial = UserProfile(level = 1, points = 0, coins = 0, lastRewardedLevel = 0)
    val repo = FakeProfileRepository(initial)
    val viewModel = ProfileViewModel(repo)

    composeRule.setContent { EduMonTheme { ProfileScreen(viewModel = viewModel) } }

    composeRule.runOnIdle { viewModel.addPoints(80) }

    // Wait until the snackbar node exists (tag-based, CI-safe).
    composeRule.waitUntil(timeoutMillis = 20_000) {
      composeRule
          .onAllNodesWithTag(ProfileSnackbarTestTags.SNACKBAR, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Advance past snackbar enter animation deterministically.
    composeRule.mainClock.advanceTimeBy(2_000)
    composeRule.waitForIdle()

    // Assert snackbar is present (assertExists is the most CI-stable).
    composeRule
        .onNodeWithTag(ProfileSnackbarTestTags.SNACKBAR, useUnmergedTree = true)
        .assertExists()

    // Optional: also assert the message contains "Level 2" (still stable since we anchor by tag).
    composeRule
        .onNodeWithTag(ProfileSnackbarTestTags.MESSAGE, useUnmergedTree = true)
        .assertExists()
        .assertTextContains("Level 2", substring = true)
  }
}
