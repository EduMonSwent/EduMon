package com.android.sample.schedule

import androidx.activity.ComponentActivity
import androidx.compose.runtime.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.android.sample.ui.schedule.ThemedTabRow
import junit.framework.TestCase.assertEquals
import org.junit.Rule
import org.junit.Test

class ThemedTabRowAndroidTest {

  @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun clickingTabs_updatesSelectedIndex() {
    var selected by mutableStateOf(0)
    val labels = listOf("Day", "Week", "Month", "Agenda")

    rule.setContent {
      ThemedTabRow(selected = selected, onSelected = { selected = it }, labels = labels)
    }

    rule.onNodeWithText("Week").performClick()
    assertEquals(1, selected)

    rule.onNodeWithText("Month").performClick()
    assertEquals(2, selected)
  }
}
