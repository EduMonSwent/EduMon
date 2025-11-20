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
    private const val FIELD_DAILY_GOAL = "dailyGoal"
    private const val FIELD_COMPLETED_GOALS = "completedGoals"
    private const val FIELD_TODAY_COMPLETED_POMODOROS = "todayCompletedPomodoros"
    private const val FIELD_COINS = "coins"
    private const val FIELD_POINTS = "points"
    private const val FIELD_LAST_UPDATED = "lastUpdated"
    private const val FIELD_LAST_STUDY_DATE = "lastStudyDate"
    private const val FIELD_COURSE_TIMES_MIN = "courseTimesMin"
    private const val FIELD_PROGRESS_BY_DAY_MIN = "progressByDayMin"

    private const val DEFAULT_INT_VALUE = 0
    private const val DEFAULT_LONG_VALUE = 0L
    private const val DEFAULT_DAILY_GOAL_MIN = 20
    private const val DEFAULT_WEEK_DAYS_COUNT = 7
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

      val dataMap = snapshot.data ?: emptyMap<String, Any>()
      val nowMillis = System.currentTimeMillis()
      val rawStats = dataMap.toUserStats()

      val isSameDayAsNow =
          if (rawStats.lastUpdated == DEFAULT_LONG_VALUE) {
            true
          } else {
            isSameDay(rawStats.lastUpdated, nowMillis)
          }

      val adjustedStats =
          if (isSameDayAsNow) {
            rawStats
          } else {
            rawStats.copy(
                todayStudyMinutes = DEFAULT_INT_VALUE,
                todayCompletedPomodoros = DEFAULT_INT_VALUE,
                // We only update local state visual, we don't persist reset immediately unless user
                // acts
            )
          }

      _stats.value = adjustedStats
    }
  }

  override suspend fun addStudyMinutes(extraMinutes: Int) {
    if (extraMinutes <= 0) {
      return
    }

    val currentUser = auth.currentUser ?: return
    val nowMillis = System.currentTimeMillis()
    val currentStats = _stats.value

    val isSameDayAsLastUpdate =
        if (currentStats.lastUpdated == DEFAULT_LONG_VALUE) {
          false
        } else {
          isSameDay(currentStats.lastUpdated, nowMillis)
        }

    val baseTodayMinutes =
        if (isSameDayAsLastUpdate) {
          currentStats.todayStudyMinutes
        } else {
          DEFAULT_INT_VALUE
        }

    // Reset pomodoro count if new day (only if this update is triggered and day changed)
    val baseTodayPomodoros =
        if (isSameDayAsLastUpdate) {
          currentStats.todayCompletedPomodoros
        } else {
          DEFAULT_INT_VALUE
        }

    val currentProgress = currentStats.progressByDayMin.toMutableList()
    if (currentProgress.isNotEmpty()) {
      if (isSameDayAsLastUpdate) {
        val lastIdx = currentProgress.lastIndex
        currentProgress[lastIdx] = currentProgress[lastIdx] + extraMinutes
      } else {
        currentProgress.removeAt(0)
        currentProgress.add(extraMinutes)
      }
    }

    val lastStudyMillis =
        if (currentStats.lastStudyDate != DEFAULT_LONG_VALUE) currentStats.lastStudyDate
        else currentStats.lastUpdated
    val isSameDayAsLastStudy =
        if (lastStudyMillis == DEFAULT_LONG_VALUE) false else isSameDay(lastStudyMillis, nowMillis)

    var newStreak = currentStats.streak
    if (!isSameDayAsLastStudy) {
      val yesterdayMillis = nowMillis - 24 * 60 * 60 * 1000
      if (isSameDay(lastStudyMillis, yesterdayMillis)) {
        newStreak += 1
      } else {
        newStreak = 1
      }
    } else {
      if (newStreak == 0) newStreak = 1
    }

    val updatedStats =
        currentStats.copy(
            totalStudyMinutes = currentStats.totalStudyMinutes + extraMinutes,
            todayStudyMinutes = baseTodayMinutes + extraMinutes,
            todayCompletedPomodoros = baseTodayPomodoros, // preserve corrected value
            streak = newStreak,
            progressByDayMin = currentProgress,
            lastUpdated = nowMillis,
            lastStudyDate = nowMillis)

    _stats.value = updatedStats
    persistStats(updatedStats, currentUser.uid)
  }

  override suspend fun updateCoins(delta: Int) {
    if (delta == 0) {
      return
    }

    val currentUser = auth.currentUser ?: return
    val nowMillis = System.currentTimeMillis()
    val currentStats = _stats.value

    val updatedStats =
        currentStats.copy(coins = currentStats.coins + delta, lastUpdated = nowMillis)

    _stats.value = updatedStats
    persistStats(updatedStats, currentUser.uid)
  }

  override suspend fun setWeeklyGoal(goalMinutes: Int) {
    val currentUser = auth.currentUser ?: return
    val nowMillis = System.currentTimeMillis()
    val currentStats = _stats.value

    val updatedStats = currentStats.copy(weeklyGoal = goalMinutes, lastUpdated = nowMillis)

    _stats.value = updatedStats
    persistStats(updatedStats, currentUser.uid)
  }

  override suspend fun addPoints(delta: Int) {
    if (delta == 0) {
      return
    }

    val currentUser = auth.currentUser ?: return
    val nowMillis = System.currentTimeMillis()
    val currentStats = _stats.value

    val updatedStats =
        currentStats.copy(points = currentStats.points + delta, lastUpdated = nowMillis)

    _stats.value = updatedStats
    persistStats(updatedStats, currentUser.uid)
  }

  override suspend fun updateCompletedGoals(count: Int) {
    val currentUser = auth.currentUser ?: return
    val nowMillis = System.currentTimeMillis()
    val currentStats = _stats.value

    val updatedStats = currentStats.copy(completedGoals = count, lastUpdated = nowMillis)

    _stats.value = updatedStats
    persistStats(updatedStats, currentUser.uid)
  }

  override suspend fun incrementCompletedPomodoros() {
    val currentUser = auth.currentUser ?: return
    val nowMillis = System.currentTimeMillis()
    val currentStats = _stats.value

    val isSameDayAsLastUpdate =
        if (currentStats.lastUpdated == DEFAULT_LONG_VALUE) {
          false
        } else {
          isSameDay(currentStats.lastUpdated, nowMillis)
        }

    val basePomodoros =
        if (isSameDayAsLastUpdate) currentStats.todayCompletedPomodoros else DEFAULT_INT_VALUE

    val updatedStats =
        currentStats.copy(todayCompletedPomodoros = basePomodoros + 1, lastUpdated = nowMillis)

    _stats.value = updatedStats
    persistStats(updatedStats, currentUser.uid)
  }

  private suspend fun ensureDefaultDocument(uid: String) {
    val nowMillis = System.currentTimeMillis()
    val defaultStats = UserStats(lastUpdated = nowMillis)
    persistStats(defaultStats, uid)
    _stats.value = defaultStats
  }

  private fun Map<String, Any>.toUserStats(): UserStats {
    val progressRaw = this[FIELD_PROGRESS_BY_DAY_MIN]
    val progressByDayMinValue: List<Int> =
        if (progressRaw is List<*>) {
          progressRaw.mapNotNull { item -> (item as? Number)?.toInt() }
        } else {
          List(DEFAULT_WEEK_DAYS_COUNT) { 0 }
        }

    val courseTimesRaw = this[FIELD_COURSE_TIMES_MIN]
    val courseTimesMinValue: Map<String, Int> =
        if (courseTimesRaw is Map<*, *>) {
          courseTimesRaw.entries
              .mapNotNull { entry ->
                val key = entry.key as? String ?: return@mapNotNull null
                val value = (entry.value as? Number)?.toInt() ?: DEFAULT_INT_VALUE
                key to value
              }
              .toMap()
        } else {
          emptyMap()
        }

    return UserStats(
        totalStudyMinutes =
            (this[FIELD_TOTAL_STUDY_MINUTES] as? Number)?.toInt() ?: DEFAULT_INT_VALUE,
        todayStudyMinutes =
            (this[FIELD_TODAY_STUDY_MINUTES] as? Number)?.toInt() ?: DEFAULT_INT_VALUE,
        streak = (this[FIELD_STREAK] as? Number)?.toInt() ?: DEFAULT_INT_VALUE,
        weeklyGoal = (this[FIELD_WEEKLY_GOAL] as? Number)?.toInt() ?: DEFAULT_INT_VALUE,
        dailyGoal = (this[FIELD_DAILY_GOAL] as? Number)?.toInt() ?: DEFAULT_DAILY_GOAL_MIN,
        completedGoals = (this[FIELD_COMPLETED_GOALS] as? Number)?.toInt() ?: DEFAULT_INT_VALUE,
        todayCompletedPomodoros =
            (this[FIELD_TODAY_COMPLETED_POMODOROS] as? Number)?.toInt() ?: DEFAULT_INT_VALUE,
        coins = (this[FIELD_COINS] as? Number)?.toInt() ?: DEFAULT_INT_VALUE,
        points = (this[FIELD_POINTS] as? Number)?.toInt() ?: DEFAULT_INT_VALUE,
        lastUpdated = (this[FIELD_LAST_UPDATED] as? Number)?.toLong() ?: DEFAULT_LONG_VALUE,
        lastStudyDate = (this[FIELD_LAST_STUDY_DATE] as? Number)?.toLong() ?: DEFAULT_LONG_VALUE,
        courseTimesMin = courseTimesMinValue,
        progressByDayMin =
            if (progressByDayMinValue.size == DEFAULT_WEEK_DAYS_COUNT) progressByDayMinValue
            else List(DEFAULT_WEEK_DAYS_COUNT) { 0 })
  }

  private fun userStatsToMap(stats: UserStats): Map<String, Any> {
    return mapOf(
        FIELD_TOTAL_STUDY_MINUTES to stats.totalStudyMinutes,
        FIELD_TODAY_STUDY_MINUTES to stats.todayStudyMinutes,
        FIELD_STREAK to stats.streak,
        FIELD_WEEKLY_GOAL to stats.weeklyGoal,
        FIELD_DAILY_GOAL to stats.dailyGoal,
        FIELD_COMPLETED_GOALS to stats.completedGoals,
        FIELD_TODAY_COMPLETED_POMODOROS to stats.todayCompletedPomodoros,
        FIELD_COINS to stats.coins,
        FIELD_POINTS to stats.points,
        FIELD_LAST_UPDATED to stats.lastUpdated,
        FIELD_LAST_STUDY_DATE to stats.lastStudyDate,
        FIELD_COURSE_TIMES_MIN to stats.courseTimesMin,
        FIELD_PROGRESS_BY_DAY_MIN to stats.progressByDayMin)
  }

  private fun isSameDay(firstMillis: Long, secondMillis: Long): Boolean {
    val calendarFirst =
        Calendar.getInstance(TimeZone.getDefault()).apply { timeInMillis = firstMillis }
    val calendarSecond =
        Calendar.getInstance(TimeZone.getDefault()).apply { timeInMillis = secondMillis }

    val firstYear = calendarFirst.get(Calendar.YEAR)
    val secondYear = calendarSecond.get(Calendar.YEAR)
    if (firstYear != secondYear) {
      return false
    }

    val firstDayOfYear = calendarFirst.get(Calendar.DAY_OF_YEAR)
    val secondDayOfYear = calendarSecond.get(Calendar.DAY_OF_YEAR)
    return firstDayOfYear == secondDayOfYear
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
