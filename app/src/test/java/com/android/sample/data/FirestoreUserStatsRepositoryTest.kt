package com.android.sample.data

// This code has been written partially using A.I (LLM).

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.Calendar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class FirestoreUserStatsRepositoryTest {

  private lateinit var auth: FirebaseAuth
  private lateinit var firestore: FirebaseFirestore
  private lateinit var user: FirebaseUser
  private lateinit var usersCollection: CollectionReference
  private lateinit var statsCollection: CollectionReference
  private lateinit var statsDoc: DocumentReference

  @Before
  fun setup() {
    auth = mock(FirebaseAuth::class.java)
    firestore = mock(FirebaseFirestore::class.java)
    user = mock(FirebaseUser::class.java)
    usersCollection = mock(CollectionReference::class.java)
    statsCollection = mock(CollectionReference::class.java)
    statsDoc = mock(DocumentReference::class.java)

    `when`(auth.currentUser).thenReturn(user)
    `when`(user.uid).thenReturn("test-uid")

    `when`(firestore.collection("users")).thenReturn(usersCollection)
    `when`(usersCollection.document("test-uid")).thenReturn(statsDoc)
    `when`(statsDoc.collection("stats")).thenReturn(statsCollection)
    `when`(statsCollection.document("stats")).thenReturn(statsDoc)
  }

  private fun createRepo(): FirestoreUserStatsRepository =
      FirestoreUserStatsRepository(auth, firestore)

  // Helper to inject _stats private field for branch coverage
  private fun setInternalStats(repo: FirestoreUserStatsRepository, stats: UserStats) {
    val field: Field =
        FirestoreUserStatsRepository::class.java.getDeclaredField("_stats").apply {
          isAccessible = true
        }
    @Suppress("UNCHECKED_CAST") val flow = field.get(repo) as MutableStateFlow<UserStats>
    flow.value = stats
  }

  @Test
  fun initial_state_is_default_without_start() {
    val repo = createRepo()

    val stats = repo.stats.value

    assertEquals(0, stats.totalStudyMinutes)
    assertEquals(0, stats.todayStudyMinutes)
    assertEquals(0, stats.coins)
    assertEquals(0, stats.points)
    assertEquals(0, stats.weeklyGoal)
    assertEquals(0L, stats.lastUpdated)
  }

  @Test
  fun start_with_no_current_user_does_not_crash_and_does_not_change_stats() {
    `when`(auth.currentUser).thenReturn(null)
    val repo = createRepo()

    repo.start()

    val stats = repo.stats.value
    assertEquals(UserStats(), stats)
  }

  @Test
  fun addStudyMinutes_same_day_increments_total_and_today() = runTest {
    val repo = createRepo()

    val initial =
        UserStats(
            totalStudyMinutes = 10, todayStudyMinutes = 4, lastUpdated = 0L // treated as "same day"
            )
    setInternalStats(repo, initial)

    repo.addStudyMinutes(20)

    val updated = repo.stats.value
    assertEquals(30, updated.totalStudyMinutes)
    assertEquals(24, updated.todayStudyMinutes)
    assertTrue(updated.lastUpdated > 0L)
  }

  @Test
  fun addStudyMinutes_cross_day_resets_today_and_keeps_total_plus_extra() = runTest {
    val repo = createRepo()

    val calendar = Calendar.getInstance()
    calendar.add(Calendar.DAY_OF_YEAR, -1)
    val yesterdayMillis = calendar.timeInMillis

    val initial =
        UserStats(totalStudyMinutes = 100, todayStudyMinutes = 40, lastUpdated = yesterdayMillis)
    setInternalStats(repo, initial)

    repo.addStudyMinutes(25)

    val updated = repo.stats.value
    assertEquals(125, updated.totalStudyMinutes)
    assertEquals(25, updated.todayStudyMinutes)
    assertTrue(updated.lastUpdated > yesterdayMillis)
  }

  @Test
  fun addStudyMinutes_non_positive_does_not_change_stats() = runTest {
    val repo = createRepo()
    val initial = UserStats(totalStudyMinutes = 50, todayStudyMinutes = 20, lastUpdated = 1234L)
    setInternalStats(repo, initial)

    repo.addStudyMinutes(0)

    val updated = repo.stats.value
    assertEquals(initial, updated)
  }

  @Test
  fun updateCoins_positive_delta_updates_and_sets_lastUpdated() = runTest {
    val repo = createRepo()
    val initial = UserStats(coins = 10, lastUpdated = 10L)
    setInternalStats(repo, initial)

    repo.updateCoins(5)

    val updated = repo.stats.value
    assertEquals(15, updated.coins)
    assertTrue(updated.lastUpdated >= initial.lastUpdated)
  }

  @Test
  fun updateCoins_zero_does_not_change_state() = runTest {
    val repo = createRepo()
    val initial = UserStats(coins = 10, lastUpdated = 100L)
    setInternalStats(repo, initial)

    repo.updateCoins(0)

    val updated = repo.stats.value
    assertEquals(initial, updated)
  }

  @Test
  fun setWeeklyGoal_updates_field_and_lastUpdated() = runTest {
    val repo = createRepo()
    val initial = UserStats(weeklyGoal = 100, lastUpdated = 10L)
    setInternalStats(repo, initial)

    repo.setWeeklyGoal(300)

    val updated = repo.stats.value
    assertEquals(300, updated.weeklyGoal)
    assertTrue(updated.lastUpdated >= initial.lastUpdated)
  }

  @Test
  fun addPoints_positive_updates_points_and_lastUpdated() = runTest {
    val repo = createRepo()
    val initial = UserStats(points = 5, lastUpdated = 10L)
    setInternalStats(repo, initial)

    repo.addPoints(7)

    val updated = repo.stats.value
    assertEquals(12, updated.points)
    assertTrue(updated.lastUpdated >= initial.lastUpdated)
  }

  @Test
  fun addPoints_zero_does_not_change_state() = runTest {
    val repo = createRepo()
    val initial = UserStats(points = 5, lastUpdated = 10L)
    setInternalStats(repo, initial)

    repo.addPoints(0)

    val updated = repo.stats.value
    assertEquals(initial, updated)
  }

  @Test
  fun userStatsToMap_and_toUserStats_are_inverse_for_typical_values() {
    val repo = createRepo()
    val stats =
        UserStats(
            totalStudyMinutes = 120,
            todayStudyMinutes = 45,
            streak = 3,
            weeklyGoal = 300,
            coins = 50,
            points = 80,
            lastUpdated = 9999L)

    val toMapMethod: Method =
        FirestoreUserStatsRepository::class
            .java
            .getDeclaredMethod("userStatsToMap", UserStats::class.java)
    toMapMethod.isAccessible = true
    @Suppress("UNCHECKED_CAST") val map = toMapMethod.invoke(repo, stats) as Map<String, Any>

    val toUserStatsMethod: Method =
        FirestoreUserStatsRepository::class.java.getDeclaredMethod("toUserStats", Map::class.java)
    toUserStatsMethod.isAccessible = true
    val reconstructed = toUserStatsMethod.invoke(repo, map) as UserStats

    assertEquals(stats.totalStudyMinutes, reconstructed.totalStudyMinutes)
    assertEquals(stats.todayStudyMinutes, reconstructed.todayStudyMinutes)
    assertEquals(stats.streak, reconstructed.streak)
    assertEquals(stats.weeklyGoal, reconstructed.weeklyGoal)
    assertEquals(stats.coins, reconstructed.coins)
    assertEquals(stats.points, reconstructed.points)
    assertEquals(stats.lastUpdated, reconstructed.lastUpdated)
  }

  @Test
  fun isSameDay_returns_true_for_same_calendar_day_and_false_otherwise() {
    val repo = createRepo()

    val method: Method =
        FirestoreUserStatsRepository::class
            .java
            .getDeclaredMethod(
                "isSameDay", Long::class.javaPrimitiveType, Long::class.javaPrimitiveType)
    method.isAccessible = true

    val cal = Calendar.getInstance()
    val t1 = cal.timeInMillis
    cal.add(Calendar.HOUR_OF_DAY, 1)
    val t2 = cal.timeInMillis
    cal.add(Calendar.DAY_OF_YEAR, 1)
    val nextDay = cal.timeInMillis

    val sameDayResult = method.invoke(repo, t1, t2) as Boolean
    val differentDayResult = method.invoke(repo, t1, nextDay) as Boolean

    assertTrue(sameDayResult)
    assertEquals(false, differentDayResult)
  }
}
