package com.android.sample.todo.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.android.sample.todo.Status
import com.android.sample.todo.ToDo
import com.android.sample.todo.ToDoRepositoryLocal
import com.android.sample.todo.ToDoRepositoryProvider
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class OverviewScreenTest {

  @get:Rule val compose = createComposeRule()

  @Before
  fun setup() {
    ToDoRepositoryProvider.repository = ToDoRepositoryLocal()
  }

  @Test
  fun show_items_toggle_status_delete_item() {
    // seed 2 tâches
    val a =
        ToDo(
            id = "A",
            title = "A",
            dueDate = LocalDate.now(),
            priority = com.android.sample.todo.Priority.MEDIUM,
            status = Status.TODO)
    val b =
        ToDo(
            id = "B",
            title = "B",
            dueDate = LocalDate.now(),
            priority = com.android.sample.todo.Priority.MEDIUM,
            status = Status.IN_PROGRESS)
    kotlinx.coroutines.runBlocking {
      ToDoRepositoryProvider.repository.add(a)
      ToDoRepositoryProvider.repository.add(b)
    }

    compose.setContent { OverviewScreen(onAddClicked = {}, onEditClicked = {}) }

    // attendre l’affichage minimal (card A)
    compose.onNodeWithTag(TestTags.card("A")).assertExists()

    // basculer le statut de A (TODO -> IN_PROGRESS)
    compose.onNodeWithTag(TestTags.status("A")).performClick()
    // supprimer B
    compose.onNodeWithTag(TestTags.delete("B")).performClick()

    compose.runOnIdle {}
    val repo = ToDoRepositoryProvider.repository
    val updatedA = kotlinx.coroutines.runBlocking { repo.getById("A")!! }
    val removedB = kotlinx.coroutines.runBlocking { repo.getById("B") }
    assertEquals(Status.IN_PROGRESS, updatedA.status)
    assertEquals(null, removedB)
  }
}
