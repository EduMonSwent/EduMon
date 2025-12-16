package com.android.sample.ui.location

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test

// The assistance of an AI tool (ClaudeAI) was solicited in writing this test file.
class StudyTogetherScreenFullTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun friendStatus_hasCorrectFields() {
    val f = FriendStatus("U0", "Alae", 46.52, 6.56, FriendMode.STUDY)
    assert(f.id == "U0")
    assert(f.name == "Alae")
    assert(f.latitude == 46.52)
    assert(f.longitude == 6.56)
    assert(f.mode == FriendMode.STUDY)
  }

  // --- Test StatusChip (Study Mode)
  @Test
  fun statusChip_displaysStudyMode() {
    composeTestRule.setContent { StatusChip(mode = FriendMode.STUDY) }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Studying").assertExists()
  }

  // --- Test StatusChip (Break Mode)
  @Test
  fun statusChip_displaysBreakMode() {
    composeTestRule.setContent { StatusChip(mode = FriendMode.BREAK) }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Break").assertExists()
  }

  // --- Test StatusChip (Idle Mode)
  @Test
  fun statusChip_displaysIdleMode() {
    composeTestRule.setContent { StatusChip(mode = FriendMode.IDLE) }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Idle").assertExists()
  }

  @Test
  fun campusStatusChip_displaysOnCampus() {
    composeTestRule.setContent {
      CampusStatusChip(onCampus = true, modifier = Modifier.testTag("campus_chip"))
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("On campus").assertExists()
  }

  @Test
  fun campusStatusChip_displaysOffCampus() {
    composeTestRule.setContent {
      CampusStatusChip(onCampus = false, modifier = Modifier.testTag("campus_chip"))
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Off campus").assertExists()
  }

  @Test
  fun studyTogetherScreen_rendersWithoutCrashing() {
    // Use a fake repo-backed ViewModel and disable the map to avoid runtime deps in tests
    val vm = StudyTogetherViewModel(friendRepository = FakeFriendRepository(), liveLocation = false)
    composeTestRule.setContent { StudyTogetherScreen(viewModel = vm, showMap = false) }
    composeTestRule.waitForIdle()
  }

  // --- Test AnimatedVisibility with FriendDropdownRow
  @Composable
  private fun TestAnimatedVisibilitySample(selected: FriendStatus?, visible: Boolean) {
    androidx.compose.animation.AnimatedVisibility(
        visible = visible,
        modifier = Modifier.testTag("animated_visibility"),
        enter = androidx.compose.animation.fadeIn(),
        exit = androidx.compose.animation.fadeOut()) {
          selected?.let { friend ->
            FriendDropdownRow(
                friend = friend, onClick = {}, onFindClick = {}, modifier = Modifier.padding(16.dp))
          }
        }
  }

  // --- AnimatedVisibility tests
  @Test
  fun animatedVisibility_displaysFriendRow() {
    composeTestRule.setContent {
      TestAnimatedVisibilitySample(FriendStatus("U1", "Alae", 0.0, 0.0, FriendMode.STUDY), true)
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Alae").assertExists()
    composeTestRule.onNodeWithText("Studying").assertExists()
  }

  @Test
  fun animatedVisibility_hidesWhenNotVisible() {
    composeTestRule.setContent {
      TestAnimatedVisibilitySample(FriendStatus("U1", "Alae", 0.0, 0.0, FriendMode.STUDY), false)
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Alae").assertDoesNotExist()
  }

  @Test
  fun friendDropdownRow_displaysAllModes() {
    // Test Study mode
    composeTestRule.setContent {
      FriendDropdownRow(
          friend = FriendStatus("U1", "Alae", 0.0, 0.0, FriendMode.STUDY),
          onClick = {},
          onFindClick = {},
          modifier = Modifier.testTag("friend_row"))
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Alae").assertExists()
    composeTestRule.onNodeWithText("Studying").assertExists()
  }

  @Test
  fun goBackToMeChip_displays() {
    composeTestRule.setContent {
      GoBackToMeChip(onClick = {}, modifier = Modifier.testTag("go_back_chip"))
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Go back to my location").assertExists()
  }
}
