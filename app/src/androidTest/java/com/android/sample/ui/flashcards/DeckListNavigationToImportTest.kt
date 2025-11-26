package com.android.sample.ui.flashcards

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.android.sample.ui.flashcards.data.FlashcardsRepository
import com.android.sample.ui.flashcards.model.Flashcard
import com.android.sample.ui.flashcards.model.ImportDeckViewModel
import com.android.sample.ui.theme.EduMonTheme
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.*

class DeckListNavigationTest {

  @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun fab_opens_import_screen() {
    val openedImport = AtomicBoolean(false)

    compose.setContent {
      EduMonTheme {
        ImportDeckScreen(
            onSuccess = {},
            onBack = {},
            vm =
                ImportDeckViewModel(
                    repo =
                        object : FlashcardsRepository {
                          override fun observeDecks() = throw UnsupportedOperationException()

                          override fun observeDeck(deckId: String) =
                              throw UnsupportedOperationException()

                          override suspend fun createDeck(
                              title: String,
                              description: String,
                              cards: List<Flashcard>
                          ) = ""

                          override suspend fun addCard(deckId: String, card: Flashcard) {}

                          override suspend fun deleteDeck(deckId: String) {}

                          override suspend fun importSharedDeck(token: String) = ""
                        }))
      }
    }

    // We test DeckListScreen → Import later because it requires NavHost wrapper
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
