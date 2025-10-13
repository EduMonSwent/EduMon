package com.android.sample.screen

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.android.sample.ui.planner.PetHeader
import com.android.sample.ui.planner.PlannerScreen
import com.android.sample.ui.planner.PlannerScreenTestTags
import com.android.sample.ui.planner.PlannerViewModel
import com.android.sample.ui.theme.SampleAppTheme
import org.junit.Rule
import org.junit.Test

class PlannerScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  private fun setContent(viewModel: PlannerViewModel = PlannerViewModel()) {
    composeTestRule.setContent { SampleAppTheme { PlannerScreen(viewModel = viewModel) } }
  }

  @Test
  fun screenRendersAllMainSections() {
    setContent()

    // Scroll to each section before asserting
    val tags =
        listOf(
            PlannerScreenTestTags.PET_HEADER,
            PlannerScreenTestTags.TODAY_CLASSES_SECTION,
            PlannerScreenTestTags.WELLNESS_CAMPUS_SECTION)

    tags.forEach { tag ->
      composeTestRule
          .onNodeWithTag(PlannerScreenTestTags.PLANNER_SCREEN)
          .performScrollToNode(hasTestTag(tag))

      composeTestRule.onNodeWithTag(tag).assertExists()
    }
  }

  @Test
  fun fabTogglesAddTaskModalVisibility() {
    val viewModel = PlannerViewModel()
    setContent(viewModel)

    composeTestRule.onNodeWithTag(PlannerScreenTestTags.ADD_TASK_MODAL).assertDoesNotExist()

    composeTestRule.onNodeWithTag("addTaskFab").performClick()

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(PlannerScreenTestTags.ADD_TASK_MODAL).assertExists()

    viewModel.onDismissAddStudyTaskModal()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(PlannerScreenTestTags.ADD_TASK_MODAL).assertDoesNotExist()
  }

  @Test
  fun petHeaderDisplaysLevelAndTriggersCallback() {
    var clicked = false
    composeTestRule.setContent {
      SampleAppTheme { PetHeader(level = 10, onEdumonNameClick = { clicked = true }) }
    }
    composeTestRule.onNodeWithText("Lv 10").assertExists()

    composeTestRule.onNodeWithTag("petNameBox").performClick()

    assert(clicked)
  }

  @Test
  fun wellnessAndClassesSectionsRenderContent() {
    setContent()

    // Ensure we scroll before asserting (lazy composition)
    composeTestRule
        .onNodeWithTag(PlannerScreenTestTags.PLANNER_SCREEN)
        .performScrollToNode(hasTestTag(PlannerScreenTestTags.WELLNESS_CAMPUS_SECTION))

    composeTestRule.onNodeWithTag(PlannerScreenTestTags.TODAY_CLASSES_SECTION).assertExists()
    composeTestRule.onNodeWithTag(PlannerScreenTestTags.WELLNESS_CAMPUS_SECTION).assertExists()
  }
}
