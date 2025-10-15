package com.android.sample.screen

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.android.sample.ui.planner.PlannerScreen
import com.android.sample.ui.planner.PlannerScreenTestTags
import org.junit.Rule
import org.junit.Test

class PlannerScreenModalTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun plannerScreen_opensAddTaskModalOnFabClick() {
    composeTestRule.setContent { PlannerScreen() }

    composeTestRule.onNodeWithTag("addTaskFab").performClick()
    // Vérifie que le modal est dans l’arbre après clic
    composeTestRule.onNodeWithTag(PlannerScreenTestTags.ADD_TASK_MODAL).assertExists()
  }

  @Test
  fun plannerScreen_opensAttendanceModalOnClassClick() {
    composeTestRule.setContent { PlannerScreen() }

    composeTestRule.onNodeWithText("Algorithms", substring = true).performClick()

    composeTestRule.onNodeWithTag(PlannerScreenTestTags.CLASS_ATTENDANCE_MODAL).assertExists()
  }

  /*@Test
  fun plannerScreen_snackbarIsShownAfterAttendanceSave()  {
      runBlocking {
          composeTestRule.setContent { PlannerScreen() }

          // Clique sur un cours pour simuler une sauvegarde
          composeTestRule.onNodeWithText("Algorithms", substring = true, ignoreCase = true)
              .assertExists()
              .performClick()

          // Laisse le temps à la coroutine de ViewModel + recomposition de Snackbar
          composeTestRule.waitForIdle()
          composeTestRule.mainClock.advanceTimeBy(4000) // accélère le temps Compose virtuel
          composeTestRule.waitForIdle()

          // Attend jusqu’à 8 secondes au total que le Snackbar apparaisse
          composeTestRule.waitUntil(timeoutMillis = 8000) {
              composeTestRule
                  .onAllNodesWithText("Attendance saved successfully!", substring = true, ignoreCase = true)
                  .fetchSemanticsNodes().isNotEmpty()
          }

          // Vérifie qu’il est bien présent à l’écran
          composeTestRule.onNodeWithText("Attendance saved successfully!", substring = true, ignoreCase = true)
              .assertExists()
      }
  }*/

}
