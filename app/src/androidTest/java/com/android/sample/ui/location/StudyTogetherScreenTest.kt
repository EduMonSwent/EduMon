package com.android.sample.ui.location

import android.Manifest
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.rule.GrantPermissionRule
import org.junit.Rule
import org.junit.Test

class StudyTogetherScreenTest {

  @get:Rule val composeTestRule = createComposeRule()
  @get:Rule
  val permissionRule: GrantPermissionRule =
      GrantPermissionRule.grant(
          Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

  private fun buildViewModel(repo: FriendRepository): StudyTogetherViewModel =
      StudyTogetherViewModel(
          friendRepository = repo, initialMode = FriendMode.STUDY, liveLocation = false)

  @Test
  fun addFriendDialog_opens_disablesAdd_whenEmpty_and_addsFriend_whenFilled() {
    val repo = FakeFriendRepository(emptyList())
    val vm = buildViewModel(repo)

    composeTestRule.setContent { StudyTogetherScreen(viewModel = vm, false) }

    // Open dialog
    composeTestRule.onNodeWithTag("fab_add_friend").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Add friend by UID").assertExists()

    // Add button disabled when empty
    composeTestRule.onNodeWithText("Add").assertIsNotEnabled()

    // Type a UID and confirm enabled
    composeTestRule.onNodeWithTag("field_friend_uid").performTextInput("U10")
    composeTestRule.onNodeWithText("Add").assertIsEnabled().performClick()

    composeTestRule.waitForIdle()
    // Dropdown should now reflect one friend
    composeTestRule.onNodeWithText("Friends (1)").assertExists()
  }

  @Test
  fun addFriendDialog_duplicateUid_showsSnackbarError() {
    val repo = FakeFriendRepository(emptyList())
    val vm = buildViewModel(repo)
    composeTestRule.setContent { StudyTogetherScreen(viewModel = vm, false) }

    // Add once
    composeTestRule.onNodeWithTag("fab_add_friend").performClick()
    composeTestRule.onNodeWithTag("field_friend_uid").performTextInput("U20")
    composeTestRule.onNodeWithText("Add").performClick()

    // Add duplicate -> expect snackbar with error message
    composeTestRule.onNodeWithTag("fab_add_friend").performClick()
    composeTestRule.onNodeWithTag("field_friend_uid").performTextInput("U20")
    composeTestRule.onNodeWithText("Add").performClick()

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("You're already friends.").assertExists()
  }

  @Test
  fun friendsDropdown_selectFriend_showsFriendInfoCard() {
    val seed =
        listOf(
            FriendStatus("U1", "Alae", 46.52, 6.56, FriendMode.STUDY),
            FriendStatus("U2", "Florian", 46.51, 6.55, FriendMode.BREAK),
            FriendStatus("U3", "Khalil", 46.50, 6.54, FriendMode.IDLE),
        )
    val repo = FakeFriendRepository(seed)
    val vm = buildViewModel(repo)

    composeTestRule.setContent { StudyTogetherScreen(viewModel = vm) }

    // Open dropdown and pick Khalil
    composeTestRule.onNodeWithText("Friends (${seed.size})").performClick()
    composeTestRule.onNodeWithText("Khalil").performClick()

    // Bottom card shows name and the Idle status chip
    composeTestRule.onNodeWithText("Khalil").assertExists()
    composeTestRule.onNodeWithText("Idle").assertExists()
  }

  @Test
  fun selectUser_showsUserStatusCard() {
    val repo = FakeFriendRepository(emptyList())
    val vm = buildViewModel(repo)

    composeTestRule.setContent { StudyTogetherScreen(viewModel = vm) }

    // Simulate clicking the user marker by invoking VM helper directly
    composeTestRule.runOnUiThread { vm.selectUser() }

    // Expect the user status card text
    composeTestRule.onNodeWithText("You’re studying").assertExists()
  }
}
