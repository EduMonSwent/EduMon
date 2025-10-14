package com.android.sample

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.android.sample.todo.ui.DueDateField
import com.android.sample.todo.ui.TestTags
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test

class DueDateFieldTest {
  @get:Rule val compose = createComposeRule()

  @Test
  fun showsFieldAndInvokesDialogClickPath() {
    var changedTo: LocalDate? = null
    compose.setContent {
      DueDateField(date = LocalDate.of(2025, 5, 20), onDateChange = { changedTo = it })
    }
    compose.onNodeWithTag(TestTags.ChangeDateBtn).performClick()
  }
}
