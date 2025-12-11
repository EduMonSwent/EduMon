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
import com.android.sample.ui.location.ProfilesFriendRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

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
      // Load current friends from repository with timeout
      val db = FirebaseFirestore.getInstance()
      val repo = ProfilesFriendRepository(db, auth)

      // Skip the initial empty emission and wait for actual friend data
      val currentFriends =
          withTimeoutOrNull(15_000) { // 15 second timeout (increased for test stability)
            repo.friendsFlow.drop(1).first() // Skip initial empty list emission
          }
              ?: run {
                Log.w(TAG, "Timeout waiting for friends data")
                return Result.success()
              }

      // Load previous friend modes from SharedPreferences
      val prefs = applicationContext.getSharedPreferences("friend_study_mode", Context.MODE_PRIVATE)
      val previousModes = mutableMapOf<String, String>()
      prefs.all.forEach { (uid, mode) -> if (mode is String) previousModes[uid] = mode }

      // Detect transitions to STUDY mode
      val friendsEnteringStudy = mutableListOf<FriendStatus>()
      currentFriends.forEach { friend ->
        val previousMode = previousModes[friend.id]
        if (previousMode != null &&
            previousMode != FriendMode.STUDY.name &&
            friend.mode == FriendMode.STUDY) {
          friendsEnteringStudy.add(friend)
        }
      }

      // Post notification if any friends entered study mode
      if (friendsEnteringStudy.isNotEmpty()) {
        try {
          postFriendStudyModeNotification(applicationContext, friendsEnteringStudy)
        } catch (se: SecurityException) {
          Log.w(TAG, "Notification post aborted due to missing permission at runtime", se)
        }
      }

      // Update stored modes for next check
      val editor = prefs.edit()
      editor.clear() // Clear old data
      currentFriends.forEach { friend -> editor.putString(friend.id, friend.mode.name) }
      editor.apply()
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
