package com.android.sample.screen

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.android.sample.feature.schedule.data.planner.AttendanceStatus
import com.android.sample.feature.schedule.data.planner.Class
import com.android.sample.feature.schedule.data.planner.ClassAttendance
import com.android.sample.feature.schedule.data.planner.ClassType
import com.android.sample.feature.schedule.data.planner.CompletionStatus
import com.android.sample.ui.planner.ActivityItem
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Rule
import org.junit.Test

class ActivityItemTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun activityItem_displaysLectureDetails() {
    val testClass =
        Class(
            id = "1",
            courseName = "Software Engineering",
            startTime = LocalTime.of(9, 0),
            endTime = LocalTime.of(11, 0),
            type = ClassType.LECTURE,
            location = "A1",
            instructor = "Prof. Ada")

    composeTestRule.setContent {
      ActivityItem(activity = testClass, attendanceRecord = null, onClick = {})
    }

    composeTestRule.onNodeWithText("Software Engineering", substring = true).assertExists()
    composeTestRule.onNodeWithContentDescription("Lecture", substring = true).assertExists()
  }

  @Test
  fun activityItem_showsAttendanceInfo() {
    val testClass =
        Class(
            id = "2",
            courseName = "Databases",
            startTime = LocalTime.of(10, 0),
            endTime = LocalTime.of(12, 0),
            type = ClassType.EXERCISE,
            location = "Lab 4",
            instructor = "Dr. Stone")
    val attendance =
        ClassAttendance(
            classId = "2",
            date = LocalDate.now(),
            attendance = AttendanceStatus.YES,
            completion = CompletionStatus.PARTIALLY)

    composeTestRule.setContent {
      ActivityItem(activity = testClass, attendanceRecord = attendance, onClick = {})
    }

    composeTestRule.onNodeWithText("Attended", substring = true).assertExists()
    composeTestRule.onNodeWithText("Completed", substring = true).assertExists()
  }

  @Test
  fun activityItem_clickTriggersCallback() {
    var clicked = false
    val testClass =
        Class(
            id = "3",
            courseName = "Machine Learning",
            startTime = LocalTime.of(8, 0),
            endTime = LocalTime.of(10, 0),
            type = ClassType.LAB)

    composeTestRule.setContent {
      ActivityItem(activity = testClass, attendanceRecord = null, onClick = { clicked = true })
    }

    // Option 1: chercher le premier node cliquable et le déclencher
    composeTestRule.onAllNodes(hasClickAction()).onFirst().performClick()

    // Option 2 (si tu veux garder le texte) : attends qu’il apparaisse puis clique
    // composeTestRule.waitUntilExists(hasText("Machine", substring = true))
    // composeTestRule.onNodeWithText("Machine", substring = true).performClick()

    assert(clicked)
  }
}
