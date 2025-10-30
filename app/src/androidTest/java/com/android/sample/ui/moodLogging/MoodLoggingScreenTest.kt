package com.android.sample.ui.moodLogging

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.android.sample.data.MoodEntry
import com.android.sample.ui.mood.ChartMode
import com.android.sample.ui.mood.MoodLoggingScreen
import com.android.sample.ui.mood.MoodUiState
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test

class MoodLoggingScreenTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun selectEmoji_editNote_switchTabs_and_save_calls_callbacks() {
    var selectedMood = -1
    var note: String? = null
    var mode = ChartMode.WEEK
    var saved = false

    val today = LocalDate.now()
    val week =
        (0..6).map { i ->
          val d = today.minusDays((6 - i).toLong())
          MoodEntry(d.toEpochDay(), mood = (i % 5) + 1, note = "n$i")
        }
    val month =
        (0..29).map { i ->
          val d = today.minusDays((29 - i).toLong())
          MoodEntry(d.toEpochDay(), mood = (i % 5) + 1, note = "m$i")
        }

    composeRule.setContent {
      MoodLoggingScreen(
          state =
              MoodUiState(
                  today = today,
                  selectedMood = 3,
                  note = "",
                  existingToday = null,
                  last7Days = week,
                  monthEntries = month,
                  canEditToday = true,
                  chartMode = mode),
          onSelectMood = { selectedMood = it },
          onNoteChanged = { note = it },
          onSave = { saved = true },
          onChartMode = { mode = it })
    }

    // Click emoji with tag "mood_4"
    composeRule.onNodeWithTag("mood_4").performClick()

    // Type into the note field
    composeRule.onNodeWithTag("noteField").performTextInput("hello")

    // Switch tab to Month
    composeRule.onNodeWithTag("tab_month").performClick()

    // Save
    composeRule.onNodeWithTag("save_button").assertIsEnabled().performClick()

    // Assertions on callback side-effects
    assert(selectedMood in 1..5)
    assert(note == "hello")
    assert(mode == ChartMode.MONTH)
    assert(saved)
  }
}
