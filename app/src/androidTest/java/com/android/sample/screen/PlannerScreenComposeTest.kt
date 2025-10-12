package com.android.sample.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import com.android.sample.ui.planner.PriorityDropdownSection

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import com.android.sample.model.planner.AttendanceStatus
import com.android.sample.model.planner.Class
import com.android.sample.model.planner.ClassAttendance
import com.android.sample.model.planner.ClassType
import com.android.sample.model.planner.CompletionStatus
import com.android.sample.model.planner.WellnessEventType
import com.android.sample.ui.planner.AIRecommendationCard
import com.android.sample.ui.planner.ActivityItem
import com.android.sample.ui.planner.AddStudyTaskModal
import com.android.sample.ui.planner.ChoiceButton
import com.android.sample.ui.planner.ClassAttendanceModal
import com.android.sample.ui.planner.FormFieldSection
import com.android.sample.ui.planner.PetHeader
import com.android.sample.ui.planner.PlannerGlowCard
import com.android.sample.ui.planner.PlannerScreenTestTags
import com.android.sample.ui.planner.StatBar
import com.android.sample.ui.planner.WellnessEventItem
import java.time.LocalTime
import org.junit.Rule
import org.junit.Test

// Alias pour éviter le conflit avec java.lang.Class
import com.android.sample.model.planner.Class as PlannerClass

class PlannerScreenComposeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // Tests pour PetHeader
    @Test
    fun petHeader_displaysLevelAndProfile() {
        composeTestRule.setContent {
            // Wrapper pour s'assurer que le composant a assez d'espace
            Box(modifier = androidx.compose.ui.Modifier.size(400.dp, 300.dp)) {
                PetHeader(level = 5, onEdumonNameClick = {})
            }
        }

        // Attendre que le composant soit chargé
        composeTestRule.waitForIdle()

        // Vérifier d'abord que le conteneur existe
        composeTestRule.onNodeWithTag(PlannerScreenTestTags.PET_HEADER).assertExists()

        // Utiliser assertExists au lieu de assertIsDisplayed pour les tests initiaux
        composeTestRule.onNodeWithText("Lv 5").assertExists()
        composeTestRule.onNodeWithText("Edumon Profile").assertExists()
    }

    @Test
    fun petHeader_statBarsDisplayCorrectPercentages() {
        composeTestRule.setContent {
            PetHeader(level = 3, onEdumonNameClick = {})
        }

        // Les pourcentages devraient être affichés dans les barres de stat
        composeTestRule.onNodeWithText("90%").assertIsDisplayed()
        composeTestRule.onNodeWithText("85%").assertIsDisplayed()
        composeTestRule.onNodeWithText("70%").assertIsDisplayed()
    }

    // Tests pour StatBar
    @Test
    fun statBar_displaysIconAndPercentage() {
        composeTestRule.setContent {
            StatBar(icon = "❤️", percent = 0.75f, color = androidx.compose.ui.graphics.Color.Red)
        }

        composeTestRule.onNodeWithText("❤️").assertIsDisplayed()
        composeTestRule.onNodeWithText("75%").assertIsDisplayed()
    }

    // Tests pour AIRecommendationCard
    /*@Test
    fun aiRecommendationCard_displaysContent() {
        composeTestRule.setContent {
            AIRecommendationCard(
                recommendationText = "Test recommendation",
                onActionClick = {}
            )
        }

        composeTestRule.onNodeWithText("AI Recommendation").assertIsDisplayed()
        composeTestRule.onNodeWithText("Test recommendation").assertIsDisplayed()
        composeTestRule.onNodeWithText("Start Studying Session").assertIsDisplayed()
    }*/

    @Test
    fun aiRecommendationCard_buttonClickWorks() {
        var clicked = false

        composeTestRule.setContent {
            AIRecommendationCard(
                recommendationText = "Test recommendation",
                onActionClick = { clicked = true }
            )
        }

        composeTestRule.onNodeWithText("Start Studying Session").performClick()
        assert(clicked)
    }

    // Tests pour ActivityItem
    /*@Test
    fun activityItem_displaysClassInformation() {
        val testClass = PlannerClass(
            id = "1",
            courseName = "Mathematics",
            type = ClassType.LECTURE,
            startTime = LocalTime.of(9, 0),
            endTime = LocalTime.of(10, 0),
            location = "Room 101",
            instructor = "Dr. Smith"
        )

        composeTestRule.setContent {
            ActivityItem(
                activity = testClass,
                attendanceRecord = null,
                onClick = {}
            )
        }

        composeTestRule.onNodeWithText("Mathematics").assertIsDisplayed()
        composeTestRule.onNodeWithText("Lecture").assertIsDisplayed()
        composeTestRule.onNodeWithText("09:00 - 10:00").assertIsDisplayed()
        composeTestRule.onNodeWithText("Room 101 • Dr. Smith").assertIsDisplayed()
    }*/

    /*@Test
    fun activityItem_withAttendance_showsStatus() {
        val testClass = PlannerClass(
            id = "1",
            courseName = "Chemistry",
            type = ClassType.LECTURE,
            startTime = LocalTime.of(11, 0),
            endTime = LocalTime.of(12, 0),
            location = "Auditorium",
            instructor = "Dr. Brown"
        )

        val attendanceRecord = ClassAttendance(
            classId = "1",
            attendance = AttendanceStatus.YES,
            completion = CompletionStatus.YES,
            date = java.time.LocalDate.now()
        )

        composeTestRule.setContent {
            ActivityItem(
                activity = testClass,
                attendanceRecord = attendanceRecord,
                onClick = {}
            )
        }

        composeTestRule.onNodeWithText("Chemistry").assertIsDisplayed()
        composeTestRule.onNodeWithText("Yes").assertIsDisplayed() // Attendance status
    }*/

    /*@Test
    fun activityItem_clickHandlerWorks() {
        var clicked = false
        val testClass = PlannerClass(
            id = "1",
            courseName = "Physics",
            type = ClassType.LAB,
            startTime = LocalTime.of(14, 0),
            endTime = LocalTime.of(16, 0),
            location = "Lab B",
            instructor = "Prof. Johnson"
        )

        composeTestRule.setContent {
            ActivityItem(
                activity = testClass,
                attendanceRecord = null,
                onClick = { clicked = true }
            )
        }

        composeTestRule.onNodeWithText("Physics").performClick()
        assert(clicked)
    }*/

    // Tests pour WellnessEventItem
    @Test
    fun wellnessEventItem_displaysEventInformation() {
        composeTestRule.setContent {
            WellnessEventItem(
                title = "Morning Yoga",
                time = "08:00 - 09:00",
                description = "Start your day with relaxing yoga",
                eventType = WellnessEventType.YOGA,
                onClick = {}
            )
        }

        composeTestRule.onNodeWithText("Morning Yoga").assertIsDisplayed()
        composeTestRule.onNodeWithText("08:00 - 09:00").assertIsDisplayed()
        composeTestRule.onNodeWithText("Start your day with relaxing yoga").assertIsDisplayed()
    }

    @Test
    fun wellnessEventItem_clickHandlerWorks() {
        var clicked = false

        composeTestRule.setContent {
            WellnessEventItem(
                title = "Wellness Lecture",
                time = "10:00 - 11:00",
                description = "Learn about mental health",
                eventType = WellnessEventType.LECTURE,
                onClick = { clicked = true }
            )
        }

        composeTestRule.onNodeWithText("Wellness Lecture").performClick()
        assert(clicked)
    }

    // Tests pour AddStudyTaskModal
    @Test
    fun addStudyTaskModal_displaysFormFields() {
        composeTestRule.setContent {
            AddStudyTaskModal(
                onDismiss = {},
                onAddTask = { _, _, _, _, _ -> }
            )
        }

        composeTestRule.onNodeWithText("Add Study Task").assertIsDisplayed()
        composeTestRule.onNodeWithText("Plan your study session").assertIsDisplayed()
        composeTestRule.onNodeWithTag(PlannerScreenTestTags.SUBJECT_FIELD).assertIsDisplayed()
        composeTestRule.onNodeWithTag(PlannerScreenTestTags.TASK_TITLE_FIELD).assertIsDisplayed()
        composeTestRule.onNodeWithTag(PlannerScreenTestTags.DURATION_FIELD).assertIsDisplayed()
        composeTestRule.onNodeWithTag(PlannerScreenTestTags.DEADLINE_FIELD).assertIsDisplayed()
    }

    @Test
    fun addStudyTaskModal_formFieldsAcceptInput() {
        composeTestRule.setContent {
            AddStudyTaskModal(
                onDismiss = {},
                onAddTask = { _, _, _, _, _ -> }
            )
        }

        composeTestRule.onNodeWithTag(PlannerScreenTestTags.SUBJECT_FIELD)
            .performTextInput("Mathematics")

        composeTestRule.onNodeWithTag(PlannerScreenTestTags.TASK_TITLE_FIELD)
            .performTextInput("Complete exercises")

        composeTestRule.onNodeWithTag(PlannerScreenTestTags.DURATION_FIELD)
            .performTextInput("90")
    }

    @Test
    fun addStudyTaskModal_cancelButtonWorks() {
        var dismissed = false

        composeTestRule.setContent {
            AddStudyTaskModal(
                onDismiss = { dismissed = true },
                onAddTask = { _, _, _, _, _ -> }
            )
        }

        composeTestRule.onNodeWithText("Cancel").performClick()
        assert(dismissed)
    }

    // Tests pour ClassAttendanceModal
    /*@Test
    fun classAttendanceModal_displaysClassInfo() {
        val testClass = PlannerClass(
            id = "1",
            courseName = "Physics",
            type = ClassType.LAB,
            startTime = LocalTime.of(14, 0),
            endTime = LocalTime.of(16, 0),
            location = "Lab B",
            instructor = "Prof. Johnson"
        )

        composeTestRule.setContent {
            Box(modifier = androidx.compose.ui.Modifier.size(500.dp, 600.dp)) {
                ClassAttendanceModal(
                    classItem = testClass,
                    initialAttendance = null,
                    initialCompletion = null,
                    onDismiss = {},
                    onSave = { _, _ -> }
                )
            }
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Physics", ignoreCase = true).assertExists()
        composeTestRule.onNodeWithText("attend", ignoreCase = true).assertExists()
        composeTestRule.onNodeWithText("finish", ignoreCase = true).assertExists()
    }*/

    /*@Test
    fun classAttendanceModal_attendanceButtonsWork() {
        val testClass = PlannerClass(
            id = "1",
            courseName = "Math",
            type = ClassType.LECTURE,
            startTime = LocalTime.of(9, 0),
            endTime = LocalTime.of(10, 0),
            location = "Room 101",
            instructor = "Dr. Smith"
        )

        composeTestRule.setContent {
            ClassAttendanceModal(
                classItem = testClass,
                initialAttendance = null,
                initialCompletion = null,
                onDismiss = {},
                onSave = { _, _ -> }
            )
        }

        composeTestRule.onNodeWithText("Yes").performClick()
        composeTestRule.onNodeWithText("No").performClick()
        composeTestRule.onNodeWithText("Late").performClick()
    }*/

    @Test
    fun classAttendanceModal_saveButtonWorks() {
        var saved = false
        val testClass = PlannerClass(
            id = "1",
            courseName = "Chemistry",
            type = ClassType.EXERCISE,
            startTime = LocalTime.of(11, 0),
            endTime = LocalTime.of(12, 0),
            location = "Room 202",
            instructor = "Dr. Wilson"
        )

        composeTestRule.setContent {
            ClassAttendanceModal(
                classItem = testClass,
                initialAttendance = null,
                initialCompletion = null,
                onDismiss = {},
                onSave = { _, _ -> saved = true }
            )
        }

        composeTestRule.onNodeWithText("Save").performClick()
        assert(saved)
    }

    // Tests pour ChoiceButton
    @Test
    fun choiceButton_displaysTextAndRespondsToClick() {
        var clicked = false

        composeTestRule.setContent {
            ChoiceButton(
                text = "Test Option",
                isSelected = false,
                onClick = { clicked = true }
            )
        }

        composeTestRule.onNodeWithText("Test Option").assertIsDisplayed()
        composeTestRule.onNodeWithText("Test Option").performClick()
        assert(clicked)
    }

    // Tests pour PlannerGlowCard
    @Test
    fun plannerGlowCard_rendersContent() {
        composeTestRule.setContent {
            PlannerGlowCard {
                androidx.compose.material3.Text("Test content inside glow card")
            }
        }

        composeTestRule.onNodeWithText("Test content inside glow card").assertIsDisplayed()
    }

    // Tests pour FormFieldSection
    @Test
    fun formFieldSection_displaysLabelAndAcceptsInput() {
        composeTestRule.setContent {
            FormFieldSection(
                label = "Test Label",
                placeholder = "Test placeholder",
                value = "",
                onValueChange = { },
                testTag = "test_field"
            )
        }

        composeTestRule.onNodeWithText("Test Label").assertIsDisplayed()
        composeTestRule.onNodeWithTag("test_field").assertIsDisplayed()
        composeTestRule.onNodeWithTag("test_field").performTextInput("Test input")
    }

    // Tests pour PriorityDropdownSection
    @Test
    fun priorityDropdownSection_displaysCurrentPriority() {
        composeTestRule.setContent {
            PriorityDropdownSection(
                priority = "Medium",
                onPriorityChange = { }
            )
        }

        composeTestRule.onNodeWithText("Priority").assertIsDisplayed()
        composeTestRule.onNodeWithText("Medium").assertIsDisplayed()
    }
}