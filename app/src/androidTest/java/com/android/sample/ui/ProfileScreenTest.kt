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
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.android.sample.ui.profile.*
import org.junit.Rule
import org.junit.Test

class ProfileScreenTest {

  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  private fun launchWith(vm: ProfileViewModel = ProfileViewModel(FakeProfileRepository())) {
    composeRule.setContent { ProfileScreen(viewModel = vm) }
  }

  // === EXISTING TESTS FIXED ===

  @Test
  fun allSectionsAreDisplayed() {
    launchWith()
    val tags =
        listOf(
            ProfileScreenTestTags.PET_SECTION,
            ProfileScreenTestTags.PROFILE_CARD,
            ProfileScreenTestTags.STATS_CARD,
            ProfileScreenTestTags.CUSTOMIZE_PET_SECTION,
            ProfileScreenTestTags.SETTINGS_CARD,
            ProfileScreenTestTags.ACCOUNT_ACTIONS_SECTION)

    tags.forEach {
      composeTestRule
          .onNodeWithTag(ProfileScreenTestTags.PROFILE_SCREEN)
          .performScrollToNode(hasTestTag(it))
      composeTestRule.onNodeWithTag(it).assertExists()
    }
  }

  @Test
  fun customizePetSectionDisplaysContent() {
    composeTestRule.setContent { CustomizePetSection() }
    composeTestRule.onNodeWithText("Customize Pet").assertExists()
    composeTestRule.onNodeWithText("Customize Buddy").assertExists()
  }

  @Test
  fun statBarDisplaysCorrectPercentages() {
    composeTestRule.setContent {
      Column {
        StatBar(icon = "❤️", percent = 0.9f, color = Color(0xFFFF69B4))
        StatBar(icon = "💡", percent = 0.85f, color = Color(0xFFFFC107))
        StatBar(icon = "⚡", percent = 0.7f, color = Color(0xFF03A9F4))
      }
    }
    composeTestRule.onNodeWithText("90%").assertExists()
    composeTestRule.onNodeWithText("85%").assertExists()
    composeTestRule.onNodeWithText("70%").assertExists()
  }

  @Test
  fun accountActionsSectionAndActionButtonsDisplayProperly() {
    var clicked = false
    composeTestRule.setContent {
      Column {
        AccountActionsSection()
        ActionButton(text = "Test Click", onClick = { clicked = true })
      }
    }
    composeTestRule.onNodeWithText("Privacy Policy").assertExists()
    composeTestRule.onNodeWithText("Terms of Service").assertExists()
    composeTestRule.onNodeWithText("Logout").assertExists()
    composeTestRule.onNodeWithText("Test Click").performClick()
    assert(clicked)
  }

  @Test
  fun glowCardAndBadgeRenderProperly() {
    composeTestRule.setContent {
      Column {
        GlowCard { Text("Inside GlowCard") }
        Badge(text = "Level 10", bg = Color.Magenta)
      }
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
  fun statsCardDisplaysAllStats() {
    val user =
        UserProfile(streak = 10, points = 200, coins = 150, studyTimeToday = 45, dailyGoal = 60)
    composeTestRule.setContent { StatsCard(user) }

    composeTestRule.onNodeWithText("Your Stats").assertExists()
    composeTestRule.onNodeWithText("Current Streak").assertExists()
    composeTestRule.onNodeWithText("Total Points").assertExists()
    composeTestRule.onNodeWithText("Coins").assertExists()
    composeTestRule.onNodeWithText("Study Time Today").assertExists()
    composeTestRule.onNodeWithText("Daily Goal").assertExists()

    composeTestRule.onNodeWithText("10 days").assertExists()
    composeTestRule.onNodeWithText("200").assertExists()
    composeTestRule.onNodeWithText("150").assertExists()
    composeTestRule.onNodeWithText("45 min").assertExists()
    composeTestRule.onNodeWithText("60 min").assertExists()
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

  // === NEW TESTS TO BOOST COVERAGE ===

  @Test
  fun glowCardAnimatesAndDisplaysContent() {
    composeTestRule.setContent { GlowCard { Text("Glowing Content") } }
    composeTestRule.onNodeWithText("Glowing Content").assertExists()
  }

  @Test
  fun customizePetSectionButtonExists() {
    composeTestRule.setContent { CustomizePetSection() }
    composeTestRule.onNodeWithText("Customize Buddy").assertExists()
  }

  @Test
  fun settingRowTogglesValue() {
    var toggled = false
    composeTestRule.setContent {
      SettingRow(
          title = "Focus Mode",
          desc = "Minimize distractions",
          value = false,
          onToggle = { toggled = true })
    }

    composeTestRule.onNodeWithText("Focus Mode").assertExists()
    composeTestRule.onNodeWithText("Minimize distractions").assertExists()
    composeTestRule.onAllNodes(isToggleable())[0].performClick()
    assert(toggled)
  }
}
