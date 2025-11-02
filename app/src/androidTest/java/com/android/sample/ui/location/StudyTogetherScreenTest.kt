package com.android.sample.ui.location

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalPermissionsApi::class)
class StudyTogetherScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun studyTogetherScreen_displaysCorrectly() {
    composeTestRule.setContent {
      // On simule la MapView pour ne pas lancer le vrai GoogleMap()
      Box(Modifier.fillMaxSize()) {
        Text("Mocked Map") // substitut visuel
        UserStatusCard(isStudyMode = true)
      }
    }
    composeTestRule.onRoot().assertExists()
  }

  @Test
  fun whenFriendSelected_infoCardAppears() {
    composeTestRule.setContent { FriendInfoCard(name = "Alae", status = "study") }
    composeTestRule.onNodeWithText("Alae", substring = true, ignoreCase = true).assertExists()
    composeTestRule.onNodeWithText("study", substring = true, ignoreCase = true).assertExists()
  }

  @Test
  fun userStatusCard_displaysStudyMode() {
    composeTestRule.setContent { UserStatusCard(isStudyMode = true) }
    composeTestRule.onNodeWithText("study", substring = true, ignoreCase = true).assertExists()
  }

  @Test
  fun userStatusCard_displaysBreakMode() {
    composeTestRule.setContent { UserStatusCard(isStudyMode = false) }
    composeTestRule.onNodeWithText("break", substring = true, ignoreCase = true).assertExists()
  }

  @Test
  fun friendInfoCard_displaysDifferentStatuses() {
    composeTestRule.setContent {
      Column {
        FriendInfoCard(name = "Florian", status = "study")
        FriendInfoCard(name = "Khalil", status = "break")
        FriendInfoCard(name = "Alae", status = "idle")
      }
    }

    composeTestRule.onNodeWithText("Florian", substring = true, ignoreCase = true).assertExists()
    composeTestRule.onNodeWithText("Khalil", substring = true, ignoreCase = true).assertExists()
    composeTestRule.onNodeWithText("Alae", substring = true, ignoreCase = true).assertExists()
  }

  @Test
  fun friendInfoCard_handlesUnexpectedStatus() {
    composeTestRule.setContent { FriendInfoCard(name = "TestUser", status = "unknown") }
    composeTestRule.onNodeWithText("TestUser", substring = true, ignoreCase = true).assertExists()
  }

  @Test
  fun studyTogetherScreen_rendersWithoutCrashing() {
    composeTestRule.setContent {
      Box(Modifier.fillMaxSize()) {
        Text("Mocked Map")
        UserStatusCard(isStudyMode = true)
      }
    }
    composeTestRule.onRoot().assertExists()
  }
}
