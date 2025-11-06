package com.android.sample.ui.profile

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.android.sample.repos_providors.FakeRepositories
import org.junit.Rule
import org.junit.Test

class ProfileScreenTest {

  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  private fun launchWith(
      vm: ProfileViewModel = ProfileViewModel(FakeRepositories.profileRepository)
  ) {
    composeRule.setContent { ProfileScreen(viewModel = vm) }
  }

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

    tags.forEach { tag ->
      composeRule
          .onNodeWithTag(ProfileScreenTestTags.PROFILE_SCREEN)
          .performScrollToNode(hasTestTag(tag))
      composeRule.onNodeWithTag(tag).assertExists()
    }
  }

  @Test
  fun customizePetSectionDisplaysContent() {
    // ⚠️ Fournir le viewModel requis par la composable
    val vm = ProfileViewModel(FakeProfileRepository())
    composeRule.setContent { CustomizePetSection(viewModel = vm) }
    composeRule.onNodeWithText("Customize Buddy").assertExists()
  }

  @Test
  fun statBarDisplaysCorrectPercentages() {
    composeRule.setContent {
      Column {
        StatBar(icon = "❤️", percent = 0.9f, color = Color(0xFFFF69B4))
        StatBar(icon = "💡", percent = 0.85f, color = Color(0xFFFFC107))
        StatBar(icon = "⚡", percent = 0.7f, color = Color(0xFF03A9F4))
      }
    }
    composeRule.onNodeWithText("90%").assertExists()
    composeRule.onNodeWithText("85%").assertExists()
    composeRule.onNodeWithText("70%").assertExists()
  }

  @Test
  fun accountActionsSectionAndActionButtonsDisplayProperly() {
    var clicked = false
    composeRule.setContent {
      Column {
        AccountActionsSection()
        ActionButton(text = "Test Click") { clicked = true }
      }
    }
    composeRule.onNodeWithText("Privacy Policy").assertExists()
    composeRule.onNodeWithText("Terms of Service").assertExists()
    composeRule.onNodeWithText("Logout").assertExists()
    composeRule.onNodeWithText("Test Click").performClick()
    assert(clicked)
  }

  @Test
  fun glowCardAndBadgeRenderProperly() {
    composeRule.setContent {
      Column {
        GlowCard { Text("Inside GlowCard") }
        Badge(text = "Level 10", bg = Color.Magenta)
      }
    }
    composeRule.onNodeWithText("Inside GlowCard").assertExists()
    composeRule.onNodeWithText("Level 10").assertExists()
  }

  @Test
  fun accentVariantChips_present_withoutMuted() {
    launchWith()
    composeRule
        .onNodeWithTag(ProfileScreenTestTags.PROFILE_SCREEN)
        .performScrollToNode(hasTestTag(ProfileScreenTestTags.CUSTOMIZE_PET_SECTION))

    composeRule.onNodeWithText("Base").assertExists()
    composeRule.onNodeWithText("Light").assertExists()
    composeRule.onNodeWithText("Dark").assertExists()
    composeRule.onNodeWithText("Vibrant").assertExists()
  }

  @Test
  fun inventory_tabs_present() {
    launchWith()
    composeRule
        .onNodeWithTag(ProfileScreenTestTags.PROFILE_SCREEN)
        .performScrollToNode(hasTestTag(ProfileScreenTestTags.CUSTOMIZE_PET_SECTION))

    composeRule.onNodeWithText("Head").assertExists()
    composeRule.onNodeWithText("Torso").assertExists()
    composeRule.onNodeWithText("Legs").assertExists()
  }

  @Test
  fun statsCardDisplaysAllStats() {

    val user =
        UserProfile(streak = 10, points = 200, coins = 150, studyTimeToday = 45, dailyGoal = 60)
    composeRule.setContent { StatsCard(user) }

    composeRule.onNodeWithText("Current Streak").assertExists()
    composeRule.onNodeWithText("Total Points").assertExists()
    composeRule.onNodeWithText("Coins").assertExists()
    composeRule.onNodeWithText("Study Time Today").assertExists()
    composeRule.onNodeWithText("Daily Goal").assertExists()

    composeRule.onNodeWithText("10 days").assertExists()
    composeRule.onNodeWithText("200").assertExists()
    composeRule.onNodeWithText("150").assertExists()
    composeRule.onNodeWithText("45 min").assertExists()
    composeRule.onNodeWithText("60 min").assertExists()
  }

  @Test
  fun stats_samples_fallback_are_displayed_when_values_zero() {
    val repo =
        FakeProfileRepository(
            UserProfile(
                name = "User",
                email = "u@u.com",
                level = 1,
                points = 0,
                streak = 0,
                coins = 0,
                studyTimeToday = 0,
                dailyGoal = 0,
                avatarAccent = 0xFF9333EAL,
                accessories = emptyList()))

    launchWith(ProfileViewModel(repo))

    composeRule
        .onNodeWithTag(ProfileScreenTestTags.PROFILE_SCREEN)
        .performScrollToNode(hasTestTag(ProfileScreenTestTags.STATS_CARD))

    composeRule.onNodeWithText("7 days").assertExists()
    composeRule.onNodeWithText("1250").assertExists()
    composeRule.onNodeWithText("0").assertExists()
    composeRule.onNodeWithText("45 min").assertExists()
    composeRule.onNodeWithText("180 min").assertExists()
  }

  // === NEW TESTS TO BOOST COVERAGE ===

  @Test
  fun glowCardAnimatesAndDisplaysContent() {
    composeRule.setContent { GlowCard { Text("Glowing Content") } }
    composeRule.onNodeWithText("Glowing Content").assertExists()
  }

  @Test
  fun customizePetSectionButtonExists() {
    val vm = ProfileViewModel(FakeProfileRepository())
    composeRule.setContent { CustomizePetSection(viewModel = vm) }
    composeRule.onNodeWithText("Customize Buddy").assertExists()
  }

  @Test
  fun settingRowTogglesValue() {
    var toggled = false
    composeRule.setContent {
      SettingRow(
          title = "Focus Mode",
          desc = "Minimize distractions",
          value = false,
          onToggle = { toggled = true })
    }
    composeRule.onNodeWithText("Focus Mode").assertExists()
    composeRule.onNodeWithText("Minimize distractions").assertExists()
    composeRule.onAllNodes(isToggleable())[0].performClick()
    assert(toggled)
  }
}
