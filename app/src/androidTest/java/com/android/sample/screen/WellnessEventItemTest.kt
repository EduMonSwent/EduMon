package com.android.sample.screen

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.android.sample.model.planner.WellnessEventType
import com.android.sample.ui.planner.WellnessEventItem
import org.junit.Rule
import org.junit.Test

class WellnessEventItemTest {
  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun displaysTitleAndDescription_andRespondsToClick() {
    var clicked = false
    composeTestRule.setContent {
      WellnessEventItem(
          title = "Yoga Session",
          time = "10:00 AM",
          description = "Morning relaxation",
          eventType = WellnessEventType.YOGA,
          onClick = { clicked = true })
    }

    composeTestRule.onNodeWithText("Yoga Session").assertExists()
    composeTestRule.onNodeWithText("Morning relaxation").assertExists()
    composeTestRule.onNodeWithContentDescription("Yoga Session").performClick()
    assert(clicked)
  }
}
