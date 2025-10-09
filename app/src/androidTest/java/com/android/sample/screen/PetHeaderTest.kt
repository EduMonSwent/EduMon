package com.android.sample.screen

import androidx.activity.ComponentActivity
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.android.sample.ui.planner.PetHeader
import org.junit.Rule
import org.junit.Test

class PetHeaderTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun displaysProgressBarAndLevel() {
    composeTestRule.setContent { PetHeader(level = 5, modifier = Modifier, onEdumonNameClick = {}) }

    // "Lv 5" is the actual text in AssistChip
    composeTestRule.onNodeWithText("Lv 5").assertExists()
  }

  @Test
  fun petHeader_displaysStatsAndRespondsToClick() {
    var clicked = false
    composeTestRule.setContent { PetHeader(level = 7, onEdumonNameClick = { clicked = true }) }

    // Vérifie le niveau
    composeTestRule.onNodeWithText("Lv 7").assertExists()

    // Vérifie que les barres de stats sont visibles
    composeTestRule.onNodeWithText("90%", substring = true).assertExists()
    composeTestRule.onNodeWithText("85%", substring = true).assertExists()

    // Clic sur le nom
    composeTestRule.onNodeWithTag("petNameBox").performClick()
    assert(clicked)
  }
}
