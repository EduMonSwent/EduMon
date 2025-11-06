package com.android.sample.screen

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.R
import com.android.sample.data.ToDo
import com.android.sample.model.planner.*
import com.android.sample.repositories.ToDoRepository
import com.android.sample.ui.planner.PlannerScreen
import com.android.sample.ui.planner.PlannerScreenTestTags
import com.android.sample.ui.viewmodel.PlannerViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlannerScreenTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun plannerScreen_rendersBasicLayout() {
    composeTestRule.setContent { PlannerScreen() }

    composeTestRule.waitForIdle()

    // Check for main screen container
    composeTestRule.onNodeWithTag(PlannerScreenTestTags.PLANNER_SCREEN).assertExists()

    // Check for FAB
    composeTestRule.onNodeWithTag("addTaskFab").assertExists()

    // Check for pet header
    composeTestRule.onNodeWithTag(PlannerScreenTestTags.PET_HEADER).assertExists()
  }

  @Test
  fun plannerScreen_displaysPetStats() {
    composeTestRule.setContent { PlannerScreen() }

    composeTestRule.waitForIdle()

    // Check pet stats are displayed
    composeTestRule.onNodeWithText("90%", substring = true).assertExists()

    composeTestRule.onNodeWithText("85%", substring = true).assertExists()

    composeTestRule.onNodeWithText("70%", substring = true).assertExists()
  }

  // --- NEW TESTS: AIRecommendationCard Coverage ---

  @Test
  fun plannerScreen_displaysNoRecommendationText_whenNoRecommendedTask() {
    // Fake ViewModel with empty recommendation
    val fakeViewModel = PlannerViewModel()

    composeTestRule.setContent { PlannerScreen(viewModel = fakeViewModel) }

    composeTestRule.waitForIdle()

    // Expect to see the "no recommendation" string
    composeTestRule
        .onNodeWithText(
            composeTestRule.activity.getString(R.string.ai_recommendation_none), substring = true)
        .assertExists()
  }

  @Test
  fun plannerScreen_displaysProperRecommendationText_whenTaskIsRecommended() {
    val testTask =
        ToDo(
            title = "Study Algorithms",
            dueDate = LocalDate.of(2025, 10, 30),
            priority = com.android.sample.data.Priority.HIGH)
    class FakeToDoRepository : ToDoRepository {
      private val state = MutableStateFlow<List<ToDo>>(listOf(testTask))
      override val todos = state.asStateFlow()

      override suspend fun add(todo: ToDo) {}

      override suspend fun update(todo: ToDo) {}

      override suspend fun remove(id: String) {}

      override suspend fun getById(id: String): ToDo? {
        return todos.value.find { it.id == id }
      }
    }
    val fakeViewModel = PlannerViewModel(toDoRepository = FakeToDoRepository())

    composeTestRule.setContent { PlannerScreen(viewModel = fakeViewModel) }

    composeTestRule.waitForIdle()

    val context = composeTestRule.activity

    // Verify dynamic text pieces
    composeTestRule
        .onNodeWithText(context.getString(R.string.ai_recommendation_top), substring = true)
        .assertExists()

    composeTestRule.onNodeWithText("Study Algorithms", substring = true).assertExists()

    composeTestRule
        .onNodeWithText(context.getString(R.string.priority_high), substring = true)
        .assertExists()

    composeTestRule
        .onNodeWithText(context.getString(R.string.ai_recommendation_due_date), substring = true)
        .assertExists()
  }

  @Test
  fun plannerScreen_displaysTodayClassesSection() {
    composeTestRule.setContent { PlannerScreen() }

    composeTestRule.waitForIdle()

    // Check today classes section exists
    composeTestRule.onNodeWithTag(PlannerScreenTestTags.TODAY_CLASSES_SECTION).assertExists()

    // Check section header
    composeTestRule.onNodeWithText("Today's Classes", ignoreCase = true).assertExists()

    val today = LocalDate.now()
    val formattedDate = today.format(DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.ENGLISH))

    composeTestRule.onNodeWithText(formattedDate, ignoreCase = true).assertExists()
  }

  @Test
  fun plannerScreen_displaysClassItems() {
    composeTestRule.setContent { PlannerScreen() }

    composeTestRule.waitForIdle()

    // Check that class items are displayed
    composeTestRule.onNodeWithText("Algorithms", substring = true, ignoreCase = true).assertExists()

    composeTestRule
        .onNodeWithText("Data Structures", substring = true, ignoreCase = true)
        .assertExists()

    composeTestRule
        .onNodeWithText("Computer Networks", substring = true, ignoreCase = true)
        .assertExists()
  }

  /*@Test
  fun plannerScreen_displaysClassDetails() {
      composeTestRule.setContent {
          PlannerScreen()
      }

      composeTestRule.waitForIdle()

      // Check class types and times
      composeTestRule.onNodeWithText("Lecture", ignoreCase = true)
          .assertExists()

      composeTestRule.onNodeWithText("Exercise", ignoreCase = true)
          .assertExists()

      composeTestRule.onNodeWithText("Lab", ignoreCase = true)
          .assertExists()

      // Check times
      composeTestRule.onNodeWithText("09:00", substring = true)
          .assertExists()

      composeTestRule.onNodeWithText("11:00", substring = true)
          .assertExists()

      composeTestRule.onNodeWithText("14:00", substring = true)
          .assertExists()
  }*/

  @Test
  fun plannerScreen_fabIsClickable() {
    composeTestRule.setContent { PlannerScreen() }

    composeTestRule.waitForIdle()

    // Verify FAB exists and is clickable
    composeTestRule.onNodeWithTag("addTaskFab").assertExists().assertIsEnabled().performClick()

    // After click, we can't test modal appearance since it's not rendered in test
    // But we verified the click action works
  }

  @Test
  fun plannerScreen_classItemsAreClickable() {
    composeTestRule.setContent { PlannerScreen() }

    composeTestRule.waitForIdle()

    // Find and click on a class item
    composeTestRule
        .onNodeWithText("Algorithms", substring = true, ignoreCase = true)
        .assertExists()
        .assertIsEnabled()
        .performClick()

    // We can't test modal appearance, but we verified the click action
  }

  @Test
  fun plannerScreen_hasInteractiveElements() {
    composeTestRule.setContent { PlannerScreen() }

    composeTestRule.waitForIdle()

    // Check that there are clickable elements (classes and FAB)
    val clickableNodes = composeTestRule.onAllNodes(hasClickAction()).fetchSemanticsNodes()

    assert(clickableNodes.size >= 4) {
      "Should have at least 4 clickable elements (3 classes + FAB)"
    }
  }

  @Test
  fun plannerScreen_displaysAllExpectedSections() {
    composeTestRule.setContent { PlannerScreen() }

    composeTestRule.waitForIdle()

    // Verify all major sections are present
    composeTestRule.onNodeWithTag(PlannerScreenTestTags.PET_HEADER).assertExists()

    composeTestRule.onNodeWithText("AI Study Assistant", ignoreCase = true).assertExists()

    composeTestRule.onNodeWithTag(PlannerScreenTestTags.TODAY_CLASSES_SECTION).assertExists()

    composeTestRule.onNodeWithTag("addTaskFab").assertExists()
  }
}
