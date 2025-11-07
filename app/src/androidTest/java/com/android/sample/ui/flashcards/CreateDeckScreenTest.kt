package com.android.sample.ui.flashcards

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.sample.ui.flashcards.data.FlashcardsRepositoryProvider
import com.android.sample.ui.theme.EduMonTheme
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.flow.first
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented UI tests for [CreateDeckScreen]. These tests host the composable directly inside a
 * blank [ComponentActivity] and avoid touching MainActivity. No text input is performed; instead,
 * we drive the underlying [CreateDeckViewModel] to keep the tests stable under CI.
 */
@RunWith(AndroidJUnit4::class)
class CreateDeckScreenTest {

  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  @Before
  fun setup() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    FlashcardsRepositoryProvider.init(context)
  }

  @Test
  fun renders_staticElements() {
    val vm = CreateDeckViewModel()
    composeRule.setContent {
      EduMonTheme { CreateDeckScreen(onSaved = {}, onCancel = {}, vm = vm) }
    }

    composeRule.onNodeWithText("New Deck").assertIsDisplayed()
    composeRule.onNodeWithText("Cancel").assertIsDisplayed()
    composeRule.onNodeWithText("Title").assertIsDisplayed()
    composeRule.onNodeWithText("Description").assertIsDisplayed()
    composeRule.onNodeWithText("Cards").assertIsDisplayed()
    composeRule.onNodeWithText("Add card").assertIsDisplayed()
    composeRule.onNodeWithText("Save Deck").assertIsDisplayed()
  }

  @Test
  fun addCard_click_addsOne() {
    val vm = CreateDeckViewModel()
    composeRule.setContent {
      EduMonTheme { CreateDeckScreen(onSaved = {}, onCancel = {}, vm = vm) }
    }

    composeRule.onAllNodesWithText("Remove").assertCountEquals(0)
    composeRule.onNodeWithText("Add card").performClick()
    composeRule.onAllNodesWithText("Remove").assertCountEquals(1)
  }

  @Test
  fun removeCard_click_removesOne() {
    val vm =
        CreateDeckViewModel().apply {
          addEmptyCard()
          addEmptyCard()
        }
    composeRule.setContent {
      EduMonTheme { CreateDeckScreen(onSaved = {}, onCancel = {}, vm = vm) }
    }

    // Two cards initially
    composeRule.onAllNodesWithText("Remove").assertCountEquals(2)
    // Remove the first card
    composeRule.onAllNodesWithText("Remove")[0].performClick()
    // One card remains
    composeRule.onAllNodesWithText("Remove").assertCountEquals(1)
  }

  @Test
  fun saveDeck_callsOnSaved_whenValidCard() {
    val vm =
        CreateDeckViewModel().apply {
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

    composeRule.waitUntil(timeoutMillis = 3_000) { savedId.get() != null }
    // After save, the callback receives a non-empty deck id
    assert(!savedId.get().isNullOrBlank())
  }

  @Test
  fun cancel_callsOnCancel() {
    val cancelled = AtomicBoolean(false)
    val vm = CreateDeckViewModel()

    composeRule.setContent {
      EduMonTheme { CreateDeckScreen(onSaved = {}, onCancel = { cancelled.set(true) }, vm = vm) }
    }

    composeRule.onNodeWithText("Cancel").performClick()
    composeRule.waitUntil(1_000) { cancelled.get() }
    assert(cancelled.get())
  }

  @Test
  fun saveDeck_trimsTitleAndDescription_andFiltersBlankCards() {
    // Use the real VM that defaults to the provider-backed repo
    val vm =
        CreateDeckViewModel().apply {
          addEmptyCard() // will be filled
          addEmptyCard() // remains blank and should be filtered
          updateCard(0, question = "  Q?  ", answer = "  A!  ")
          setTitle("  Title  ")
          setDescription("  Desc  ")
        }

    val savedId = java.util.concurrent.atomic.AtomicReference<String?>()

    composeRule.setContent {
      EduMonTheme { CreateDeckScreen(onSaved = { savedId.set(it) }, onCancel = {}, vm = vm) }
    }

    // Tap Save
    composeRule.onNodeWithText("Save Deck").performClick()

    // Wait until the VM calls onSaved(id)
    composeRule.waitUntil(3_000) { savedId.get() != null }

    val id = requireNotNull(savedId.get())

    val repo = FlashcardsRepositoryProvider.repository
    val deck = kotlinx.coroutines.runBlocking { repo.observeDeck(id).first() }
    requireNotNull(deck)

    assert(deck.title == "Title")
    assert(deck.description == "Desc")

    assert(deck.cards.size == 1)

    assert(deck.cards[0].question == "  Q?  ")
    assert(deck.cards[0].answer == "  A!  ")
  }
}
