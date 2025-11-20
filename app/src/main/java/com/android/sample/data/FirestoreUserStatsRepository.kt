package com.android.sample.data

// This code has been written partially using A.I (LLM).

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Firestore-backed implementation of UserStatsRepository.
 *
 * Single canonical document: /users/{uid}/stats/stats
 */
class FirestoreUserStatsRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : UserStatsRepository {

  private companion object {
    private const val USERS_COLLECTION = "users"
    private const val STATS_COLLECTION = "stats"
    private const val STATS_DOCUMENT_ID = "stats"

    private const val FIELD_TOTAL_STUDY_MINUTES = "totalStudyMinutes"
    private const val FIELD_TODAY_STUDY_MINUTES = "todayStudyMinutes"
    private const val FIELD_STREAK = "streak"
    private const val FIELD_WEEKLY_GOAL = "weeklyGoal"
    private const val FIELD_COINS = "coins"
    private const val FIELD_POINTS = "points"
    private const val FIELD_LAST_UPDATED = "lastUpdated"

    private const val DEFAULT_INT_VALUE = 0
    private const val DEFAULT_LONG_VALUE = 0L
  }

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  private val _stats = MutableStateFlow(UserStats())
  override val stats: StateFlow<UserStats> = _stats

  private var hasStarted = false

  override fun start() {
    if (hasStarted) {
      return
    }

    val currentUser = auth.currentUser ?: return
    hasStarted = true

    val docRef =
        firestore
            .collection(USERS_COLLECTION)
            .document(currentUser.uid)
            .collection(STATS_COLLECTION)
            .document(STATS_DOCUMENT_ID)

    docRef.addSnapshotListener { snapshot, error ->
      if (error != null) {
        // You can log this error if needed.
        return@addSnapshotListener
      }

      if (snapshot == null || !snapshot.exists()) {
        scope.launch { ensureDefaultDocument(currentUser.uid) }
        return@addSnapshotListener
      }

      val data = snapshot.data ?: emptyMap<String, Any>()
      val rawStats = data.toUserStats()
      val now = System.currentTimeMillis()

      val sameDay =
          if (rawStats.lastUpdated == DEFAULT_LONG_VALUE) {
            true
          } else {
            isSameDay(rawStats.lastUpdated, now)
          }

      val adjusted =
          if (sameDay) {
            rawStats
          } else {
            rawStats.copy(todayStudyMinutes = DEFAULT_INT_VALUE, lastUpdated = now)
          }

      _stats.value = adjusted

      if (!sameDay) {
        scope.launch { persistStats(adjusted, currentUser.uid) }
      }
    }
  }

  override suspend fun addStudyMinutes(extraMinutes: Int) {
    if (extraMinutes <= 0) {
      return
    }

    val currentUser = auth.currentUser ?: return
    val now = System.currentTimeMillis()
    val current = _stats.value

    val sameDay =
        if (current.lastUpdated == DEFAULT_LONG_VALUE) {
          true
        } else {
          isSameDay(current.lastUpdated, now)
        }

    val baseToday =
        if (sameDay) {
          current.todayStudyMinutes
        } else {
          DEFAULT_INT_VALUE
        }

    val updated =
        current.copy(
            totalStudyMinutes = current.totalStudyMinutes + extraMinutes,
            todayStudyMinutes = baseToday + extraMinutes,
            lastUpdated = now)

    _stats.value = updated
    persistStats(updated, currentUser.uid)
  }

  override suspend fun updateCoins(delta: Int) {
    if (delta == 0) {
      return
    }

    val currentUser = auth.currentUser ?: return
    val now = System.currentTimeMillis()
    val current = _stats.value

    val updated = current.copy(coins = current.coins + delta, lastUpdated = now)

    _stats.value = updated
    persistStats(updated, currentUser.uid)
  }

  override suspend fun setWeeklyGoal(goalMinutes: Int) {
    val currentUser = auth.currentUser ?: return
    val now = System.currentTimeMillis()
    val current = _stats.value

    val updated = current.copy(weeklyGoal = goalMinutes, lastUpdated = now)

    _stats.value = updated
    persistStats(updated, currentUser.uid)
  }

  override suspend fun addPoints(delta: Int) {
    if (delta == 0) {
      return
    }

    val currentUser = auth.currentUser ?: return
    val now = System.currentTimeMillis()
    val current = _stats.value

    val updated = current.copy(points = current.points + delta, lastUpdated = now)

    _stats.value = updated
    persistStats(updated, currentUser.uid)
  }

  private suspend fun ensureDefaultDocument(uid: String) {
    val now = System.currentTimeMillis()
    val defaults = UserStats(lastUpdated = now)
    persistStats(defaults, uid)
    _stats.value = defaults
  }

  private fun Map<String, Any>.toUserStats(): UserStats {
    return UserStats(
        totalStudyMinutes =
            (this[FIELD_TOTAL_STUDY_MINUTES] as? Number)?.toInt() ?: DEFAULT_INT_VALUE,
        todayStudyMinutes =
            (this[FIELD_TODAY_STUDY_MINUTES] as? Number)?.toInt() ?: DEFAULT_INT_VALUE,
        streak = (this[FIELD_STREAK] as? Number)?.toInt() ?: DEFAULT_INT_VALUE,
        weeklyGoal = (this[FIELD_WEEKLY_GOAL] as? Number)?.toInt() ?: DEFAULT_INT_VALUE,
        coins = (this[FIELD_COINS] as? Number)?.toInt() ?: DEFAULT_INT_VALUE,
        points = (this[FIELD_POINTS] as? Number)?.toInt() ?: DEFAULT_INT_VALUE,
        lastUpdated = (this[FIELD_LAST_UPDATED] as? Number)?.toLong() ?: DEFAULT_LONG_VALUE)
  }

  private fun userStatsToMap(stats: UserStats): Map<String, Any> {
    return mapOf(
        FIELD_TOTAL_STUDY_MINUTES to stats.totalStudyMinutes,
        FIELD_TODAY_STUDY_MINUTES to stats.todayStudyMinutes,
        FIELD_STREAK to stats.streak,
        FIELD_WEEKLY_GOAL to stats.weeklyGoal,
        FIELD_COINS to stats.coins,
        FIELD_POINTS to stats.points,
        FIELD_LAST_UPDATED to stats.lastUpdated)
  }

  private fun isSameDay(firstMillis: Long, secondMillis: Long): Boolean {
    val zone = java.time.ZoneId.systemDefault()
    val d1 = java.time.Instant.ofEpochMilli(firstMillis).atZone(zone).toLocalDate()
    val d2 = java.time.Instant.ofEpochMilli(secondMillis).atZone(zone).toLocalDate()
    return d1 == d2
  }

  private fun persistStats(stats: UserStats, uid: String) {
    val docRef =
        firestore
            .collection(USERS_COLLECTION)
            .document(uid)
            .collection(STATS_COLLECTION)
            .document(STATS_DOCUMENT_ID)

    val payload = userStatsToMap(stats)
    docRef.set(payload, SetOptions.merge())
  }
}
