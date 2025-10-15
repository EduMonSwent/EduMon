package com.android.sample.screen

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.android.sample.ui.planner.PlannerScreen
import com.android.sample.ui.planner.PlannerScreenTestTags
import org.junit.Rule
import org.junit.Test

class PlannerScreenModalTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun plannerScreen_opensAddTaskModalOnFabClick() {
        composeTestRule.setContent { PlannerScreen() }

        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.mainClock.advanceTimeBy(4000)
        composeTestRule.waitForIdle()

        // Clique sur le FAB
        composeTestRule.onNodeWithTag("addTaskFab")
            .assertExists("Floating action button not found")
            .performClick()

        composeTestRule.mainClock.advanceTimeBy(4000)
        composeTestRule.waitForIdle()

        // Attendre que le modal soit visible (tolérance CI lente)
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            try {
                composeTestRule.onAllNodesWithTag(PlannerScreenTestTags.ADD_TASK_MODAL)
                    .fetchSemanticsNodes().isNotEmpty()
            } catch (e: Exception) {
                false
            }
        }

        // Vérifie qu'au moins un noeud du modal existe
        val nodes = composeTestRule
            .onAllNodesWithTag(PlannerScreenTestTags.ADD_TASK_MODAL)
            .fetchSemanticsNodes()
        assert(nodes.isNotEmpty()) { " Add Task modal not visible even after waiting." }
    }

    @Test
    fun plannerScreen_opensAttendanceModalOnClassClick() {
        composeTestRule.setContent { PlannerScreen() }

        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.mainClock.advanceTimeBy(4000)
        composeTestRule.waitForIdle()

        // Clique sur le cours "Algorithms"
        composeTestRule.onNodeWithText("Algorithms", substring = true, ignoreCase = true)
            .assertExists("No course named 'Algorithms' found on screen.")
            .performClick()

        composeTestRule.mainClock.advanceTimeBy(4000)
        composeTestRule.waitForIdle()

        // Attendre que le modal d’assiduité apparaisse
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            try {
                composeTestRule.onAllNodesWithTag(PlannerScreenTestTags.CLASS_ATTENDANCE_MODAL)
                    .fetchSemanticsNodes().isNotEmpty()
            } catch (e: Exception) {
                false
            }
        }

        // Vérifie qu’au moins un noeud du modal d’assiduité existe
        val attendanceNodes = composeTestRule
            .onAllNodesWithTag(PlannerScreenTestTags.CLASS_ATTENDANCE_MODAL)
            .fetchSemanticsNodes()
        assert(attendanceNodes.isNotEmpty()) { " Class Attendance modal not visible even after waiting." }
    }
}
