// app/src/test/java/com/android/sample/ui/stats/repository/FirestoreStatsRepositoryTest.kt
package com.android.sample.ui.stats.repository

// Parts of this code have been written using an LLM.

import com.android.sample.ui.stats.model.StudyStats
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.util.LinkedHashMap
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class FirestoreStatsRepositoryTest {

  private lateinit var auth: FirebaseAuth
  private lateinit var firestore: FirebaseFirestore
  private lateinit var user: FirebaseUser
  private lateinit var usersCollection: CollectionReference
  private lateinit var userDoc: DocumentReference
  private lateinit var statsCollection: CollectionReference
  private lateinit var statsDoc: DocumentReference

  @Before
  fun setup() {
    auth = mock()
    firestore = mock()
    user = mock()
    usersCollection = mock()
    userDoc = mock()
    statsCollection = mock()
    statsDoc = mock()

    whenever(user.uid).thenReturn("test-uid")

    whenever(firestore.collection("users")).thenReturn(usersCollection)
    whenever(usersCollection.document("test-uid")).thenReturn(userDoc)
    whenever(userDoc.collection("stats")).thenReturn(statsCollection)
    whenever(statsCollection.document("stats")).thenReturn(statsDoc)
  }

  private fun repoWithNoUser(): FirestoreStatsRepository {
    whenever(auth.currentUser).thenReturn(null)
    return FirestoreStatsRepository(firestore, auth)
  }

  private fun repoWithUser(): FirestoreStatsRepository {
    whenever(auth.currentUser).thenReturn(user)
    return FirestoreStatsRepository(firestore, auth)
  }

  private fun defaultStats(): StudyStats =
      StudyStats(
          totalTimeMin = 0,
          courseTimesMin = linkedMapOf(),
          completedGoals = 0,
          progressByDayMin = listOf(0, 0, 0, 0, 0, 0, 0),
          weeklyGoalMin = 0)

  @Test
  fun titles_are_fixed() {
    val repo = repoWithNoUser()
    assertEquals(listOf("Cloud"), repo.titles)
  }

  @Test
  fun loadScenario_anyIndex_sets_selectedIndex_to_zero() {
    val repo = repoWithNoUser()
    repo.loadScenario(3)
    assertEquals(0, repo.selectedIndex.value)
  }

  @Test
  fun loadScenario_negative_index_sets_to_zero() {
    val repo = repoWithNoUser()
    repo.loadScenario(-5)
    assertEquals(0, repo.selectedIndex.value)
  }

  @Test
  fun loadScenario_zero_index_sets_to_zero() {
    val repo = repoWithNoUser()
    repo.loadScenario(0)
    assertEquals(0, repo.selectedIndex.value)
  }

  @Test
  fun initial_stats_are_defaults_and_refresh_unsigned_is_noop() = runBlocking {
    val repo = repoWithNoUser()

    val expected = defaultStats()
    assertEquals(expected, repo.stats.value)

    repo.refresh()
    assertEquals(expected, repo.stats.value)
  }

  @Test
  fun update_unsigned_does_not_change_stats_or_touch_firestore() = runBlocking {
    val repo = repoWithNoUser()

    val before = repo.stats.value
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

    repo.update(updated)

    val after = repo.stats.value
    assertEquals(before, after)
    assertEquals(defaultStats(), after)
  }

  @Test
  fun refresh_with_user_creates_default_doc_if_not_exists() = runBlocking {
    val repo = repoWithUser()

    val snapshot: DocumentSnapshot = mock()
    val getTask: Task<DocumentSnapshot> = Tasks.forResult(snapshot)

    whenever(statsDoc.get()).thenReturn(getTask)
    whenever(snapshot.exists()).thenReturn(false)
    whenever(snapshot.data).thenReturn(null)
    whenever(statsDoc.set(any<Map<String, Any>>(), any())).thenReturn(Tasks.forResult(null))

    repo.refresh()

    val stats = repo.stats.value
    assertEquals(defaultStats(), stats)
  }

  @Test
  fun refresh_with_existing_doc_loads_stats() = runBlocking {
    val repo = repoWithUser()

    val snapshot: DocumentSnapshot = mock()
    val getTask: Task<DocumentSnapshot> = Tasks.forResult(snapshot)

    val dataMap =
        mapOf(
            "totalTimeMin" to 120,
            "weeklyGoalMin" to 300,
            "completedGoals" to 5,
            "courseTimesMin" to mapOf("Math" to 60, "Physics" to 60),
            "progressByDayMin" to listOf(10, 20, 30, 15, 25, 10, 10))

    whenever(statsDoc.get()).thenReturn(getTask)
    whenever(snapshot.exists()).thenReturn(true)
    whenever(snapshot.data).thenReturn(dataMap)
    whenever(statsDoc.set(any<Map<String, Any>>(), any())).thenReturn(Tasks.forResult(null))

    repo.refresh()

    val stats = repo.stats.value
    assertEquals(120, stats.totalTimeMin)
    assertEquals(300, stats.weeklyGoalMin)
    assertEquals(5, stats.completedGoals)
    assertEquals(2, stats.courseTimesMin.size)
    assertEquals(60, stats.courseTimesMin["Math"])
  }

  @Test
  fun update_with_user_persists_to_firestore() = runBlocking {
    val repo = repoWithUser()

    whenever(statsDoc.set(any<Map<String, Any>>(), any())).thenReturn(Tasks.forResult(null))

    val newStats =
        StudyStats(
            totalTimeMin = 150,
            weeklyGoalMin = 400,
            completedGoals = 7,
            courseTimesMin = linkedMapOf("CS" to 90, "Math" to 60),
            progressByDayMin = listOf(20, 25, 30, 20, 15, 20, 20))

    repo.update(newStats)

    assertEquals(newStats, repo.stats.value)
  }

  @Test
  fun mapToStats_handles_null_map() = runBlocking {
    val repo = repoWithUser()

    val snapshot: DocumentSnapshot = mock()
    val getTask: Task<DocumentSnapshot> = Tasks.forResult(snapshot)

    whenever(statsDoc.get()).thenReturn(getTask)
    whenever(snapshot.exists()).thenReturn(true)
    whenever(snapshot.data).thenReturn(null)
    whenever(statsDoc.set(any<Map<String, Any>>(), any())).thenReturn(Tasks.forResult(null))

    repo.refresh()

    assertEquals(defaultStats(), repo.stats.value)
  }

  @Test
  fun mapToStats_handles_missing_fields() = runBlocking {
    val repo = repoWithUser()

    val snapshot: DocumentSnapshot = mock()
    val getTask: Task<DocumentSnapshot> = Tasks.forResult(snapshot)

    val incompleteMap = mapOf("totalTimeMin" to 50)

    whenever(statsDoc.get()).thenReturn(getTask)
    whenever(snapshot.exists()).thenReturn(true)
    whenever(snapshot.data).thenReturn(incompleteMap)
    whenever(statsDoc.set(any<Map<String, Any>>(), any())).thenReturn(Tasks.forResult(null))

    repo.refresh()

    val stats = repo.stats.value
    assertEquals(50, stats.totalTimeMin)
    assertEquals(0, stats.weeklyGoalMin)
    assertEquals(0, stats.completedGoals)
  }

  @Test
  fun mapToStats_pads_short_progressByDayMin() = runBlocking {
    val repo = repoWithUser()

    val snapshot: DocumentSnapshot = mock()
    val getTask: Task<DocumentSnapshot> = Tasks.forResult(snapshot)

    val dataMap = mapOf("totalTimeMin" to 100, "progressByDayMin" to listOf(10, 20, 30))

    whenever(statsDoc.get()).thenReturn(getTask)
    whenever(snapshot.exists()).thenReturn(true)
    whenever(snapshot.data).thenReturn(dataMap)
    whenever(statsDoc.set(any<Map<String, Any>>(), any())).thenReturn(Tasks.forResult(null))

    repo.refresh()

    val stats = repo.stats.value
    assertEquals(7, stats.progressByDayMin.size)
    assertEquals(10, stats.progressByDayMin[0])
    assertEquals(0, stats.progressByDayMin[6])
  }

  @Test
  fun mapToStats_truncates_long_progressByDayMin() = runBlocking {
    val repo = repoWithUser()

    val snapshot: DocumentSnapshot = mock()
    val getTask: Task<DocumentSnapshot> = Tasks.forResult(snapshot)

    val dataMap =
        mapOf(
            "totalTimeMin" to 100,
            "progressByDayMin" to listOf(10, 20, 30, 40, 50, 60, 70, 80, 90, 100))

    whenever(statsDoc.get()).thenReturn(getTask)
    whenever(snapshot.exists()).thenReturn(true)
    whenever(snapshot.data).thenReturn(dataMap)
    whenever(statsDoc.set(any<Map<String, Any>>(), any())).thenReturn(Tasks.forResult(null))

    repo.refresh()

    val stats = repo.stats.value
    assertEquals(7, stats.progressByDayMin.size)
    assertEquals(10, stats.progressByDayMin[0])
    assertEquals(70, stats.progressByDayMin[6])
  }

  @Test
  fun mapToStats_handles_null_progressByDayMin_entries() = runBlocking {
    val repo = repoWithUser()

    val snapshot: DocumentSnapshot = mock()
    val getTask: Task<DocumentSnapshot> = Tasks.forResult(snapshot)

    val dataMap =
        mapOf("totalTimeMin" to 100, "progressByDayMin" to listOf(10, null, 30, 40, null, 60, 70))

    whenever(statsDoc.get()).thenReturn(getTask)
    whenever(snapshot.exists()).thenReturn(true)
    whenever(snapshot.data).thenReturn(dataMap)
    whenever(statsDoc.set(any<Map<String, Any>>(), any())).thenReturn(Tasks.forResult(null))

    repo.refresh()

    val stats = repo.stats.value
    assertEquals(7, stats.progressByDayMin.size)
    assertEquals(10, stats.progressByDayMin[0])
    assertEquals(0, stats.progressByDayMin[1])
    assertEquals(0, stats.progressByDayMin[4])
  }
}
