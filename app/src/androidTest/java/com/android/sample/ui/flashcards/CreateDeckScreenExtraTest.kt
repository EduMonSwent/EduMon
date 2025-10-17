package com.android.sample.ui.flashcards

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.ui.theme.EduMonTheme
import java.util.concurrent.atomic.AtomicReference
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Additional coverage tests for CreateDeckScreen save behavior. */
@RunWith(AndroidJUnit4::class)
class CreateDeckScreenExtraTest {

  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun saveDeck_trimsTitleAndDescription_andFiltersBlankCards() {
    val vm =
        CreateDeckViewModel().apply {
          addEmptyCard() // will be filled
          addEmptyCard() // remains blank and should be filtered
          updateCard(0, question = "  Q?  ", answer = "  A!  ")
          setTitle("  Title  ")
          setDescription("  Desc  ")
        }
    val savedId = AtomicReference<String?>()

    composeRule.setContent {
      EduMonTheme { CreateDeckScreen(onSaved = { savedId.set(it) }, onCancel = {}, vm = vm) }
    }

    composeRule.onNodeWithText("Save Deck").performClick()
    composeRule.waitUntil(3_000) { savedId.get() != null }

    val id = requireNotNull(savedId.get())
    val deck = com.android.sample.ui.flashcards.data.InMemoryFlashcardsRepository.deck(id)
    requireNotNull(deck)
    // Title/description are trimmed in VM before save
    assert(deck.title == "Title")
    assert(deck.description == "Desc")
    // Blank card filtered out -> only one card saved
    assert(deck.cards.size == 1)
    // VM doesn't trim card fields; it just filters blanks
    assert(deck.cards[0].question == "  Q?  ")
    assert(deck.cards[0].answer == "  A!  ")
  }
}
