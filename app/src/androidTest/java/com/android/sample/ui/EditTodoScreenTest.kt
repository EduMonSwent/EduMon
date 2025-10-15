package com.android.sample.todo.ui

import androidx.compose.ui.test.junit4.createComposeRule
import com.android.sample.todo.ToDo
import com.android.sample.todo.testutils.ToDoTest
import com.android.sample.ui.todo.EditToDoScreen
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class EditToDoScreenTest : ToDoTest {

  @get:Rule val compose = createComposeRule()
  private lateinit var existing: ToDo

  @Before
  override fun setUpProvider() {
    super.setUpProvider()
    existing = sampleToDo(id = "E1", title = "Orig")
    runBlocking { repository.add(existing) }
  }

  @Test
  fun edit_title_and_links_then_save_updates_repo() {
    compose.setContent { EditToDoScreen(id = "E1", onBack = {}) }

    compose.clearAndEnterTitle(" Updated ")
    compose.enterLinks("x.com, y.com")
    compose.clickSave()

    runBlocking {
      val updated = repository.getById("E1")!!
      assertEquals("Updated", updated.title)
      assertEquals(listOf("x.com", "y.com"), updated.links)
    }
  }
}
