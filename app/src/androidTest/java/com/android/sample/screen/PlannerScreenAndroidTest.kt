package com.android.sample.planner

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.android.sample.ui.planner.PlannerScreen
import com.android.sample.ui.viewmodel.PlannerViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class PlannerScreenAndroidTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun plannerScreenShouldDisplaySections() {
    composeRule.setContent { PlannerScreen(viewModel = PlannerViewModel()) }

    composeRule.waitForIdle()

    composeRule.onNodeWithTag("plannerScreen").assertExists()
    composeRule.onNodeWithTag("petHeader").assertExists()
    composeRule.onNodeWithTag("TODAY_CLASSES_SECTION").assertExists()

    // 🧩 FIX: Scroll until WELLNESS_CAMPUS_SECTION is visible
    composeRule
        .onNodeWithTag("plannerScreen")
        .performScrollToNode(hasTestTag("WELLNESS_CAMPUS_SECTION"))

    composeRule.onNodeWithTag("WELLNESS_CAMPUS_SECTION").assertExists()
  }

  @OptIn(ExperimentalTestApi::class)
  @Test
  fun clickingFabShouldOpenAddTaskModal() {
    composeRule.setContent { PlannerScreen(viewModel = PlannerViewModel()) }

    composeRule.waitForIdle()

    composeRule.onNodeWithTag("addTaskFab").performClick()

    composeRule.waitUntilExactlyOneExists(hasTestTag("addTaskModal"), timeoutMillis = 5000)

    composeRule.onNodeWithTag("addTaskModal").assertExists()
  }
}
