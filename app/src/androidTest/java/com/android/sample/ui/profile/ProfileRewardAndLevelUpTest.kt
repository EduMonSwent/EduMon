package com.android.sample.ui.profile

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.data.UserProfile
import com.android.sample.data.UserStats
import com.android.sample.data.UserStatsRepository
import com.android.sample.profile.FakeProfileRepository
import com.android.sample.ui.theme.EduMonTheme
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileRewardAndLevelUpTest {

  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  private class FakeUserStatsRepository(initial: UserStats = UserStats(points = 0, coins = 0)) :
      UserStatsRepository {

    private val _stats = MutableStateFlow(initial)
    override val stats: StateFlow<UserStats> = _stats

    override suspend fun start() = Unit

    override suspend fun addStudyMinutes(extraMinutes: Int) = Unit

    override suspend fun addPoints(delta: Int) {
      _stats.value = _stats.value.copy(points = _stats.value.points + delta)
    }

    override suspend fun updateCoins(delta: Int) {
      _stats.value = _stats.value.copy(coins = _stats.value.coins + delta)
    }

    override suspend fun setWeeklyGoal(goalMinutes: Int) = Unit
  }

  private fun scrollProfileUntilTagExists(tag: String, maxSwipes: Int = 12) {
    // Ensure the profile root exists first
    composeRule.onNodeWithTag(ProfileScreenTestTags.PROFILE_SCREEN).assertExists()

    repeat(maxSwipes) {
      val existsNow =
          runCatching {
                composeRule
                    .onAllNodesWithTag(tag, useUnmergedTree = true)
                    .fetchSemanticsNodes()
                    .isNotEmpty()
              }
              .getOrNull() == true

      if (existsNow) return

      // Swipe the LazyColumn itself
      composeRule.onNodeWithTag(ProfileScreenTestTags.PROFILE_SCREEN).performTouchInput {
        swipeUp()
      }
      composeRule.waitForIdle()
    }

    // Final hard assertion (gives a clear failure if never found)
    composeRule.onNodeWithTag(tag, useUnmergedTree = true).assertExists()
  }

  @Test
  fun levelProgressBar_showsCorrectProgress_forGivenLevelAndPoints() {
    val user = UserProfile(level = 1, points = 0)

    composeRule.setContent { EduMonTheme { ProfileCard(user = user) } }

    composeRule.onNodeWithText("Progress to next level").assertExists()
    composeRule.onNode(hasText("0 / 80 pts", substring = true)).assertExists()
  }

  @Test
  fun profileScreen_manageNotifications_callsCallback() {
    val initialProfile = UserProfile(level = 1, points = 0, coins = 0, lastRewardedLevel = 0)
    val profileRepo = FakeProfileRepository(initialProfile)
    val statsRepo = FakeUserStatsRepository(UserStats(points = 0, coins = 0))

    val viewModel =
        ProfileViewModel(
            profileRepository = profileRepo,
            userStatsRepository = statsRepo,
        )

    val called = AtomicBoolean(false)

    composeRule.setContent {
      EduMonTheme {
        ProfileScreen(
            viewModel = viewModel,
            onOpenNotifications = { called.set(true) },
        )
      }
    }

    // Scroll until the notifications button is actually in the tree, then click it.
    scrollProfileUntilTagExists("open_notifications_screen")

    composeRule.onNodeWithTag("open_notifications_screen", useUnmergedTree = true).performClick()

    composeRule.waitUntil(timeoutMillis = 5_000) { called.get() }
  }
}
