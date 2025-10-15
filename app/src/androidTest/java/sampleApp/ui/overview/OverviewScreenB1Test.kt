package sampleApp.ui.overview

// ***************************************************************************** //
// ***                                                                       *** //
// *** THIS FILE WILL BE OVERWRITTEN DURING GRADING. IT SHOULD BE LOCATED IN *** //
// *** `app/src/androidTest/java/com/github/se/bootcamp/ui/overview`.        *** //
// *** DO **NOT** IMPLEMENT YOUR OWN TESTS IN THIS FILE                      *** //
// ***                                                                       *** //
// ***************************************************************************** //

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToNode
import com.android.sample.model.map.Location
import com.android.sample.model.todo.ToDo
import com.android.sample.model.todo.ToDoStatus
import com.android.sample.model.todo.ToDosRepository
import com.android.sample.ui.overview.OverviewScreen
import com.android.sample.ui.overview.OverviewScreenTestTags
import com.android.sample.ui.overview.OverviewViewModel
import com.google.firebase.Timestamp
import java.util.Calendar
import kotlin.collections.last
import org.junit.Rule
import org.junit.Test

class OverviewScreenB1Test {
  @get:Rule val composeTestRule = createComposeRule()

  private class ToDosRepositoryImpl(val todoList: List<ToDo> = listOf<ToDo>()) : ToDosRepository {
    override suspend fun addTodo(toDo: ToDo) {
      return
    }

    override suspend fun editTodo(todoID: String, newValue: ToDo) {
      TODO("Not yet implemented")
    }

    override suspend fun deleteTodo(todoID: String) {
      TODO("Not yet implemented")
    }

    override fun getNewUid(): String {
      return "${todoList.size}"
    }

    override suspend fun getAllTodos(): List<ToDo> {
      return todoList
    }

    override suspend fun getTodo(todoID: String): ToDo {
      TODO("Not yet implemented")
    }
  }

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

  fun setContent(withRepository: ToDosRepository) {
    composeTestRule.setContent {
      OverviewScreen(overviewViewModel = OverviewViewModel(todoRepository = withRepository))
    }
  }

  @Test
  fun testTagsCorrectlySetWhenListIsEmpty() {
    setContent(withRepository = ToDosRepositoryImpl())
    composeTestRule.onNodeWithTag(OverviewScreenTestTags.TODO_LIST).assertIsNotDisplayed()
    composeTestRule.onNodeWithTag(OverviewScreenTestTags.EMPTY_TODO_LIST_MSG).assertIsDisplayed()
  }

  @Test
  fun testTagsCorrectlySetWhenListIsNotEmpty() {
    setContent(withRepository = ToDosRepositoryImpl(listOf(todo1, todo2)))
    composeTestRule.onNodeWithTag(OverviewScreenTestTags.TODO_LIST).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(OverviewScreenTestTags.getTestTagForTodoItem(todo1))
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(OverviewScreenTestTags.getTestTagForTodoItem(todo2))
        .assertIsDisplayed()
  }

  @Test
  fun todoListDisplaysTaskName() {
    val todoList = listOf(todo1)
    setContent(withRepository = ToDosRepositoryImpl(todoList = todoList))
    onTodoItem(todo1, hasText(todo1.name))
  }

  @Test
  fun todoListDisplaysAssigneeName() {
    val todoList = listOf(todo1)
    setContent(withRepository = ToDosRepositoryImpl(todoList = todoList))
    onTodoItem(todo1, hasText(todo1.assigneeName))
  }

  @Test
  fun todoListDisplaysDueDate() {
    val todo = todo1.copy(dueDate = getTimestamp(2023, Calendar.DECEMBER, 25))
    val todoList = listOf(todo)
    val dueDate = "25/12/2023"
    setContent(withRepository = ToDosRepositoryImpl(todoList = todoList))

    onTodoItem(todo, hasText(dueDate))
  }

  @Test
  fun todoListDisplaysStatus() {
    val todoList = listOf(todo1)
    setContent(withRepository = ToDosRepositoryImpl(todoList = todoList))

    onTodoItem(todo1, hasText(todo1.status.toString(), substring = false, ignoreCase = true))
  }

  @Test
  fun todoListDisplaysExistingTodos() {
    val todoList = listOf(todo1, todo2)
    setContent(withRepository = ToDosRepositoryImpl(todoList = todoList))

    // Check that the todo item is displayed correctly.
    todoList.forEach {
      onTodoItem(it, hasText(it.name))
      onTodoItem(it, hasText(it.assigneeName))
      onTodoItem(it, hasText(it.status.toString(), substring = false, ignoreCase = true))
    }
  }

  @Test
  fun dueDateIsCorrectlyFormatted() {
    val todo1 = todo1.copy(uid = "1", dueDate = getTimestamp(2023, Calendar.DECEMBER, 25))
    val todoList = listOf(todo1)
    val dueDate1 = "25/12/2023"
    setContent(withRepository = ToDosRepositoryImpl(todoList = todoList))

    onTodoItem(todo1, hasText(dueDate1))
  }

  @Test
  fun canScrollOnTheTodoList() {
    val todos =
        (1..50).toList<Int>().map { todo1.copy(uid = it.toString(), name = "${todo1.name} #$it") }
    val repository = ToDosRepositoryImpl(todos)
    setContent(withRepository = repository)
    composeTestRule
        .onNodeWithTag(OverviewScreenTestTags.getTestTagForTodoItem(todos.first()))
        .assertIsDisplayed()
    val lastNode =
        composeTestRule.onNodeWithTag(OverviewScreenTestTags.getTestTagForTodoItem(todos.last()))
    lastNode.assertIsNotDisplayed()
    composeTestRule
        .onNodeWithTag(OverviewScreenTestTags.TODO_LIST)
        .performScrollToNode(hasTestTag(OverviewScreenTestTags.getTestTagForTodoItem(todos.last())))
    lastNode.assertIsDisplayed()
  }

  private fun onTodoItem(todo: ToDo, matcher: SemanticsMatcher) {
    composeTestRule
        .onNode(
            hasTestTag(OverviewScreenTestTags.getTestTagForTodoItem(todo))
                .and(hasAnyDescendant(matcher)),
            useUnmergedTree = true)
        .assertIsDisplayed()
  }
}
