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

// The assistance of an AI tool (ChatGPT) was solicited in writing this test file.
class StudyTogetherScreenFullTest {

  @get:Rule val composeTestRule = createComposeRule()

  // --- 1️⃣ Test de la data class
  @Test
  fun friendStatus_hasCorrectFields() {
    val f = FriendStatus("U0", "Alae", 46.52, 6.56, FriendMode.STUDY)
    assert(f.id == "U0")
    assert(f.name == "Alae")
    assert(f.latitude == 46.52)
    assert(f.longitude == 6.56)
    assert(f.mode == FriendMode.STUDY)
  }

  // --- 2️⃣-A Test UserStatusCard (Study Mode)
  @Test
  fun userStatusCard_displaysStudyMode() {
    composeTestRule.setContent {
      UserStatusCard(isStudyMode = true, modifier = Modifier.testTag("study_card"))
    }
    composeTestRule.onNodeWithText("You’re studying").assertExists()
  }

  // --- 2️⃣-B Test UserStatusCard (Break Mode)
  @Test
  fun userStatusCard_displaysBreakMode() {
    composeTestRule.setContent {
      UserStatusCard(isStudyMode = false, modifier = Modifier.testTag("break_card"))
    }
    composeTestRule.onNodeWithText("You’re on a break").assertExists()
  }

  // --- 3️⃣ Test FriendInfoCard pour tous les statuts
  @Test
  fun friendInfoCard_displaysStudyStatus() {
    composeTestRule.setContent {
      FriendInfoCard(
          name = "Alae", mode = FriendMode.STUDY, modifier = Modifier.testTag("friend_study"))
    }
    // Shows name and chip text
    composeTestRule.onNodeWithText("Alae").assertExists()
    composeTestRule.onNodeWithText("Studying").assertExists()
  }

  @Test
  fun friendInfoCard_displaysBreakStatus() {
    composeTestRule.setContent {
      FriendInfoCard(
          name = "Florian", mode = FriendMode.BREAK, modifier = Modifier.testTag("friend_break"))
    }
    composeTestRule.onNodeWithText("Florian").assertExists()
    composeTestRule.onNodeWithText("Break").assertExists()
  }

  @Test
  fun friendInfoCard_displaysIdleStatus() {
    composeTestRule.setContent {
      FriendInfoCard(
          name = "Khalil", mode = FriendMode.IDLE, modifier = Modifier.testTag("friend_idle"))
    }
    composeTestRule.onNodeWithText("Khalil").assertExists()
    composeTestRule.onNodeWithText("Idle").assertExists()
  }

  // --- 4️⃣ Test global du composable principal (vérifie qu'il ne crash pas)
  @Test
  fun studyTogetherScreen_rendersWithoutCrashing() {
    composeTestRule.setContent { StudyTogetherScreen() }
    composeTestRule.waitForIdle()
  }

  // --- 5️⃣ Composable interne pour tester AnimatedVisibility
  @Composable
  private fun TestAnimatedVisibilitySample(selected: FriendStatus?, isUserSelected: Boolean) {
    androidx.compose.animation.AnimatedVisibility(
        visible = selected != null || isUserSelected,
        modifier = Modifier.testTag("animated_visibility"),
        enter = androidx.compose.animation.fadeIn(),
        exit = androidx.compose.animation.fadeOut()) {
          when {
            isUserSelected -> {
              UserStatusCard(isStudyMode = true, modifier = Modifier.padding(16.dp))
            }
            selected != null -> {
              FriendInfoCard(
                  name = selected.name, mode = selected.mode, modifier = Modifier.padding(16.dp))
            }
          }
        }
  }

  // --- 6️⃣ AnimatedVisibility tests séparés
  @Test
  fun animatedVisibility_displaysFriendCard() {
    composeTestRule.setContent {
      TestAnimatedVisibilitySample(FriendStatus("U1", "Alae", 0.0, 0.0, FriendMode.STUDY), false)
    }
    composeTestRule.onNodeWithText("Alae").assertExists()
    composeTestRule.onNodeWithText("Studying").assertExists()
  }

  @Test
  fun animatedVisibility_displaysUserCard() {
    composeTestRule.setContent { TestAnimatedVisibilitySample(null, true) }
    composeTestRule.onNodeWithText("You’re studying").assertExists()
  }
}
