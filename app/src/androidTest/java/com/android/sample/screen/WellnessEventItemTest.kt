package com.android.sample.screen

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.android.sample.feature.schedule.data.planner.WellnessEventType
import com.android.sample.ui.planner.WellnessEventItem
import org.junit.Rule
import org.junit.Test

class WellnessEventItemTest {
  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun displaysTitleAndDescription() {
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
  }
}
