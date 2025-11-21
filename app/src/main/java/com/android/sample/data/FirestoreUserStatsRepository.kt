package com.android.sample.data

// This code has been written partially using A.I (LLM).

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.util.Calendar
import java.util.TimeZone
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

    private const val MIN_STREAK_ON_STUDY = 1
    private const val NEXT_DAY_DIFFERENCE = 1
    private const val NEXT_YEAR_DIFFERENCE = 1
    private const val FIRST_DAY_OF_YEAR = 1
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
        // Optionally log the error.
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
    if (extraMinutes <= DEFAULT_INT_VALUE) {
      return
    }

    val currentUser = auth.currentUser ?: return
    val now = System.currentTimeMillis()
    val current = _stats.value

    // Same calendar day as last update?
    val sameDay = current.lastUpdated != DEFAULT_LONG_VALUE && isSameDay(current.lastUpdated, now)

    // Reset today's minutes if we are on a new day.
    val baseToday =
        if (sameDay) {
          current.todayStudyMinutes
        } else {
          DEFAULT_INT_VALUE
        }

    // Streak rules:
    // - If you never had a streak (>0), first study sets it to 1.
    // - If you study again the same day, keep the streak.
    // - If you study on a new day and already had a streak, increment it.
    val newStreak =
        when {
          current.streak <= 0 -> MIN_STREAK_ON_STUDY // from 0 -> 1
          sameDay -> current.streak // more study same day
          else -> current.streak + MIN_STREAK_ON_STUDY // new day -> +1
        }

    val updated =
        current.copy(
            totalStudyMinutes = current.totalStudyMinutes + extraMinutes,
            todayStudyMinutes = baseToday + extraMinutes,
            streak = newStreak,
            lastUpdated = now,
        )

    _stats.value = updated
    persistStats(updated, currentUser.uid)
  }

  override suspend fun updateCoins(delta: Int) {
    if (delta == DEFAULT_INT_VALUE) {
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
    if (delta == DEFAULT_INT_VALUE) {
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
    val cal1 = Calendar.getInstance(TimeZone.getDefault()).apply { timeInMillis = firstMillis }
    val cal2 = Calendar.getInstance(TimeZone.getDefault()).apply { timeInMillis = secondMillis }

    val year1 = cal1.get(Calendar.YEAR)
    val year2 = cal2.get(Calendar.YEAR)
    if (year1 != year2) {
      return false
    }

    val day1 = cal1.get(Calendar.DAY_OF_YEAR)
    val day2 = cal2.get(Calendar.DAY_OF_YEAR)
    return day1 == day2
  }

  /** Returns true if [secondMillis] is exactly the next calendar day after [firstMillis]. */
  private fun isNextDay(firstMillis: Long, secondMillis: Long): Boolean {
    if (firstMillis == DEFAULT_LONG_VALUE) {
      return false
    }

    val cal1 = Calendar.getInstance(TimeZone.getDefault()).apply { timeInMillis = firstMillis }
    val cal2 = Calendar.getInstance(TimeZone.getDefault()).apply { timeInMillis = secondMillis }

    val year1 = cal1.get(Calendar.YEAR)
    val year2 = cal2.get(Calendar.YEAR)

    val day1 = cal1.get(Calendar.DAY_OF_YEAR)
    val day2 = cal2.get(Calendar.DAY_OF_YEAR)

    // Same year, consecutive days.
    if (year1 == year2 && day2 - day1 == NEXT_DAY_DIFFERENCE) {
      return true
    }

    // Year rollover: Dec 31 -> Jan 1.
    if (year2 - year1 == NEXT_YEAR_DIFFERENCE) {
      val maxDayYear1 = cal1.getActualMaximum(Calendar.DAY_OF_YEAR)
      return day1 == maxDayYear1 && day2 == FIRST_DAY_OF_YEAR
    }

    return false
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
