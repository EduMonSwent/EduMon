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

    // Synchronisation initiale (important pour CI)
    composeTestRule.mainClock.autoAdvance = false
    composeTestRule.mainClock.advanceTimeBy(3000)
    composeTestRule.waitForIdle()

    // Remplissage des champs
    composeTestRule
        .onNodeWithTag(PlannerScreenTestTags.SUBJECT_FIELD)
        .performTextInput("Mathematics")

    composeTestRule
        .onNodeWithTag(PlannerScreenTestTags.TASK_TITLE_FIELD)
        .performTextInput("Integration Practice")

    composeTestRule.onNodeWithTag(PlannerScreenTestTags.DURATION_FIELD).performTextInput("90")

    // Attente d'activation du bouton
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule.onAllNodes(hasClickAction()).fetchSemanticsNodes().size >= 2
    }

    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    // Clique sur le bouton Add Task (dernier cliquable)
    composeTestRule.onAllNodes(hasClickAction()).onLast().performClick()

    composeTestRule.mainClock.advanceTimeBy(2000)
    composeTestRule.waitForIdle()

    assert(added) { "X onAddTask was not triggered — Add button likely not enabled in CI timing." }
  }

  @Test
  fun addStudyTaskModal_cancelButtonDismisses() {
    var dismissed = false

    composeTestRule.setContent {
      AddStudyTaskModal(onDismiss = { dismissed = true }, onAddTask = { _, _, _, _, _ -> })
    }

    composeTestRule.mainClock.autoAdvance = false
    composeTestRule.mainClock.advanceTimeBy(3000)
    composeTestRule.waitForIdle()

    // Attente du bouton Cancel
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule.onAllNodes(hasClickAction()).fetchSemanticsNodes().isNotEmpty()
    }

    // Clique sur le premier bouton (Cancel)
    composeTestRule.onAllNodes(hasClickAction()).onFirst().performClick()

    composeTestRule.mainClock.advanceTimeBy(1000)
    composeTestRule.waitForIdle()

    assert(dismissed) {
      "X onDismiss not triggered — Cancel button may not have been clickable yet."
    }
  }
}
