package com.android.sample.screen

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.android.sample.ui.planner.PetHeader
import org.junit.Rule
import org.junit.Test

class PetHeaderTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun displaysLevelText_whenLevelProvided() {
    composeTestRule.setContent { PetHeader(level = 5, modifier = Modifier) }

    // "Lv 5" should exist and be visible
    composeTestRule.onNodeWithText("Lv 5").assertExists().assertIsDisplayed()
  }

  @Test
  fun recompose_updatesLevelCorrectly() {
    var level by mutableStateOf(5)

    composeTestRule.setContent { PetHeader(level = level) }

    // initial
    composeTestRule.onNodeWithText("Lv 5").assertExists()

    // update state on UI thread to trigger recomposition
    composeTestRule.runOnUiThread { level = 10 }
    composeTestRule.waitForIdle()

    // new level visible, old level gone
    composeTestRule.onNodeWithText("Lv 10").assertIsDisplayed()
    assert(composeTestRule.onAllNodesWithText("Lv 5").fetchSemanticsNodes().isEmpty())
  }

  @Test
  fun rendersMultipleInstancesWithoutConflict() {
    composeTestRule.setContent {
      Column {
        PetHeader(level = 1)
        PetHeader(level = 2)
        PetHeader(level = 3)
      }
    }

    // Ensure all three appear
    composeTestRule.onNodeWithText("Lv 1").assertExists()
    composeTestRule.onNodeWithText("Lv 2").assertExists()
    composeTestRule.onNodeWithText("Lv 3").assertExists()
  }

  @Test
  fun layout_doesNotCrash_whenUsingCustomModifiers() {
    // Using a more complex modifier chain should not crash or affect visibility
    composeTestRule.setContent {
      PetHeader(level = 9, modifier = Modifier.padding(8.dp).fillMaxWidth())
    }
    composeTestRule.onNodeWithText("Lv 9").assertExists().assertIsDisplayed()
  }

  @Test
  fun click_doesNotCrashEvenIfNoHandler() {
    // There's no click listener inside, but performing a click shouldn't throw
    composeTestRule.setContent { PetHeader(level = 4) }

    composeTestRule.onNodeWithText("Lv 4").performClick()
    composeTestRule.waitForIdle()

    // UI remains stable
    composeTestRule.onNodeWithText("Lv 4").assertIsDisplayed()
  }
}
