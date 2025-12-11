package com.android.sample.data

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any

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

  private fun stubStatsDocGet() {
    @Suppress("UNCHECKED_CAST") val task = mock(Task::class.java) as Task<DocumentSnapshot>

    `when`(statsDoc.get()).thenReturn(task)
    `when`(task.addOnSuccessListener(any())).thenReturn(task)
    `when`(task.addOnFailureListener(any())).thenReturn(task)
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
    assertEquals(0, stats.streak)
    assertEquals(null, stats.lastStudyDateEpochDay)
  }

  @Test
  fun addStudyMinutes_same_day_increments_total_and_today() = runTest {
    val repo = createRepo()

    val today = LocalDate.now()
    val initial =
        UserStats(
            totalStudyMinutes = 10,
            todayStudyMinutes = 4,
            streak = 1,
            lastStudyDateEpochDay = today.toEpochDay())
    setInternalStats(repo, initial)

    repo.addStudyMinutes(20)

    val updated = repo.stats.value
    assertEquals(30, updated.totalStudyMinutes)
    assertEquals(24, updated.todayStudyMinutes)
    assertEquals(1, updated.streak) // Streak doesn't increment when continuing same day
    assertEquals(today.toEpochDay(), updated.lastStudyDateEpochDay)
  }

  @Test
  fun addStudyMinutes_first_study_of_day_increments_streak() = runTest {
    val repo = createRepo()

    val yesterday = LocalDate.now().minusDays(1)
    val initial =
        UserStats(
            totalStudyMinutes = 100,
            todayStudyMinutes = 30,
            streak = 2,
            lastStudyDateEpochDay = yesterday.toEpochDay())
    setInternalStats(repo, initial)

    repo.addStudyMinutes(25)

    val updated = repo.stats.value
    assertEquals(125, updated.totalStudyMinutes)
    assertEquals(25, updated.todayStudyMinutes)
    assertEquals(3, updated.streak) // Streak increments on first study of new day
    assertEquals(LocalDate.now().toEpochDay(), updated.lastStudyDateEpochDay)
  }

  @Test
  fun addStudyMinutes_cross_day_with_gap_resets_streak() = runTest {
    val repo = createRepo()

    val twoDaysAgo = LocalDate.now().minusDays(2)
    val initial =
        UserStats(
            totalStudyMinutes = 100,
            todayStudyMinutes = 40,
            streak = 5,
            lastStudyDateEpochDay = twoDaysAgo.toEpochDay())
    setInternalStats(repo, initial)

    repo.addStudyMinutes(25)

    val updated = repo.stats.value
    assertEquals(125, updated.totalStudyMinutes)
    assertEquals(25, updated.todayStudyMinutes)
    assertEquals(1, updated.streak) // Streak resets and starts at 1 when gap > 1 day
    assertEquals(LocalDate.now().toEpochDay(), updated.lastStudyDateEpochDay)
  }

  @Test
  fun addStudyMinutes_non_positive_does_not_change_stats() = runTest {
    val repo = createRepo()
    val initial =
        UserStats(
            totalStudyMinutes = 50,
            todayStudyMinutes = 20,
            lastStudyDateEpochDay = LocalDate.now().toEpochDay())
    setInternalStats(repo, initial)

    repo.addStudyMinutes(0)

    val updated = repo.stats.value
    assertEquals(initial, updated)
  }

  @Test
  fun updateCoins_positive_delta_updates_coins() = runTest {
    val repo = createRepo()
    val initial = UserStats(coins = 10)
    setInternalStats(repo, initial)

    repo.updateCoins(5)

    val updated = repo.stats.value
    assertEquals(15, updated.coins)
  }

  @Test
  fun updateCoins_negative_delta_never_goes_below_zero() = runTest {
    val repo = createRepo()
    val initial = UserStats(coins = 10)
    setInternalStats(repo, initial)

    repo.updateCoins(-15)

    val updated = repo.stats.value
    assertEquals(0, updated.coins)
  }

  @Test
  fun updateCoins_zero_does_not_change_state() = runTest {
    val repo = createRepo()
    val initial = UserStats(coins = 10)
    setInternalStats(repo, initial)

    repo.updateCoins(0)

    val updated = repo.stats.value
    assertEquals(initial, updated)
  }

  @Test
  fun setWeeklyGoal_updates_field() = runTest {
    val repo = createRepo()
    val initial = UserStats(weeklyGoal = 100)
    setInternalStats(repo, initial)

    repo.setWeeklyGoal(300)

    val updated = repo.stats.value
    assertEquals(300, updated.weeklyGoal)
  }

  @Test
  fun setWeeklyGoal_negative_value_coerced_to_zero() = runTest {
    val repo = createRepo()
    val initial = UserStats(weeklyGoal = 100)
    setInternalStats(repo, initial)

    repo.setWeeklyGoal(-50)

    val updated = repo.stats.value
    assertEquals(0, updated.weeklyGoal)
  }

  @Test
  fun addPoints_positive_updates_points() = runTest {
    val repo = createRepo()
    val initial = UserStats(points = 5)
    setInternalStats(repo, initial)

    repo.addPoints(7)

    val updated = repo.stats.value
    assertEquals(12, updated.points)
  }

  @Test
  fun addPoints_negative_never_goes_below_zero() = runTest {
    val repo = createRepo()
    val initial = UserStats(points = 5)
    setInternalStats(repo, initial)

    repo.addPoints(-10)

    val updated = repo.stats.value
    assertEquals(0, updated.points)
  }

  @Test
  fun addPoints_zero_does_not_change_state() = runTest {
    val repo = createRepo()
    val initial = UserStats(points = 5)
    setInternalStats(repo, initial)

    repo.addPoints(0)

    val updated = repo.stats.value
    assertEquals(initial, updated)
  }

  @Test
  fun toMap_and_toUserStats_are_inverse_for_typical_values() {
    val repo = createRepo()
    val stats =
        UserStats(
            totalStudyMinutes = 120,
            todayStudyMinutes = 45,
            streak = 3,
            weeklyGoal = 300,
            coins = 50,
            points = 80,
            lastStudyDateEpochDay = LocalDate.now().toEpochDay())

    val toMapMethod: Method =
        FirestoreUserStatsRepository::class.java.getDeclaredMethod("toMap", UserStats::class.java)
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
    assertEquals(stats.lastStudyDateEpochDay, reconstructed.lastStudyDateEpochDay)
  }

  @Test
  fun applyDailyRollover_same_day_returns_unchanged_stats() {
    val repo = createRepo()
    val today = LocalDate.now()

    val method: Method =
        FirestoreUserStatsRepository::class
            .java
            .getDeclaredMethod("applyDailyRollover", UserStats::class.java, LocalDate::class.java)
    method.isAccessible = true

    val stats =
        UserStats(
            totalStudyMinutes = 100,
            todayStudyMinutes = 40,
            streak = 3,
            lastStudyDateEpochDay = today.toEpochDay())

    val result = method.invoke(repo, stats, today) as UserStats

    assertEquals(stats, result)
  }

  @Test
  fun applyDailyRollover_next_day_with_study_preserves_streak() {
    val repo = createRepo()
    val yesterday = LocalDate.now().minusDays(1)
    val today = LocalDate.now()

    val method: Method =
        FirestoreUserStatsRepository::class
            .java
            .getDeclaredMethod("applyDailyRollover", UserStats::class.java, LocalDate::class.java)
    method.isAccessible = true

    val stats =
        UserStats(
            totalStudyMinutes = 100,
            todayStudyMinutes = 40,
            streak = 3,
            lastStudyDateEpochDay = yesterday.toEpochDay())

    val result = method.invoke(repo, stats, today) as UserStats

    assertEquals(0, result.todayStudyMinutes)
    assertEquals(3, result.streak) // Streak preserved when studied yesterday
    assertEquals(today.toEpochDay(), result.lastStudyDateEpochDay)
  }

  @Test
  fun applyDailyRollover_gap_resets_streak() {
    val repo = createRepo()
    val twoDaysAgo = LocalDate.now().minusDays(2)
    val today = LocalDate.now()

    val method: Method =
        FirestoreUserStatsRepository::class
            .java
            .getDeclaredMethod("applyDailyRollover", UserStats::class.java, LocalDate::class.java)
    method.isAccessible = true

    val stats =
        UserStats(
            totalStudyMinutes = 100,
            todayStudyMinutes = 40,
            streak = 5,
            lastStudyDateEpochDay = twoDaysAgo.toEpochDay())

    val result = method.invoke(repo, stats, today) as UserStats

    assertEquals(0, result.todayStudyMinutes)
    assertEquals(0, result.streak) // Streak reset due to gap
    assertEquals(today.toEpochDay(), result.lastStudyDateEpochDay)
  }

  @Test
  fun applyDailyRollover_no_study_yesterday_resets_streak() {
    val repo = createRepo()
    val yesterday = LocalDate.now().minusDays(1)
    val today = LocalDate.now()

    val method: Method =
        FirestoreUserStatsRepository::class
            .java
            .getDeclaredMethod("applyDailyRollover", UserStats::class.java, LocalDate::class.java)
    method.isAccessible = true

    val stats =
        UserStats(
            totalStudyMinutes = 100,
            todayStudyMinutes = 0, // No study yesterday
            streak = 5,
            lastStudyDateEpochDay = yesterday.toEpochDay())

    val result = method.invoke(repo, stats, today) as UserStats

    assertEquals(0, result.todayStudyMinutes)
    assertEquals(0, result.streak) // Streak reset because no study on last recorded day
    assertEquals(today.toEpochDay(), result.lastStudyDateEpochDay)
  }

  @Test
  fun start_with_snapshot_same_day_uses_raw_stats_without_rollover() = runTest {
    val repo = createRepo()
    stubStatsDocGet() // <--- ADD THIS

    var capturedListener: EventListener<DocumentSnapshot>? = null
    `when`(statsDoc.addSnapshotListener(any<EventListener<DocumentSnapshot>>())).thenAnswer {
        invocation ->
      @Suppress("UNCHECKED_CAST")
      capturedListener = invocation.getArgument(0) as EventListener<DocumentSnapshot>
      mock(ListenerRegistration::class.java)
    }

    repo.start()

    val snapshot = mock(DocumentSnapshot::class.java)
    val today = LocalDate.now()
    val map =
        mapOf(
            "totalStudyMinutes" to 100,
            "todayStudyMinutes" to 40,
            "streak" to 2,
            "weeklyGoal" to 300,
            "coins" to 10,
            "points" to 50,
            "lastStudyDateEpochDay" to today.toEpochDay())

    `when`(snapshot.exists()).thenReturn(true)
    `when`(snapshot.data).thenReturn(map)

    capturedListener!!.onEvent(snapshot, null)

    val stats = repo.stats.value
    assertEquals(100, stats.totalStudyMinutes)
    assertEquals(40, stats.todayStudyMinutes)
    assertEquals(2, stats.streak)
    assertEquals(300, stats.weeklyGoal)
    assertEquals(10, stats.coins)
    assertEquals(50, stats.points)
  }

  @Test
  fun start_with_snapshot_previous_day_resets_today() = runTest {
    val repo = createRepo()
    stubStatsDocGet() // <--- ADD THIS

    var capturedListener: EventListener<DocumentSnapshot>? = null
    `when`(statsDoc.addSnapshotListener(any<EventListener<DocumentSnapshot>>())).thenAnswer {
        invocation ->
      @Suppress("UNCHECKED_CAST")
      capturedListener = invocation.getArgument(0) as EventListener<DocumentSnapshot>
      mock(ListenerRegistration::class.java)
    }

    repo.start()

    val snapshot = mock(DocumentSnapshot::class.java)
    val yesterday = LocalDate.now().minusDays(1)

    val map =
        mapOf(
            "totalStudyMinutes" to 200,
            "todayStudyMinutes" to 80,
            "streak" to 3,
            "weeklyGoal" to 400,
            "coins" to 25,
            "points" to 90,
            "lastStudyDateEpochDay" to yesterday.toEpochDay())

    `when`(snapshot.exists()).thenReturn(true)
    `when`(snapshot.data).thenReturn(map)

    capturedListener!!.onEvent(snapshot, null)

    val stats = repo.stats.value
    assertEquals(200, stats.totalStudyMinutes)
    assertEquals(0, stats.todayStudyMinutes) // Reset to 0 for new day
    assertEquals(3, stats.streak) // Preserved because studied yesterday
    assertEquals(400, stats.weeklyGoal)
    assertEquals(25, stats.coins)
    assertEquals(90, stats.points)
    assertEquals(LocalDate.now().toEpochDay(), stats.lastStudyDateEpochDay)
  }

  @Test
  fun start_with_null_snapshot_uses_default_stats() = runTest {
    val repo = createRepo()
    stubStatsDocGet() // <--- ADD THIS

    var capturedListener: EventListener<DocumentSnapshot>? = null
    `when`(statsDoc.addSnapshotListener(any<EventListener<DocumentSnapshot>>())).thenAnswer {
        invocation ->
      @Suppress("UNCHECKED_CAST")
      capturedListener = invocation.getArgument(0) as EventListener<DocumentSnapshot>
      mock(ListenerRegistration::class.java)
    }

    repo.start()

    capturedListener!!.onEvent(null, null)

    val stats = repo.stats.value

    assertEquals(0, stats.totalStudyMinutes)
    assertEquals(0, stats.todayStudyMinutes)
    assertEquals(0, stats.streak)
    assertEquals(0, stats.weeklyGoal)
    assertEquals(0, stats.coins)
    assertEquals(0, stats.points)
  }

  @Test
  fun toUserStats_handles_null_lastStudyDateEpochDay() {
    val repo = createRepo()

    val toUserStatsMethod: Method =
        FirestoreUserStatsRepository::class.java.getDeclaredMethod("toUserStats", Map::class.java)
    toUserStatsMethod.isAccessible = true

    val map =
        mapOf(
            "totalStudyMinutes" to 100,
            "todayStudyMinutes" to 40,
            "streak" to 2,
            "weeklyGoal" to 300,
            "coins" to 10,
            "points" to 50)

    val result = toUserStatsMethod.invoke(repo, map) as UserStats

    assertEquals(null, result.lastStudyDateEpochDay)
  }

  @Test
  fun addReward_single_atomic_update_covers_all_branches() = runTest {
    val repo = createRepo()

    // Test 1: All zeros - early return (line coverage for early return)
    val initial1 =
        UserStats(totalStudyMinutes = 50, todayStudyMinutes = 20, points = 100, coins = 50)
    setInternalStats(repo, initial1)
    repo.addReward(minutes = 0, points = 0, coins = 0)
    assertEquals(initial1, repo.stats.value) // No change

    // Test 2: All positive values - covers all update branches
    val initial2 =
        UserStats(totalStudyMinutes = 50, todayStudyMinutes = 20, points = 100, coins = 50)
    setInternalStats(repo, initial2)
    repo.addReward(minutes = 25, points = 10, coins = 5)
    val updated = repo.stats.value
    assertEquals(75, updated.totalStudyMinutes) // Covers minutes > 0 branch
    assertEquals(45, updated.todayStudyMinutes) // Covers minutes > 0 branch
    assertEquals(110, updated.points) // Covers points addition
    assertEquals(55, updated.coins) // Covers coins addition

    // Test 3: Negative values - covers coerceAtLeast(0) branches
    val initial3 = UserStats(totalStudyMinutes = 50, todayStudyMinutes = 20, points = 5, coins = 10)
    setInternalStats(repo, initial3)
    repo.addReward(minutes = 0, points = -10, coins = -20)
    val updated3 = repo.stats.value
    assertEquals(50, updated3.totalStudyMinutes) // Unchanged (minutes not > 0)
    assertEquals(20, updated3.todayStudyMinutes) // Unchanged (minutes not > 0)
    assertEquals(0, updated3.points) // Coerced to 0
    assertEquals(0, updated3.coins) // Coerced to 0
  }
}
