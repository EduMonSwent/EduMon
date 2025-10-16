package com.android.sample.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.android.sample.ui.profile.*
import org.junit.Rule
import org.junit.Test

class ProfileScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

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
  fun settingsCardCallbacksWork() {
    var notif = false
    var loc = false
    var focus = false

    composeTestRule.setContent {
      SettingsCard(
          user = UserProfile(),
          onToggleNotifications = { notif = true },
          onToggleLocation = { loc = true },
          onToggleFocusMode = { focus = true })
    }

    composeTestRule.onNodeWithTag(ProfileScreenTestTags.SWITCH_NOTIFICATIONS).performClick()
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.SWITCH_LOCATION).performClick()
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.SWITCH_FOCUS_MODE).performClick()

    assert(notif)
    assert(loc)
    assert(focus)
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
    val user = UserProfile(streak = 10, points = 200, studyTimeToday = 45, dailyGoal = 60)
    composeTestRule.setContent { StatsCard(user) }

    composeTestRule.onNodeWithText("Your Stats").assertExists()
    composeTestRule.onNodeWithText("🔥 Current Streak").assertExists()
    composeTestRule.onNodeWithText("⭐ Total Points").assertExists()
    composeTestRule.onNodeWithText("📚 Study Time Today").assertExists()
    composeTestRule.onNodeWithText("🎯 Daily Goal").assertExists()
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
}
