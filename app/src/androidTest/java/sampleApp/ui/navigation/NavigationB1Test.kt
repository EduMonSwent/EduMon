package sampleApp.ui.navigation

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.android.sample.SampleApp
import com.android.sample.model.map.Location
import com.android.sample.model.todo.ToDo
import com.android.sample.model.todo.ToDoStatus
import com.android.sample.model.todo.ToDosRepositoryLocal
import com.android.sample.model.todo.ToDosRepositoryProvider
import com.android.sample.ui.navigation.NavigationTestTags
import com.android.sample.ui.overview.OverviewScreenTestTags
import com.google.firebase.Timestamp
import java.util.Calendar
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

// ***************************************************************************** //
// ***                                                                       *** //
// *** THIS FILE WILL BE OVERWRITTEN DURING GRADING. IT SHOULD BE LOCATED IN *** //
// *** `app/src/androidTest/java/com/github/se/bootcamp/ui/navigation`.        *** //
// *** DO **NOT** IMPLEMENT YOUR OWN TESTS IN THIS FILE                      *** //
// ***                                                                       *** //
// ***************************************************************************** //

class NavigationB1Test {
  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()
  val repository = ToDosRepositoryLocal()

  private fun getTimestamp(year: Int, month: Int, day: Int): Timestamp {
    val calendar = Calendar.getInstance()
    calendar.set(year, month, day)
    return Timestamp(calendar.time)
  }

  private val todo1 =
      ToDo(
          uid = "0",
          name = "Buy groceries",
          description = "Milk, eggs, bread, and butter",
          assigneeName = "Alice",
          dueDate = getTimestamp(2025, Calendar.SEPTEMBER, 1),
          location = Location(46.5191, 6.5668, "Lausanne Coop"),
          status = ToDoStatus.CREATED,
          ownerId = "user")

  private val todo2 =
      ToDo(
          uid = "1",
          name = "Walk the dog",
          description = "Take Fido for a walk in the park",
          assigneeName = "Bob",
          dueDate = getTimestamp(2025, Calendar.OCTOBER, 15),
          location = Location(46.5210, 6.5790, "Parc de Mon Repos"),
          status = ToDoStatus.STARTED,
          ownerId = "user")
  private val lastTodo =
      ToDo(
          uid = "1000",
          name = "Swent Bootcamp",
          description = "Complete the SE Bootcamp",
          assigneeName = "Me",
          dueDate = getTimestamp(2025, Calendar.SEPTEMBER, 29),
          location = null,
          status = ToDoStatus.STARTED,
          ownerId = "user")

  init {
    runTest {
      repository.addTodo(todo1)
      repository.addTodo(todo2)
      for (i in 3..50) {
        val todo =
            ToDo(
                uid = "$i",
                name = "Task $i",
                description = "Description for task $i",
                assigneeName = if (i % 2 == 0) "Alice" else "Bob",
                dueDate = getTimestamp(2025, Calendar.NOVEMBER, i),
                location = null,
                status = ToDoStatus.CREATED,
                ownerId = "user")
        repository.addTodo(todo)
      }
      repository.addTodo(lastTodo)
    }
  }

  private fun checkOverviewScreenIsDisplayed() {
    composeTestRule
        .onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE)
        .assertIsDisplayed()
        .assertTextContains("overview", substring = true, ignoreCase = true)
    composeTestRule.onNodeWithTag(OverviewScreenTestTags.TODO_LIST).assertIsDisplayed()
  }

  private fun checkOverviewScreenIsNotDisplayed() {
    composeTestRule.onNodeWithTag(OverviewScreenTestTags.TODO_LIST).assertDoesNotExist()
  }

  private fun checkMapScreenIsDisplayed() {
    composeTestRule
        .onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE)
        .assertIsDisplayed()
        .assertTextContains("map", substring = true, ignoreCase = true)
  }

  private fun checkAddToDoScreenIsDisplayed() {
    composeTestRule
        .onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE)
        .assertIsDisplayed()
        .assertTextContains("Create a new task", substring = false, ignoreCase = true)
  }

  private fun scrollToLastTodo() {
    composeTestRule
        .onNodeWithTag(OverviewScreenTestTags.TODO_LIST)
        .performScrollToNode(hasTestTag(OverviewScreenTestTags.getTestTagForTodoItem(lastTodo)))
  }

  @Before
  fun setUp() {
    ToDosRepositoryProvider.repository = repository
    composeTestRule.setContent { SampleApp() }
  }

  @Test
  fun topBarTitleIsCorrectForOverview() {
    composeTestRule
        .onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE)
        .assertIsDisplayed()
        .assertTextContains(value = "Overview")
  }

  @Test
  fun topBarTitleIsCorrectForAddToDo() {
    composeTestRule.onNodeWithTag(OverviewScreenTestTags.CREATE_TODO_BUTTON).performClick()
    composeTestRule
        .onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE)
        .assertIsDisplayed()
        .assertTextContains(value = "Create a new task", substring = false, ignoreCase = true)
  }

  @Test
  fun navigationStartsOnOverviewTab() {
    checkOverviewScreenIsDisplayed()
  }

  @Test
  fun canNavigateToAddToDo() {
    composeTestRule.onNodeWithTag(OverviewScreenTestTags.CREATE_TODO_BUTTON).performClick()
    checkAddToDoScreenIsDisplayed()
    checkOverviewScreenIsNotDisplayed()
  }

  @Test
  fun canNavigateBackToOverviewFromAddToDo() {
    composeTestRule.onNodeWithTag(OverviewScreenTestTags.CREATE_TODO_BUTTON).performClick()
    checkAddToDoScreenIsDisplayed()
    checkOverviewScreenIsNotDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.GO_BACK_BUTTON).performClick()
    checkOverviewScreenIsDisplayed()
  }

  private fun pressBack(shouldFinish: Boolean) {
    composeTestRule.activityRule.scenario.onActivity { activity ->
      activity.onBackPressedDispatcher.onBackPressed()
    }
    composeTestRule.waitUntil { composeTestRule.activity.isFinishing == shouldFinish }
    assertEquals(shouldFinish, composeTestRule.activity.isFinishing)
  }
}
