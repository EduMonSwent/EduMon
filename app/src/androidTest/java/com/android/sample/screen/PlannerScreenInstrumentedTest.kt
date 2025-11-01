package com.android.sample.screen

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.ui.planner.PlannerScreen
import com.android.sample.ui.planner.PlannerScreenTestTags
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlannerScreenInstrumentedTest {

  @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun renders_core_sections_and_fab_clicks() {
    compose.setContent { PlannerScreen() }
    compose.waitForIdle()

    // Stable anchors by tag
    compose.onNodeWithTag(PlannerScreenTestTags.PLANNER_SCREEN).assertExists()
    compose.onNodeWithTag(PlannerScreenTestTags.PET_HEADER).assertExists()

    // FAB is tagged (more reliable than contentDescription on some devices)
    compose.onNodeWithTag("addTaskFab").assertExists().assertIsEnabled().performClick()

    compose.waitForIdle()
  }

  @Test
  fun has_some_clickable_elements() {
    compose.setContent { PlannerScreen() }
    compose.waitForIdle()

    val clickable = compose.onAllNodes(hasClickAction()).fetchSemanticsNodes()
    check(clickable.isNotEmpty()) { "Expected at least one clickable element on PlannerScreen." }
  }
}
