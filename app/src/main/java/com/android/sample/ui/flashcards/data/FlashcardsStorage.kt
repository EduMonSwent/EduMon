package com.android.sample.ui.flashcards.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

// One preferences DataStore for flashcards
private val Context.flashcardsDataStore by preferencesDataStore(name = "flashcards_store")

private object Keys {
  val DECKS_JSON: Preferences.Key<String> = stringPreferencesKey("decks_json")
}

/**
 * Low-level storage for flashcards using a single JSON blob. Schema (versioned): { "version": 1,
 * "decks":
 * [ { "id": "...", "title": "...", "description": "...", "createdAt": 0, "cards": [ { "q": "...", "a": "..." }, ... ]
 * } ] }
 */
class FlashcardsStorage(private val context: Context) {

  fun observeAll(): Flow<List<DeckDTO>> =
      context.flashcardsDataStore.data.map { prefs -> parseDecks(prefs[Keys.DECKS_JSON]) }

  suspend fun readAllOnce(): List<DeckDTO> =
      parseDecks(context.flashcardsDataStore.data.first()[Keys.DECKS_JSON])

  suspend fun upsertDeck(deck: DeckDTO): String {
    context.flashcardsDataStore.edit { prefs ->
      val current = parseDecks(prefs[Keys.DECKS_JSON]).toMutableList()
      val idx = current.indexOfFirst { it.id == deck.id }
      if (idx >= 0) current[idx] = deck else current += deck
      prefs[Keys.DECKS_JSON] = encodeDecks(current)
    }
    return deck.id
  }

  suspend fun addCard(deckId: String, card: CardDTO) {
    context.flashcardsDataStore.edit { prefs ->
      val current = parseDecks(prefs[Keys.DECKS_JSON]).toMutableList()
      val idx = current.indexOfFirst { it.id == deckId }
      if (idx >= 0) {
        val d = current[idx]
        current[idx] = d.copy(cards = d.cards + card)
        prefs[Keys.DECKS_JSON] = encodeDecks(current)
      }
    }
  }

  suspend fun deleteDeck(deckId: String) {
    context.flashcardsDataStore.edit { prefs ->
      val current = parseDecks(prefs[Keys.DECKS_JSON]).toMutableList()
      val idx = current.indexOfFirst { it.id == deckId }
      if (idx >= 0) {
        current.removeAt(idx)
        prefs[Keys.DECKS_JSON] = encodeDecks(current)
      }
    }
  }
}

/* ---------- DTOs (storage layer) ---------- */
data class DeckDTO(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val createdAt: Long = System.currentTimeMillis(),
    val cards: List<CardDTO> = emptyList()
)

data class CardDTO(val q: String, val a: String)

/* ---------- JSON encode/decode (versioned) ---------- */

private fun parseDecks(raw: String?): List<DeckDTO> {
  return try {
    if (raw.isNullOrBlank()) return emptyList()
    val root = JSONObject(raw)
    val version = root.optInt("version", 1)
    if (version != 1) return emptyList() // simple guard for future changes
    val arr = root.optJSONArray("decks") ?: JSONArray()
    List(arr.length()) { i ->
      val d = arr.getJSONObject(i)
      DeckDTO(
          id = d.optString("id"),
          title = d.optString("title"),
          description = d.optString("description"),
          createdAt = d.optLong("createdAt", 0L),
          cards =
              run {
                val ca = d.optJSONArray("cards") ?: JSONArray()
                List(ca.length()) { j ->
                  val c = ca.getJSONObject(j)
                  CardDTO(q = c.optString("q"), a = c.optString("a"))
                }
              })
    }
  } catch (_: Exception) {
    emptyList()
  }
}

private fun encodeDecks(decks: List<DeckDTO>): String {
  val root = JSONObject()
  root.put("version", 1)
  val arr = JSONArray()
  decks.forEach { d ->
    val obj =
        JSONObject()
            .put("id", d.id)
            .put("title", d.title)
            .put("description", d.description)
            .put("createdAt", d.createdAt)

    val cards = JSONArray()
    d.cards.forEach { c -> cards.put(JSONObject().put("q", c.q).put("a", c.a)) }
    obj.put("cards", cards)

    arr.put(obj)
  }
  root.put("decks", arr)
  return root.toString()
}
