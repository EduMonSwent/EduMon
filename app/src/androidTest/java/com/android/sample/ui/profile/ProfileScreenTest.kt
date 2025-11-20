package com.android.sample.ui.profile

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.android.sample.data.AccentVariant
import com.android.sample.data.AccessoryItem
import com.android.sample.data.AccessorySlot
import com.android.sample.data.Rarity
import com.android.sample.data.UserProfile
import com.android.sample.data.UserStats
import com.android.sample.profile.FakeProfileRepository
import com.android.sample.repos_providors.FakeRepositories
import com.android.sample.ui.stats.model.StudyStats
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
  fun profileScreenCallbacksAreCalled() {
    var focusModeCalled = false
    var notificationsCalled = false
    val vm = ProfileViewModel(FakeProfileRepository())

    composeRule.setContent {
      ProfileScreen(
          viewModel = vm,
          onOpenFocusMode = { focusModeCalled = true },
          onOpenNotifications = { notificationsCalled = true })
    }

    // Scroll to settings and toggle focus mode
    composeRule
        .onNodeWithTag(ProfileScreenTestTags.PROFILE_SCREEN)
        .performScrollToNode(hasTestTag(ProfileScreenTestTags.SETTINGS_CARD))

    composeRule.onNodeWithTag(ProfileScreenTestTags.SWITCH_FOCUS_MODE).performClick()
    assert(focusModeCalled)

    // Click on manage notifications
    composeRule.onNodeWithTag("open_notifications_screen").performClick()
    assert(notificationsCalled)
  }

  @Test
  fun petSectionDisplaysAllElements() {
    val vm = ProfileViewModel(FakeProfileRepository())
    composeRule.setContent {
      PetSection(
          level = 5,
          accent = Color.Magenta,
          accessories = listOf("head:wizard_hat", "torso:cape", "back:wings"),
          variant = AccentVariant.Base,
          viewModel = vm)
    }

    composeRule.onNodeWithText("Level 5").assertExists()
    composeRule.onNodeWithText("90%").assertExists()
    composeRule.onNodeWithText("85%").assertExists()
    composeRule.onNodeWithText("70%").assertExists()
  }

  @Test
  fun petSectionWithEmptyAccessories() {
    val vm = ProfileViewModel(FakeProfileRepository())
    composeRule.setContent {
      PetSection(
          level = 1,
          accent = Color.Blue,
          accessories = emptyList(),
          variant = AccentVariant.Light,
          viewModel = vm)
    }

    composeRule.onNodeWithText("Level 1").assertExists()
  }

  @Test
  fun customizePetSectionDisplaysContent() {
    val vm = ProfileViewModel(FakeProfileRepository())
    composeRule.setContent { CustomizePetSection(viewModel = vm) }
    composeRule.onNodeWithText("Customize Buddy").assertExists()
    composeRule.onNodeWithText("Accent color").assertExists()
    composeRule.onNodeWithText("Inventory").assertExists()
  }

  @Test
  fun customizePetSection_accentColorSelection() {
    val vm = ProfileViewModel(FakeProfileRepository())
    composeRule.setContent { CustomizePetSection(viewModel = vm) }

    // Click on a color (should have clickable circles)
    composeRule.onAllNodes(hasClickAction()).onFirst().performClick()
  }

  @Test
  fun customizePetSection_variantSelection() {
    val vm = ProfileViewModel(FakeProfileRepository())
    composeRule.setContent { CustomizePetSection(viewModel = vm) }

    // Click on each variant chip
    composeRule.onNodeWithText("Base").performClick()
    composeRule.onNodeWithText("Light").performClick()
    composeRule.onNodeWithText("Dark").performClick()
    composeRule.onNodeWithText("Vibrant").performClick()
  }

  @Test
  fun customizePetSection_tabSwitching() {
    val vm = ProfileViewModel(FakeProfileRepository())
    composeRule.setContent { CustomizePetSection(viewModel = vm) }

    composeRule.onNodeWithText("Head").performClick()
    composeRule.onNodeWithText("Torso").performClick()
    composeRule.onNodeWithText("Legs").performClick()
    composeRule.onNodeWithText("Back").performClick()
  }

  @Test
  fun accessoriesGrid_displaysItems() {
    val items =
        listOf(
            AccessoryItem(
                id = "wizard_hat",
                slot = AccessorySlot.HEAD,
                label = "Wizard Hat",
                rarity = Rarity.RARE,
                iconRes = null),
            AccessoryItem(
                id = "cape",
                slot = AccessorySlot.TORSO,
                label = "Cape",
                rarity = Rarity.EPIC,
                iconRes = null),
            AccessoryItem(
                id = "legendary_crown",
                slot = AccessorySlot.HEAD,
                label = "Crown",
                rarity = Rarity.LEGENDARY,
                iconRes = null),
            AccessoryItem(
                id = "basic_shirt",
                slot = AccessorySlot.TORSO,
                label = "Shirt",
                rarity = Rarity.COMMON,
                iconRes = null))

    var selectedId: String? = null
    composeRule.setContent {
      AccessoriesGrid(items = items, selectedId = "wizard_hat", onSelect = { selectedId = it })
    }

    composeRule.onNodeWithText("Wizard Hat").assertExists()
    composeRule.onNodeWithText("Cape").assertExists()
    composeRule.onNodeWithText("Crown").assertExists()
    composeRule.onNodeWithText("Shirt").assertExists()

    // Click on an item
    composeRule.onNodeWithText("Cape").performClick()
    assert(selectedId == "cape")
  }

  @Test
  fun accessoriesGrid_noneSelectedByDefault() {
    val items =
        listOf(
            AccessoryItem(
                id = "none",
                slot = AccessorySlot.HEAD,
                label = "None",
                rarity = Rarity.COMMON,
                iconRes = null))

    composeRule.setContent { AccessoriesGrid(items = items, selectedId = null, onSelect = {}) }

    composeRule.onNodeWithText("None").assertExists()
  }

  @Test
  fun statBarDisplaysCorrectPercentages() {
    composeRule.setContent {
      Column {
        StatBar(icon = "❤️", percent = 0.9f, color = Color(0xFFFF69B4))
        StatBar(icon = "💡", percent = 0.85f, color = Color(0xFFFFC107))
        StatBar(icon = "⚡", percent = 0.7f, color = Color(0xFF03A9F4))
        StatBar(icon = "🔥", percent = 1.0f, color = Color.Red)
        StatBar(icon = "⭐", percent = 0.0f, color = Color.Yellow)
      }
    }
    composeRule.onNodeWithText("90%").assertExists()
    composeRule.onNodeWithText("85%").assertExists()
    composeRule.onNodeWithText("70%").assertExists()
    composeRule.onNodeWithText("100%").assertExists()
    composeRule.onNodeWithText("0%").assertExists()
  }

  @Test
  fun profileCardDisplaysAllInfo() {
    val user =
        UserProfile(
            name = "John Doe", email = "john.doe@epfl.ch", level = 10, points = 500, coins = 300)
    composeRule.setContent { ProfileCard(user) }

    composeRule.onNodeWithText("John Doe").assertExists()
    composeRule.onNodeWithText("john.doe@epfl.ch").assertExists()
    composeRule.onNodeWithText("Level 10").assertExists()
    composeRule.onNodeWithText("500 pts").assertExists()
    composeRule.onNodeWithText("JO").assertExists() // Initials
  }

  @Test
  fun badgeRendersWithCustomColors() {
    composeRule.setContent {
      Column {
        Badge(text = "Level 10", bg = Color.Magenta)
        Badge(text = "Premium", bg = Color.Yellow, textColor = Color.Black)
        Badge(text = "VIP", bg = Color.Red, textColor = Color.White)
      }
    }
    composeRule.onNodeWithText("Level 10").assertExists()
    composeRule.onNodeWithText("Premium").assertExists()
    composeRule.onNodeWithText("VIP").assertExists()
  }

  @Test
  fun statsCardDisplaysAllStats() {
    val profile =
        UserProfile(
            streak = 10,
            points = 200,
            coins = 150,
            studyStats = StudyStats(totalTimeMin = 45, dailyGoalMin = 60))
    val stats = UserStats(streak = 10, points = 200, coins = 150, todayStudyMinutes = 45)
    composeRule.setContent { StatsCard(profile, stats) }

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
  fun statRowDisplaysCorrectly() {
    composeRule.setContent {
      Column {
        StatRow(
            icon = androidx.compose.material.icons.Icons.Outlined.Star,
            label = "Points",
            value = "1000")
      }
    }
    composeRule.onNodeWithText("Points").assertExists()
    composeRule.onNodeWithText("1000").assertExists()
  }

  @Test
  fun settingsCardDisplaysAllSettings() {
    val user = UserProfile(locationEnabled = true, focusModeEnabled = false)
    var locationToggled = false
    var focusModeToggled = false
    var notificationsOpened = false

    composeRule.setContent {
      SettingsCard(
          user = user,
          onToggleLocation = { locationToggled = true },
          onToggleFocusMode = { focusModeToggled = true },
          onOpenNotifications = { notificationsOpened = true },
          onEnterFocusMode = {})
    }

    // Verify switches exist by their test tags
    composeRule.onNodeWithTag(ProfileScreenTestTags.SWITCH_LOCATION).assertExists()
    composeRule.onNodeWithTag(ProfileScreenTestTags.SWITCH_FOCUS_MODE).assertExists()
    composeRule.onNodeWithTag("open_notifications_screen").assertExists()

    // Toggle location
    composeRule.onNodeWithTag(ProfileScreenTestTags.SWITCH_LOCATION).performClick()
    assert(locationToggled)

    // Toggle focus mode
    composeRule.onNodeWithTag(ProfileScreenTestTags.SWITCH_FOCUS_MODE).performClick()
    assert(focusModeToggled)

    // Open notifications
    composeRule.onNodeWithTag("open_notifications_screen").performClick()
    assert(notificationsOpened)
  }

  @Test
  fun settingsCard_focusModeActivationCallsEnterFocusMode() {
    val user = UserProfile(focusModeEnabled = false)
    var focusModeEntered = false

    composeRule.setContent {
      SettingsCard(
          user = user,
          onToggleLocation = {},
          onToggleFocusMode = {},
          onOpenNotifications = {},
          onEnterFocusMode = { focusModeEntered = true })
    }

    // When focus mode is OFF and we toggle it, it should call onEnterFocusMode
    composeRule.onNodeWithTag(ProfileScreenTestTags.SWITCH_FOCUS_MODE).performClick()
    assert(focusModeEntered)
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

  @Test
  fun settingRowWithTrueValue() {
    composeRule.setContent {
      SettingRow(title = "Enabled Setting", desc = "This is on", value = true, onToggle = {})
    }
    composeRule.onNodeWithText("Enabled Setting").assertExists()
    composeRule.onNodeWithText("This is on").assertExists()
  }

  @Test
  fun accountActionsSectionDisplaysAllActions() {
    composeRule.setContent { AccountActionsSection() }

    composeRule.onNodeWithText("Privacy Policy").assertExists()
    composeRule.onNodeWithText("Terms of Service").assertExists()
    composeRule.onNodeWithText("Logout").assertExists()
  }

  @Test
  fun actionButtonClickWorks() {
    var clicked = false
    composeRule.setContent {
      Column {
        ActionButton(text = "Test Click") { clicked = true }
        ActionButton(text = "Red Button", textColor = Color.Red) {}
      }
    }
    composeRule.onNodeWithText("Test Click").performClick()
    assert(clicked)
    composeRule.onNodeWithText("Red Button").assertExists()
  }

  @Test
  fun glowCardAnimatesAndDisplaysContent() {
    composeRule.setContent { GlowCard { Text("Glowing Content") } }
    composeRule.onNodeWithText("Glowing Content").assertExists()

    // Wait for animation to run
    composeRule.mainClock.advanceTimeBy(3000)
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
    composeRule.onNodeWithText("Back").assertExists()
  }

  @Test
  fun inventory_switchingTabsUpdatesGrid() {
    val vm = ProfileViewModel(FakeProfileRepository())
    composeRule.setContent { CustomizePetSection(viewModel = vm) }

    // Switch to different tabs
    composeRule.onNodeWithText("Torso").performClick()
    composeRule.mainClock.advanceTimeByFrame()

    composeRule.onNodeWithText("Legs").performClick()
    composeRule.mainClock.advanceTimeByFrame()

    composeRule.onNodeWithText("Back").performClick()
    composeRule.mainClock.advanceTimeByFrame()

    composeRule.onNodeWithText("Head").performClick()
  }

  @Test
  fun petSectionWithDifferentAccessorySlots() {
    val vm = ProfileViewModel(FakeProfileRepository())

    // Test head accessory
    composeRule.setContent {
      PetSection(
          level = 3,
          accent = Color.Cyan,
          accessories = listOf("head:wizard_hat"),
          variant = AccentVariant.Dark,
          viewModel = vm)
    }
    composeRule.onNodeWithText("Level 3").assertExists()
  }

  @Test
  fun petSectionWithTorsoAccessory() {
    val vm = ProfileViewModel(FakeProfileRepository())

    composeRule.setContent {
      PetSection(
          level = 4,
          accent = Color.Green,
          accessories = listOf("torso:cape"),
          variant = AccentVariant.Vibrant,
          viewModel = vm)
    }
    composeRule.onNodeWithText("Level 4").assertExists()
  }

  @Test
  fun petSectionWithBackAccessory() {
    val vm = ProfileViewModel(FakeProfileRepository())

    composeRule.setContent {
      PetSection(
          level = 5,
          accent = Color.Yellow,
          accessories = listOf("back:wings"),
          variant = AccentVariant.Base,
          viewModel = vm)
    }
    composeRule.onNodeWithText("Level 5").assertExists()
  }

  @Test
  fun profileCardWithLongName() {
    val user =
        UserProfile(
            name = "Alexander Christopher",
            email = "alexander.christopher@epfl.ch",
            level = 99,
            points = 9999)
    composeRule.setContent { ProfileCard(user) }

    composeRule.onNodeWithText("Alexander Christopher").assertExists()
    composeRule.onNodeWithText("AL").assertExists() // First 2 chars uppercase
  }

  @Test
  fun profileCardWithShortName() {
    val user = UserProfile(name = "A", email = "a@epfl.ch")
    composeRule.setContent { ProfileCard(user) }

    // Verify the email exists (unique identifier)
    composeRule.onNodeWithText("a@epfl.ch").assertExists()
    // "A" appears at least once (could be in name and/or initials)
    composeRule.onAllNodesWithText("A").fetchSemanticsNodes().isNotEmpty()
  }

  @Test
  fun statsCardWithZeroValues() {
    val profile =
        UserProfile(
            streak = 0,
            points = 0,
            coins = 0,
            studyStats = StudyStats(totalTimeMin = 0, dailyGoalMin = 0))
    val stats = UserStats(streak = 0, points = 0, coins = 0, todayStudyMinutes = 0)
    composeRule.setContent { StatsCard(profile, stats) }

    composeRule.onNodeWithText("0 day").assertExists() // Singular form
    // "0" appears twice (points and coins)
    composeRule.onAllNodesWithText("0").assertCountEquals(2)
    // "0 min" appears twice (todayStudyMinutes and dailyGoalMin)
    composeRule.onAllNodesWithText("0 min").assertCountEquals(2)
  }

  @Test
  fun statsCardWithLargeValues() {
    val profile =
        UserProfile(
            streak = 365,
            points = 999999,
            coins = 888888,
            studyStats = StudyStats(totalTimeMin = 9999, dailyGoalMin = 500))
    val stats = UserStats(streak = 365, points = 999999, coins = 888888, todayStudyMinutes = 9999)
    composeRule.setContent { StatsCard(profile, stats) }

    composeRule.onNodeWithText("365 days").assertExists()
    composeRule.onNodeWithText("999999").assertExists()
    composeRule.onNodeWithText("888888").assertExists()
    composeRule.onNodeWithText("9999 min").assertExists()
    composeRule.onNodeWithText("500 min").assertExists()
  }

  @Test
  fun fullProfileScreenIntegration() {
    var focusModeOpened = false
    var notificationsOpened = false
    val vm = ProfileViewModel(FakeProfileRepository())

    composeRule.setContent {
      ProfileScreen(
          viewModel = vm,
          onOpenFocusMode = { focusModeOpened = true },
          onOpenNotifications = { notificationsOpened = true })
    }

    // Scroll through all sections
    composeRule.onNodeWithTag(ProfileScreenTestTags.PET_SECTION).assertExists()

    composeRule
        .onNodeWithTag(ProfileScreenTestTags.PROFILE_SCREEN)
        .performScrollToNode(hasTestTag(ProfileScreenTestTags.CUSTOMIZE_PET_SECTION))

    // Interact with customization
    composeRule.onNodeWithText("Base").performClick()

    composeRule
        .onNodeWithTag(ProfileScreenTestTags.PROFILE_SCREEN)
        .performScrollToNode(hasTestTag(ProfileScreenTestTags.SETTINGS_CARD))

    // Test toggles
    composeRule.onNodeWithTag(ProfileScreenTestTags.SWITCH_LOCATION).performClick()
    composeRule.onNodeWithTag(ProfileScreenTestTags.SWITCH_FOCUS_MODE).performClick()

    assert(focusModeOpened)

    composeRule.onNodeWithTag("open_notifications_screen").performClick()
    assert(notificationsOpened)
  }

  @Test
  fun levelProgressBar_displaysCorrectly() {
    composeRule.setContent { LevelProgressBar(level = 5, points = 450, pointsPerLevel = 100) }

    composeRule.onNodeWithText("Progress to next level").assertExists()
    // At level 5 with 450 points: levelBase = 4*100 = 400, so 50/100 progress
    composeRule.onNodeWithText("50 / 100 pts  •  50 pts to next level").assertExists()
  }

  @Test
  fun levelProgressBar_atMaxProgress() {
    composeRule.setContent { LevelProgressBar(level = 3, points = 300, pointsPerLevel = 100) }

    // At level 3 with 300 points: levelBase = 2*100 = 200, so 100/100 progress
    composeRule.onNodeWithText("100 / 100 pts  •  0 pts to next level").assertExists()
  }

  @Test
  fun levelProgressBar_atStartOfLevel() {
    composeRule.setContent { LevelProgressBar(level = 2, points = 100, pointsPerLevel = 100) }

    // At level 2 with 100 points: levelBase = 1*100 = 100, so 0/100 progress
    composeRule.onNodeWithText("0 / 100 pts  •  100 pts to next level").assertExists()
  }

  @Test
  fun petSectionWithMalformedAccessories() {
    val vm = ProfileViewModel(FakeProfileRepository())
    composeRule.setContent {
      PetSection(
          level = 5,
          accent = Color.Magenta,
          accessories = listOf("malformed", "head:hat", "invalid_format", "torso:cape"),
          variant = AccentVariant.Base,
          viewModel = vm)
    }

    // Should still display level without crashing
    composeRule.onNodeWithText("Level 5").assertExists()
  }

  @Test
  fun accessoriesGrid_multipleSelection() {
    val items =
        listOf(
            AccessoryItem(
                id = "hat1",
                slot = AccessorySlot.HEAD,
                label = "Hat 1",
                rarity = Rarity.COMMON,
                iconRes = null),
            AccessoryItem(
                id = "hat2",
                slot = AccessorySlot.HEAD,
                label = "Hat 2",
                rarity = Rarity.RARE,
                iconRes = null))

    var selectedId: String? = "hat1"
    composeRule.setContent {
      AccessoriesGrid(items = items, selectedId = selectedId, onSelect = { selectedId = it })
    }

    // Select second item
    composeRule.onNodeWithText("Hat 2").performClick()
    assert(selectedId == "hat2")
  }

  @Test
  fun statsCard_singularDayForm() {
    val profile = UserProfile(studyStats = StudyStats(totalTimeMin = 10, dailyGoalMin = 30))
    val stats = UserStats(streak = 1, points = 50, coins = 25, todayStudyMinutes = 10)

    composeRule.setContent { StatsCard(profile, stats) }

    // Should use "day" instead of "days" for streak = 1
    composeRule.onNodeWithText("1 day").assertExists()
  }
}
