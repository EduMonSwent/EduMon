package com.android.sample.feature.weeks.repository

import com.android.sample.feature.weeks.model.WeekContent
import com.android.sample.feature.weeks.model.WeekProgressItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class FirestoreWeeksRepositoryTest {

  private fun repoWithNoUser(): FirestoreWeeksRepository {
    val db: FirebaseFirestore = mock()
    val auth: FirebaseAuth = mock()
    // Unsigned path -> currentUser == null
    whenever(auth.currentUser).thenReturn(null)
    return FirestoreWeeksRepository(db, auth)
  }

  @Test
  fun getWeeks_unsigned_returnsDefaultsWithRecomputedPercents() = runBlocking {
    val repo = repoWithNoUser()

    val weeks = repo.getWeeks()

    // Expect seeded defaults: 3 weeks
    assertEquals(3, weeks.size)
    // Recomputed percents based on hardcoded content within repo:
    // Week1: 4/4 done -> 100
    // Week2: 1/3 done -> 33
    // Week3: 0/2 done -> 0
    assertEquals(listOf(100, 33, 0), weeks.map { it.percent })
    // Labels in order
    assertEquals(listOf("Week 1", "Week 2", "Week 3"), weeks.map { it.label })
  }

  @Test
  fun getDayStatuses_unsigned_returnsDefaultPattern() = runBlocking {
    val repo = repoWithNoUser()

    val days = repo.getDayStatuses()

    assertEquals(7, days.size)
    // Default pattern: idx % 2 == 0 -> true
    assertTrue(days[0].metTarget)
    assertTrue(!days[1].metTarget)
    assertTrue(days[2].metTarget)
  }

  @Test
  fun updateWeekPercent_unsigned_clamps_and_updates_only_target_index() = runBlocking {
    val repo = repoWithNoUser()
    val updated: List<WeekProgressItem> = repo.updateWeekPercent(index = 1, percent = 135)

    // Size preserved
    assertEquals(3, updated.size)
    // Only index 1 changed to clamped 100
    assertEquals(100, updated[1].percent)
    // Others remain their DEFAULT percents from buildDefaultWeeks(): 100 and 10
    assertEquals(100, updated[0].percent)
    assertEquals(10, updated[2].percent)
  }

  @Test
  fun markExerciseDone_unsigned_updates_content_and_percent() = runBlocking {
    val repo = repoWithNoUser()

    // Week 2 initially: default percent 55, but actual content is 1/3 done (33).
    // markExerciseDone recomputes percent for the targeted week only.
    val list = repo.markExerciseDone(weekIndex = 1, exerciseId = "e3", done = true)

    // Now: 2/3 done -> 66
    assertEquals(66, list[1].percent)
    // Verify exercise toggled
    val ex = list[1].content.exercises.find { it.id == "e3" }
    assertTrue(ex?.done == true)
  }

  @Test
  fun markCourseRead_unsigned_updates_content_and_percent() = runBlocking {
    val repo = repoWithNoUser()

    // Week 3 initially: default percent 10, but actual content is 0/2 done -> 0.
    // markCourseRead recomputes percent for the targeted week only -> 1/2 -> 50
    val list = repo.markCourseRead(weekIndex = 2, courseId = "c5", read = true)

    assertEquals(50, list[2].percent)
    val course = list[2].content.courses.find { it.id == "c5" }
    assertTrue(course?.read == true)
  }

  @Test
  fun getWeekContent_unsigned_returns_content_or_empty_on_invalid_index() = runBlocking {
    val repo = repoWithNoUser()

    val content0: WeekContent = repo.getWeekContent(0)
    assertTrue(content0.courses.isNotEmpty())
    assertTrue(content0.exercises.isNotEmpty())

    val invalid = repo.getWeekContent(99)
    assertTrue(invalid.courses.isEmpty())
    assertTrue(invalid.exercises.isEmpty())
  }
}
