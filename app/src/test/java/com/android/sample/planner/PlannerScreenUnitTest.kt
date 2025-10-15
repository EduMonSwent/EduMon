package com.android.sample.planner

import androidx.compose.ui.test.junit4.createComposeRule
import com.android.sample.ui.planner.PlannerScreen
import org.junit.Rule
import org.junit.Test

class PlannerScreenUnitTest {
  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun composePlannerScreen_doesNotCrash() {
    composeTestRule.setContent { PlannerScreen() }
  }
}
