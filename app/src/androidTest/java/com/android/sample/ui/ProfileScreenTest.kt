package com.android.sample.ui.profile

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import com.android.sample.ui.login.UserProfile
import com.android.sample.ui.profile.ProfileScreenTestTags.ACCOUNT_ACTIONS_SECTION
import com.android.sample.ui.profile.ProfileScreenTestTags.CUSTOMIZE_PET_SECTION
import com.android.sample.ui.profile.ProfileScreenTestTags.PET_SECTION
import com.android.sample.ui.profile.ProfileScreenTestTags.PROFILE_CARD
import com.android.sample.ui.profile.ProfileScreenTestTags.PROFILE_SCREEN
import com.android.sample.ui.profile.ProfileScreenTestTags.SETTINGS_CARD
import com.android.sample.ui.profile.ProfileScreenTestTags.STATS_CARD
import org.junit.Rule
import org.junit.Test

class ProfileScreenTest {

  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  private fun launchWith(vm: ProfileViewModel = ProfileViewModel(FakeProfileRepository())) {
    composeRule.setContent { ProfileScreen(viewModel = vm) }
  }

  @Test
  fun allSectionsAreDisplayed() {
    launchWith()
    val tags =
        listOf(
            PET_SECTION,
            PROFILE_CARD,
            STATS_CARD,
            CUSTOMIZE_PET_SECTION,
            SETTINGS_CARD,
            ACCOUNT_ACTIONS_SECTION)

    tags.forEach { tag ->
      composeRule.onNodeWithTag(PROFILE_SCREEN).performScrollToNode(hasTestTag(tag))
      composeRule.onNodeWithTag(tag).assertExists()
    }
  }

  @Test
  fun accentVariantChips_present_withoutMuted() {
    launchWith()
    composeRule.onNodeWithTag(PROFILE_SCREEN).performScrollToNode(hasTestTag(CUSTOMIZE_PET_SECTION))

    composeRule.onNodeWithText("Base").assertExists()
    composeRule.onNodeWithText("Light").assertExists()
    composeRule.onNodeWithText("Dark").assertExists()
    composeRule.onNodeWithText("Vibrant").assertExists()
  }

  @Test
  fun inventory_tabs_present_but_noHands() {
    launchWith()
    composeRule.onNodeWithTag(PROFILE_SCREEN).performScrollToNode(hasTestTag(CUSTOMIZE_PET_SECTION))

    composeRule.onNodeWithText("Head").assertExists()
    composeRule.onNodeWithText("Torso").assertExists()
    composeRule.onNodeWithText("Legs").assertExists()
    composeRule.onAllNodesWithText("Hands").assertCountEquals(0)
  }

  @Test
  fun stats_samples_fallback_are_displayed_when_values_zero() {
    val repo =
        FakeProfileRepository(
            UserProfile(
                name = "User",
                email = "u@u.com",
                level = 1,
                points = 0, // -> 1280
                streak = 0, // -> 7
                studyTimeToday = 0, // -> 54
                dailyGoal = 0, // -> 60
                avatarAccent = 0xFF9333EAL,
                accessories = emptyList()))
    launchWith(ProfileViewModel(repo))

    composeRule.onNodeWithTag(STATS_CARD).assertExists()
    composeRule.onNodeWithText("7 days").assertExists()
    composeRule.onNodeWithText("1280").assertExists()
    composeRule.onNodeWithText("54 min").assertExists()
    composeRule.onNodeWithText("60 min").assertExists()
  }
}
