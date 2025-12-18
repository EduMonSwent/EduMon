package com.android.sample.data.notifications

import android.app.Notification
import android.content.Context
import androidx.annotation.RequiresPermission
import androidx.annotation.VisibleForTesting
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.android.sample.R

// app/src/main/java/com/android/sample/data/notifications/KeepStreakWorker.kt
@VisibleForTesting
internal fun buildKeepStreakMessage(days: Int): String {
  val unit = if (days == 1) "day" else "days"
  return "Don’t let your streak of $days $unit disappear"
}

@VisibleForTesting
internal fun buildKeepStreakNotification(ctx: Context, days: Int): Notification =
    NotificationCompat.Builder(ctx, NotificationUtils.CHANNEL_ID)
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentTitle("Keep your streak 🔥")
        .setContentText(buildKeepStreakMessage(days))
        .setAutoCancel(true)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // Show on lock screen
        .build()

@RequiresPermission(value = "android.permission.POST_NOTIFICATIONS", conditional = true)
@VisibleForTesting
internal fun postNotification(ctx: Context, id: Int, n: Notification) {
  NotificationManagerCompat.from(ctx).notify(id, n)
}

class KeepStreakWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
  @RequiresPermission(value = "android.permission.POST_NOTIFICATIONS", conditional = true)
  override suspend fun doWork(): Result {
    NotificationUtils.ensureChannel(applicationContext)
    val days = StreakPrefs.get(applicationContext)
    val n = buildKeepStreakNotification(applicationContext, days)
    postNotification(applicationContext, NotificationUtils.ID_KEEP_STREAK, n)
    return Result.success()
  }
}
