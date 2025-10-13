package com.android.sample.ui.games

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.TravelExplore
import org.junit.Assert.*
import org.junit.Test

class MemoryGameLogicTest {

    @Test
    fun generateCards_creates18UniqueCards() {
        val icons = listOf(
            Icons.Filled.School,
            Icons.Filled.Book,
            Icons.Filled.Psychology,
            Icons.Filled.Lightbulb,
            Icons.Filled.AutoAwesome,
            Icons.Filled.Science,
            Icons.Filled.SportsEsports,
            Icons.Filled.TravelExplore,
            Icons.Filled.Bolt
        )
        val cards = generateCards(icons)
        assertEquals(18, cards.size)
    }


    @Test
    fun memoryCard_equalityAndCopyWork() {
        val card = MemoryCard(1, Icons.Default.Book, isFlipped = true)
        val copy = card.copy(isMatched = true)
        assertEquals(1, copy.id)
        assertTrue(copy.isMatched)
        assertTrue(card.isFlipped)
    }

    @Test
    fun timesOperator_duplicatesListCorrectly() {
        val icons = listOf("A", "B")
        val result = icons * 2
        assertEquals(4, result.size)
        assertTrue(result.containsAll(listOf("A", "B")))
    }
}
