package com.android.sample.ui.stats.repository

import com.android.sample.ui.stats.model.StudyStats
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.LinkedHashMap
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class FirestoreStatsRepositoryTest {

  private fun repoWithNoUser(): FirestoreStatsRepository {
    val db: FirebaseFirestore = mock()
    val auth: FirebaseAuth = mock()
    // Unsigned path -> currentUser == null (so repo never touches Firestore)
    whenever(auth.currentUser).thenReturn(null)
    return FirestoreStatsRepository(db, auth)
  }

  private fun defaultStats(): StudyStats =
      StudyStats(
          totalTimeMin = 5,
          courseTimesMin =
              linkedMapOf(
                  "Analyse I" to 60,
                  "Algèbre linéaire" to 45,
                  "Physique mécanique" to 25,
                  "AICC I" to 15),
          completedGoals = 10,
          progressByDayMin = listOf(0, 25, 30, 15, 50, 20, 5),
          weeklyGoalMin = 300)

  @Test
  fun titles_are_fixed() {
    val repo = repoWithNoUser()
    assertEquals(listOf("Cloud"), repo.titles)
  }

  @Test
  fun loadScenario_anyIndex_sets_selectedIndex_to_zero() {
    val repo = repoWithNoUser()
    // Pick any index; repo always switches to the single scenario (0)
    repo.loadScenario(3)
    assertEquals(0, repo.selectedIndex.value)
  }

  @Test
  fun initial_stats_are_defaults_and_refresh_unsigned_is_noop() = runBlocking {
    val repo = repoWithNoUser()

    // Initial value in StateFlow is defaultStats()
    val expected = defaultStats()
    assertEquals(expected, repo.stats.value)

    // refresh() returns early when unsigned; stats remain unchanged
    repo.refresh()
    assertEquals(expected, repo.stats.value)
  }

  @Test
  fun update_unsigned_does_not_change_stats_or_touch_firestore() = runBlocking {
    val repo = repoWithNoUser()

    val before = repo.stats.value
    // Propose a completely different payload
    val updated =
        StudyStats(
            totalTimeMin = 999,
            weeklyGoalMin = 123,
            completedGoals = 42,
            courseTimesMin =
                LinkedHashMap<String, Int>().apply {
                  put("X", 1)
                  put("Y", 2)
                },
            progressByDayMin = listOf(1, 2, 3, 4, 5, 6, 7))

    // Because there is no user, update() returns early and does NOT mutate _stats
    repo.update(updated)

    val after = repo.stats.value
    assertEquals(before, after)
    // and still equals to defaults
    assertEquals(defaultStats(), after)
  }
}
