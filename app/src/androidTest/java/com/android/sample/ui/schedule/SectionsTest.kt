package com.android.sample.ui.schedule

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class SectionsTest {

  @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun sectionHeader_renders_text() {
    compose.setContent { SectionHeader("Most important this month") }
    compose.onNodeWithText("Most important this month").assertExists()
  }

  @Test
  fun framedSection_renders_slot_content() {
    compose.setContent { MaterialTheme { FramedSection { SectionHeader("Inside Frame") } } }
    compose.onNodeWithText("Inside Frame").assertExists()
  }

  @Test
  fun themedTabRow_renders_labels_and_updates_selected_on_click() {
    val labels = listOf("Day", "Week", "Month", "Agenda")

    compose.setContent {
      MaterialTheme {
        var selected by remember { mutableStateOf(0) }
        // Render the tabs and also a small marker showing current selection
        ThemedTabRow(selected = selected, onSelected = { selected = it }, labels = labels)
        Text("SEL=$selected")
      }
    }

    // all labels present
    labels.forEach { label -> compose.onNodeWithText(label, ignoreCase = true).assertExists() }

    // Click "Month" (index 2) and verify selection marker changed
    compose.onNodeWithText("Month", ignoreCase = true).performClick()
    compose.onNodeWithText("SEL=2").assertExists()

    // Click "Agenda" (index 3) and verify again
    compose.onNodeWithText("Agenda", ignoreCase = true).performClick()
    compose.onNodeWithText("SEL=3").assertExists()
  }
}
