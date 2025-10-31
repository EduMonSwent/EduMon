package com.android.sample.data

import android.content.Context
import java.time.LocalDate
import kotlin.collections.filter
import kotlin.collections.firstOrNull
import kotlin.collections.sortedBy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Real implementation of MoodRepositoryInterface that manages mood data using MoodStorage. */
class MoodRepository(context: Context) : MoodRepositoryInterface {
  private val storage = MoodStorage(context)

  override suspend fun upsertForDate(date: LocalDate, mood: Int, note: String) =
      withContext(Dispatchers.IO) { storage.upsert(date.toEntry(mood, note)) }

  override suspend fun getForDate(date: LocalDate): MoodEntry? =
      withContext(Dispatchers.IO) {
        val epoch = date.toEpochDay()
        storage.readAll().firstOrNull { it.dateEpochDay == epoch }
      }

  override suspend fun getRange(start: LocalDate, end: LocalDate): List<MoodEntry> =
      withContext(Dispatchers.IO) {
        val s = start.toEpochDay()
        val e = end.toEpochDay()
        storage.readAll().filter { it.dateEpochDay in s..e }.sortedBy { it.dateEpochDay }
      }
}
