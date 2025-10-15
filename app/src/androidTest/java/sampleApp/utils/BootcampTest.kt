package sampleApp.utils

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.android.sample.model.map.Location
import com.android.sample.model.todo.ToDo
import com.android.sample.model.todo.ToDoStatus
import com.android.sample.model.todo.ToDosRepository
import com.android.sample.model.todo.ToDosRepositoryProvider
import com.android.sample.ui.navigation.NavigationTestTags
import com.android.sample.ui.overview.AddToDoScreenTestTags
import com.android.sample.ui.overview.EditToDoScreenTestTags
import com.android.sample.ui.overview.OverviewScreenTestTags
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Before

const val UI_WAIT_TIMEOUT = 5_000L

interface BootcampTest {

  fun createInitializedRepository(): ToDosRepository

  val repository: ToDosRepository
    get() = ToDosRepositoryProvider.repository

  @Before
  fun setUp() {
    ToDosRepositoryProvider.repository = createInitializedRepository()
  }

  fun ComposeTestRule.enterAddTodoTitle(title: String) =
      onNodeWithTag(AddToDoScreenTestTags.INPUT_TODO_TITLE).performTextInput(title)

  fun ComposeTestRule.enterAddTodoDescription(description: String) =
      onNodeWithTag(AddToDoScreenTestTags.INPUT_TODO_DESCRIPTION).performTextInput(description)

  fun ComposeTestRule.enterAddTodoAssignee(assignee: String) =
      onNodeWithTag(AddToDoScreenTestTags.INPUT_TODO_ASSIGNEE).performTextInput(assignee)

  fun ComposeTestRule.enterAddTodoDate(date: String) =
      onNodeWithTag(AddToDoScreenTestTags.INPUT_TODO_DATE).performTextInput(date)

  fun ComposeTestRule.enterAddTodoLocation(location: String) =
      onNodeWithTag(AddToDoScreenTestTags.INPUT_TODO_LOCATION).performTextInput(location)

  fun ComposeTestRule.enterEditTodoTitle(title: String) {
    onNodeWithTag(EditToDoScreenTestTags.INPUT_TODO_TITLE).performTextClearance()
    onNodeWithTag(EditToDoScreenTestTags.INPUT_TODO_TITLE).performTextInput(title)
  }

  fun ComposeTestRule.enterEditTodoDescription(description: String) {
    onNodeWithTag(EditToDoScreenTestTags.INPUT_TODO_DESCRIPTION).performTextClearance()
    onNodeWithTag(EditToDoScreenTestTags.INPUT_TODO_DESCRIPTION).performTextInput(description)
  }

  fun ComposeTestRule.enterEditTodoAssignee(assignee: String) {
    onNodeWithTag(EditToDoScreenTestTags.INPUT_TODO_ASSIGNEE).performTextClearance()
    onNodeWithTag(EditToDoScreenTestTags.INPUT_TODO_ASSIGNEE).performTextInput(assignee)
  }

  fun ComposeTestRule.enterEditTodoDate(date: String) {
    onNodeWithTag(EditToDoScreenTestTags.INPUT_TODO_DATE).performTextClearance()
    onNodeWithTag(EditToDoScreenTestTags.INPUT_TODO_DATE).performTextInput(date)
  }

  fun ComposeTestRule.enterEditTodoLocation(location: String) {
    onNodeWithTag(EditToDoScreenTestTags.INPUT_TODO_LOCATION).performTextClearance()
    onNodeWithTag(EditToDoScreenTestTags.INPUT_TODO_LOCATION).performTextInput(location)
  }

  fun ComposeTestRule.enterEditTodoDetails(todo: ToDo, date: String = todo.dueDate.toDateString()) {
    enterEditTodoTitle(todo.name)
    enterEditTodoDescription(todo.description)
    enterEditTodoAssignee(todo.assigneeName)
    enterEditTodoDate(date)
    enterEditTodoLocation(todo.location?.name ?: "Any")
  }

  fun ComposeTestRule.enterAddTodoDetails(todo: ToDo, date: String = todo.dueDate.toDateString()) {
    enterAddTodoTitle(todo.name)
    enterAddTodoDescription(todo.description)
    enterAddTodoAssignee(todo.assigneeName)
    enterAddTodoDate(date)
    enterAddTodoLocation(todo.location?.name ?: "Any")
  }

  fun ComposeTestRule.clickOnSaveForAddTodo(waitForRedirection: Boolean = false) {
    onNodeWithTag(AddToDoScreenTestTags.TODO_SAVE).assertIsDisplayed().performClick()
    waitUntil(UI_WAIT_TIMEOUT) {
      !waitForRedirection ||
          onAllNodesWithTag(AddToDoScreenTestTags.TODO_SAVE).fetchSemanticsNodes().isEmpty()
    }
  }

  fun ComposeTestRule.clickOnSaveForEditTodo(waitForRedirection: Boolean = false) {
    onNodeWithTag(EditToDoScreenTestTags.TODO_SAVE).assertIsDisplayed().performClick()
    waitUntil(UI_WAIT_TIMEOUT) {
      !waitForRedirection ||
          onAllNodesWithTag(EditToDoScreenTestTags.TODO_SAVE).fetchSemanticsNodes().isEmpty()
    }
  }

  fun ComposeTestRule.clickOnDeleteForEditTodo(waitForRedirection: Boolean = false) {
    onNodeWithTag(EditToDoScreenTestTags.TODO_DELETE).assertIsDisplayed().performClick()
    waitUntil(UI_WAIT_TIMEOUT) {
      !waitForRedirection ||
          onAllNodesWithTag(EditToDoScreenTestTags.TODO_DELETE).fetchSemanticsNodes().isEmpty()
    }
  }

  fun ComposeTestRule.navigateToAddToDoScreen() {
    onNodeWithTag(OverviewScreenTestTags.CREATE_TODO_BUTTON).assertIsDisplayed().performClick()
  }

  private fun ComposeTestRule.waitUntilTodoIsDisplayed(todo: ToDo): SemanticsNodeInteraction {
    checkOverviewScreenIsDisplayed()
    waitUntil(UI_WAIT_TIMEOUT) {
      onAllNodesWithTag(OverviewScreenTestTags.getTestTagForTodoItem(todo))
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    return checkTodoItemIsDisplayed(todo)
  }

  fun ComposeTestRule.clickOnTodoItem(todo: ToDo) {
    waitUntilTodoIsDisplayed(todo).performClick()
  }

  fun ComposeTestRule.checkTodoItemIsDisplayed(todo: ToDo): SemanticsNodeInteraction =
      onNodeWithTag(OverviewScreenTestTags.getTestTagForTodoItem(todo)).assertIsDisplayed()

  fun ComposeTestRule.navigateToEditToDoScreen(editedToDo: ToDo) {
    // Wait for the todo item to be displayed before trying to click it
    clickOnTodoItem(editedToDo)
  }

  fun ComposeTestRule.navigateBack() {
    onNodeWithTag(NavigationTestTags.GO_BACK_BUTTON).assertIsDisplayed().performClick()
  }

  fun ComposeTestRule.checkAddToDoScreenIsDisplayed() {
    onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE)
        .assertIsDisplayed()
        .assertTextContains("Create a new task", substring = false, ignoreCase = true)
  }

  fun ComposeTestRule.checkOverviewScreenIsNotDisplayed() {
    onNodeWithTag(OverviewScreenTestTags.TODO_LIST).assertDoesNotExist()
  }

  fun ComposeTestRule.checkOverviewScreenIsDisplayed() {
    onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE)
        .assertIsDisplayed()
        .assertTextContains("overview", substring = true, ignoreCase = true)
  }

  fun ComposeTestRule.checkEditToDoScreenIsDisplayed() {
    onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE)
        .assertIsDisplayed()
        .assertTextContains("Edit Todo", substring = false, ignoreCase = true)
  }

  fun ComposeTestRule.checkErrorMessageIsDisplayedForAddTodo() =
      onNodeWithTag(AddToDoScreenTestTags.ERROR_MESSAGE, useUnmergedTree = true).assertIsDisplayed()

  fun ComposeTestRule.checkErrorMessageIsDisplayedForEditTodo() =
      onNodeWithTag(EditToDoScreenTestTags.ERROR_MESSAGE, useUnmergedTree = true)
          .assertIsDisplayed()

  fun checkNoTodoWereAdded(action: () -> Unit) {
    val numberOfTodos = runBlocking { repository.getAllTodos().size }
    action()
    runTest { assertEquals(numberOfTodos, repository.getAllTodos().size) }
  }

  fun checkTodoWasNotEdited(editingTodo: ToDo = todo1, block: () -> Unit) {
    val todoBeforeEdit = runBlocking { repository.getTodo(editingTodo.uid) }
    block()
    runTest {
      val todoAfterEdit = repository.getTodo(editingTodo.uid)
      assertEquals(todoBeforeEdit, todoAfterEdit)
    }
  }

  @OptIn(ExperimentalStdlibApi::class)
  fun ComposeTestRule.enterEditTodoStatus(currentStatus: ToDoStatus, status: ToDoStatus) {
    val numberOfClick =
        (status.ordinal - currentStatus.ordinal + ToDoStatus.values().toList().size) %
            ToDoStatus.values().toList().size
    for (i in 0 until numberOfClick) {
      onNodeWithTag(EditToDoScreenTestTags.INPUT_TODO_STATUS).assertIsDisplayed().performClick()
    }
  }

  fun ToDo.b2Equals(other: ToDo): Boolean =
      name == other.name &&
          description == other.description &&
          assigneeName == other.assigneeName &&
          dueDate.toDateString() == other.dueDate.toDateString() &&
          status == other.status

  fun ToDosRepository.getTodoByName(name: String): ToDo = runBlocking {
    getAllTodos().first { it.name == name }
  }

  companion object {
    val todo1 =
        ToDo(
            uid = "0",
            name = "Buy groceries",
            description = "Milk, eggs, bread, and butter",
            assigneeName = "Alice",
            dueDate = Timestamp.Companion.fromDate(2025, Calendar.SEPTEMBER, 1),
            location = Location(46.5191, 6.5668, "Lausanne Coop"),
            status = ToDoStatus.CREATED,
            ownerId = "user")

    val todo2 =
        ToDo(
            uid = "1",
            name = "Walk the dog",
            description = "Take Fido for a walk in the park",
            assigneeName = "Bob",
            dueDate = Timestamp.Companion.fromDate(2025, Calendar.OCTOBER, 15),
            location = Location(46.5210, 6.5790, "Parc de Mon Repos"),
            status = ToDoStatus.STARTED,
            ownerId = "user")

    val todo3 =
        ToDo(
            uid = "2",
            name = "Read a book",
            description = "Finish reading 'Clean Code'",
            assigneeName = "Charlie",
            dueDate = Timestamp.Companion.fromDate(2025, Calendar.NOVEMBER, 10),
            location = Location(46.5200, 6.5800, "City Library"),
            status = ToDoStatus.ARCHIVED,
            ownerId = "user")

    val invalidTodo =
        ToDo(
            uid = "invalid",
            name = " ",
            description = " ",
            assigneeName = " ",
            dueDate = Timestamp.now(),
            location = null,
            status = ToDoStatus.ARCHIVED,
            ownerId = "user")

    fun Timestamp.toDateString(): String {
      val date = this.toDate()
      val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.ROOT)
      return dateFormat.format(date)
    }

    fun Timestamp.Companion.fromDate(year: Int, month: Int, day: Int): Timestamp {
      val calendar = Calendar.getInstance()
      calendar.set(year, month, day, 0, 0, 0)
      return Timestamp(calendar.time)
    }
  }
}
