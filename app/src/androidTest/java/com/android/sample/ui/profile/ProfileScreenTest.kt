package com.android.sample.ui.profile

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.android.sample.data.AccessoryItem
import com.android.sample.data.AccessorySlot
import com.android.sample.data.FakeUserStatsRepository
import com.android.sample.data.Rarity
import com.android.sample.data.UserProfile
import com.android.sample.data.UserStats
import com.android.sample.data.UserStatsRepository
import com.android.sample.profile.FakeProfileRepository
import com.android.sample.ui.stats.model.StudyStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Rule
import org.junit.Test

class ProfileScreenTest {

  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  // Mock the ProfileViewModel with the necessary dependencies
  private fun launchWith(
      vm: ProfileViewModel =
          ProfileViewModel(
              profileRepository = FakeProfileRepository(),
              userStatsRepository = FakeUserStatsRepository())
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
    val vm =
        ProfileViewModel(
            profileRepository = FakeProfileRepository(),
            userStatsRepository = FakeUserStatsRepository())

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
    val vm =
        ProfileViewModel(
            profileRepository = FakeProfileRepository(),
            userStatsRepository = FakeUserStatsRepository())
    composeRule.setContent { PetSection(viewModel = vm) }

    composeRule.onNodeWithText("Level 5").assertExists()
    composeRule.onNodeWithText("90%").assertExists()
    composeRule.onNodeWithText("85%").assertExists()
    composeRule.onNodeWithText("70%").assertExists()
  }

  @Test
  fun customizePetSectionDisplaysContent() {
    val vm =
        ProfileViewModel(
            profileRepository = FakeProfileRepository(),
            userStatsRepository = FakeUserStatsRepository())
    composeRule.setContent { CustomizePetSection(viewModel = vm) }
    composeRule.onNodeWithText("Customize Buddy").assertExists()
    composeRule.onNodeWithText("Accent color").assertExists()
    composeRule.onNodeWithText("Inventory").assertExists()
  }

  @Test
  fun customizePetSection_accentColorSelection() {
    val vm =
        ProfileViewModel(
            profileRepository = FakeProfileRepository(),
            userStatsRepository = FakeUserStatsRepository())
    composeRule.setContent { CustomizePetSection(viewModel = vm) }

    // Click on a color (should have clickable circles)
    composeRule.onAllNodes(hasClickAction()).onFirst().performClick()
  }

  @Test
  fun customizePetSection_variantSelection() {
    val vm =
        ProfileViewModel(
            profileRepository = FakeProfileRepository(),
            userStatsRepository = FakeUserStatsRepository())
    composeRule.setContent { CustomizePetSection(viewModel = vm) }

    // Click on each variant chip
    composeRule.onNodeWithText("Base").performClick()
    composeRule.onNodeWithText("Light").performClick()
    composeRule.onNodeWithText("Dark").performClick()
    composeRule.onNodeWithText("Vibrant").performClick()
  }

  @Test
  fun customizePetSection_tabSwitching() {
    val vm =
        ProfileViewModel(
            profileRepository = FakeProfileRepository(),
            userStatsRepository = FakeUserStatsRepository())
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
  fun petSectionWithMalformedAccessories() {
    val profile =
        UserProfile(
            level = 5,
            accessories = listOf("malformed", "head:hat", "invalid_format", "torso:cape"),
        )

    val profileRepo = FakeProfileRepository(profile)

    class TestUserStatsRepository(initial: UserStats = UserStats()) : UserStatsRepository {
      private val _stats = MutableStateFlow(initial)
      override val stats: StateFlow<UserStats> = _stats

      override suspend fun start() {}

      override suspend fun addStudyMinutes(extraMinutes: Int) {}

      override suspend fun updateCoins(delta: Int) {}

      override suspend fun setWeeklyGoal(goalMinutes: Int) {}

      override suspend fun addPoints(delta: Int) {}
    }

    val statsRepo = TestUserStatsRepository()

    val vm =
        ProfileViewModel(
            profileRepository = profileRepo,
            userStatsRepository = statsRepo,
        )

    composeRule.setContent { PetSection(viewModel = vm) }

    composeRule.onNodeWithText("Level 5").assertExists()
  }

  @Test
  fun petSectionWithBackAccessory() {
    val vm =
        ProfileViewModel(
            profileRepository = FakeProfileRepository(),
            userStatsRepository = FakeUserStatsRepository())

    composeRule.setContent { PetSection(viewModel = vm) }
    composeRule.onNodeWithText("Level 5").assertExists()
  }
}
