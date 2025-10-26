package com.android.sample.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.android.sample.ui.profile.*
import org.junit.Rule
import org.junit.Test

class ProfileScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  // === EXISTING TESTS FIXED ===

  @Test
  fun allSectionsAreDisplayed() {
    composeTestRule.setContent { ProfileScreen() }

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
    composeTestRule.onNodeWithText("Inside GlowCard").assertExists()
    composeTestRule.onNodeWithText("Level 10").assertExists()
  }

  @Test
  fun profileCardDisplaysUserInformation() {
    val user = UserProfile(name = "Abdellah", email = "abdellah@epfl.ch", level = 5, points = 1200)
    composeTestRule.setContent { ProfileCard(user) }

    composeTestRule.onNodeWithText("Abdellah").assertExists()
    composeTestRule.onNodeWithText("abdellah@epfl.ch").assertExists()
    composeTestRule.onNodeWithText("Level 5").assertExists()
    composeTestRule.onNodeWithText("1200 pts").assertExists()
  }

  @Test
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
  fun petSectionDisplaysElements() {
    composeTestRule.setContent { PetSection(level = 3) }
    composeTestRule.onNodeWithText("Lv 3").assertExists()
    composeTestRule.onNodeWithText("Edumon").assertExists()
  }

  @Test
  fun settingRowDisplaysTexts() {
    composeTestRule.setContent {
      SettingRow(title = "🔔 Notifications", desc = "Study reminders", value = false, onToggle = {})
    }
    composeTestRule.onNodeWithText("🔔 Notifications").assertExists()
    composeTestRule.onNodeWithText("Study reminders").assertExists()
  }

  @Test
  fun actionButtonDisplaysTextAndTriggersClick() {
    var clicked = false

    composeTestRule.setContent {
      ActionButton(text = "Test Action", textColor = Color.Red, onClick = { clicked = true })
    }

    composeTestRule.onNodeWithText("Test Action").assertExists()
    composeTestRule.onNodeWithText("Test Action").performClick()
    assert(clicked)
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
