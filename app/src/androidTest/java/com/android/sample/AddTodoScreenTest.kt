package com.android.sample

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.android.sample.todo.ToDoRepositoryProvider
import com.android.sample.todo.ui.AddToDoScreen
import com.android.sample.todo.ui.TestTags
import java.time.LocalDate
import org.junit.*

class AddToDoScreenTest {
  @get:Rule val compose = createComposeRule()

  private val realRepo by lazy { ToDoRepositoryProvider.repository }
  private val fakeRepo = FakeToDoRepository()

  @Before
  fun swapRepo() {
    ToDoRepositoryProvider.repository = fakeRepo
  }

  @After
  fun restoreRepo() {
    ToDoRepositoryProvider.repository = realRepo
  }

  @Test
  fun saveCreatesItem_andCallsBack() =
      kotlinx.coroutines.test.runTest {
        var back = false
        compose.setContent { MaterialTheme { AddToDoScreen(onBack = { back = true }) } }

        // Title (required)
        compose
            .onNodeWithTag(TestTags.TitleField, useUnmergedTree = true)
            .assertExists()
            .performClick()
            .performTextReplacement("Finish lab 4")

        // Optional fields are hidden initially on Add — toggle them
        compose.onNodeWithTag(TestTags.OptionalToggle).performClick()

        // Fill optional fields
        compose
            .onNodeWithTag(TestTags.LocationField, useUnmergedTree = true)
            .performClick()
            .performTextInput("INM 202")
        compose
            .onNodeWithTag(TestTags.LinksField, useUnmergedTree = true)
            .performClick()
            .performTextInput("https://docs, https://moodle")
        compose
            .onNodeWithTag(TestTags.NoteField, useUnmergedTree = true)
            .performClick()
            .performTextInput("Pair with Lea")
        compose.onNodeWithTag(TestTags.NotificationsSwitch).performClick()

        // Save
        compose.onNodeWithTag(TestTags.SaveButton).assertIsEnabled().performClick()

        val saved = fakeRepo.todos.value.single()
        Assert.assertEquals("Finish lab 4", saved.title)
        Assert.assertEquals(LocalDate.now(), saved.dueDate) // default today
        Assert.assertEquals("INM 202", saved.location)
        Assert.assertEquals(listOf("https://docs", "https://moodle"), saved.links)
        Assert.assertTrue(saved.note!!.contains("Lea"))
        Assert.assertTrue(saved.notificationsEnabled)
        Assert.assertTrue(back)
      }
}
