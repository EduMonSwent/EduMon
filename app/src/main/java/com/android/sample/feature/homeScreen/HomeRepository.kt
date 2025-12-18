package com.android.sample.feature.homeScreen

import com.android.sample.data.CreatureStats
import com.android.sample.data.Priority
import com.android.sample.data.ToDo
import com.android.sample.data.UserProfile
import com.android.sample.feature.weeks.model.Objective
import com.android.sample.feature.weeks.model.ObjectiveType
import com.android.sample.profile.ProfileRepositoryProvider
import java.time.DayOfWeek
import java.time.LocalDate

// ---------- Repository ----------
interface HomeRepository {
  suspend fun fetchTodos(): List<ToDo>

  suspend fun fetchObjectives(): List<Objective>

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

  private val sampleObjectives =
      listOf(
          Objective(
              title = "Revise Week 3 – Calculus",
              course = "Math",
              estimateMinutes = 45,
              completed = false,
              day = DayOfWeek.MONDAY,
              type = ObjectiveType.COURSE_OR_EXERCISES),
          Objective(
              title = "Quiz practice – Algorithms basics",
              course = "CS-101",
              estimateMinutes = 25,
              completed = false,
              day = DayOfWeek.WEDNESDAY,
              type = ObjectiveType.QUIZ),
          Objective(
              title = "Write resume draft",
              course = "Career",
              estimateMinutes = 30,
              completed = true,
              day = DayOfWeek.FRIDAY,
              type = ObjectiveType.RESUME))

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

  override suspend fun fetchObjectives(): List<Objective> = sampleObjectives

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
