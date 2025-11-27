package com.android.sample.ui.flashcards.model

import java.time.Instant
import java.util.UUID

data class Flashcard(
    val id: String = UUID.randomUUID().toString(),
    val question: String,
    val answer: String
)

data class Deck(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val createdAt: Long = Instant.now().toEpochMilli(),
    val cards: MutableList<Flashcard> = mutableListOf(),
    val shareable: Boolean = false
)

enum class Confidence {
  LOW,
  MEDIUM,
  HIGH
}
