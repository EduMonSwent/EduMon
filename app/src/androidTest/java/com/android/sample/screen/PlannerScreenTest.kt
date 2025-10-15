package com.android.sample.screen

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.android.sample.model.planner.*
import com.android.sample.ui.planner.PlannerScreen
import com.android.sample.ui.planner.PlannerScreenTestTags
import com.android.sample.ui.planner.PlannerViewModel
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Rule
import org.junit.Test

class PlannerScreenTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private val fakeClasses =
      listOf(
          Class(
              id = "1",
              courseName = "Math Analysis",
              instructor = "Dr. Euler",
              location = "Room 201",
              type = ClassType.LECTURE,
              startTime = LocalTime.of(8, 0),
              endTime = LocalTime.of(9, 30)),
          Class(
              id = "2",
              courseName = "Physics Lab",
              instructor = "Dr. Maxwell",
              location = "Lab 3",
              type = ClassType.LAB,
              startTime = LocalTime.of(10, 0),
              endTime = LocalTime.of(12, 0)))

  private val fakeAttendance =
      listOf(
          ClassAttendance(
              classId = "1",
              date = LocalDate.now(),
              attendance = AttendanceStatus.YES,
              completion = CompletionStatus.YES))

  private fun PlannerViewModel.injectFakeData() {
    updateTestData(fakeClasses, fakeAttendance)
  }

  /*@Test
  fun plannerScreen_displaysMainSections() {
    val vm = PlannerViewModel()
    vm.injectFakeData()

    composeTestRule.setContent { PlannerScreen(viewModel = vm) }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(PlannerScreenTestTags.PLANNER_SCREEN).assertIsDisplayed()
    composeTestRule.onNodeWithTag(PlannerScreenTestTags.TODAY_CLASSES_SECTION).assertIsDisplayed()
    composeTestRule.onNodeWithTag(PlannerScreenTestTags.WELLNESS_CAMPUS_SECTION).assertIsDisplayed()
  }*/

  /*@Test
  fun plannerScreen_displaysClassesAndAttendance() {
    val vm = PlannerViewModel()
    vm.injectFakeData()

    composeTestRule.setContent { PlannerScreen(viewModel = vm) }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("Math Analysis", substring = true).assertIsDisplayed()
    composeTestRule.onNodeWithText("Physics Lab", substring = true).assertIsDisplayed()
  }*/

  @Test
  fun plannerScreen_fabClick_opensAddTaskModal() {
    val vm = PlannerViewModel()
    vm.injectFakeData()

    composeTestRule.setContent { PlannerScreen(viewModel = vm) }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("addTaskFab").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(PlannerScreenTestTags.ADD_TASK_MODAL).assertIsDisplayed()
  }

  /*@Test
  fun plannerScreen_clickClass_opensAttendanceModal() {
    val vm = PlannerViewModel()
    vm.injectFakeData()

    composeTestRule.setContent { PlannerScreen(viewModel = vm) }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("Math Analysis").assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(PlannerScreenTestTags.CLASS_ATTENDANCE_MODAL).assertIsDisplayed()
  }*/

  @Test
  fun plannerScreen_displaysPetHeader_andAIRecommendationCard() {
    val vm = PlannerViewModel()
    vm.injectFakeData()

    composeTestRule.setContent { PlannerScreen(viewModel = vm) }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(PlannerScreenTestTags.PET_HEADER).assertIsDisplayed()
    composeTestRule.onNodeWithText("AI", substring = true).assertExists()
  }
}
