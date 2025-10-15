package sampleApp.utils

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToNode
import com.android.sample.model.todo.ToDo
import com.android.sample.model.todo.ToDoStatus
import com.android.sample.model.todo.ToDosRepository
import com.android.sample.ui.overview.OverviewScreenTestTags
import com.google.firebase.Timestamp
import java.util.Calendar
import kotlinx.coroutines.test.runTest
import sampleApp.utils.BootcampTest.Companion.fromDate
import sampleApp.utils.BootcampTest.Companion.todo1
import sampleApp.utils.StateCheckerBootcampTest.ScrollableToDosRepository.Companion.lastTodo

open class StateCheckerBootcampTest(private val bootcampTest: BootcampTest) : BootcampTest {
  override fun createInitializedRepository(): ToDosRepository =
      ScrollableToDosRepository(bootcampTest.createInitializedRepository())

  fun ComposeTestRule.scrollToLastTodo() {
    onNodeWithTag(OverviewScreenTestTags.TODO_LIST)
        .assertIsDisplayed()
        .performScrollToNode(hasTestTag(OverviewScreenTestTags.getTestTagForTodoItem(lastTodo)))
  }

  fun ComposeTestRule.checkTodoListIsStillScrolledDown() {
    onNodeWithTag(OverviewScreenTestTags.getTestTagForTodoItem(ScrollableToDosRepository.firstTodo))
        .assertIsNotDisplayed()
    onNodeWithTag(OverviewScreenTestTags.getTestTagForTodoItem(lastTodo)).assertIsDisplayed()
  }

  fun ComposeTestRule.checkTodoListIsNotScrolledDown() {
    onNodeWithTag(OverviewScreenTestTags.getTestTagForTodoItem(lastTodo)).assertIsNotDisplayed()
    onNodeWithTag(OverviewScreenTestTags.getTestTagForTodoItem(ScrollableToDosRepository.firstTodo))
        .assertIsDisplayed()
  }

  fun ComposeTestRule.scrollToTodoItem(todo: ToDo): SemanticsNodeInteraction =
      onNodeWithTag(OverviewScreenTestTags.TODO_LIST)
          .assertIsDisplayed()
          .performScrollToNode(hasTestTag(OverviewScreenTestTags.getTestTagForTodoItem(todo)))

  class ScrollableToDosRepository(repository: ToDosRepository) : ToDosRepository by repository {
    init {
      runTest {
        addTodo(firstTodo)
        for (i in 3 until 50) {
          addTodo(
              ToDo(
                  uid = i.toString(),
                  name = "Task $i",
                  description = "Description for task $i",
                  assigneeName = "User $i",
                  dueDate = Timestamp.fromDate(2025, Calendar.DECEMBER, i),
                  location = null,
                  status = ToDoStatus.CREATED,
                  ownerId = "user"))
        }
        addTodo(lastTodo)
      }
    }

    companion object {
      val firstTodo = todo1
      val lastTodo =
          ToDo(
              uid = "1000",
              name = "Swent Bootcamp",
              description = "Complete the SE Bootcamp",
              assigneeName = "Me",
              dueDate = Timestamp.Companion.fromDate(2025, Calendar.SEPTEMBER, 29),
              location = null,
              status = ToDoStatus.STARTED,
              ownerId = "user")
    }
  }
}
