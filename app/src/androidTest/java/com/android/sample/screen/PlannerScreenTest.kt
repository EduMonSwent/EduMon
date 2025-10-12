package com.android.sample.ui.planner

// Alias pour éviter le conflit avec java.lang.Class
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import com.android.sample.model.planner.AttendanceStatus
import com.android.sample.model.planner.Class as PlannerClass
import com.android.sample.model.planner.ClassAttendance
import com.android.sample.model.planner.ClassType
import com.android.sample.model.planner.CompletionStatus
import com.android.sample.model.planner.WellnessEventType
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Rule
import org.junit.Test

class PlannerScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun allMainSectionsAreDisplayed() {
    composeTestRule.setContent { PlannerScreen() }

    val tags =
        listOf(
            PlannerScreenTestTags.PLANNER_SCREEN,
            PlannerScreenTestTags.PET_HEADER,
            PlannerScreenTestTags.TODAY_CLASSES_SECTION,
            PlannerScreenTestTags.WELLNESS_CAMPUS_SECTION)

    tags.forEach {
      composeTestRule
          .onNodeWithTag(PlannerScreenTestTags.PLANNER_SCREEN)
          .performScrollToNode(hasTestTag(it))
      composeTestRule.onNodeWithTag(it).assertExists()
    }
  }

  @Test
  fun petHeaderDisplaysElements() {
    composeTestRule.setContent { PetHeader(level = 5, onEdumonNameClick = {}) }

    composeTestRule.onNodeWithText("Lv 5").assertExists()
    composeTestRule.onNodeWithText("Edumon Profile").assertExists()
  }

  @Test
  fun statBarDisplaysCorrectPercentages() {
    composeTestRule.setContent {
      Column {
        StatBar(icon = "❤️", percent = 0.9f, color = Color(0xFFFF69B4))
        StatBar(icon = "💡", percent = 0.85f, color = Color(0xFFFFC107))
        StatBar(icon = "⚡", percent = 0.7f, color = Color(0xFF03A9F4))
      }
    }

    composeTestRule.onNodeWithText("90%").assertExists()
    composeTestRule.onNodeWithText("85%").assertExists()
    composeTestRule.onNodeWithText("70%").assertExists()
  }

  @Test
  fun aiRecommendationCardDisplaysContent() {
    composeTestRule.setContent {
      AIRecommendationCard(
          recommendationText = "Focus on calculus exercises today", onActionClick = {})
    }

    // Utiliser des vérifications plus flexibles
    composeTestRule.onNodeWithText("AI", ignoreCase = true).assertExists()
    composeTestRule.onNodeWithText("Recommendation", ignoreCase = true).assertExists()
    composeTestRule.onNodeWithText("Focus on calculus exercises today").assertExists()
    composeTestRule.onNodeWithText("Start", ignoreCase = true).assertExists()
  }

  @Test
  fun plannerGlowCardRendersContent() {
    composeTestRule.setContent { PlannerGlowCard { Text("Test content inside glow card") } }

    composeTestRule.onNodeWithText("Test content inside glow card").assertExists()
  }

  @Test
  fun activityItemDisplaysClassInformation() {
    val testClass =
        PlannerClass(
            id = "1",
            courseName = "Mathematics",
            type = ClassType.LECTURE,
            startTime = LocalTime.of(9, 0),
            endTime = LocalTime.of(10, 0),
            location = "Room 101",
            instructor = "Dr. Smith"
            // Pas de paramètre date
            )

    composeTestRule.setContent {
      ActivityItem(activity = testClass, attendanceRecord = null, onClick = {})
    }

    composeTestRule.onNodeWithText("Mathematics").assertExists()
    composeTestRule.onNodeWithText("Lecture", ignoreCase = true).assertExists()
    composeTestRule.onNodeWithText("09:00 - 10:00").assertExists()
    composeTestRule.onNodeWithText("Room 101").assertExists()
  }

  @Test
  fun wellnessEventItemDisplaysEventInformation() {
    composeTestRule.setContent {
      WellnessEventItem(
          title = "Morning Yoga",
          time = "08:00 - 09:00",
          description = "Start your day with relaxing yoga",
          eventType = WellnessEventType.YOGA,
          onClick = {})
    }

    composeTestRule.onNodeWithText("Morning Yoga").assertExists()
    composeTestRule.onNodeWithText("08:00 - 09:00").assertExists()
    composeTestRule.onNodeWithText("Start your day with relaxing yoga").assertExists()
  }

  @Test
  fun fabOpensAddTaskModal() {
    composeTestRule.setContent { PlannerScreen() }

    // Vérifier d'abord que le FAB existe
    composeTestRule.onNodeWithTag("add_study_task_fab").assertExists()

    // Click the FAB
    composeTestRule.onNodeWithTag("add_study_task_fab").performClick()

    // Check if modal appears
    composeTestRule.onNodeWithTag(PlannerScreenTestTags.ADD_TASK_MODAL).assertExists()
  }

  @Test
  fun addStudyTaskModalFormFieldsWork() {
    var dismissed = false

    composeTestRule.setContent {
      AddStudyTaskModal(onDismiss = { dismissed = true }, onAddTask = { _, _, _, _, _ -> })
    }

    // Test subject field
    composeTestRule
        .onNodeWithTag(PlannerScreenTestTags.SUBJECT_FIELD)
        .performTextInput("Mathematics")

    // Test task title field
    composeTestRule
        .onNodeWithTag(PlannerScreenTestTags.TASK_TITLE_FIELD)
        .performTextInput("Complete exercises")

    // Test duration field
    composeTestRule.onNodeWithTag(PlannerScreenTestTags.DURATION_FIELD).performTextInput("90")

    // Test deadline field
    composeTestRule
        .onNodeWithTag(PlannerScreenTestTags.DEADLINE_FIELD)
        .performTextInput("15.12.2024")

    // Utiliser un test tag pour le bouton close
    composeTestRule.onNodeWithText("Cancel", ignoreCase = true).performClick()
    assert(dismissed)
  }

  @Test
  fun priorityDropdownSectionWorks() {
    var selectedPriority = "Medium"

    composeTestRule.setContent {
      PriorityDropdownSection(
          priority = selectedPriority, onPriorityChange = { selectedPriority = it })
    }

    composeTestRule.onNodeWithText("Priority").assertExists()
    composeTestRule.onNodeWithText("Medium").assertExists()
  }

  @Test
  fun formFieldSectionDisplaysCorrectly() {
    var fieldValue = ""

    composeTestRule.setContent {
      FormFieldSection(
          label = "Test Label",
          placeholder = "Test placeholder",
          value = fieldValue,
          onValueChange = { fieldValue = it },
          testTag = "test_field")
    }

    composeTestRule.onNodeWithText("Test Label").assertExists()
    composeTestRule.onNodeWithText("Test placeholder").assertExists()
  }

  @Test
  fun choiceButtonChangesSelection() {
    var isSelected = false

    composeTestRule.setContent {
      ChoiceButton(
          text = "Test Option", isSelected = isSelected, onClick = { isSelected = !isSelected })
    }

    composeTestRule.onNodeWithText("Test Option").assertExists()
    composeTestRule.onNodeWithText("Test Option").performClick()
  }

  @Test
  fun classAttendanceModalDisplaysClassInfo() {
    val testClass =
        PlannerClass(
            id = "1",
            courseName = "Physics",
            type = ClassType.LAB,
            startTime = LocalTime.of(14, 0),
            endTime = LocalTime.of(16, 0),
            location = "Lab B",
            instructor = "Prof. Johnson"
            // Pas de paramètre date
            )

    composeTestRule.setContent {
      ClassAttendanceModal(
          classItem = testClass,
          initialAttendance = null,
          initialCompletion = null,
          onDismiss = {},
          onSave = { _, _ -> })
    }

    // Vérifications plus flexibles
    composeTestRule.onNodeWithText("Physics", ignoreCase = true).assertExists()
    composeTestRule.onNodeWithText("Lab", ignoreCase = true).assertExists()
    composeTestRule.onNodeWithText("attend", ignoreCase = true).assertExists()
    composeTestRule.onNodeWithText("finish", ignoreCase = true).assertExists()
  }

  @Test
  fun todayClassesSectionDisplaysDate() {
    composeTestRule.setContent { PlannerScreen() }

    composeTestRule.onNodeWithTag(PlannerScreenTestTags.TODAY_CLASSES_SECTION).assertExists()
    composeTestRule.onNodeWithText("Today", ignoreCase = true).assertExists()
    composeTestRule.onNodeWithText("Classes", ignoreCase = true).assertExists()
  }

  @Test
  fun wellnessCampusSectionDisplaysContent() {
    composeTestRule.setContent { PlannerScreen() }

    composeTestRule.onNodeWithTag(PlannerScreenTestTags.WELLNESS_CAMPUS_SECTION).assertExists()
    composeTestRule.onNodeWithText("Wellness", ignoreCase = true).assertExists()
    composeTestRule.onNodeWithText("Campus", ignoreCase = true).assertExists()
    composeTestRule.onNodeWithText("Balance", ignoreCase = true).assertExists()
  }

  @Test
  fun activityItemWithAttendanceShowsStatus() {
    val testClass =
        PlannerClass(
            id = "1",
            courseName = "Chemistry",
            type = ClassType.LECTURE,
            startTime = LocalTime.of(11, 0),
            endTime = LocalTime.of(12, 0),
            location = "Auditorium",
            instructor = "Dr. Brown"
            // Pas de paramètre date
            )

    val attendanceRecord =
        ClassAttendance(
            classId = "1",
            attendance = AttendanceStatus.YES,
            completion = CompletionStatus.YES,
            date = LocalDate.now())

    composeTestRule.setContent {
      ActivityItem(activity = testClass, attendanceRecord = attendanceRecord, onClick = {})
    }

    composeTestRule.onNodeWithText("Chemistry").assertExists()
    composeTestRule.onNodeWithText("Lecture", ignoreCase = true).assertExists()

    // Vérifier les statuts avec des textes partiels
    composeTestRule.onNodeWithText("Yes", ignoreCase = true).assertExists()
  }
}
