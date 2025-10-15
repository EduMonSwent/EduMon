package sampleApp.ui.navigation

import androidx.compose.ui.test.junit4.createComposeRule
import com.android.sample.SampleApp
import com.android.sample.model.map.Location
import com.android.sample.model.todo.ToDo
import com.android.sample.model.todo.ToDoStatus
import com.google.firebase.Timestamp
import java.util.Calendar
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import sampleApp.utils.BootcampTest.Companion.fromDate
import sampleApp.utils.InMemoryTest
import sampleApp.utils.StateCheckerBootcampTest
import sampleApp.utils.StateCheckerBootcampTest.ScrollableToDosRepository.Companion.firstTodo

// *****************************************************************************
// ***                                                                       ***
// *** THIS FILE WILL BE OVERWRITTEN DURING GRADING. IT SHOULD BE LOCATED IN ***
// *** app/src/androidTest/java/com/github/se/bootcamp/ui/navigation.        ***
// *** DO **NOT** IMPLEMENT YOUR OWN TESTS IN THIS FILE                      ***
// ***                                                                       ***
// *****************************************************************************

class NavigationB2Test : StateCheckerBootcampTest(InMemoryTest) {
  @get:Rule val composeTestRule = createComposeRule()

  private val validTodo =
      ToDo(
          uid = "uid_not_used",
          name = "title",
          description = "description",
          assigneeName = "test",
          dueDate = Timestamp.fromDate(2025, Calendar.SEPTEMBER, 1),
          location = Location(46.5191, 6.5668, "Any"),
          status = ToDoStatus.CREATED,
          ownerId = "user")

  @Before
  override fun setUp() {
    super.setUp()
    composeTestRule.setContent { SampleApp() }
  }

  @Test
  fun canNavigateToEditToDoScreenFromOverview() {
    composeTestRule.navigateToEditToDoScreen(firstTodo)
    composeTestRule.checkEditToDoScreenIsDisplayed()
    composeTestRule.checkOverviewScreenIsNotDisplayed()
  }

  @Test
  fun addTodo_saveButtonNavigatesToOverviewToDoIfInputIsValid() {
    composeTestRule.navigateToAddToDoScreen()
    composeTestRule.enterAddTodoDetails(todo = validTodo)
    composeTestRule.clickOnSaveForAddTodo()
    composeTestRule.checkOverviewScreenIsDisplayed()
  }

  @Test
  fun editTodo_saveButtonNavigatesToOverviewToDoIfInputIsValid() {
    composeTestRule.navigateToEditToDoScreen(firstTodo)
    composeTestRule.clickOnSaveForEditTodo()
    composeTestRule.checkOverviewScreenIsDisplayed()
  }

  @Test
  fun topAppTitleIsCorrectOnEditToDoScreen() {
    composeTestRule.checkOverviewScreenIsDisplayed()
    composeTestRule.navigateToEditToDoScreen(firstTodo)
    composeTestRule.checkEditToDoScreenIsDisplayed()
  }
}
