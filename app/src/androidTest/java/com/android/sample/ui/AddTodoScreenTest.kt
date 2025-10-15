package com.android.sample.todo.ui

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.android.sample.todo.ToDo
import com.android.sample.todo.testutils.ToDoTest
import com.android.sample.ui.todo.AddToDoScreen
import com.android.sample.ui.todo.TestTags
import junit.framework.TestCase.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AddToDoScreenTest : ToDoTest {

  @get:Rule val compose = createComposeRule()

  @Before override fun setUpProvider() = super.setUpProvider()

  @Test
  fun save_disabled_when_blank_then_enabled_and_persists() {
    compose.setContent { AddToDoScreen(onBack = {}) }

    compose.onNodeWithTag(TestTags.SaveButton).assertIsNotEnabled()

    compose.enterTitle("New Task")
    compose.openOptionalSection()
    compose.enterLocation("Library")
    compose.enterLinks("a.com, b.com")
    compose.enterNote("bring book")
    compose.toggleNotifications()
    compose.clickSave()

    val list = (repository.todos as kotlinx.coroutines.flow.MutableStateFlow<List<ToDo>>).value
    assertEquals(1, list.size)
    assertEquals("New Task", list.first().title)
  }
}
