// filepath:
// c:\Users\Khalil\EduMon\app\src\main\java\com\android\sample\feature\weeks\repository\ObjectivesRepository.kt
package com.android.sample.feature.weeks.repository

import com.android.sample.feature.weeks.model.Objective

interface ObjectivesRepository {
  suspend fun getObjectives(): List<Objective>

  suspend fun addObjective(obj: Objective): List<Objective>

  suspend fun updateObjective(index: Int, obj: Objective): List<Objective>

  suspend fun removeObjective(index: Int): List<Objective>

  suspend fun moveObjective(fromIndex: Int, toIndex: Int): List<Objective>

  // New: replace all objectives with the provided list and return the current list
  suspend fun setObjectives(objs: List<Objective>): List<Objective>
}

object FakeObjectivesRepository : ObjectivesRepository {
  private val items =
      mutableListOf(
          Objective(
              title = "Finish Quiz 3",
              course = "CS101",
              estimateMinutes = 30,
              completed = false,
              day = java.time.DayOfWeek.MONDAY),
          Objective(
              title = "Outline lab report",
              course = "CS101",
              estimateMinutes = 20,
              completed = false,
              day = java.time.DayOfWeek.TUESDAY),
          Objective(
              title = "Review 15 flashcards",
              course = "ENG200",
              estimateMinutes = 10,
              completed = false,
              day = java.time.DayOfWeek.WEDNESDAY),
      )

  override suspend fun getObjectives(): List<Objective> {
    // No artificial delay: avoids races in tests
    return items.toList()
  }

  override suspend fun addObjective(obj: Objective): List<Objective> {
    items.add(obj)
    return items.toList()
  }

  override suspend fun updateObjective(index: Int, obj: Objective): List<Objective> {
    if (index in items.indices) items[index] = obj
    return items.toList()
  }

  override suspend fun removeObjective(index: Int): List<Objective> {
    if (index in items.indices) items.removeAt(index)
    return items.toList()
  }

  override suspend fun moveObjective(fromIndex: Int, toIndex: Int): List<Objective> {
    if (items.isEmpty()) return items.toList()
    val from = fromIndex.coerceIn(0, items.lastIndex)
    val to = toIndex.coerceIn(0, items.lastIndex)
    if (from == to) return items.toList()
    val item = items.removeAt(from)
    items.add(to, item)
    return items.toList()
  }

  override suspend fun setObjectives(objs: List<Objective>): List<Objective> {
    items.clear()
    items.addAll(objs)
    return items.toList()
  }
}
