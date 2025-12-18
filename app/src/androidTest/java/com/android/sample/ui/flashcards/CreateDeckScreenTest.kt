package com.android.sample.ui.flashcards

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.repos_providors.FakeRepositories
import com.android.sample.ui.flashcards.data.InMemoryFlashcardsRepository
import com.android.sample.ui.theme.EduMonTheme
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.flow.first
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CreateDeckScreenTest {

  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  private fun cardsCount(vm: CreateDeckViewModel): Int =
      kotlinx.coroutines.runBlocking { vm.cards.first().size }

  @Test
  fun renders_staticElements() {
    val vm = CreateDeckViewModel(FakeRepositories.toDoRepository, InMemoryFlashcardsRepository)
    composeRule.setContent {
      EduMonTheme { CreateDeckScreen(onSaved = {}, onCancel = {}, vm = vm) }
    }

    composeRule.onNodeWithText("New Deck").assertExists()
    composeRule.onNodeWithText("Cancel").assertExists()
    composeRule.onNodeWithText("Title").assertExists()
    composeRule.onNodeWithText("Description").assertExists()
    composeRule.onNodeWithText("Cards").assertExists()
    composeRule.onNodeWithText("Add card").assertExists()
    composeRule.onNodeWithText("Save Deck").assertExists()
  }

  @Test
  fun addCard_click_addsOne() {
    val vm = CreateDeckViewModel(FakeRepositories.toDoRepository, InMemoryFlashcardsRepository)
    composeRule.setContent {
      EduMonTheme { CreateDeckScreen(onSaved = {}, onCancel = {}, vm = vm) }
    }

    // Initial state should be empty
    composeRule.runOnIdle { assert(cardsCount(vm) == 0) }

    composeRule.onNodeWithText("Add card").performClick()

    // Wait until VM reflects the change (CI-safe)
    composeRule.waitUntil(timeoutMillis = 10_000) { cardsCount(vm) == 1 }

    // UI sanity: at least one Remove exists (don't assert exact count; LazyColumn may virtualize)
    composeRule.waitUntil(timeoutMillis = 10_000) {
      composeRule
          .onAllNodesWithText("Remove", useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
  }

  @Test
  fun removeCard_click_removesOne() {
    val vm =
        CreateDeckViewModel(FakeRepositories.toDoRepository, InMemoryFlashcardsRepository).apply {
          addEmptyCard()
          addEmptyCard()
        }

    composeRule.setContent {
      EduMonTheme { CreateDeckScreen(onSaved = {}, onCancel = {}, vm = vm) }
    }

    // Verify VM starts with 2 cards (reliable, not dependent on LazyColumn composition)
    composeRule.runOnIdle { assert(cardsCount(vm) == 2) }

    // Wait until at least one "Remove" action is available in the semantics tree
    composeRule.waitUntil(timeoutMillis = 10_000) {
      composeRule
          .onAllNodesWithText("Remove", useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Click the first visible remove (even if only one item is composed on CI)
    composeRule.onAllNodesWithText("Remove", useUnmergedTree = true)[0].performClick()

    // Wait until VM reflects the removal
    composeRule.waitUntil(timeoutMillis = 10_000) { cardsCount(vm) == 1 }

    // UI sanity: still at least one Remove exists (one card remains)
    composeRule.waitUntil(timeoutMillis = 10_000) {
      composeRule
          .onAllNodesWithText("Remove", useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
  }

  @Test
  fun saveDeck_callsOnSaved_whenValidCard() {
    val vm =
        CreateDeckViewModel(FakeRepositories.toDoRepository, InMemoryFlashcardsRepository).apply {
          addEmptyCard()
          updateCard(0, question = "Q?", answer = "A!")
          setTitle("Algorithms")
          setDescription("Basics")
        }
    val savedId = AtomicReference<String?>()

    composeRule.setContent {
      EduMonTheme { CreateDeckScreen(onSaved = { id -> savedId.set(id) }, onCancel = {}, vm = vm) }
    }

    composeRule.onNodeWithText("Save Deck").performClick()
    composeRule.waitUntil(timeoutMillis = 10_000) { savedId.get() != null }

    assert(!savedId.get().isNullOrBlank())
  }

  @Test
  fun cancel_callsOnCancel() {
    val cancelled = AtomicBoolean(false)
    val vm = CreateDeckViewModel(FakeRepositories.toDoRepository, InMemoryFlashcardsRepository)

    composeRule.setContent {
      EduMonTheme { CreateDeckScreen(onSaved = {}, onCancel = { cancelled.set(true) }, vm = vm) }
    }

    composeRule.onNodeWithText("Cancel").performClick()
    composeRule.waitUntil(5_000) { cancelled.get() }
    assert(cancelled.get())
  }

  @Test
  fun saveDeck_trimsTitleAndDescription_andFiltersBlankCards() {
    val sharedRepo = InMemoryFlashcardsRepository

    val vm =
        CreateDeckViewModel(FakeRepositories.toDoRepository, sharedRepo).apply {
          addEmptyCard() // filled
          addEmptyCard() // blank -> should be filtered out
          updateCard(0, question = "  Q?  ", answer = "  A!  ")
          setTitle("  Title  ")
          setDescription("  Desc  ")
        }

    val savedId = AtomicReference<String?>()

    composeRule.setContent {
      EduMonTheme { CreateDeckScreen(onSaved = { savedId.set(it) }, onCancel = {}, vm = vm) }
    }

    composeRule.onNodeWithText("Save Deck").performClick()
    composeRule.waitUntil(10_000) { savedId.get() != null }

    val id = requireNotNull(savedId.get())
    val deck = kotlinx.coroutines.runBlocking { sharedRepo.observeDeck(id).first() }
    requireNotNull(deck)

    assert(deck.title == "Title")
    assert(deck.description == "Desc")
    assert(deck.cards.size == 1)
    assert(deck.cards[0].question == "  Q?  ")
    assert(deck.cards[0].answer == "  A!  ")
  }
}
