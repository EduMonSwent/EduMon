package com.android.sample.todo.ui

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.android.sample.todo.ToDo
import com.android.sample.todo.ToDoRepositoryLocal
import com.android.sample.todo.ToDoRepositoryProvider
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AddToDoScreenTest {

  @get:Rule val compose = createComposeRule()

  @Before
  fun setup() {
    ToDoRepositoryProvider.repository = ToDoRepositoryLocal()
  }

  @Test
  fun save_disabled_then_enabled_and_persists() {
    compose.setContent { AddToDoScreen(onBack = {}) }

    // bouton désactivé au début
    compose.onNodeWithTag(TestTags.SaveButton).assertIsNotEnabled()

    // saisir un titre + quelques champs optionnels
    compose.onNodeWithTag(TestTags.TitleField).performTextInput("New Task")
    compose.onNodeWithTag(TestTags.OptionalToggle).performClick()
    compose.onNodeWithTag(TestTags.LinksField).performTextInput("a.com, b.com")

    // devient activé
    compose.onNodeWithTag(TestTags.SaveButton).assertIsEnabled().performClick()

    // synchroniser et vérifier le dépôt
    compose.runOnIdle {}
    val list =
        (ToDoRepositoryProvider.repository.todos
                as kotlinx.coroutines.flow.MutableStateFlow<List<ToDo>>)
            .value
    assertEquals(1, list.size)
    assertEquals("New Task", list.first().title)
  }
}
