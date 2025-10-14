package com.android.sample

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.android.sample.todo.*
import com.android.sample.todo.ui.OverviewScreen
import com.android.sample.todo.ui.TestTags
import org.junit.*

class OverviewScreenTest {
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
  fun emptyState_andFabVisible() {
    compose.setContent { MaterialTheme { OverviewScreen(onAddClicked = {}, onEditClicked = {}) } }
    compose.onNodeWithTag(TestTags.OverviewScreen).assertExists()
    compose.onNodeWithText("No tasks yet. Tap + to add one.").assertExists()
    compose.onNodeWithTag(TestTags.FabAdd).assertExists()
  }
}
