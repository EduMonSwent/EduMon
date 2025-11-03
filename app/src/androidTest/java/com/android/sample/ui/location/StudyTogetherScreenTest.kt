package com.android.sample.ui.location

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test

// The assistance of an AI tool (ChatGPT) was solicited in writing this test file.
class StudyTogetherScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  /**
   * Mock composable that mimics StudyTogetherScreen logic but replaces GoogleMap with simple
   * buttons.
   */
  @Composable
  private fun StudyTogetherScreenMock() {
    var selectedFriend by remember { mutableStateOf<FriendStatus?>(null) }
    var isUserSelected by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().testTag("study_together_root").padding(16.dp)) {
      // Displayed card depending on state
      if (isUserSelected) {
        UserStatusCard(isStudyMode = true, modifier = Modifier.testTag("user_status_card"))
      } else if (selectedFriend != null) {
        FriendInfoCard(
            name = selectedFriend!!.name,
            status = selectedFriend!!.status,
            modifier = Modifier.testTag("friend_info_card"))
      }

      Spacer(modifier = Modifier.height(8.dp))

      Button(
          onClick = {
            selectedFriend = null
            isUserSelected = true
          },
          modifier = Modifier.testTag("btn_user")) {
            Text("Select User")
          }

      Button(
          onClick = {
            selectedFriend = FriendStatus("Alae", 0.0, 0.0, "study")
            isUserSelected = false
          },
          modifier = Modifier.testTag("btn_study")) {
            Text("Select Study Friend")
          }

      Button(
          onClick = {
            selectedFriend = FriendStatus("Florian", 0.0, 0.0, "break")
            isUserSelected = false
          },
          modifier = Modifier.testTag("btn_break")) {
            Text("Select Break Friend")
          }

      Button(
          onClick = {
            selectedFriend = FriendStatus("Khalil", 0.0, 0.0, "idle")
            isUserSelected = false
          },
          modifier = Modifier.testTag("btn_idle")) {
            Text("Select Idle Friend")
          }
    }
  }

  @Test
  fun studyTogetherScreen_displaysAllCardsCorrectly() {
    composeTestRule.setContent { StudyTogetherScreenMock() }

    // --- Test affichage carte utilisateur
    composeTestRule.onNodeWithTag("btn_user").performClick()
    composeTestRule.onNodeWithTag("user_status_card").assertExists()

    // --- Test affichage carte ami "study"
    composeTestRule.onNodeWithTag("btn_study").performClick()
    composeTestRule.onNodeWithTag("friend_info_card").assertExists()
    composeTestRule.onNodeWithText("📚 Alae is currently in Study Mode").assertExists()

    // --- Test affichage carte ami "break"
    composeTestRule.onNodeWithTag("btn_break").performClick()
    composeTestRule.onNodeWithText("☕ Florian is currently in Break Mode").assertExists()

    // --- Test affichage carte ami "idle"
    composeTestRule.onNodeWithTag("btn_idle").performClick()
    composeTestRule.onNodeWithText("💤 Khalil is currently in Idle Mode").assertExists()

    // --- Vérifie que le composant racine est toujours visible
    composeTestRule.onNodeWithTag("study_together_root").assertExists()
  }
}
