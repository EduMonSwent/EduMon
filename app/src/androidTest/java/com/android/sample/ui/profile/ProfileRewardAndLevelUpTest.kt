package com.android.sample.ui.profile

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.android.sample.data.UserProfile
import com.android.sample.data.UserStats
import com.android.sample.data.UserStatsRepository
import com.android.sample.profile.FakeProfileRepository
import com.android.sample.ui.theme.EduMonTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Rule
import org.junit.Test

class ProfileRewardAndLevelUpTest {

  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  private class FakeUserStatsRepository(initial: UserStats = UserStats(points = 0, coins = 0)) :
      UserStatsRepository {

    private val _stats = MutableStateFlow(initial)
    override val stats: StateFlow<UserStats> = _stats

    override suspend fun start() {
      // no-op
    }

    override suspend fun addStudyMinutes(delta: Int) {
      // no op
    }

    override suspend fun addPoints(delta: Int) {
      _stats.value = _stats.value.copy(points = _stats.value.points + delta)
    }

    override suspend fun updateCoins(delta: Int) {
      _stats.value = _stats.value.copy(coins = _stats.value.coins + delta)
    }

    override suspend fun setWeeklyGoal(minutes: Int) {
      // no op
    }
  }
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

  @Test
  fun profileScreen_showsSnackbarOnLevelUpReward() {
    val initialProfile = UserProfile(level = 1, points = 0, coins = 0, lastRewardedLevel = 0)
    val profileRepo = FakeProfileRepository(initialProfile)
    val statsRepo = FakeUserStatsRepository(UserStats(points = 0, coins = 0))

    val viewModel =
        ProfileViewModel(
            profileRepository = profileRepo,
            userStatsRepository = statsRepo,
        )

    composeRule.setContent { EduMonTheme { ProfileScreen(viewModel = viewModel) } }

    // Ensure the snackbar host is composed (collector attached).
    composeRule.onNodeWithTag(ProfileSnackbarTestTags.HOST, useUnmergedTree = true).assertExists()
    composeRule.waitForIdle()

    // Trigger: Fake repo updates stats flow => VM processes level-up => emits event => snackbar.
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
