package com.android.sample.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

/**
 * Firestore-backed implementation of UserStatsRepository.
 *
 * Document path: /users/{uid}/stats/stats Fields (Long):
 * - totalStudyMinutes
 * - todayStudyMinutes
 * - streak
 * - weeklyGoal
 * - points
 * - coins
 * - lastStudyDateEpochDay (epochDay of LocalDate, may be missing)
 */
class FirestoreUserStatsRepository(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : UserStatsRepository {

  private val _stats = MutableStateFlow(UserStats())
  override val stats: StateFlow<UserStats> = _stats

  private var listener: ListenerRegistration? = null

  private val uid: String
    get() =
        auth.currentUser?.uid
            ?: throw IllegalStateException(
                "User must be logged in before using UserStatsRepository.")

  private val doc: DocumentReference
    get() = db.collection("users").document(uid).collection("stats").document("stats")

  override suspend fun start() =
      withContext(ioDispatcher) {
        if (listener != null) return@withContext

        ensureDocExists()
        addSnapshotListener()
      }

  private fun ensureDocExists() {
    doc.get().addOnSuccessListener { snap ->
      if (!snap.exists()) {
        doc.set(UserStats().toMap(), SetOptions.merge())
      }
    }
  }

  private fun addSnapshotListener() {
    listener =
        doc.addSnapshotListener { snap, error ->
          if (error != null) return@addSnapshotListener
          val raw = getRawStats(snap)
          val rolled = applyDailyRollover(raw, LocalDate.now())
          _stats.value = rolled
          persistRollOverIfNeeded(raw, rolled)
        }
  }

  private fun getRawStats(snap: DocumentSnapshot?): UserStats {
    return if (snap != null && snap.exists()) {
      snap.data?.toUserStats() ?: UserStats()
    } else {
      UserStats()
    }
  }

  private fun persistRollOverIfNeeded(raw: UserStats, rolled: UserStats) {
    if (rolled != raw) {
      doc.set(rolled.toMap(), SetOptions.merge())
    }
  }

  override suspend fun addStudyMinutes(extraMinutes: Int) {
    withContext(ioDispatcher) {
      if (extraMinutes <= 0) return@withContext

      val today = LocalDate.now()
      val currentRolled = applyDailyRollover(_stats.value, today)

      val firstToday = currentRolled.todayStudyMinutes == 0
      val updated =
          currentRolled.copy(
              totalStudyMinutes = currentRolled.totalStudyMinutes + extraMinutes,
              todayStudyMinutes = currentRolled.todayStudyMinutes + extraMinutes,
              streak = currentRolled.streak + if (firstToday) 1 else 0,
              lastStudyDateEpochDay = today.toEpochDay())

      _stats.value = updated
      doc.set(updated.toMap(), SetOptions.merge())
      // last expression now effectively Unit
    }
  }



  override suspend fun updateCoins(delta: Int) {
    withContext(ioDispatcher) {
      if (delta == 0) return@withContext
      val c = _stats.value
      val updated = c.copy(coins = (c.coins + delta).coerceAtLeast(0))
      _stats.value = updated
      doc.set(updated.toMap(), SetOptions.merge())
    }
  }

  override suspend fun setWeeklyGoal(goalMinutes: Int) {
    withContext(ioDispatcher) {
      val c = _stats.value
      val updated = c.copy(weeklyGoal = goalMinutes.coerceAtLeast(0))
      _stats.value = updated
      doc.set(updated.toMap(), SetOptions.merge())
    }
  }

  override suspend fun addPoints(delta: Int) {
    withContext(ioDispatcher) {
      if (delta == 0) return@withContext
      val c = _stats.value
      val updated = c.copy(points = (c.points + delta).coerceAtLeast(0))
      _stats.value = updated
      doc.set(updated.toMap(), SetOptions.merge())
    }
  }

  // ---------- Helpers ----------

    override suspend fun addReward(minutes: Int, points: Int, coins: Int) {
        withContext(ioDispatcher) {
            if (minutes == 0 && points == 0 && coins == 0) return@withContext

            val current = _stats.value
            val updated = current.copy(
                totalStudyMinutes = if (minutes > 0) {
                    (current.totalStudyMinutes + minutes).coerceAtLeast(0)
                } else current.totalStudyMinutes,
                todayStudyMinutes = if (minutes > 0) {
                    (current.todayStudyMinutes + minutes).coerceAtLeast(0)
                } else current.todayStudyMinutes,
                points = (current.points + points).coerceAtLeast(0),
                coins = (current.coins + coins).coerceAtLeast(0)
            )

            _stats.value = updated
            doc.set(updated.toMap(), SetOptions.merge())
        }
    }


    private fun applyDailyRollover(stats: UserStats, today: LocalDate): UserStats {
    val lastEpoch =
        stats.lastStudyDateEpochDay ?: return stats.copy(lastStudyDateEpochDay = today.toEpochDay())
    val last = LocalDate.ofEpochDay(lastEpoch)

    if (last.isEqual(today)) return stats

    val gap = ChronoUnit.DAYS.between(last, today)
    val hadStudyThatDay = stats.todayStudyMinutes > 0

    val newStreak =
        if (!hadStudyThatDay || gap > 1L) {
          // did not study on last recorded day OR we skipped >= 1 full day
          0
        } else {
          // studied yesterday and today is consecutive → keep current streak value
          stats.streak
        }

    return stats.copy(
        todayStudyMinutes = 0, streak = newStreak, lastStudyDateEpochDay = today.toEpochDay())
  }

  private fun Map<String, Any>.toUserStats(): UserStats {
    fun int(name: String) = (this[name] as? Number)?.toInt() ?: 0
    val lastEpoch = (this["lastStudyDateEpochDay"] as? Number)?.toLong()

    return UserStats(
        totalStudyMinutes = int("totalStudyMinutes"),
        todayStudyMinutes = int("todayStudyMinutes"),
        streak = int("streak"),
        weeklyGoal = int("weeklyGoal"),
        points = int("points"),
        coins = int("coins"),
        lastStudyDateEpochDay = lastEpoch)
  }

  private fun UserStats.toMap(): Map<String, Any> =
      mapOf(
          "totalStudyMinutes" to totalStudyMinutes,
          "todayStudyMinutes" to todayStudyMinutes,
          "streak" to streak,
          "weeklyGoal" to weeklyGoal,
          "points" to points,
          "coins" to coins,
          "lastStudyDateEpochDay" to (lastStudyDateEpochDay ?: LocalDate.now().toEpochDay()))
}
