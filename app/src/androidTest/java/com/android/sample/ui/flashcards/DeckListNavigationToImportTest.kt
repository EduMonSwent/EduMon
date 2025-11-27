package com.android.sample.ui.flashcards

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.android.sample.ui.theme.EduMonTheme
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.*

class DeckListNavigationTest {

  @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun fab_opens_import_screen() {
    var openedImport = false

    compose.setContent {
      EduMonTheme {
        DeckListScreen(
            onCreateDeck = {},
            onStudyDeck = {},
            onImportDeck = { openedImport = true } // <- what FAB triggers
            )
      }
    }

    // Click the FAB (pink share icon)
    compose.onNodeWithContentDescription("Import deck").performClick()

    assert(openedImport)
  }

  @Test
  fun deckList_navigatesToImport() {
    val navigateImport = AtomicBoolean(false)

    compose.setContent {
      EduMonTheme {
        DeckListScreen(
            onCreateDeck = {}, onStudyDeck = {}, onImportDeck = { navigateImport.set(true) })
      }
    }

    compose.onNodeWithContentDescription("Import deck").performClick()

    compose.waitUntil { navigateImport.get() }

    Assert.assertTrue(navigateImport.get())
  }
}
