package com.android.sample.data.notifications

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.android.sample.R
import com.android.sample.ui.location.FriendMode
import com.android.sample.ui.location.FriendStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.tasks.await

/**
 * Background worker that periodically checks if any friends have entered STUDY mode. If a friend
 * transitions from non-STUDY to STUDY mode, posts a notification. Polls every 5 minutes to balance
 * battery usage with timely notifications.
 */
class FriendStudyModeWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

  override suspend fun doWork(): Result {
    // Chain next run (unless disabled by tests)
    if (!inputData.getBoolean(KEY_DISABLE_CHAIN, false)) scheduleNext(applicationContext)

    // Guard: user must be logged in
    val auth = FirebaseAuth.getInstance()
    val currentUid = auth.currentUser?.uid
    if (currentUid == null) {
      Log.w(TAG, "User not logged in, skipping friend study mode check")
      return Result.success()
    }

    // Guard: notifications permission (Android 13+)
    if (!hasNotifPermission(applicationContext)) {
      Log.w(TAG, "Notification permission not granted, skipping friend study mode check")
      return Result.success()
    }

    try {
      val currentFriends = loadCurrentFriends(currentUid)
      if (currentFriends.isEmpty()) {
        Log.d(TAG, "No valid friend profiles found")
        return Result.success()
      }

      val previousModes = loadPreviousModes()
      val friendsEnteringStudy = detectStudyModeTransitions(currentFriends, previousModes)

      if (friendsEnteringStudy.isNotEmpty()) {
        notifyFriendsEnteringStudy(friendsEnteringStudy)
      }

      updateStoredModes(currentFriends)
    } catch (e: Exception) {
      Log.e(TAG, "Error checking friend study modes", e)
    }

    return Result.success()
  }

  /* ---------------------------- helpers: permissions ---------------------------- */

  private fun hasNotifPermission(ctx: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) ==
          PackageManager.PERMISSION_GRANTED
    } else true
  }

  /* ---------------------------- helpers: friend data loading ---------------------------- */

  private suspend fun loadCurrentFriends(currentUid: String): List<FriendStatus> {
    val db = FirebaseFirestore.getInstance()

    // Query friend IDs directly
    val friendIdsSnapshot =
        db.collection("users").document(currentUid).collection("friendIds").get().await()

    val friendIds = friendIdsSnapshot.documents.map { it.id }

    if (friendIds.isEmpty()) {
      Log.d(TAG, "User has no friends, nothing to check")
      return emptyList()
    }

    // Query each friend's profile directly
    val currentFriends = mutableListOf<FriendStatus>()
    for (friendId in friendIds) {
      val friendStatus = loadFriendProfile(db, friendId)
      if (friendStatus != null) {
        currentFriends.add(friendStatus)
      }
    }

    return currentFriends
  }

  private suspend fun loadFriendProfile(db: FirebaseFirestore, friendId: String): FriendStatus? {
    return try {
      val profileDoc = db.collection("profiles").document(friendId).get().await()

      if (profileDoc.exists()) {
        val name = profileDoc.getString("name") ?: "Unknown"
        val modeStr = profileDoc.getString("mode") ?: "IDLE"
        val mode =
            try {
              FriendMode.valueOf(modeStr)
            } catch (_: IllegalArgumentException) {
              FriendMode.IDLE
            }

        // Get location data (use 0.0 as default if not present)
        val geoPoint = profileDoc.getGeoPoint("location")
        val latitude = geoPoint?.latitude ?: 0.0
        val longitude = geoPoint?.longitude ?: 0.0

        FriendStatus(
            id = friendId, name = name, latitude = latitude, longitude = longitude, mode = mode)
      } else {
        null
      }
    } catch (e: Exception) {
      Log.w(TAG, "Failed to load profile for friend $friendId", e)
      null
    }
  }

  /* ---------------------------- helpers: mode transition detection ---------------------------- */

  private fun loadPreviousModes(): Map<String, String> {
    val prefs = applicationContext.getSharedPreferences("friend_study_mode", Context.MODE_PRIVATE)
    val previousModes = mutableMapOf<String, String>()
    prefs.all.forEach { (uid, mode) -> if (mode is String) previousModes[uid] = mode }
    return previousModes
  }

  private fun detectStudyModeTransitions(
      currentFriends: List<FriendStatus>,
      previousModes: Map<String, String>
  ): List<FriendStatus> {
    return currentFriends.filter { friend ->
      val previousMode = previousModes[friend.id]
      previousMode != null &&
          previousMode != FriendMode.STUDY.name &&
          friend.mode == FriendMode.STUDY
    }
  }

  private fun notifyFriendsEnteringStudy(friendsEnteringStudy: List<FriendStatus>) {
    try {
      postFriendStudyModeNotification(applicationContext, friendsEnteringStudy)
    } catch (se: SecurityException) {
      Log.w(TAG, "Notification post aborted due to missing permission at runtime", se)
    }
  }

  private fun updateStoredModes(currentFriends: List<FriendStatus>) {
    val prefs = applicationContext.getSharedPreferences("friend_study_mode", Context.MODE_PRIVATE)
    val editor = prefs.edit()
    editor.clear() // Clear old data
    currentFriends.forEach { friend -> editor.putString(friend.id, friend.mode.name) }
    editor.apply()
  }

  /* ---------------------------- helpers: notification ---------------------------- */

  @RequiresPermission(value = "android.permission.POST_NOTIFICATIONS", conditional = true)
  private fun postFriendStudyModeNotification(ctx: Context, friends: List<FriendStatus>) {
    NotificationUtils.ensureChannel(ctx)

    val message =
        when {
          friends.size == 1 -> ctx.getString(R.string.friend_study_mode_single, friends[0].name)
          friends.size == 2 ->
              ctx.getString(R.string.friend_study_mode_two, friends[0].name, friends[1].name)
          else -> {
            val names = friends.take(2).joinToString(", ") { it.name }
            val count = friends.size - 2
            ctx.getString(R.string.friend_study_mode_multiple, names, count)
          }
        }

    val n =
        NotificationCompat.Builder(ctx, NotificationUtils.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(ctx.getString(R.string.friend_study_mode_title))
            .setContentText(message)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

    NotificationManagerCompat.from(ctx).notify(FRIEND_STUDY_MODE_NOTIFICATION_ID, n)
  }

  companion object {
    private const val TAG = "FriendStudyModeWorker"
    private const val FRIEND_STUDY_MODE_NOTIFICATION_ID = 9102
    private const val UNIQUE_WORK_NAME = "friend_study_mode_poll"
    private const val DELAY_MINUTES = 5L

    internal const val KEY_DISABLE_CHAIN = "disable_chain" // for tests only

    /**
     * Schedule the next friend study mode check after DELAY_MINUTES. This creates a chaining
     * pattern where each run schedules the next.
     */
    fun scheduleNext(ctx: Context) {
      val request =
          OneTimeWorkRequestBuilder<FriendStudyModeWorker>()
              .setInitialDelay(DELAY_MINUTES, TimeUnit.MINUTES)
              .addTag(UNIQUE_WORK_NAME)
              .build()
      WorkManager.getInstance(ctx)
          .enqueueUniqueWork(UNIQUE_WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    /** Start the chaining pattern of friend study mode checks. Call once at app startup. */
    fun startChain(ctx: Context) = scheduleNext(ctx)

    /** Cancel all friend study mode checks. */
    fun cancel(ctx: Context) {
      WorkManager.getInstance(ctx).cancelUniqueWork(UNIQUE_WORK_NAME)
    }
  }
}
