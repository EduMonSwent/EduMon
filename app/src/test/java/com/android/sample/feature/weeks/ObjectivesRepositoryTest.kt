package com.android.sample.feature.weeks

import com.android.sample.feature.weeks.model.Objective
import com.android.sample.feature.weeks.repository.FakeObjectivesRepository
import java.time.DayOfWeek
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ObjectivesRepositoryTest {

  @Before
  fun resetRepo() = runTest {
    // Ensure each test starts from a known, identical state
    FakeObjectivesRepository.setObjectives(initialThree())
  }

  @Test
  fun getObjectives_initial_hasThree() = runTest {
    val repo = FakeObjectivesRepository
    val list = repo.getObjectives()
    assertEquals(3, list.size)
    assertTrue(list.first().title.contains("Finish Quiz"))
  }

  @Test
  fun addObjective_appends_and_returns_snapshot() = runTest {
    val repo = FakeObjectivesRepository
    val added =
        Objective(
            title = "Write summary",
            course = "ENG200",
            estimateMinutes = 15,
            completed = false,
            day = DayOfWeek.THURSDAY)

    val afterAdd = repo.addObjective(added)
    assertEquals(4, afterAdd.size)
    assertEquals("Write summary", afterAdd.last().title)
  }

  @Test
  fun updateObjective_updates_when_index_valid_otherwise_noop() = runTest {
    val repo = FakeObjectivesRepository

    val updated =
        Objective(
            title = "Finish Quiz 3 (revised)",
            course = "CS101",
            estimateMinutes = 25,
            completed = true,
            day = DayOfWeek.MONDAY)

    val after = repo.updateObjective(0, updated)
    assertEquals("Finish Quiz 3 (revised)", after[0].title)
    assertTrue(after[0].completed)

    val beforeNoop = repo.getObjectives()
    val afterNoop = repo.updateObjective(99, updated)
    assertEquals(beforeNoop, afterNoop)
  }

  @Test
  fun removeObjective_removes_when_index_valid_otherwise_noop() = runTest {
    val repo = FakeObjectivesRepository

    val afterRemove = repo.removeObjective(1)
    assertEquals(2, afterRemove.size)
    // Ensure the removed one was at index 1 (Outline lab report initially)
    assertTrue(afterRemove.none { it.title.contains("Outline lab") })

    val beforeNoop = repo.getObjectives()
    val noop = repo.removeObjective(42)
    assertEquals(beforeNoop, noop)
  }

  @Test
  fun moveObjective_moves_and_clamps_indices_and_noop_when_same() = runTest {
    val repo = FakeObjectivesRepository

    // Move index 0 -> 2
    val moved = repo.moveObjective(0, 2)
    assertEquals(3, moved.size)
    // Initially [Finish Quiz 3, Outline lab report, Review 15 flashcards]
    // After move 0->2: [Outline lab report, Review 15 flashcards, Finish Quiz 3]
    assertEquals("Outline lab report", moved[0].title)
    assertEquals("Review 15 flashcards", moved[1].title)

    // No-op when from == to
    val unchanged = repo.moveObjective(1, 1)
    assertEquals(moved, unchanged)

    // Clamp indices outside range: -5 -> 99 effectively 0 -> last
    val previousFirst = repo.getObjectives().first()
    val clamped = repo.moveObjective(-5, 99)
    assertEquals(3, clamped.size)
    // After clamped 0 -> last, the previous first should now be last
    assertEquals(previousFirst.title, clamped.last().title)
  }

  @Test
  fun setObjectives_replaces_entire_list() = runTest {
    val repo = FakeObjectivesRepository
    val replacement =
        listOf(
            Objective("A", "X", 5, false, DayOfWeek.MONDAY),
            Objective("B", "Y", 10, true, DayOfWeek.TUESDAY))
    val after = repo.setObjectives(replacement)
    assertEquals(2, after.size)
    assertEquals(listOf("A", "B"), after.map { it.title })

    val snapshot = repo.getObjectives()
    assertEquals(after, snapshot)
  }

  private fun initialThree(): List<Objective> =
      listOf(
          Objective(
              title = "Finish Quiz 3",
              course = "CS101",
              estimateMinutes = 30,
              completed = false,
              day = DayOfWeek.MONDAY),
          Objective(
              title = "Outline lab report",
              course = "CS101",
              estimateMinutes = 20,
              completed = false,
              day = DayOfWeek.TUESDAY),
          Objective(
              title = "Review 15 flashcards",
              course = "ENG200",
              estimateMinutes = 10,
              completed = false,
              day = DayOfWeek.WEDNESDAY),
      )
}
