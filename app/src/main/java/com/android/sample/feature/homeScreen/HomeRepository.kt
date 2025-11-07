package com.android.sample.feature.homeScreen

import com.android.sample.data.CreatureStats
import com.android.sample.data.Priority
import com.android.sample.data.ToDo
import com.android.sample.data.UserProfile
import com.android.sample.profile.ProfileRepositoryProvider
import java.time.LocalDate

// ---------- Repository ----------
interface HomeRepository {
  suspend fun fetchTodos(): List<ToDo>

  suspend fun fetchCreatureStats(): CreatureStats

  suspend fun fetchUserStats(): UserProfile

  fun dailyQuote(nowMillis: Long = System.currentTimeMillis()): String
}

class FakeHomeRepository : HomeRepository {
  private val sampleTodos =
      listOf(
          ToDo(
              title = "CS-101: Finish exercise sheet",
              dueDate = LocalDate.now(),
              priority = Priority.HIGH,
              location = "Library – 2nd floor",
              links = listOf("https://university.example/cs101/sheet-5"),
              notificationsEnabled = true),
          ToDo(
              title = "Math review: sequences",
              dueDate = LocalDate.now(),
              priority = Priority.MEDIUM,
              note = "Focus on convergence tests"),
          ToDo(
              title = "Pack lab kit for tomorrow",
              dueDate = LocalDate.now().plusDays(1),
              priority = Priority.LOW),
      )

  private val quotes =
      listOf(
          "Small consistent steps beat intense sprints.",
          "Study now, thank yourself later.",
          "Progress over perfection, always.",
          "You don't have to do it fast — just do it.",
          "Your future self is watching. Keep going.",
      )

  override suspend fun fetchTodos(): List<ToDo> {
    return sampleTodos
  }

  override suspend fun fetchCreatureStats(): CreatureStats {
    return CreatureStats()
  }

  override suspend fun fetchUserStats(): UserProfile {
    return ProfileRepositoryProvider.repository.profile.value
  }

  override fun dailyQuote(nowMillis: Long): String {
    val idx = ((nowMillis / 86_400_000L) % quotes.size).toInt()
    return quotes[idx]
  }
}
