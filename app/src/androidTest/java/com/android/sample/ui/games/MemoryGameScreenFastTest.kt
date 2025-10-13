package com.android.sample.ui.games

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.*
import org.junit.Rule
import org.junit.Test

// The assistance of an AI tool (ChatGPT) was solicited in writing this test file.

class MemoryGameScreenFastTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsWinOverlay_whenInitialWinTrue() {
        val cards = List(18) { MemoryCard(it, Icons.Default.Book, isMatched = true) }

        composeRule.setContent {
            MemoryGameScreenTestable(initialCards = cards, initialWin = true)
        }

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("Well done!").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithText("Well done!").assertExists()
        composeRule.onAllNodes(hasText("Score:", substring = true))[1].assertExists()
    }


    @Test
    fun restartButton_resetsStateInstantly() {
        val cards = List(18) { MemoryCard(it, Icons.Default.Book, isMatched = true) }

        composeRule.setContent {
            MemoryGameScreenTestable(initialCards = cards, initialWin = true)
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithText("Restart").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Memory Game").assertExists()
    }
}
