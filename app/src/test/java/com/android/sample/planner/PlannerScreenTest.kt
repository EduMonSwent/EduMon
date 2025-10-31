package com.android.sample.planner

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.android.sample.ui.planner.PlannerScreen
import com.android.sample.ui.planner.PlannerScreenTestTags
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PlannerScreenRobolectricTest {

  @get:Rule val compose = createComposeRule()

  @Test
  fun renders_core_sections() {
    compose.setContent { PlannerScreen() }
    compose.waitForIdle()

    // Pet header & main container exist (anchors are stable)
    compose.onNodeWithTag(PlannerScreenTestTags.PET_HEADER).assertExists()
    compose.onNodeWithTag(PlannerScreenTestTags.PLANNER_SCREEN).assertExists()

    // FAB uses a TAG in your code, not contentDescription
    compose.onNodeWithTag("addTaskFab").assertExists().assertIsEnabled()
  }

  @Test
  fun has_clickable_elements() {
    compose.setContent { PlannerScreen() }
    compose.waitForIdle()

    // Should have some clickable nodes (classes, buttons, FAB…)
    val clickable = compose.onAllNodes(hasClickAction()).fetchSemanticsNodes()
    check(clickable.isNotEmpty()) { "Expected at least one clickable element on PlannerScreen." }
  }
}
