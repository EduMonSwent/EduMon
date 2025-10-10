package com.android.sample.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.android.sample.todo.ui.OverviewScreen
import com.android.sample.todo.ui.TestTags
import org.junit.Rule
import org.junit.Test

class OverviewScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun showsEmptyStateWhenNoTodos() {
    composeTestRule.setContent { OverviewScreen(onAddClicked = {}, onEditClicked = {}) }
    composeTestRule.onNodeWithText("No tasks yet. Tap + to add one.").assertExists()
  }

  @Test
  fun fabExistsAndClickable() {
    composeTestRule.setContent { OverviewScreen(onAddClicked = {}, onEditClicked = {}) }
    composeTestRule.onNodeWithTag(TestTags.FabAdd).assertExists().performClick()
  }
}
