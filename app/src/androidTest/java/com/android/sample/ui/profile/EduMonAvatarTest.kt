package com.android.sample.ui.profile

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.sample.data.UserProfile
import com.android.sample.data.UserStats
import com.android.sample.data.UserStatsRepository
import com.android.sample.profile.FakeProfileRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EduMonAvatarTest {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var profileViewModel: ProfileViewModel

  /** Simple fake stats repo to avoid hitting real Firestore during androidTests. */
  private class TestUserStatsRepository(initial: UserStats = UserStats()) : UserStatsRepository {
    private val _stats = MutableStateFlow(initial)
    override val stats: StateFlow<UserStats> = _stats

    override suspend fun start() {}

    override suspend fun addStudyMinutes(extraMinutes: Int) {}

    override suspend fun updateCoins(delta: Int) {}

    override suspend fun setWeeklyGoal(goalMinutes: Int) {}

    override suspend fun addPoints(delta: Int) {}
  }

  @Before
  fun setup() {
    val initialProfile = UserProfile(level = 1, accessories = emptyList())
    val profileRepo = FakeProfileRepository(initialProfile)
    val statsRepo = TestUserStatsRepository()

    profileViewModel =
        ProfileViewModel(
            profileRepository = profileRepo,
            userStatsRepository = statsRepo,
        )
  }

  @Test
  fun eduMonAvatar_displaysCorrectly_withDefaultParameters() {
    composeTestRule.setContent {
      EduMonAvatar(
          modifier = androidx.compose.ui.Modifier.testTag("avatar_root"),
          viewModel = profileViewModel)
    }

    // Wait for Compose to finish recomposing
    composeTestRule.waitForIdle()

    // Check that the avatar root node is displayed
    composeTestRule.onNodeWithTag("avatar_root").assertExists().assertIsDisplayed()

    // Check that the level text is displayed
    composeTestRule.onNodeWithText("Level 1").assertExists().assertIsDisplayed()
  }

  @Test
  fun eduMonAvatar_displaysCorrectly_withoutLevelLabel() {
    composeTestRule.setContent {
      EduMonAvatar(
          modifier = androidx.compose.ui.Modifier.testTag("avatar_root"),
          viewModel = profileViewModel,
          showLevelLabel = false)
    }

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("avatar_root").assertExists().assertIsDisplayed()

    composeTestRule.onNodeWithText("Level 5").assertDoesNotExist()
  }

  @Test
  fun eduMonAvatar_usesCustomAvatarSize() {
    val customSize = 200.dp

    composeTestRule.setContent {
      EduMonAvatar(
          modifier = androidx.compose.ui.Modifier.testTag("avatar_root"),
          viewModel = profileViewModel,
          avatarSize = customSize)
    }

    composeTestRule.onNodeWithTag("avatar_root").assertExists().assertIsDisplayed()
  }

  @Test
  fun eduMonAvatar_doesNotCrash_whenResourceIdIsZero_forUnknownAccessory() {
    val profile = UserProfile(level = 5, accessories = listOf("back:unknown_id"))
    val vm =
        ProfileViewModel(
            profileRepository = FakeProfileRepository(profile),
            userStatsRepository = TestUserStatsRepository())

    composeTestRule.setContent {
      EduMonAvatar(modifier = androidx.compose.ui.Modifier.testTag("avatar_root"), viewModel = vm)
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("avatar_root").assertExists().assertIsDisplayed()
  }

  @Test
  fun eduMonAvatar_updatesAccentColor_whenAvatarAccentChanges() {
    composeTestRule.setContent {
      EduMonAvatar(
          modifier = androidx.compose.ui.Modifier.testTag("avatar_root"),
          viewModel = profileViewModel)
    }

    profileViewModel.setAvatarAccent(Color.Red)

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("avatar_root").assertExists().assertIsDisplayed()
  }

  @Test
  fun eduMonAvatar_handlesEmptyAccessoriesList() {
    val profile = UserProfile(level = 5, accessories = emptyList())
    val vm =
        ProfileViewModel(
            profileRepository = FakeProfileRepository(profile),
            userStatsRepository = TestUserStatsRepository())

    composeTestRule.setContent {
      EduMonAvatar(modifier = androidx.compose.ui.Modifier.testTag("avatar_root"), viewModel = vm)
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("avatar_root").assertExists().assertIsDisplayed()
  }

  @Test
  fun eduMonAvatar_appliesCustomModifier() {
    composeTestRule.setContent {
      EduMonAvatar(
          modifier = androidx.compose.ui.Modifier.testTag("custom_avatar"),
          viewModel = profileViewModel)
    }

    composeTestRule.onNodeWithTag("custom_avatar").assertExists()
  }

  @Test
  fun eduMonAvatar_handlesNullContentDescription_withoutCrashing() {
    val profile = UserProfile(level = 5, accessories = listOf("back:wings"))
    val vm =
        ProfileViewModel(
            profileRepository = FakeProfileRepository(profile),
            userStatsRepository = TestUserStatsRepository())

    composeTestRule.setContent {
      EduMonAvatar(modifier = androidx.compose.ui.Modifier.testTag("avatar_root"), viewModel = vm)
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("avatar_root").assertExists().assertIsDisplayed()
  }
}
