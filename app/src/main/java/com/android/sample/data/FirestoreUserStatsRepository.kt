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
  private var currentUid: String? = null

  private val uid: String
    get() =
        auth.currentUser?.uid
            ?: throw IllegalStateException(
                "User must be logged in before using UserStatsRepository.")

  private val doc: DocumentReference
    get() = db.collection("users").document(uid).collection("stats").document("stats")

  override suspend fun start() =
      withContext(ioDispatcher) {
        val newUid = uid

        // If user changed, stop old listener and reset state
        if (currentUid != null && currentUid != newUid) {
          listener?.remove()
          listener = null
          _stats.value = UserStats()
        }

        currentUid = newUid

        if (listener != null) return@withContext

        ensureDocExists()
        addSnapshotListener()
      }

  private fun ensureDocExists() {
    doc.get().addOnSuccessListener { snap ->
      if (!snap.exists()) {
        val initialStats = UserStats(lastStudyDateEpochDay = null)
        doc.set(initialStats.toMap(), SetOptions.merge())
      }
    }
  }

  private fun addSnapshotListener() {
    listener =
        doc.addSnapshotListener { snap, error ->
          if (error != null) {
            // Handle error - you might want to log this
            return@addSnapshotListener
          }

          val raw = getRawStats(snap)
          val today = LocalDate.now()

          // Only apply display rollover if needed (for UI purposes only)
          val displayed = applyDisplayRollover(raw, today)
          _stats.value = displayed
        }
  }

  // This rollover is ONLY for display - it doesn't persist anything
  private fun applyDisplayRollover(stats: UserStats, today: LocalDate): UserStats {
    val lastEpoch = stats.lastStudyDateEpochDay ?: return stats
    val lastDate = LocalDate.ofEpochDay(lastEpoch)

    // If it's a different day, reset todayStudyMinutes for display
    // but DON'T change streak or persist anything
    if (!lastDate.isEqual(today)) {
      return stats.copy(todayStudyMinutes = 0)
    }

    return stats
  }

  private fun getRawStats(snap: DocumentSnapshot?): UserStats {
    return if (snap != null && snap.exists()) {
      snap.data?.toUserStats() ?: UserStats()
    } else {
      UserStats()
    }
  }

  override suspend fun addStudyMinutes(delta: Int) {
    withContext(ioDispatcher) {
      if (delta <= 0) return@withContext

      val today = LocalDate.now()
      val current = _stats.value

      // Determine what kind of study session this is
      val sessionType = determineSessionType(current, today)

      val newStreak =
          when (sessionType) {
            SessionType.FIRST_EVER -> 1
            SessionType.SAME_DAY -> current.streak // Don't increment
            SessionType.CONSECUTIVE_DAY -> current.streak + 1
            SessionType.AFTER_GAP -> 1 // Reset to 1 (start new streak)
          }

      // Reset todayStudyMinutes if it's a new day
      val baseTodayMinutes =
          if (sessionType == SessionType.SAME_DAY) {
            current.todayStudyMinutes
          } else {
            0
          }

      val updated =
          current.copy(
              totalStudyMinutes = current.totalStudyMinutes + delta,
              todayStudyMinutes = baseTodayMinutes + delta,
              streak = newStreak,
              lastStudyDateEpochDay = today.toEpochDay())

      // Update local state immediately
      _stats.value = updated

      // Save to Firestore
      doc.set(updated.toMap(), SetOptions.merge()).addOnSuccessListener {}.addOnFailureListener {}
    }
  }

  private enum class SessionType {
    FIRST_EVER, // Never studied before
    SAME_DAY, // Already studied today
    CONSECUTIVE_DAY, // Studied yesterday, now studying today
    AFTER_GAP // Haven't studied in 2+ days
  }

  private fun determineSessionType(stats: UserStats, today: LocalDate): SessionType {
    val lastEpoch = stats.lastStudyDateEpochDay

    // Never studied before
    if (lastEpoch == null) {
      return SessionType.FIRST_EVER
    }

    val lastDate = LocalDate.ofEpochDay(lastEpoch)
    val daysBetween = ChronoUnit.DAYS.between(lastDate, today)

    return when {
      daysBetween == 0L -> SessionType.SAME_DAY
      daysBetween == 1L -> SessionType.CONSECUTIVE_DAY
      else -> SessionType.AFTER_GAP
    }
  }

  override suspend fun updateCoins(delta: Int) {
    withContext(ioDispatcher) {
      if (delta == 0) return@withContext
      val c = _stats.value
      val newCoins = (c.coins + delta).coerceAtLeast(0)

      // Only update coins field, don't touch other fields
      doc.update("coins", newCoins).addOnSuccessListener { _stats.value = c.copy(coins = newCoins) }
    }
  }

  override suspend fun setWeeklyGoal(minutes: Int) {
    withContext(ioDispatcher) {
      val c = _stats.value
      val newGoal = minutes.coerceAtLeast(0)

      // Only update weeklyGoal field
      doc.update("weeklyGoal", newGoal).addOnSuccessListener {
        _stats.value = c.copy(weeklyGoal = newGoal)
      }
    }
  }

  override suspend fun addPoints(delta: Int) {
    withContext(ioDispatcher) {
      if (delta == 0) return@withContext
      val c = _stats.value
      val newPoints = (c.points + delta).coerceAtLeast(0)

      // Only update points field, don't touch other fields
      doc.update("points", newPoints).addOnSuccessListener {
        _stats.value = c.copy(points = newPoints)
      }
    }
  }

  override suspend fun addReward(minutes: Int, points: Int, coins: Int) {
    withContext(ioDispatcher) {
      if (minutes == 0 && points == 0 && coins == 0) return@withContext

      // If adding minutes, use addStudyMinutes to handle streak properly
      if (minutes > 0) {
        addStudyMinutes(minutes)
      }

      // Then update points and coins separately (only if non-zero)
      val updates = mutableMapOf<String, Any>()

      if (points != 0) {
        val current = _stats.value
        updates["points"] = (current.points + points).coerceAtLeast(0)
      }

      if (coins != 0) {
        val current = _stats.value
        updates["coins"] = (current.coins + coins).coerceAtLeast(0)
      }

      if (updates.isNotEmpty()) {
        doc.update(updates).addOnSuccessListener {
          val current = _stats.value
          _stats.value =
              current.copy(
                  points = updates["points"] as? Int ?: current.points,
                  coins = updates["coins"] as? Int ?: current.coins)
        }
      }
    }
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

  private fun UserStats.toMap(): Map<String, Any> {
    return mapOf(
        "totalStudyMinutes" to totalStudyMinutes,
        "todayStudyMinutes" to todayStudyMinutes,
        "streak" to streak,
        "weeklyGoal" to weeklyGoal,
        "points" to points,
        "coins" to coins,
        "lastStudyDateEpochDay" to (lastStudyDateEpochDay ?: LocalDate.now().toEpochDay()))
  }

  // Clean up listener when repository is cleared
  fun stop() {
    listener?.remove()
    listener = null
    currentUid = null
    _stats.value = UserStats()
  }
}
