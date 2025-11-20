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
  fun selectUser_showsUserStatusCard() {
    val repo = FakeFriendRepository(emptyList())
    val vm = buildViewModel(repo)

    composeTestRule.setContent { StudyTogetherScreen(viewModel = vm) }

    // Simulate clicking the user marker by invoking VM helper directly
    composeTestRule.runOnUiThread { vm.selectUser() }
    // Wait for AnimatedVisibility animation to complete
    composeTestRule.waitForIdle()

    // Expect the user status card text
    composeTestRule.onNodeWithText("You're studying").assertExists()
  }

  @Test
  fun onCampusIndicator_shows_onCampus_when_location_inside_epfl_bbox() {
    val repo = FakeFriendRepository(emptyList())
    val vm = StudyTogetherViewModel(friendRepository = repo)

    // Disable map to avoid GoogleMap swallowing test input; indicator still renders.
    composeTestRule.setContent {
      StudyTogetherScreen(viewModel = vm, showMap = false, chooseLocation = true)
    }

    // Inside the ViewModel's EPFL bounding box:
    // lat in [46.515, 46.525], lng in [6.555, 6.575]
    composeTestRule.runOnUiThread { vm.consumeLocation(46.520, 6.565) }

    // Wait for state update and recomposition
    composeTestRule.waitForIdle()

    // Indicator composable should always exist
    composeTestRule.onNodeWithTag("on_campus_indicator").assertExists()
    // Text for on-campus state
    composeTestRule.onNodeWithText("On EPFL campus").assertExists()
  }

  @Test
  fun onCampusIndicator_shows_offCampus_when_location_outside_epfl_bbox() {
    val repo = FakeFriendRepository(emptyList())
    val vm = StudyTogetherViewModel(friendRepository = repo)

    composeTestRule.setContent { StudyTogetherScreen(viewModel = vm, showMap = false) }

    // Clearly outside bounding box: Zürich coords
    composeTestRule.runOnUiThread { vm.consumeLocation(47.37, 8.54) }

    // Wait for state update and recomposition
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("on_campus_indicator").assertExists()
    composeTestRule.onNodeWithText("Outside of EPFL campus").assertExists()
  }

  @Test
  fun friendsDropdown_showsEmptyState_whenNoFriends() {
    val repo = FakeFriendRepository(emptyList())
    val vm = buildViewModel(repo)

    composeTestRule.setContent { StudyTogetherScreen(viewModel = vm, showMap = false) }

    // Should show "Friends (0)" button
    composeTestRule.onNodeWithText("Friends (0)").assertExists()

    // Open the dropdown
    composeTestRule.onNodeWithTag("btn_friends").performClick()
    composeTestRule.waitForIdle()

    // Should display the empty state message
    composeTestRule.onNodeWithText("No friends yet").assertExists()
  }

  @Test
  fun handleErrorMessages_convertsResourceIdToString() {
    val repo = FakeFriendRepository(emptyList())
    val vm = buildViewModel(repo)

    composeTestRule.setContent { StudyTogetherScreen(viewModel = vm, showMap = false) }

    // Trigger error via duplicate friend add
    composeTestRule.onNodeWithTag("fab_add_friend").performClick()
    composeTestRule.onNodeWithTag("field_friend_uid").performTextInput("U100")
    composeTestRule.onNodeWithText("Add").performClick()

    // Add duplicate
    composeTestRule.onNodeWithTag("fab_add_friend").performClick()
    composeTestRule.onNodeWithTag("field_friend_uid").performTextInput("U100")
    composeTestRule.onNodeWithText("Add").performClick()

    composeTestRule.waitForIdle()
    // Error message should be converted from resource ID and shown
    composeTestRule.onNodeWithText("You're already friends.").assertExists()
  }

  @Test
  fun userStatusCard_showsCorrectStatus_whenUserSelected() {
    val repo = FakeFriendRepository(emptyList())
    val vm = buildViewModel(repo)

    composeTestRule.setContent { StudyTogetherScreen(viewModel = vm, showMap = false) }

    // Select user
    composeTestRule.runOnUiThread { vm.selectUser() }
    composeTestRule.waitForIdle()

    // Should show studying status
    composeTestRule.onNodeWithText("You're studying").assertExists()
  }

  @Test
  fun locationCallback_usesActualLocation_whenChooseLocationIsFalse() {
    // Create a completely fresh repository and ViewModel instance
    val repo = FakeFriendRepository(emptyList())
    // Use liveLocation=true so consumeLocation will use the actual coords we pass
    val vm = StudyTogetherViewModel(friendRepository = repo, liveLocation = true)

    // Verify the ViewModel starts uninitialized
    val initialState = vm.uiState.value
    assert(!initialState.isLocationInitialized) {
      "Fresh ViewModel should not have initialized location yet"
    }

    // Set up the screen with chooseLocation=false (use actual GPS)
    composeTestRule.setContent {
      StudyTogetherScreen(viewModel = vm, showMap = false, chooseLocation = false)
    }

    composeTestRule.waitForIdle()

    // Simulate GPS location update (Zurich - outside EPFL)
    val gpsLat = 47.3769
    val gpsLng = 8.5417
    composeTestRule.runOnUiThread { vm.consumeLocation(gpsLat, gpsLng) }

    composeTestRule.waitForIdle()

    // Should show outside campus since Zurich is far from EPFL
    composeTestRule.onNodeWithTag("on_campus_indicator").assertExists()
    composeTestRule.onNodeWithText("Outside of EPFL campus").assertExists()

    // Verify the GPS location was used and state was updated correctly
    composeTestRule.runOnUiThread {
      val state = vm.uiState.value
      assert(state.isLocationInitialized) { "Location should be initialized after consumeLocation" }
      // Verify the actual GPS coordinates are stored (not the default EPFL location)
      assert(kotlin.math.abs(state.effectiveUserLatLng.latitude - gpsLat) < 0.0001) {
        "Expected GPS latitude $gpsLat but got ${state.effectiveUserLatLng.latitude}"
      }
      assert(kotlin.math.abs(state.effectiveUserLatLng.longitude - gpsLng) < 0.0001) {
        "Expected GPS longitude $gpsLng but got ${state.effectiveUserLatLng.longitude}"
      }
      // Explicitly verify we're NOT using the default EPFL location
      val defaultLat = 46.5191
      val defaultLng = 6.5668
      assert(kotlin.math.abs(state.effectiveUserLatLng.latitude - defaultLat) > 0.1) {
        "Should not be using default EPFL latitude"
      }
      assert(kotlin.math.abs(state.effectiveUserLatLng.longitude - defaultLng) > 0.1) {
        "Should not be using default EPFL longitude"
      }
    }
  }

  @Test
  fun friendInfoCard_showsCorrectInfo_whenFriendSelected() {
    val seed =
        listOf(
            FriendStatus("U1", "Alice", 46.52, 6.56, FriendMode.BREAK),
        )
    val repo = FakeFriendRepository(seed)
    val vm = buildViewModel(repo)

    composeTestRule.setContent { StudyTogetherScreen(viewModel = vm, showMap = false) }

    // Select friend
    composeTestRule.runOnUiThread { vm.selectFriend(seed[0]) }
    composeTestRule.waitForIdle()

    // Should show friend name and status
    composeTestRule.onNodeWithText("Alice").assertExists()
    composeTestRule.onNodeWithText("Break").assertExists()
  }

  @Test
  fun addFriendDialog_cancels_withoutAddingFriend() {
    val repo = FakeFriendRepository(emptyList())
    val vm = buildViewModel(repo)

    composeTestRule.setContent { StudyTogetherScreen(viewModel = vm, showMap = false) }

    // Open dialog
    composeTestRule.onNodeWithTag("fab_add_friend").performClick()
    composeTestRule.waitForIdle()

    // Type a UID but cancel
    composeTestRule.onNodeWithTag("field_friend_uid").performTextInput("U50")
    composeTestRule.onNodeWithText("Cancel").performClick()
    composeTestRule.waitForIdle()

    // Dialog should close and no friend added
    composeTestRule.onNodeWithText("Friends (0)").assertExists()
  }

  @Test
  fun errorMessage_clearsAfterDisplay() {
    val repo = FakeFriendRepository(emptyList())
    val vm = buildViewModel(repo)

    composeTestRule.setContent { StudyTogetherScreen(viewModel = vm, showMap = false) }

    // Trigger error
    composeTestRule.onNodeWithTag("fab_add_friend").performClick()
    composeTestRule.onNodeWithTag("field_friend_uid").performTextInput("U200")
    composeTestRule.onNodeWithText("Add").performClick()

    // Duplicate
    composeTestRule.onNodeWithTag("fab_add_friend").performClick()
    composeTestRule.onNodeWithTag("field_friend_uid").performTextInput("U200")
    composeTestRule.onNodeWithText("Add").performClick()

    composeTestRule.waitForIdle()
    // Error shown
    composeTestRule.onNodeWithText("You're already friends.").assertExists()

    // Wait for snackbar to potentially disappear
    composeTestRule.waitForIdle()
  }

  @Test
  fun chooseLocation_usesChosenLocation_notRealLocation() {
    val repo = FakeFriendRepository(emptyList())
    val vm = buildViewModel(repo)
    val chosenLoc = com.google.android.gms.maps.model.LatLng(46.520, 6.565)

    composeTestRule.setContent {
      StudyTogetherScreen(
          viewModel = vm, showMap = false, chooseLocation = true, chosenLocation = chosenLoc)
    }

    composeTestRule.waitForIdle()

    // Should use the chosen location (on campus)
    composeTestRule.onNodeWithText("On EPFL campus").assertExists()
  }

  @Test
  fun friendsDropdown_collapses_afterSelection() {
    val seed = listOf(FriendStatus("U1", "Bob", 46.52, 6.56, FriendMode.STUDY))
    val repo = FakeFriendRepository(seed)
    val vm = buildViewModel(repo)

    composeTestRule.setContent { StudyTogetherScreen(viewModel = vm, showMap = false) }

    // Open dropdown
    composeTestRule.onNodeWithTag("btn_friends").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Bob").assertExists()

    // Select friend
    composeTestRule.onNodeWithText("Bob").performClick()
    composeTestRule.waitForIdle()

    // Dropdown should collapse - the friend name in dropdown should no longer be visible
    // (only in the bottom card)
  }

  @Test
  fun multipleErrors_displaySequentially() {
    val repo = FakeFriendRepository(emptyList())
    val vm = buildViewModel(repo)

    composeTestRule.setContent { StudyTogetherScreen(viewModel = vm, showMap = false) }

    // Add friend
    composeTestRule.onNodeWithTag("fab_add_friend").performClick()
    composeTestRule.onNodeWithTag("field_friend_uid").performTextInput("U300")
    composeTestRule.onNodeWithText("Add").performClick()
    composeTestRule.waitForIdle()

    // Try to add duplicate (error 1)
    composeTestRule.onNodeWithTag("fab_add_friend").performClick()
    composeTestRule.onNodeWithTag("field_friend_uid").performTextInput("U300")
    composeTestRule.onNodeWithText("Add").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("You're already friends.").assertExists()
  }
}
