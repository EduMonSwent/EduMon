package com.android.sample.screen

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.android.sample.model.planner.*
import com.android.sample.ui.planner.ClassAttendanceModal
import org.junit.Rule
import org.junit.Test

class ClassAttendanceModalTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun classAttendanceModal_allowsSelectionAndSave() {
    var saved = false
    val testClass =
        Class(
            id = "1",
            courseName = "AI Fundamentals",
            startTime = java.time.LocalTime.of(10, 0),
            endTime = java.time.LocalTime.of(11, 0),
            type = ClassType.LECTURE,
            location = "A1.01",
            instructor = "Dr. Turing")

    composeTestRule.setContent {
      ClassAttendanceModal(
          classItem = testClass,
          initialAttendance = null,
          initialCompletion = null,
          onDismiss = {},
          onSave = { attendance, completion ->
            saved = attendance == AttendanceStatus.YES && completion == CompletionStatus.PARTIALLY
          })
    }

    // Sélectionne le premier "YES" (pour attendance)
    composeTestRule.onAllNodesWithText("YES", ignoreCase = true)[0].performClick()

    // Sélectionne "PARTIALLY"
    composeTestRule.onNodeWithText("PARTIALLY", ignoreCase = true).performClick()

    // Clique sur "Save"
    composeTestRule.onNodeWithText("Save", ignoreCase = true).performClick()

    assert(saved)
  }

  @Test
  fun classAttendanceModal_dismissButtonWorks() {
    var dismissed = false
    val testClass =
        Class(
            id = "2",
            courseName = "Linear Algebra",
            startTime = java.time.LocalTime.of(9, 0),
            endTime = java.time.LocalTime.of(10, 0),
            type = ClassType.EXERCISE)

    composeTestRule.setContent {
      ClassAttendanceModal(
          classItem = testClass,
          initialAttendance = null,
          initialCompletion = null,
          onDismiss = { dismissed = true },
          onSave = { _, _ -> })
    }

    composeTestRule.onNodeWithText("Cancel", ignoreCase = true).assertExists().performClick()

    assert(dismissed)
  }
}
