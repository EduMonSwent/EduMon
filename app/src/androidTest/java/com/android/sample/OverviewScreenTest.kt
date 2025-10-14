package com.android.sample

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.android.sample.todo.Priority
import com.android.sample.todo.Status
import com.android.sample.todo.ToDo
import com.android.sample.todo.ToDoRepositoryLocal
import com.android.sample.todo.ToDoRepositoryProvider
import com.android.sample.todo.ui.OverviewScreen
import com.android.sample.todo.ui.TestTags
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Assert
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
            priority = Priority.MEDIUM,
            status = Status.TODO)
    val b =
        ToDo(
            id = "B",
            title = "B",
            dueDate = LocalDate.now(),
            priority = Priority.MEDIUM,
            status = Status.IN_PROGRESS)
    runBlocking {
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
    val updatedA = runBlocking { repo.getById("A")!! }
    val removedB = runBlocking { repo.getById("B") }
    Assert.assertEquals(Status.IN_PROGRESS, updatedA.status)
    Assert.assertEquals(null, removedB)
  }
}
