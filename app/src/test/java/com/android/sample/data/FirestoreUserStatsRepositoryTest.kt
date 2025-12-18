package com.android.sample.data

import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor

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

  private fun stubStatsDocSet(): Task<Void> {
    @Suppress("UNCHECKED_CAST") val task = mock(Task::class.java) as Task<Void>
    `when`(statsDoc.set(any(), any())).thenReturn(task)

    val successCaptor = argumentCaptor<OnSuccessListener<Void>>()
    val failureCaptor = argumentCaptor<OnFailureListener>()

    `when`(task.addOnSuccessListener(successCaptor.capture())).thenAnswer {
      successCaptor.firstValue.onSuccess(null)
      task
    }
    `when`(task.addOnFailureListener(failureCaptor.capture())).thenReturn(task)

    return task
  }

  private fun stubStatsDocUpdate(): Task<Void> {
    @Suppress("UNCHECKED_CAST") val task = mock(Task::class.java) as Task<Void>
    `when`(statsDoc.update(any<String>(), any())).thenReturn(task)
    `when`(statsDoc.update(any<Map<String, Any>>())).thenReturn(task)

    val successCaptor = argumentCaptor<OnSuccessListener<Void>>()

    `when`(task.addOnSuccessListener(successCaptor.capture())).thenAnswer {
      successCaptor.firstValue.onSuccess(null)
      task
    }
    `when`(task.addOnFailureListener(any())).thenReturn(task)

    return task
  }

  private fun createRepo(): FirestoreUserStatsRepository =
      FirestoreUserStatsRepository(auth, firestore)

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
  fun addStudyMinutes_first_ever_sets_streak_to_1() = runTest {
    val repo = createRepo()
    stubStatsDocSet()

    val initial = UserStats(lastStudyDateEpochDay = null)
    setInternalStats(repo, initial)

    repo.addStudyMinutes(25)

    val updated = repo.stats.value
    assertEquals(25, updated.totalStudyMinutes)
    assertEquals(25, updated.todayStudyMinutes)
    assertEquals(1, updated.streak)
    assertEquals(LocalDate.now().toEpochDay(), updated.lastStudyDateEpochDay)
  }

  @Test
  fun addStudyMinutes_same_day_does_not_increment_streak() = runTest {
    val repo = createRepo()
    stubStatsDocSet()

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
    assertEquals(1, updated.streak)
    assertEquals(today.toEpochDay(), updated.lastStudyDateEpochDay)
  }

  @Test
  fun addStudyMinutes_consecutive_day_increments_streak() = runTest {
    val repo = createRepo()
    stubStatsDocSet()

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
    assertEquals(3, updated.streak)
    assertEquals(LocalDate.now().toEpochDay(), updated.lastStudyDateEpochDay)
  }

  @Test
  fun addStudyMinutes_after_gap_resets_streak_to_1() = runTest {
    val repo = createRepo()
    stubStatsDocSet()

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
    assertEquals(1, updated.streak)
    assertEquals(LocalDate.now().toEpochDay(), updated.lastStudyDateEpochDay)
  }

  @Test
  fun addStudyMinutes_non_positive_does_not_change_stats() = runTest {
    val repo = createRepo()
    val initial =
        UserStats(
            totalStudyMinutes = 50,
            todayStudyMinutes = 20,
            streak = 1,
            lastStudyDateEpochDay = LocalDate.now().toEpochDay())
    setInternalStats(repo, initial)

    repo.addStudyMinutes(0)

    val updated = repo.stats.value
    assertEquals(initial, updated)
  }

  @Test
  fun updateCoins_positive_delta_updates_coins() = runTest {
    val repo = createRepo()
    stubStatsDocUpdate()

    val initial = UserStats(coins = 10)
    setInternalStats(repo, initial)

    repo.updateCoins(5)

    val updated = repo.stats.value
    assertEquals(15, updated.coins)
  }

  @Test
  fun updateCoins_negative_delta_never_goes_below_zero() = runTest {
    val repo = createRepo()
    stubStatsDocUpdate()

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
    stubStatsDocUpdate()

    val initial = UserStats(weeklyGoal = 100)
    setInternalStats(repo, initial)

    repo.setWeeklyGoal(300)

    val updated = repo.stats.value
    assertEquals(300, updated.weeklyGoal)
  }

  @Test
  fun setWeeklyGoal_negative_value_coerced_to_zero() = runTest {
    val repo = createRepo()
    stubStatsDocUpdate()

    val initial = UserStats(weeklyGoal = 100)
    setInternalStats(repo, initial)

    repo.setWeeklyGoal(-50)

    val updated = repo.stats.value
    assertEquals(0, updated.weeklyGoal)
  }

  @Test
  fun addPoints_positive_updates_points() = runTest {
    val repo = createRepo()
    stubStatsDocUpdate()

    val initial = UserStats(points = 5)
    setInternalStats(repo, initial)

    repo.addPoints(7)

    val updated = repo.stats.value
    assertEquals(12, updated.points)
  }

  @Test
  fun addPoints_negative_never_goes_below_zero() = runTest {
    val repo = createRepo()
    stubStatsDocUpdate()

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
  fun addReward_with_all_zeros_does_nothing() = runTest {
    val repo = createRepo()
    val initial = UserStats(totalStudyMinutes = 50, points = 100, coins = 50)
    setInternalStats(repo, initial)

    repo.addReward(minutes = 0, points = 0, coins = 0)

    assertEquals(initial, repo.stats.value)
  }

  @Test
  fun addReward_with_minutes_calls_addStudyMinutes() = runTest {
    val repo = createRepo()
    stubStatsDocSet()
    stubStatsDocUpdate()

    val today = LocalDate.now()
    val initial =
        UserStats(
            totalStudyMinutes = 50,
            todayStudyMinutes = 20,
            streak = 1,
            points = 100,
            coins = 50,
            lastStudyDateEpochDay = today.toEpochDay())
    setInternalStats(repo, initial)

    repo.addReward(minutes = 25, points = 10, coins = 5)

    val updated = repo.stats.value
    assertEquals(75, updated.totalStudyMinutes)
    assertEquals(45, updated.todayStudyMinutes)
    assertEquals(110, updated.points)
    assertEquals(55, updated.coins)
  }

  @Test
  fun addReward_only_points_and_coins_updates_correctly() = runTest {
    val repo = createRepo()
    stubStatsDocUpdate()

    val initial = UserStats(totalStudyMinutes = 50, points = 100, coins = 50)
    setInternalStats(repo, initial)

    repo.addReward(minutes = 0, points = 20, coins = 10)

    val updated = repo.stats.value
    assertEquals(50, updated.totalStudyMinutes) // Unchanged
    assertEquals(120, updated.points)
    assertEquals(60, updated.coins)
  }

  @Test
  fun addReward_negative_values_coerced_to_zero() = runTest {
    val repo = createRepo()
    stubStatsDocUpdate()

    val initial = UserStats(points = 5, coins = 10)
    setInternalStats(repo, initial)

    repo.addReward(minutes = 0, points = -10, coins = -20)

    val updated = repo.stats.value
    assertEquals(0, updated.points)
    assertEquals(0, updated.coins)
  }

  @Test
  fun toMap_and_toUserStats_are_inverse() {
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
  fun toMap_with_null_lastStudyDateEpochDay_uses_today() {
    val repo = createRepo()
    val stats = UserStats(lastStudyDateEpochDay = null)

    val toMapMethod: Method =
        FirestoreUserStatsRepository::class.java.getDeclaredMethod("toMap", UserStats::class.java)
    toMapMethod.isAccessible = true
    @Suppress("UNCHECKED_CAST") val map = toMapMethod.invoke(repo, stats) as Map<String, Any>

    assertEquals(LocalDate.now().toEpochDay(), map["lastStudyDateEpochDay"])
  }

  @Test
  fun applyDisplayRollover_same_day_returns_unchanged() {
    val repo = createRepo()
    val today = LocalDate.now()

    val method: Method =
        FirestoreUserStatsRepository::class
            .java
            .getDeclaredMethod("applyDisplayRollover", UserStats::class.java, LocalDate::class.java)
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
  fun applyDisplayRollover_different_day_resets_todayStudyMinutes() {
    val repo = createRepo()
    val yesterday = LocalDate.now().minusDays(1)
    val today = LocalDate.now()

    val method: Method =
        FirestoreUserStatsRepository::class
            .java
            .getDeclaredMethod("applyDisplayRollover", UserStats::class.java, LocalDate::class.java)
    method.isAccessible = true

    val stats =
        UserStats(
            totalStudyMinutes = 100,
            todayStudyMinutes = 40,
            streak = 3,
            lastStudyDateEpochDay = yesterday.toEpochDay())

    val result = method.invoke(repo, stats, today) as UserStats

    assertEquals(0, result.todayStudyMinutes)
    assertEquals(3, result.streak) // Unchanged by display rollover
  }

  @Test
  fun applyDisplayRollover_null_lastStudyDateEpochDay_returns_unchanged() {
    val repo = createRepo()
    val today = LocalDate.now()

    val method: Method =
        FirestoreUserStatsRepository::class
            .java
            .getDeclaredMethod("applyDisplayRollover", UserStats::class.java, LocalDate::class.java)
    method.isAccessible = true

    val stats = UserStats(lastStudyDateEpochDay = null)

    val result = method.invoke(repo, stats, today) as UserStats

    assertEquals(stats, result)
  }

  @Test
  fun start_with_existing_user_sets_up_listener() = runTest {
    val repo = createRepo()
    stubStatsDocGet()

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
  }

  @Test
  fun start_with_user_change_resets_stats() = runTest {
    val repo = createRepo()
    stubStatsDocGet()

    // Set current uid
    val currentUidField = FirestoreUserStatsRepository::class.java.getDeclaredField("currentUid")
    currentUidField.isAccessible = true
    currentUidField.set(repo, "old-uid")

    // Set non-default stats
    setInternalStats(repo, UserStats(coins = 100, points = 200))

    // Mock listener
    `when`(statsDoc.addSnapshotListener(any<EventListener<DocumentSnapshot>>()))
        .thenReturn(mock(ListenerRegistration::class.java))

    // Start with new user (different uid)
    repo.start()

    // Stats should be reset
    assertEquals(UserStats(), repo.stats.value)
  }

  @Test
  fun stop_clears_listener_and_resets_state() {
    val repo = createRepo()

    // Set some state
    setInternalStats(repo, UserStats(coins = 100))

    val currentUidField = FirestoreUserStatsRepository::class.java.getDeclaredField("currentUid")
    currentUidField.isAccessible = true
    currentUidField.set(repo, "test-uid")

    repo.stop()

    assertEquals(UserStats(), repo.stats.value)
    assertNull(currentUidField.get(repo))
  }

  @Test
  fun determineSessionType_returns_correct_types() {
    val repo = createRepo()
    val today = LocalDate.now()
    val yesterday = today.minusDays(1)
    val twoDaysAgo = today.minusDays(2)

    val method: Method =
        FirestoreUserStatsRepository::class
            .java
            .getDeclaredMethod("determineSessionType", UserStats::class.java, LocalDate::class.java)
    method.isAccessible = true

    // FIRST_EVER
    val firstEver = method.invoke(repo, UserStats(lastStudyDateEpochDay = null), today)
    assertEquals("FIRST_EVER", firstEver.toString())

    // SAME_DAY
    val sameDay = method.invoke(repo, UserStats(lastStudyDateEpochDay = today.toEpochDay()), today)
    assertEquals("SAME_DAY", sameDay.toString())

    // CONSECUTIVE_DAY
    val consecutive =
        method.invoke(repo, UserStats(lastStudyDateEpochDay = yesterday.toEpochDay()), today)
    assertEquals("CONSECUTIVE_DAY", consecutive.toString())

    // AFTER_GAP
    val afterGap =
        method.invoke(repo, UserStats(lastStudyDateEpochDay = twoDaysAgo.toEpochDay()), today)
    assertEquals("AFTER_GAP", afterGap.toString())
  }

  @Test
  fun snapshot_listener_handles_null_snapshot() = runTest {
    val repo = createRepo()
    stubStatsDocGet()

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
    assertEquals(UserStats(), stats)
  }
}
