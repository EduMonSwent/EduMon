package com.android.sample.screen

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.android.sample.ui.planner.AddStudyTaskModal
import com.android.sample.ui.planner.PlannerScreenTestTags
import org.junit.Rule
import org.junit.Test

class AddStudyTaskModalTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun addStudyTaskModal_allowsInputAndTriggersAddTask() {
    var added = false

    composeTestRule.setContent {
      AddStudyTaskModal(
          onDismiss = {},
          onAddTask = { subject, title, duration, deadline, priority ->
            added = subject.isNotEmpty() && title.isNotEmpty() && duration > 0
          })
    }

    composeTestRule
        .onNodeWithTag(PlannerScreenTestTags.SUBJECT_FIELD)
        .performTextInput("Mathematics")

    composeTestRule
        .onNodeWithTag(PlannerScreenTestTags.TASK_TITLE_FIELD)
        .performTextInput("Integration Practice")

    composeTestRule.onNodeWithTag(PlannerScreenTestTags.DURATION_FIELD).performTextInput("90")

    composeTestRule.waitForIdle()

    composeTestRule.onAllNodes(hasClickAction()).onLast().performClick()

    composeTestRule.waitForIdle()

    assert(added) { "onAddTask was not triggered — check that Add button is enabled and clicked." }
  }

  @Test
  fun addStudyTaskModal_cancelButtonDismisses() {
    var dismissed = false
    composeTestRule.setContent {
      AddStudyTaskModal(onDismiss = { dismissed = true }, onAddTask = { _, _, _, _, _ -> })
    }

    /*composeTestRule.onNodeWithText("Cancel", ignoreCase = true).assertExists().performClick()

    assert(dismissed)*/
    composeTestRule
        .onAllNodesWithText("Cancel", substring = true, ignoreCase = true)
        .onFirst()
        .performClick()

    composeTestRule.waitForIdle()
    assert(dismissed)
  }
}
