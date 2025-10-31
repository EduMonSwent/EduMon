package com.android.sample.planner

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.android.sample.ui.planner.PlannerScreen
import com.android.sample.ui.planner.PlannerScreenTestTags.PET_HEADER
import com.android.sample.ui.planner.PlannerScreenTestTags.PLANNER_SCREEN
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PlannerScreenRobolectricTest {
  @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun renders_core_sections() {
    compose.setContent { PlannerScreen() }
    compose.onNodeWithTag(PLANNER_SCREEN).assertExists()
    compose.onNodeWithTag(PET_HEADER).assertExists()
    compose.onNodeWithTag("addTaskFab").assertExists()
  }
}
