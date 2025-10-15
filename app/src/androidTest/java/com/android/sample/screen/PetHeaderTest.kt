package com.android.sample.screen

import androidx.activity.ComponentActivity
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
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
}
