package com.android.sample.schedule

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.android.sample.ui.schedule.SectionBox
import org.junit.Rule
import org.junit.Test

/**
 * Instrumented UI tests for SectionBox composable.
 *
 * Verifies that:
 * 1. The title text is shown when no header is provided.
 * 2. The header content replaces the title when provided.
 * 3. The inner content is always displayed.
 */
class SectionBoxTest {

  @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

  // --- Test 1: Title branch ---
  @Test
  fun sectionBox_showsTitle_whenNoHeaderProvided() {
    rule.setContent { SectionBox(title = "Tasks for Today") { Text("Inner content") } }

    // Title text visible
    rule.onNodeWithText("Tasks for Today").assertIsDisplayed()
    // Inner content visible
    rule.onNodeWithText("Inner content").assertIsDisplayed()
  }

  // --- Test 2: Header branch ---
  @Test
  fun sectionBox_showsHeader_whenHeaderProvided() {
    rule.setContent { SectionBox(header = { Text("Custom Header") }) { Text("Body content") } }

    // Custom header replaces title
    rule.onNodeWithText("Custom Header").assertIsDisplayed()
    // Body content still shown
    rule.onNodeWithText("Body content").assertIsDisplayed()
  }

  // --- Test 3: Visual style basics (rounded shape present) ---
  // This one just checks both texts exist; we can’t directly assert RoundedCornerShape via
  // semantics.
  @Test
  fun sectionBox_rendersRoundedSurface_andContent() {
    rule.setContent { SectionBox(title = "Rounded Card") { Text("Hello world") } }

    // Both elements visible
    rule.onNodeWithText("Rounded Card").assertIsDisplayed()
    rule.onNodeWithText("Hello world").assertIsDisplayed()
  }
}
