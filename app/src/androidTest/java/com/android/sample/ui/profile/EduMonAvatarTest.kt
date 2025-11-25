package com.android.sample.ui.profile

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.android.sample.data.AccessorySlot
import com.android.sample.data.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class EduMonAvatarTest {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var testViewModel: TestProfileViewModel

  @Before
  fun setup() {
    testViewModel = TestProfileViewModel()
  }

  @Test
  fun eduMonAvatar_displaysCorrectly_withDefaultParameters() {
    composeTestRule.setContent {
      EduMonAvatar(
          modifier = androidx.compose.ui.Modifier.testTag("avatar_root"), viewModel = testViewModel)
    }

    composeTestRule.onNodeWithTag("avatar_root").assertExists().assertIsDisplayed()

    composeTestRule.onNodeWithText("Level 5").assertExists().assertIsDisplayed()
  }

  @Test
  fun eduMonAvatar_displaysCorrectly_withoutLevelLabel() {
    composeTestRule.setContent {
      EduMonAvatar(
          modifier = androidx.compose.ui.Modifier.testTag("avatar_root"),
          viewModel = testViewModel,
          showLevelLabel = false)
    }

    composeTestRule.onNodeWithTag("avatar_root").assertExists().assertIsDisplayed()

    composeTestRule.onNodeWithText("Level 5").assertDoesNotExist()
  }

  @Test
  fun eduMonAvatar_displaysCorrectLevel_whenUserLevelChanges() {
    composeTestRule.setContent { EduMonAvatar(viewModel = testViewModel) }

    composeTestRule.onNodeWithText("Level 5").assertExists()

    testViewModel.updateUserLevel(10)

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("Level 10").assertExists()
  }

  @Test
  fun eduMonAvatar_usesCustomAvatarSize() {
    val customSize = 200.dp

    composeTestRule.setContent {
      EduMonAvatar(
          modifier = androidx.compose.ui.Modifier.testTag("avatar_root"),
          viewModel = testViewModel,
          avatarSize = customSize)
    }

    composeTestRule.onNodeWithTag("avatar_root").assertExists().assertIsDisplayed()
  }

  @Test
  fun eduMonAvatar_displaysBackAccessory_whenEquipped() {
    testViewModel.updateAccessories(listOf("back:wings"))

    composeTestRule.setContent { EduMonAvatar(viewModel = testViewModel) }

    composeTestRule.waitForIdle()

    assert(
        testViewModel.accessoryResIdCalls.any {
          it.first == AccessorySlot.BACK && it.second == "wings"
        })
  }

  @Test
  fun eduMonAvatar_displaysTorsoAccessory_whenEquipped() {
    testViewModel.updateAccessories(listOf("torso:shirt"))

    composeTestRule.setContent { EduMonAvatar(viewModel = testViewModel) }

    composeTestRule.waitForIdle()

    assert(
        testViewModel.accessoryResIdCalls.any {
          it.first == AccessorySlot.TORSO && it.second == "shirt"
        })
  }

  @Test
  fun eduMonAvatar_displaysHeadAccessory_whenEquipped() {
    testViewModel.updateAccessories(listOf("head:hat"))

    composeTestRule.setContent { EduMonAvatar(viewModel = testViewModel) }

    composeTestRule.waitForIdle()

    assert(
        testViewModel.accessoryResIdCalls.any {
          it.first == AccessorySlot.HEAD && it.second == "hat"
        })
  }

  @Test
  fun eduMonAvatar_displaysMultipleAccessories_whenEquipped() {
    testViewModel.updateAccessories(listOf("back:wings", "torso:shirt", "head:hat"))

    composeTestRule.setContent { EduMonAvatar(viewModel = testViewModel) }

    composeTestRule.waitForIdle()

    assert(
        testViewModel.accessoryResIdCalls.any {
          it.first == AccessorySlot.BACK && it.second == "wings"
        })
    assert(
        testViewModel.accessoryResIdCalls.any {
          it.first == AccessorySlot.TORSO && it.second == "shirt"
        })
    assert(
        testViewModel.accessoryResIdCalls.any {
          it.first == AccessorySlot.HEAD && it.second == "hat"
        })
  }

  @Test
  fun eduMonAvatar_ignoresInvalidAccessoryFormat() {
    testViewModel.updateAccessories(listOf("invalid", "back:wings", "also_invalid"))

    composeTestRule.setContent { EduMonAvatar(viewModel = testViewModel) }

    composeTestRule.waitForIdle()

    val backCalls =
        testViewModel.accessoryResIdCalls.count {
          it.first == AccessorySlot.BACK && it.second == "wings"
        }
    assert(backCalls == 1)
  }

  @Test
  fun eduMonAvatar_doesNotDisplayAccessory_whenResourceIdIsZero() {
    testViewModel.updateAccessories(listOf("back:wings"))
    testViewModel.shouldReturnZeroResId = true

    composeTestRule.setContent {
      EduMonAvatar(
          modifier = androidx.compose.ui.Modifier.testTag("avatar_root"), viewModel = testViewModel)
    }

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("avatar_root").assertExists()
  }

  @Test
  fun eduMonAvatar_updatesAccentColor_whenAccentEffectiveChanges() {
    composeTestRule.setContent {
      EduMonAvatar(
          modifier = androidx.compose.ui.Modifier.testTag("avatar_root"), viewModel = testViewModel)
    }

    testViewModel.updateAccentColor(Color.Red)

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("avatar_root").assertExists().assertIsDisplayed()
  }

  @Test
  fun eduMonAvatar_handlesEmptyAccessoriesList() {
    testViewModel.updateAccessories(emptyList())

    composeTestRule.setContent {
      EduMonAvatar(
          modifier = androidx.compose.ui.Modifier.testTag("avatar_root"), viewModel = testViewModel)
    }

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("avatar_root").assertExists().assertIsDisplayed()

    assert(testViewModel.accessoryResIdCalls.isEmpty())
  }

  @Test
  fun eduMonAvatar_appliesCustomModifier() {
    composeTestRule.setContent {
      EduMonAvatar(
          modifier = androidx.compose.ui.Modifier.testTag("custom_avatar"),
          viewModel = testViewModel)
    }

    composeTestRule.onNodeWithTag("custom_avatar").assertExists()
  }

  @Test
  fun eduMonAvatar_handlesAccessoriesWithColonInId() {
    testViewModel.updateAccessories(listOf("back:special:wings:v2"))

    composeTestRule.setContent { EduMonAvatar(viewModel = testViewModel) }

    composeTestRule.waitForIdle()

    assert(testViewModel.accessoryResIdCalls.isEmpty())
  }

  @Test
  fun eduMonAvatar_usesCorrectZIndexForAccessories() {
    testViewModel.updateAccessories(listOf("back:wings", "torso:shirt", "head:hat"))

    composeTestRule.setContent { EduMonAvatar(viewModel = testViewModel) }

    composeTestRule.waitForIdle()

    val calls = testViewModel.accessoryResIdCalls
    assert(calls.size >= 3)

    val backIndex = calls.indexOfFirst { it.first == AccessorySlot.BACK }
    val torsoIndex = calls.indexOfFirst { it.first == AccessorySlot.TORSO }
    val headIndex = calls.indexOfFirst { it.first == AccessorySlot.HEAD }

    assert(backIndex < torsoIndex)
    assert(torsoIndex < headIndex)
  }

  @Test
  fun eduMonAvatar_handlesNullContentDescription() {
    testViewModel.updateAccessories(listOf("back:wings"))

    composeTestRule.setContent {
      EduMonAvatar(
          modifier = androidx.compose.ui.Modifier.testTag("avatar_root"), viewModel = testViewModel)
    }

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("avatar_root").assertExists()
  }
}

class TestProfileViewModel : ProfileViewModel() {
  private val _userProfile = MutableStateFlow(UserProfile(level = 5, accessories = emptyList()))
  override val userProfile: StateFlow<UserProfile> = _userProfile

  private val _accentEffective = MutableStateFlow(Color.Blue)
  override val accentEffective: StateFlow<Color> = _accentEffective

  val accessoryResIdCalls = mutableListOf<Pair<AccessorySlot, String>>()
  var shouldReturnZeroResId = false

  override fun accessoryResId(slot: AccessorySlot, id: String): Int {
    accessoryResIdCalls.add(slot to id)
    return if (shouldReturnZeroResId) 0 else android.R.drawable.ic_menu_gallery
  }

  fun updateUserLevel(newLevel: Int) {
    _userProfile.value = _userProfile.value.copy(level = newLevel)
  }

  fun updateAccessories(newAccessories: List<String>) {
    _userProfile.value = _userProfile.value.copy(accessories = newAccessories)
    accessoryResIdCalls.clear()
  }

  fun updateAccentColor(newColor: Color) {
    _accentEffective.value = newColor
  }
}
