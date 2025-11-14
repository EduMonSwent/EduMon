package com.android.sample.ui.session

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertAny
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.android.sample.R
import com.android.sample.data.Priority
import com.android.sample.data.Status
import com.android.sample.data.ToDo
import com.android.sample.session.StudySessionRepository
import com.android.sample.ui.session.components.SessionStatsPanel
import com.android.sample.ui.session.components.SessionStatsPanelTestTags
import com.android.sample.ui.theme.SampleAppTheme
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

// Parts of this code were written using ChatGPT and AndroidStudio Gemini tool.
private class RepoWithSuggestions(private val items: List<ToDo>) : StudySessionRepository {
  override suspend fun saveCompletedSession(session: StudySessionUiState) {
    /* no-op */
  }

  override suspend fun getSuggestedTasks(): List<ToDo> = items
}

class StudySessionScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun studySessionScreen_displaysTitleAndComponents() {
    val items =
        listOf(
            ToDo(
                title = "A",
                dueDate = LocalDate.of(2025, 1, 1),
                priority = Priority.LOW,
                status = Status.TODO))
    val vm = StudySessionViewModel(repository = RepoWithSuggestions(items))

    composeTestRule.setContent {
      SampleAppTheme {
        StudySessionScreen(viewModel = vm, pomodoroViewModel = vm.pomodoroViewModel)
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(StudySessionTestTags.TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(StudySessionTestTags.TASK_LIST).assertIsDisplayed()
    composeTestRule.onNodeWithTag(StudySessionTestTags.TIMER_SECTION).assertIsDisplayed()
    composeTestRule.onNodeWithTag(StudySessionTestTags.STATS_PANEL).assertIsDisplayed()
  }

  @Test
  fun statsPanel_displaysValuesCorrectly() {
    // Arrange
    composeTestRule.setContent {
      SampleAppTheme { // or MaterialTheme { ... }
        SessionStatsPanel(pomodoros = 3, totalMinutes = 75, streak = 5)
      }
    }
    val context = ApplicationProvider.getApplicationContext<Context>()

    // Assert each stat card displays correct text

    // Pomodoros
    composeTestRule
        .onNodeWithTag(SessionStatsPanelTestTags.POMODOROS)
        .onChildren()
        .assertAny(hasText(context.getString(R.string.pomodoros_completed_txt)))
    composeTestRule
        .onNodeWithTag(SessionStatsPanelTestTags.POMODOROS)
        .onChildren()
        .assertAny(hasText("3"))

    // Study time
    composeTestRule
        .onNodeWithTag(SessionStatsPanelTestTags.TIME)
        .onChildren()
        .assertAny(hasText(context.getString(R.string.pomodoro_time_txt)))
    composeTestRule
        .onNodeWithTag(SessionStatsPanelTestTags.TIME)
        .onChildren()
        .assertAny(hasText("75 " + context.getString(R.string.minute)))

    // Streak
    composeTestRule
        .onNodeWithTag(SessionStatsPanelTestTags.STREAK)
        .onChildren()
        .assertAny(hasText(context.getString(R.string.current_streak)))
    composeTestRule
        .onNodeWithTag(SessionStatsPanelTestTags.STREAK)
        .onChildren()
        .assertAny(hasText("5 " + context.getString(R.string.days)))
  }

  @Test
  fun suggestedTasksList_displaysTasksAndHandlesSelection() {
    // Arrange: build ToDo items (titles are what the chips render)
    val tasks =
        listOf(
            ToDo(
                title = "Task A",
                dueDate = LocalDate.of(2025, 1, 1),
                priority = Priority.LOW,
                status = Status.TODO),
            ToDo(
                title = "Task B",
                dueDate = LocalDate.of(2025, 1, 2),
                priority = Priority.MEDIUM,
                status = Status.TODO),
            ToDo(
                title = "Task C",
                dueDate = LocalDate.of(2025, 1, 3),
                priority = Priority.HIGH,
                status = Status.IN_PROGRESS))

    var selected = tasks[1] // preselect "Task B"
    var lastSelectedTitle: String? = selected.title

    composeTestRule.setContent {
      MaterialTheme {
        com.android.sample.ui.session.components.SuggestedTasksList(
            tasks = tasks,
            selectedTask = selected,
            onTaskSelected = {
              selected = it
              lastSelectedTitle = it.title
            })
      }
    }

    // Assert: all titles are shown
    composeTestRule.onNodeWithText("Task A").assertIsDisplayed()
    composeTestRule.onNodeWithText("Task B").assertIsDisplayed()
    composeTestRule.onNodeWithText("Task C").assertIsDisplayed()

    // Act: select "Task C"
    composeTestRule.onNodeWithText("Task C").performClick()
    composeTestRule.waitForIdle()

    // Assert: callback received the new selection
    assertEquals("Task C", lastSelectedTitle)
  }

  @Test
  fun selectedTaskText_displaysWhenTaskIsSelected_realViewModel() {
    val fakeTask =
        ToDo(
            title = "Read Chapter 3",
            dueDate = LocalDate.of(2025, 1, 1),
            priority = Priority.MEDIUM,
            status = Status.TODO)
    val vm = StudySessionViewModel(repository = RepoWithSuggestions(listOf(fakeTask)))
    vm.selectTask(fakeTask)

    composeTestRule.setContent {
      SampleAppTheme {
        StudySessionScreen(viewModel = vm, pomodoroViewModel = vm.pomodoroViewModel)
      }
    }
    composeTestRule.waitForIdle()

    val context = ApplicationProvider.getApplicationContext<Context>()
    val expectedText = context.getString(R.string.selected_task_txt) + " " + fakeTask.title

    // Assert the selected-task text is shown
    composeTestRule
        .onNodeWithTag(StudySessionTestTags.SELECTED_TASK)
        .assertIsDisplayed()
        .assert(hasText(expectedText))
  }

  @Test
  fun deepLinkEventId_preselectsTask() {
    val target =
        com.android.sample.data.ToDo(
            title = "Deep Linked Task",
            priority = com.android.sample.data.Priority.MEDIUM,
            status = com.android.sample.data.Status.TODO,
            dueDate = java.time.LocalDate.of(2025, 1, 10))
    val customRepo =
        object : com.android.sample.repositories.ToDoRepository {
          private val local = com.android.sample.repositories.ToDoRepositoryLocal()
          override val todos = local.todos

          override suspend fun add(todo: com.android.sample.data.ToDo) = local.add(todo)

          override suspend fun update(todo: com.android.sample.data.ToDo) = local.update(todo)

          override suspend fun remove(id: String) = local.remove(id)

          override suspend fun getById(id: String) = local.getById(id)
        }
    // Injecter la tâche
    kotlinx.coroutines.runBlocking { customRepo.add(target) }
    com.android.sample.repositories.ToDoRepositoryProvider.repository = customRepo

    val vm =
        StudySessionViewModel(
            repository =
                object : StudySessionRepository {
                  override suspend fun saveCompletedSession(session: StudySessionUiState) {}

                  override suspend fun getSuggestedTasks(): List<com.android.sample.data.ToDo> =
                      listOf(target)
                })

    composeTestRule.setContent {
      SampleAppTheme {
        StudySessionScreen(
            eventId = target.id, viewModel = vm, pomodoroViewModel = vm.pomodoroViewModel)
      }
    }
    composeTestRule.waitForIdle()

    val context = ApplicationProvider.getApplicationContext<Context>()
    val expected = context.getString(R.string.selected_task_txt) + " " + target.title
    composeTestRule
        .onNodeWithTag(StudySessionTestTags.SELECTED_TASK)
        .assertIsDisplayed()
        .assert(hasText(expected))
  }
}
