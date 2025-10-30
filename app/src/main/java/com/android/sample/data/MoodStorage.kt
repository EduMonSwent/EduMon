package com.android.sample.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

private val Context.moodDataStore by preferencesDataStore(name = "mood_store")

private object Keys {
  val ENTRIES_JSON: Preferences.Key<String> = stringPreferencesKey("mood_entries_json")
}

class MoodStorage(private val context: Context) {

  // read all entries from JSON (small list)
  suspend fun readAll(): List<MoodEntry> {
    val prefs = context.moodDataStore.data.first()
    val raw = prefs[Keys.ENTRIES_JSON] ?: "[]"
    return raw.toMoodList()
  }

  // upsert today’s entry and trim list length (keep up to last 60 days)
  suspend fun upsert(entry: MoodEntry) {
    val current = readAll().toMutableList()
    val idx = current.indexOfFirst { it.dateEpochDay == entry.dateEpochDay }
    if (idx >= 0) current[idx] = entry else current += entry
    // keep recent first by date
    val sorted = current.sortedByDescending { it.dateEpochDay }.take(60)
    val json = sorted.toJsonString()
    context.moodDataStore.edit { it[Keys.ENTRIES_JSON] = json }
  }
}

/* ------------ tiny JSON helpers (no serialization plugin needed) ------------ */

private fun List<MoodEntry>.toJsonString(): String {
  val arr = JSONArray()
  for (e in this) {
    val o = JSONObject()
    o.put("d", e.dateEpochDay)
    o.put("m", e.mood)
    o.put("n", e.note)
    arr.put(o)
  }
  return arr.toString()
}

private fun String.toMoodList(): List<MoodEntry> {
  return try {
    val arr = JSONArray(this)
    List(arr.length()) { i ->
      val o = arr.getJSONObject(i)
      MoodEntry(
          dateEpochDay = o.optLong("d", 0L), mood = o.optInt("m", 0), note = o.optString("n", ""))
    }
  } catch (_: Exception) {
    emptyList()
  }
}
